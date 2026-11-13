package com.rideguard.mobile.app

import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rideguard.mobile.data.*
import com.rideguard.mobile.shared.networking.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

enum class Screen { LOGIN, REGISTER, FORGOT, RESET, HOME, DEVICES, DEVICE, BIND, HISTORY, FEEDBACKS, FEEDBACK, NEW_FEEDBACK, ACCOUNT, SESSIONS, PASSWORD }
val mainTabs = listOf(Screen.HOME, Screen.DEVICES, Screen.FEEDBACKS, Screen.ACCOUNT)
fun isAuth(screen: Screen) = screen in listOf(Screen.LOGIN, Screen.REGISTER, Screen.FORGOT, Screen.RESET)
fun allowedScreen(identity: Identity, requested: Screen): Screen =
    if (identity.phase != Phase.AUTHENTICATED) { if (isAuth(requested)) requested else Screen.LOGIN }
    else if (isAuth(requested)) Screen.HOME else requested

class RideGuardViewModel(val repo: RideGuardRepository) : ViewModel() {
    val identity = repo.client.identity
    var screen by mutableStateOf(Screen.LOGIN); private set
    var busy by mutableStateOf(false); private set
    var error by mutableStateOf<String?>(null); private set
    var notice by mutableStateOf<String?>(null); private set
    var profile by mutableStateOf<User?>(null); private set
    var devices by mutableStateOf(Page<Device>(emptyList(), null)); private set
    var feedbacks by mutableStateOf(Page<Feedback>(emptyList(), null)); private set
    var history by mutableStateOf(Page<Binding>(emptyList(), null)); private set
    var sessions by mutableStateOf(Page<Session>(emptyList(), null)); private set
    var selectedDevice by mutableStateOf<Device?>(null); private set
    var config by mutableStateOf<Config?>(null); private set
    var conflictingConfig by mutableStateOf<Config?>(null); private set
    var selectedFeedback by mutableStateOf<Feedback?>(null); private set
    var pendingCount by mutableStateOf<Int?>(null); private set
    var overviewReady by mutableStateOf(false); private set
    var selectedId: String? = null; private set
    // Memory only. Survives rotation via ViewModel, never written to Bundle/disk.
    val fields = mutableStateMapOf<String, String>()
    private val keys = DraftKeys()
    private var work: Job? = null
    private var generation = 0
    init {
        viewModelScope.launch { runCatching { repo.client.restore() } }
        viewModelScope.launch {
            var seen = -1L
            identity.collectLatest { state ->
                if (seen != state.epoch) {
                    seen = state.epoch; wipe()
                    screen = if (state.phase == Phase.AUTHENTICATED) Screen.HOME else Screen.LOGIN
                }
                if (state.phase == Phase.AUTHENTICATED && isAuth(screen)) {
                    fields.clear(); screen = Screen.HOME; load()
                } else if (state.phase == Phase.AUTHENTICATED && profile == null && !busy) load()
            }
        }
    }
    private fun wipe() {
        generation++; work?.cancel(); busy = false; error = null; notice = null
        fields.clear(); keys.clear(); profile = null; selectedDevice = null; config = null
        conflictingConfig = null; selectedFeedback = null; selectedId = null; pendingCount = null; overviewReady = false
        devices = Page(emptyList(), null); feedbacks = Page(emptyList(), null)
        history = Page(emptyList(), null); sessions = Page(emptyList(), null)
    }
    private fun run(action: suspend () -> Unit) {
        if (busy) return
        busy = true; error = null; notice = null
        val own = generation
        work = viewModelScope.launch {
            try { action() }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) {
                if (own == generation) error = when (e) {
                    is ApiFailure -> e.code
                    is java.io.IOException -> "NETWORK"
                    else -> "UNEXPECTED"
                }
            } finally { if (own == generation) busy = false }
        }
    }
    fun value(key: String) = fields[key].orEmpty()
    fun set(key: String, value: String) { fields[key] = value }
    fun go(next: Screen, id: String? = null) {
        generation++; work?.cancel(); busy = false; error = null; notice = null
        screen = allowedScreen(identity.value, next); selectedId = id
        selectedDevice = null; config = null; conflictingConfig = null; selectedFeedback = null
        load()
    }
    fun back() { go(when(screen) {
        Screen.DEVICE, Screen.BIND, Screen.HISTORY -> Screen.DEVICES
        Screen.FEEDBACK, Screen.NEW_FEEDBACK -> Screen.FEEDBACKS
        Screen.SESSIONS, Screen.PASSWORD -> Screen.ACCOUNT
        else -> if (isAuth(screen)) Screen.LOGIN else Screen.HOME
    }) }
    fun load(more: Boolean = false) = run {
        when (screen) {
            Screen.HOME -> {
                overviewReady = false
                val loadedProfile = repo.profile(); val loadedDevices = repo.devices(); val loadedFeedback = repo.feedbacks()
                var pending = 0
                loadedDevices.items.forEach { if (repo.config(it.id).pending) pending++ }
                profile = loadedProfile; devices = loadedDevices; feedbacks = loadedFeedback
                pendingCount = pending; overviewReady = true
            }
            Screen.ACCOUNT -> { profile = repo.profile(); fields.putIfAbsent("name", profile!!.full_name) }
            Screen.DEVICES -> { val page = repo.devices(if (more) devices.next_cursor else null); devices = if (more) page.copy(items = (devices.items + page.items).distinctBy { it.id }) else page }
            Screen.FEEDBACKS -> { val page = repo.feedbacks(if (more) feedbacks.next_cursor else null); feedbacks = if (more) page.copy(items = (feedbacks.items + page.items).distinctBy { it.id }) else page }
            Screen.HISTORY -> { val page = repo.history(if (more) history.next_cursor else null); history = if (more) page.copy(items = (history.items + page.items).distinctBy { it.id }) else page }
            Screen.SESSIONS -> { val page = repo.sessions(if (more) sessions.next_cursor else null); sessions = if (more) page.copy(items = (sessions.items + page.items).distinctBy { it.id }) else page }
            Screen.DEVICE -> {
                selectedDevice = repo.device(requireNotNull(selectedId)); config = repo.config(requireNotNull(selectedId))
                fields.putIfAbsent("nickname:$selectedId", selectedDevice!!.nickname.orEmpty())
                fields.putIfAbsent("brightness:$selectedId", config!!.settings.display_brightness.toString())
                fields.putIfAbsent("vibration:$selectedId", config!!.settings.vibration_enabled.toString())
            }
            Screen.FEEDBACK -> selectedFeedback = repo.feedback(requireNotNull(selectedId))
            Screen.NEW_FEEDBACK -> devices = repo.devices()
            else -> Unit
        }
    }
    fun authenticate() = run {
        when (screen) {
            Screen.LOGIN -> repo.client.login(value("email").trim(), value("password"))
            Screen.REGISTER -> { repo.register(value("email").trim(), value("registerName").trim(), value("password")); fields.remove("password"); screen = Screen.LOGIN; notice = "REGISTERED" }
            Screen.FORGOT -> { repo.forgot(value("email").trim()); notice = "EMAIL_SENT" }
            Screen.RESET -> { repo.reset(value("resetToken"), value("password")); fields.remove("password"); fields.remove("resetToken"); screen = Screen.LOGIN; notice = "RESET_DONE" }
            else -> Unit
        }
    }
    fun logout() = run { repo.client.logout() }
    fun saveProfile() = run { profile = repo.updateName(value("name").trim()); notice = "SAVED" }
    fun changePassword() = run { repo.changePassword(value("currentPassword"), value("newPassword")); fields.remove("currentPassword"); fields.remove("newPassword"); notice = "PASSWORD_CHANGED" }
    fun revoke(session: Session) = run { repo.revoke(session); if (!session.is_current) sessions = repo.sessions() }
    fun bind() = run {
        val serial = value("serial").trim().uppercase(java.util.Locale.ROOT); val nickname = value("bindName").trim()
        repo.bind(serial, nickname, keys.forPayload("bind", obj("serial" to serial, "nickname" to nickname).toString()))
        keys.done("bind"); fields.remove("serial"); fields.remove("bindName"); devices = repo.devices(); screen = Screen.DEVICES; notice = "BOUND"
    }
    fun rename() = run { selectedDevice = repo.rename(requireNotNull(selectedId), value("nickname:$selectedId")); notice = "SAVED" }
    fun unbind() = run {
        repo.unbind(requireNotNull(selectedDevice)); selectedDevice = null; config = null
        fields.keys.filter { it.endsWith(":$selectedId") }.forEach { fields.remove(it) }
        devices = repo.devices(); screen = Screen.DEVICES; notice = "UNBOUND"
    }
    fun saveConfig() = run {
        val current = requireNotNull(config)
        val brightness = value("brightness:$selectedId").toIntOrNull()
        if (brightness == null || brightness !in 0..100) throw ApiFailure(400, "BRIGHTNESS")
        try {
            config = repo.saveConfig(requireNotNull(selectedId), current.desired_version, brightness, value("vibration:$selectedId").toBoolean())
            notice = "PENDING_SYNC"
        } catch (e: ApiFailure) {
            if (e.status == 409) {
                // Keep the user's draft. No second PATCH; explicit choice required.
                conflictingConfig = repo.config(requireNotNull(selectedId)); error = "CONFIG_CONFLICT"
            } else throw e
        }
    }
    fun adoptServerConfig() {
        config = conflictingConfig ?: return
        conflictingConfig = null; error = null
        fields["brightness:$selectedId"] = config!!.settings.display_brightness.toString()
        fields["vibration:$selectedId"] = config!!.settings.vibration_enabled.toString()
    }
    fun createFeedback() = run {
        val device = value("feedbackDevice").ifEmpty { null }; val category = value("category").ifEmpty { "SUGGESTION" }; val content = value("content").trim()
        repo.createFeedback(device, category, content, keys.forPayload("feedback", obj("device" to device, "category" to category, "content" to content).toString()))
        keys.done("feedback"); fields.remove("content"); fields.remove("feedbackDevice"); feedbacks = repo.feedbacks(); screen = Screen.FEEDBACKS; notice = "SENT"
    }
    fun moreFeedbackDevices() = run {
        val page = repo.devices(devices.next_cursor)
        devices = page.copy(items = (devices.items + page.items).distinctBy { it.id })
    }
}
