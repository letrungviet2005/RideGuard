package com.rideguard.mobile

import androidx.lifecycle.viewModelScope
import com.rideguard.mobile.app.*
import com.rideguard.mobile.data.RideGuardRepository
import com.rideguard.mobile.shared.networking.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import okhttp3.mockwebserver.*
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ViewModelTest {
    private val id = "00000000-0000-4000-8000-000000000001"
    private val user = """{"id":"$id","email":"fixture@example.invalid","full_name":"Fixture","role":"USER","status":"ACTIVE"}"""
    private val device = """{"id":"$id","binding_id":"$id","serial_number":"RG-FIXTURE","nickname":null,"model":"Fixture","hardware_revision":null,"status":"ACTIVE","latest_report":null}"""
    private fun config(version: Int) = """{"binding_id":"$id","schema_version":1,"desired_version":$version,"reported_applied_version":null,"settings":{"display_brightness":70,"vibration_enabled":true}}"""
    private suspend fun TestScope.until(predicate: () -> Boolean) {
        repeat(200) { runCurrent(); if (predicate()) return; withContext(Dispatchers.IO) { Thread.sleep(10) } }
        error("State transition did not complete")
    }
    @Test fun loginNavigationDraftRetentionAndLogoutClearPrivateState() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        MockWebServer().use { server ->
            server.dispatcher = object : Dispatcher() {
                override fun dispatch(request: RecordedRequest) = when(request.path) {
                    "/api/v1/auth/login" -> response(grant())
                    "/api/v1/auth/logout" -> response(status = 204)
                    "/api/v1/users/me" -> response(user)
                    else -> response("""{"items":[],"next_cursor":null}""")
                }
            }
            val vm = RideGuardViewModel(RideGuardRepository(ApiClient(server.url("/").toString(), MemoryStore())))
            try {
                until { vm.identity.value.phase == Phase.ANONYMOUS }
                vm.go(Screen.DEVICES); assertEquals(Screen.LOGIN, vm.screen)
                until { !vm.busy }; vm.set("email", "fixture@example.invalid"); vm.set("password", "synthetic-password")
                vm.authenticate(); until { vm.profile != null && !vm.busy }
                assertEquals(Screen.HOME, vm.screen); assertTrue(vm.value("password").isEmpty())
                vm.go(Screen.BIND); until { !vm.busy }; vm.set("serial", "RG-FIXTURE")
                vm.go(Screen.DEVICES); until { !vm.busy }; vm.go(Screen.BIND); until { !vm.busy }
                assertEquals("RG-FIXTURE", vm.value("serial"))
                vm.logout(); until { vm.identity.value.phase == Phase.ANONYMOUS && !vm.busy }
                assertEquals(Screen.LOGIN, vm.screen); assertNull(vm.profile); assertTrue(vm.fields.isEmpty()); assertTrue(vm.devices.items.isEmpty())
            } finally { vm.viewModelScope.cancel(); Dispatchers.resetMain() }
        }
    }
    @Test fun config409ReadsLatestButNeverPatchesUntilUserDecides() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        MockWebServer().use { server ->
            var configReads = 0; var patches = 0
            server.dispatcher = object : Dispatcher() {
                override fun dispatch(request: RecordedRequest): MockResponse = when {
                    request.path == "/api/v1/auth/refresh" -> response(grant())
                    request.path == "/api/v1/users/me" -> response(user)
                    request.path == "/api/v1/devices/$id" -> response(device)
                    request.path == "/api/v1/devices/$id/config" && request.method == "PATCH" -> { patches++; response("", 409) }
                    request.path == "/api/v1/devices/$id/config" -> { configReads++; response(config(if (configReads == 1) 1 else 2)) }
                    else -> response("""{"items":[],"next_cursor":null}""")
                }
            }
            val vm = RideGuardViewModel(RideGuardRepository(ApiClient(server.url("/").toString(), MemoryStore("stored"))))
            try {
                until { vm.profile != null && !vm.busy }; vm.go(Screen.DEVICE, id); until { vm.config != null && !vm.busy }
                vm.set("brightness:$id", "90"); vm.saveConfig(); until { vm.conflictingConfig != null && !vm.busy }
                assertEquals(1, patches); assertEquals("90", vm.value("brightness:$id")); assertEquals(1L, vm.config!!.desired_version)
                vm.adoptServerConfig(); assertEquals(2L, vm.config!!.desired_version); assertEquals("70", vm.value("brightness:$id")); assertEquals(1, patches)
            } finally { vm.viewModelScope.cancel(); Dispatchers.resetMain() }
        }
    }
}
