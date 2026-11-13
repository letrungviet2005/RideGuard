package com.rideguard.mobile.shared.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.rideguard.mobile.R
import com.rideguard.mobile.app.*

private val colors = lightColorScheme(primary = Color(0xFF24634F), onPrimary = Color.White,
    primaryContainer = Color(0xFFDCEBCB), secondary = Color(0xFF506651),
    background = Color(0xFFF3F6F0), surface = Color(0xFFFAFCF7), onSurface = Color(0xFF18352E))
@Composable fun RideGuardTheme(content: @Composable () -> Unit) = MaterialTheme(colorScheme = colors, content = content)

fun title(screen: Screen): Int = when(screen) {
    Screen.LOGIN -> R.string.login; Screen.REGISTER -> R.string.register; Screen.FORGOT -> R.string.forgot; Screen.RESET -> R.string.reset
    Screen.HOME -> R.string.home; Screen.DEVICES -> R.string.devices; Screen.DEVICE -> R.string.device; Screen.BIND -> R.string.bind
    Screen.HISTORY -> R.string.history; Screen.FEEDBACKS -> R.string.feedbacks; Screen.FEEDBACK -> R.string.feedback
    Screen.NEW_FEEDBACK -> R.string.new_feedback; Screen.ACCOUNT -> R.string.account; Screen.SESSIONS -> R.string.sessions; Screen.PASSWORD -> R.string.change_password
}
@Composable fun Field(vm: RideGuardViewModel, key: String, label: Int, secret: Boolean = false, number: Boolean = false, multiline: Boolean = false) {
    OutlinedTextField(value = vm.value(key), onValueChange = { vm.set(key, it) }, label = { Text(stringResource(label)) },
        modifier = Modifier.fillMaxWidth(), enabled = !vm.busy, singleLine = !multiline, minLines = if (multiline) 4 else 1,
        visualTransformation = if (secret) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = when { secret -> KeyboardType.Password; number -> KeyboardType.Number; key == "email" -> KeyboardType.Email; else -> KeyboardType.Text }))
}
@Composable fun Action(label: Int, busy: Boolean = false, enabled: Boolean = true, onClick: () -> Unit) {
    Button(onClick = onClick, enabled = enabled && !busy, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
        Text(stringResource(if (busy) R.string.sending else label))
    }
}
@Composable fun Panel(content: @Composable ColumnScope.() -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
    }
}
@Composable fun Empty() { Text(stringResource(R.string.empty), style = MaterialTheme.typography.bodyLarge) }
@Composable fun More(cursor: String?, busy: Boolean, click: () -> Unit) { if (cursor != null) OutlinedButton(onClick = click, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.more)) } }
@Composable fun statusLabel(value: String): String = stringResource(when(value) {
    "ACTIVE" -> R.string.active; "DISABLED" -> R.string.disabled; "RETIRED" -> R.string.retired
    "OPEN" -> R.string.open; "IN_PROGRESS" -> R.string.feedback_in_progress; "RESOLVED" -> R.string.resolved
    "UNBOUND" -> R.string.binding_closed; else -> R.string.not_available
})
@Composable fun ErrorText(code: String) {
    val resource = when(code) {
        "NETWORK" -> R.string.network_error; "SESSION_EXPIRED", "SESSION_CHANGED" -> R.string.session_expired
        "INVALID_CREDENTIALS", "UNAUTHORIZED", "ACCOUNT_LOCKED" -> R.string.invalid_login
        "EMAIL_UNAVAILABLE" -> R.string.email_unavailable; "INVALID_REQUEST" -> R.string.invalid_request
        "FORBIDDEN", "NOT_FOUND" -> R.string.permission_error; "CONFIG_CONFLICT", "VERSION_CONFLICT" -> R.string.conflict
        "BRIGHTNESS" -> R.string.brightness_error; "CURRENT_PASSWORD_INVALID" -> R.string.password_wrong
        "DEVICE_UNAVAILABLE", "DEVICE_NOT_ACTIVE", "BINDING_NO_LONGER_ACTIVE", "IDEMPOTENCY_CONFLICT" -> R.string.operation_conflict
        "DEVICE_NOT_AVAILABLE" -> R.string.permission_error
        else -> R.string.generic_error
    }
    Text(stringResource(resource, code), color = MaterialTheme.colorScheme.error)
}
@Composable fun Notice(code: String) {
    val resource = when(code) {
        "REGISTERED" -> R.string.registered; "EMAIL_SENT" -> R.string.email_sent; "RESET_DONE" -> R.string.reset_done
        "PASSWORD_CHANGED" -> R.string.password_changed; "PENDING_SYNC" -> R.string.pending_sync
        "BOUND" -> R.string.bound_notice; "UNBOUND" -> R.string.unbound_notice; "SENT" -> R.string.sent_notice
        else -> R.string.saved
    }
    Text(stringResource(resource), color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodyLarge)
}
