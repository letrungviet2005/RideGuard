package com.rideguard.mobile

import com.rideguard.mobile.shared.networking.*
import com.rideguard.mobile.shared.storage.TokenStore
import kotlinx.coroutines.*
import okhttp3.mockwebserver.*
import org.junit.Assert.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class MemoryStore(var token: String? = null) : TokenStore {
    override fun read() = token
    override fun write(token: String) { this.token = token }
    override fun clear() { token = null }
}
fun grant(access: String = "access-a", refresh: String = "refresh-a") = """{"access_token":"$access","refresh_token":"$refresh","user":{"id":"00000000-0000-4000-8000-000000000001","email":"fixture@example.invalid","full_name":"Fixture","role":"USER","status":"ACTIVE"}}"""
fun response(body: String = "", status: Int = 200) = MockResponse().setResponseCode(status).setHeader("Content-Type", "application/json").setBody(body)

class ApiClientTest {
    private lateinit var server: MockWebServer
    private lateinit var client: ApiClient
    private lateinit var store: MemoryStore
    @Before fun setup() { server = MockWebServer(); server.start(); store = MemoryStore(); client = ApiClient(server.url("/").toString(), store) }
    @After fun close() { server.shutdown() }
    private suspend fun login() { server.enqueue(response(grant())); client.login("fixture@example.invalid", "synthetic-only-password"); server.takeRequest() }

    @Test fun missingTokenRestoreIsAnonymousWithoutHttp() = runBlocking {
        client.restore(); assertEquals(Phase.ANONYMOUS, client.identity.value.phase); assertEquals(0, server.requestCount)
    }
    @Test fun restorationRotatesUsingMobileJsonAndNeverCookie() = runBlocking {
        store.token = "old-refresh"; server.enqueue(response(grant()))
        client.restore(); val request = server.takeRequest()
        assertEquals("mobile", request.getHeader("X-Client-Type")); assertNull(request.getHeader("Cookie"))
        assertEquals("/api/v1/auth/refresh", request.path); assertTrue(request.body.readUtf8().contains("old-refresh"))
        assertEquals("refresh-a", store.token); assertEquals(Phase.AUTHENTICATED, client.identity.value.phase)
    }
    @Test fun concurrent401sUseOneRefreshAndIdenticalRetryKey() = runBlocking {
        login()
        val old = CountDownLatch(2); val rotations = AtomicInteger()
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                if (request.path == "/api/v1/auth/refresh") { rotations.incrementAndGet(); return response(grant("access-b", "refresh-b")) }
                if (request.getHeader("Authorization") == "Bearer access-a") { old.countDown(); check(old.await(5, TimeUnit.SECONDS)); return response("", 401) }
                return response("{}")
            }
        }
        coroutineScope { listOf(async { client.call("GET", "/users/me") }, async { client.call("POST", "/devices/bind", obj("serial_number" to "RG-FIXTURE"), "stable-key") }).awaitAll() }
        assertEquals(1, rotations.get()); assertEquals("refresh-b", store.token)
        val requests = (1..5).map { server.takeRequest(2, TimeUnit.SECONDS)!! }
        val binds = requests.filter { it.path == "/api/v1/devices/bind" }
        assertEquals(2, binds.size); assertEquals(binds[0].body.readUtf8(), binds[1].body.readUtf8())
        assertTrue(binds.all { it.getHeader("Idempotency-Key") == "stable-key" })
    }
    @Test fun second401EndsSessionWithoutLoop() = runBlocking {
        login(); server.enqueue(response("", 401)); server.enqueue(response(grant("access-b", "refresh-b"))); server.enqueue(response("", 401))
        assertTrue(runCatching { client.call("GET", "/users/me") }.isFailure)
        assertEquals(Phase.ANONYMOUS, client.identity.value.phase); assertNull(store.token); assertEquals(4, server.requestCount)
    }
    @Test fun rejectedRefreshClearsPrivateIdentity() = runBlocking {
        login(); server.enqueue(response("", 401)); server.enqueue(response("", 401))
        runCatching { client.call("GET", "/users/me") }
        assertNull(client.identity.value.user); assertNull(store.token); assertEquals("SESSION_EXPIRED", client.identity.value.reason)
    }
    @Test fun uncertainRefreshNeverReplaysOldToken() = runBlocking {
        store.token = "old"; server.enqueue(response("", 503)); runCatching { client.restore() }
        assertEquals(Phase.UNAVAILABLE, client.identity.value.phase); assertNull(store.token)
        client.restore(); assertEquals(1, server.requestCount)
    }
    @Test fun noContentLogoutClearsMemoryAndDisk() = runBlocking {
        login(); server.enqueue(response(status = 204)); client.logout()
        assertNull(client.identity.value.user); assertNull(store.token)
        val request = server.takeRequest(); assertEquals("mobile", request.getHeader("X-Client-Type")); assertTrue(request.body.readUtf8().contains("refresh-a"))
        assertTrue(runCatching { client.call("GET", "/users/me") }.isFailure)
    }
    @Test fun forbiddenDoesNotTriggerRefreshAndEnvelopeIsParsed() = runBlocking {
        login(); server.enqueue(response("""{"status":403,"code":"FORBIDDEN","message":"Denied","request_id":"trace-id"}""", 403))
        val error = runCatching { client.call("GET", "/devices") }.exceptionOrNull() as ApiFailure
        assertEquals("FORBIDDEN", error.code); assertEquals("trace-id", error.requestId)
        assertEquals(2, server.requestCount); assertEquals(Phase.AUTHENTICATED, client.identity.value.phase)
    }
    @Test fun delayedOldAccountResponseIsDiscarded() = runBlocking {
        login(); val started = CountDownLatch(1); val release = CountDownLatch(1)
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse { started.countDown(); check(release.await(5, TimeUnit.SECONDS)); return response("old-private-data") }
        }
        val request = async { runCatching { client.call("GET", "/users/me") } }
        withContext(Dispatchers.IO) { check(started.await(5, TimeUnit.SECONDS)) }
        client.forget(); release.countDown()
        assertTrue(request.await().isFailure); assertNull(client.identity.value.user)
    }
    @Test fun invalidLoginDoesNotLeaveRestoringOrTokens() = runBlocking {
        client.restore(); server.enqueue(response("", 401)); assertTrue(runCatching { client.login("x", "y") }.isFailure)
        assertEquals(Phase.ANONYMOUS, client.identity.value.phase); assertNull(store.token)
    }
}
