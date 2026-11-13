package com.rideguard.mobile

import com.rideguard.mobile.data.RideGuardRepository
import com.rideguard.mobile.shared.networking.*
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

/** Explicit -Prideguard.liveSmoke=true only. Uses real Kotlin client, no Android device claim. */
class LiveBackendSmoke {
    @Test fun localBackendFlow() = runBlocking {
        val store = MemoryStore()
        val client = ApiClient("http://127.0.0.1:8080", store)
        val repo = RideGuardRepository(client)
        val email = "mobile-smoke-${UUID.randomUUID()}@example.invalid"
        val password = UUID.randomUUID().toString() + "aA1!"
        val nextPassword = UUID.randomUUID().toString() + "bB2!"
        repo.register(email, "Android REST smoke", password)
        assertNull(client.identity.value.user)
        client.login(email, password)
        try {
            val me = repo.profile(); assertEquals(email, me.email)
            assertEquals("Android REST smoke updated", repo.updateName("Android REST smoke updated").full_name)
            assertTrue(repo.sessions().items.any { it.is_current })
            repo.changePassword(password, nextPassword)
            val serial = System.getProperty("rideguard.liveSerial").orEmpty()
            var bound: Device? = null
            try {
                if (serial.isNotBlank()) {
                    check(serial.startsWith("RG-DEMO-")) { "Only explicit demo serials are accepted" }
                    val key = UUID.randomUUID().toString()
                    bound = repo.bind(serial, "Android REST smoke", key)
                    assertEquals(bound.binding_id, repo.bind(serial, "Android REST smoke", key).binding_id)
                    val config = repo.config(bound.id)
                    val edited = repo.saveConfig(bound.id, config.desired_version, 71, true)
                    assertEquals(config.reported_applied_version, edited.reported_applied_version)
                }
                val key = UUID.randomUUID().toString()
                val feedback = repo.createFeedback(bound?.id, "OTHER", "[ANDROID REST SMOKE] Client JVM, không phải dữ liệu phần cứng.", key)
                assertEquals(feedback.id, repo.createFeedback(bound?.id, "OTHER", "[ANDROID REST SMOKE] Client JVM, không phải dữ liệu phần cứng.", key).id)
                assertEquals(feedback.id, repo.feedback(feedback.id).id)
                assertTrue(repo.feedbacks().items.any { it.id == feedback.id })
            } finally { bound?.let { repo.unbind(it) } }
            if (serial.isNotBlank()) assertTrue(repo.history().items.any { it.serial_number == serial && it.status == "UNBOUND" })
            else assertTrue(repo.history().items.isEmpty())
            val rotated = ApiClient("http://127.0.0.1:8080", store)
            rotated.restore(); assertEquals(email, RideGuardRepository(rotated).profile().email)
            rotated.logout(); assertNull(store.token)
            client.forget()
            client.login(email, nextPassword)
            repo.revoke(repo.sessions().items.single { it.is_current })
            assertEquals(Phase.ANONYMOUS, client.identity.value.phase)
            println("LIVE PASS: register/login/profile/password/sessions/feedback/idempotency/restore/logout/revoke. Test email: $email")
            println(if (serial.isBlank()) "DEVICE FLOW NOT RUN: no authorized demo serial supplied; no ACK/reports sent." else "DEVICE FLOW PASS: bind/config/unbind/history; no ACK/reports sent.")
        } finally {
            if (client.identity.value.phase == Phase.AUTHENTICATED) client.logout()
        }
    }
}
