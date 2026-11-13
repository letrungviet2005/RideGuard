package com.rideguard.mobile.shared.networking

import com.rideguard.mobile.shared.storage.TokenStore
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

enum class Phase { RESTORING, ANONYMOUS, AUTHENTICATED, UNAVAILABLE }
data class Identity(val phase: Phase = Phase.RESTORING, val user: User? = null, val epoch: Long = 0, val reason: String? = null)

class ApiClient(baseUrl: String, private val store: TokenStore, private val http: OkHttpClient = OkHttpClient.Builder()
    .connectTimeout(10, TimeUnit.SECONDS).readTimeout(20, TimeUnit.SECONDS).callTimeout(30, TimeUnit.SECONDS)
    .retryOnConnectionFailure(false).followRedirects(false).followSslRedirects(false).build()) {
    val json = Json { ignoreUnknownKeys = true; explicitNulls = true }
    private val base = baseUrl.trimEnd('/') + "/api/v1"
    private val lock = Mutex()
    private val mutable = MutableStateFlow(Identity())
    val identity = mutable.asStateFlow()
    @Volatile private var access: String? = null
    @Volatile private var epoch = 0L

    private fun clear(phase: Phase = Phase.ANONYMOUS, reason: String? = null) {
        access = null; epoch++; store.clear()
        mutable.value = Identity(phase, epoch = epoch, reason = reason)
    }
    private fun accept(body: String) {
        val grant = json.decodeFromString<Grant>(body)
        // Persist rotated credential before publishing the authenticated state.
        store.write(grant.refresh_token)
        access = grant.access_token
        mutable.value = Identity(Phase.AUTHENTICATED, grant.user, epoch)
    }
    private fun wire(method: String, path: String, body: JsonObject? = null, token: String? = null, key: String? = null): String {
        val request = Request.Builder().url(base + path).header("Accept", "application/json")
        if (path in listOf("/auth/login", "/auth/refresh", "/auth/logout")) request.header("X-Client-Type", "mobile")
        token?.let { request.header("Authorization", "Bearer $it") }
        key?.let { request.header("Idempotency-Key", it) }
        val content = body?.toString()?.toRequestBody("application/json; charset=utf-8".toMediaType())
        request.method(method, content)
        http.newCall(request.build()).execute().use { response ->
            val text = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                val error = runCatching { json.decodeFromString<ErrorEnvelope>(text) }.getOrNull()
                throw ApiFailure(response.code, error?.code ?: "HTTP_ERROR", error?.request_id)
            }
            return if (response.code == 204) "" else text
        }
    }
    suspend fun publicCall(path: String, body: JsonObject): String = withContext(Dispatchers.IO) { wire("POST", path, body) }
    suspend fun login(email: String, password: String) = withContext(Dispatchers.IO) { lock.withLock {
        check(mutable.value.phase != Phase.AUTHENTICATED)
        access = null; epoch++; store.clear()
        try { accept(wire("POST", "/auth/login", obj("email" to email, "password" to password, "client_name" to "RideGuard Android"))) }
        catch (e: Exception) { access = null; store.clear(); throw e }
    } }
    suspend fun restore() = withContext(Dispatchers.IO) { lock.withLock {
        if (mutable.value.phase != Phase.RESTORING) return@withLock
        if (store.read() == null) clear() else rotate()
    } }
    private fun rotate() {
        val token = store.read() ?: run { clear(reason = "SESSION_EXPIRED"); throw ApiFailure(401, "SESSION_EXPIRED") }
        // Consume locally before sending: after timeout/crash the old token must never be replayed.
        store.clear()
        try { accept(wire("POST", "/auth/refresh", obj("refresh_token" to token))) }
        catch (e: Exception) {
            val denied = e is ApiFailure && e.status in listOf(401, 403)
            clear(if (denied) Phase.ANONYMOUS else Phase.UNAVAILABLE, if (denied) "SESSION_EXPIRED" else "NETWORK")
            throw e
        }
    }
    suspend fun call(method: String, path: String, body: JsonObject? = null, key: String? = null): String = withContext(Dispatchers.IO) {
        val generation = epoch
        val original = access ?: throw ApiFailure(401, "SESSION_EXPIRED")
        try {
            val result = wire(method, path, body, original, key)
            if (epoch != generation) throw ApiFailure(401, "SESSION_CHANGED")
            result
        } catch (e: ApiFailure) {
            if (e.status != 401) throw e
            lock.withLock {
                if (epoch != generation) throw ApiFailure(401, "SESSION_CHANGED")
                if (access == original) rotate()
            }
            // Only an explicit 401 is retried, once, with the identical body/idempotency key.
            try {
                val result = wire(method, path, body, access, key)
                if (epoch != generation) throw ApiFailure(401, "SESSION_CHANGED")
                result
            } catch (second: ApiFailure) {
                if (second.status == 401) lock.withLock { if (epoch == generation) clear(reason = "SESSION_EXPIRED") }
                throw second
            }
        }
    }
    suspend fun logout() = withContext(Dispatchers.IO) { lock.withLock {
        val token = store.read()
        clear()
        if (token != null) wire("POST", "/auth/logout", obj("refresh_token" to token))
    } }
    suspend fun forget() = withContext(Dispatchers.IO) { lock.withLock { clear() } }
}

fun obj(vararg values: Pair<String, Any?>): JsonObject = buildJsonObject {
    values.forEach { (key, value) -> put(key, when (value) {
        null -> JsonNull
        is JsonElement -> value
        is Boolean -> JsonPrimitive(value)
        is Number -> JsonPrimitive(value)
        else -> JsonPrimitive(value.toString())
    }) }
}
