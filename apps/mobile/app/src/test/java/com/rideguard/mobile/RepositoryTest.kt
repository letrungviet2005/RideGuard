package com.rideguard.mobile

import com.rideguard.mobile.data.*
import com.rideguard.mobile.shared.networking.*
import com.rideguard.mobile.app.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.*
import org.junit.Test

class RepositoryTest {
    private val id = "00000000-0000-4000-8000-000000000001"
    @Test fun navigationGuardsEveryPrivateScreenDuringRestoreAndAnonymous() {
        Screen.entries.filterNot { isAuth(it) }.forEach { screen ->
            assertEquals(Screen.LOGIN, allowedScreen(Identity(), screen))
            assertEquals(Screen.LOGIN, allowedScreen(Identity(Phase.ANONYMOUS), screen))
        }
        assertEquals(Screen.HOME, allowedScreen(Identity(Phase.AUTHENTICATED), Screen.LOGIN))
        assertEquals(Screen.DEVICES, allowedScreen(Identity(Phase.AUTHENTICATED), Screen.DEVICES))
    }
    @Test fun stableKeysSurviveRetryButSeparateCompletedActions() {
        val keys = DraftKeys(); val first = keys.forPayload("bind", "a")
        assertEquals(first, keys.forPayload("bind", "a")); assertNotEquals(first, keys.forPayload("feedback", "a"))
        keys.done("bind"); assertNotEquals(first, keys.forPayload("bind", "a"))
    }
    @Test fun nullableConfigAndUtcFeedbackDecodeWithoutFakeAppliedVersion() {
        val json = Json { ignoreUnknownKeys = true }
        val config = json.decodeFromString<Config>("""{"binding_id":"$id","schema_version":1,"desired_version":2,"reported_applied_version":null,"settings":{"display_brightness":70,"vibration_enabled":true}}""")
        assertNull(config.reported_applied_version); assertTrue(config.pending)
        val page = json.decodeFromString<Page<Feedback>>("""{"items":[{"id":"$id","device_id":null,"category":"OTHER","content":"Fixture","status":"OPEN","admin_response":null,"created_at":"2026-11-13T00:00:00Z","responded_at":null}],"next_cursor":null}""")
        assertNull(page.next_cursor); assertNull(page.items.single().admin_response)
        assertEquals(java.time.Instant.parse("2026-11-13T00:00:00Z"), java.time.Instant.parse(page.items.single().created_at))
    }
    @Test fun unbindUsesBindingPreconditionAndAccepts204() = runBlocking {
        MockWebServer().use { server ->
            val client = ApiClient(server.url("/").toString(), MemoryStore()); val repo = RideGuardRepository(client)
            server.enqueue(response(grant())); client.login("fixture", "synthetic"); server.takeRequest()
            server.enqueue(response(status = 204))
            repo.unbind(Device(id, id, "RG-FIXTURE", null, "Fixture", null, "ACTIVE", null))
            val request = server.takeRequest(); assertEquals("DELETE", request.method)
            assertEquals("/api/v1/devices/$id/binding?expected_binding_id=$id", request.path)
        }
    }
    @Test fun cursorIsEncodedAndConfigConflictDoesNotAutoWriteOrAck() = runBlocking {
        MockWebServer().use { server ->
            val client = ApiClient(server.url("/").toString(), MemoryStore()); val repo = RideGuardRepository(client)
            server.enqueue(response(grant())); client.login("fixture", "synthetic"); server.takeRequest()
            server.enqueue(response("""{"items":[],"next_cursor":null}""")); repo.feedbacks("a+/=")
            assertEquals("/api/v1/feedbacks?limit=20&cursor=a%2B%2F%3D", server.takeRequest().path)
            server.enqueue(response("", 409)); val failure = runCatching { repo.saveConfig(id, 7, 42, false) }.exceptionOrNull() as ApiFailure
            assertEquals(409, failure.status)
            val request = server.takeRequest(); assertEquals("PATCH", request.method); assertEquals("/api/v1/devices/$id/config", request.path)
            assertTrue(request.body.readUtf8().contains("\"expected_version\":7")); assertEquals(3, server.requestCount)
        }
    }
}
