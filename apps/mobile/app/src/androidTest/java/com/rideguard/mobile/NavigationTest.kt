package com.rideguard.mobile

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.lifecycle.viewModelScope
import com.rideguard.mobile.app.*
import com.rideguard.mobile.data.RideGuardRepository
import com.rideguard.mobile.shared.networking.*
import com.rideguard.mobile.shared.storage.TokenStore
import kotlinx.coroutines.cancel
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/** Android UI fixtures. These are not real backend/Keystore acceptance tests. */
class NavigationTest {
    @get:Rule val rule = createComposeRule()
    private lateinit var vm: RideGuardViewModel
    private val user = """{"id":"00000000-0000-4000-8000-000000000001","email":"fixture@example.invalid","full_name":"Fixture","role":"USER","status":"ACTIVE"}"""
    private fun setup(restoring: CountDownLatch? = null) {
        val store = object : TokenStore {
            var value: String? = if (restoring != null) "fixture-refresh" else null
            override fun read() = value
            override fun write(token: String) { value = token }
            override fun clear() { value = null }
        }
        val http = OkHttpClient.Builder().addInterceptor { chain ->
            val path = chain.request().url.encodedPath
            if (path.endsWith("/refresh")) check(restoring?.await(10, TimeUnit.SECONDS) != false)
            val text = when {
                path.endsWith("/login") || path.endsWith("/refresh") -> """{"access_token":"fixture","refresh_token":"fixture-rotated","user":$user}"""
                path.endsWith("/users/me") -> user
                path.endsWith("/logout") -> ""
                else -> """{"items":[],"next_cursor":null}"""
            }
            Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1).code(if (path.endsWith("/logout")) 204 else 200)
                .message("fixture").body(text.toResponseBody("application/json".toMediaType())).build()
        }.build()
        rule.runOnUiThread { vm = RideGuardViewModel(RideGuardRepository(ApiClient("https://fixture.invalid", store, http))) }
        rule.setContent { RideGuardApp(vm) }
    }
    @After fun clean() { if (::vm.isInitialized) rule.runOnUiThread { vm.viewModelScope.cancel() } }
    @Test fun anonymousLoginAndLogoutSwitchNavigation() {
        setup(); rule.waitUntil(5000) { vm.identity.value.phase == Phase.ANONYMOUS }
        rule.onNodeWithTag("main-navigation").assertDoesNotExist()
        rule.onNodeWithText("Email").performTextInput("fixture@example.invalid")
        rule.onNodeWithText("Mật khẩu", substring = false).performTextInput("fixture-password-only")
        rule.onNode(hasText("Đăng nhập") and hasClickAction()).performScrollTo().performClick()
        rule.waitUntil(5000) { vm.profile != null && !vm.busy }
        rule.onNodeWithTag("main-navigation").assertIsDisplayed()
        rule.onNode(hasText("Tài khoản") and hasClickAction()).performClick()
        rule.waitUntil(5000) { !vm.busy }
        rule.onNode(hasText("Đăng xuất") and hasClickAction()).performScrollTo().performClick()
        rule.waitUntil(5000) { vm.identity.value.phase == Phase.ANONYMOUS }
        rule.onNodeWithTag("main-navigation").assertDoesNotExist()
        rule.onNodeWithText("fixture@example.invalid").assertDoesNotExist()
    }
    @Test fun pendingRestoreNeverShowsPrivateNavigation() {
        val latch = CountDownLatch(1)
        try {
            setup(latch)
            rule.onNodeWithText("Đang khôi phục phiên…").assertIsDisplayed()
            rule.onNodeWithTag("main-navigation").assertDoesNotExist()
        } finally { latch.countDown() }
        rule.waitUntil(5000) { vm.profile != null && !vm.busy }
        rule.onNodeWithTag("main-navigation").assertIsDisplayed()
    }
}
