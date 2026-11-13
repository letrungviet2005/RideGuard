package com.rideguard.mobile.features.account

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.res.stringResource
import com.rideguard.mobile.R
import com.rideguard.mobile.app.*
import com.rideguard.mobile.shared.networking.Session
import com.rideguard.mobile.shared.ui.*

@Composable fun AccountScreens(vm: RideGuardViewModel, screen: Screen) {
    var revoking by remember { mutableStateOf<Session?>(null) }
    when (screen) {
        Screen.ACCOUNT -> {
            vm.profile?.let { profile -> Panel {
                Text(profile.email, style = MaterialTheme.typography.titleMedium)
                Field(vm, "name", R.string.full_name)
                Action(R.string.save, vm.busy) { vm.saveProfile() }
            } }
            Action(R.string.change_password) { vm.go(Screen.PASSWORD) }
            Action(R.string.sessions) { vm.go(Screen.SESSIONS) }
            OutlinedButton(onClick = { vm.logout() }, enabled = !vm.busy) { Text(stringResource(R.string.logout)) }
        }
        Screen.PASSWORD -> Panel {
            Field(vm, "currentPassword", R.string.current_password, secret = true)
            Field(vm, "newPassword", R.string.new_password, secret = true)
            Text(stringResource(R.string.password_hint))
            Action(R.string.change_password, vm.busy) { vm.changePassword() }
        }
        Screen.SESSIONS -> {
            if (!vm.busy && vm.error == null && vm.sessions.items.isEmpty()) Empty()
            vm.sessions.items.forEach { session -> Panel {
                Text(session.client_name ?: session.client_type, style = MaterialTheme.typography.titleMedium)
                if (session.is_current) Text(stringResource(R.string.current_session), color = MaterialTheme.colorScheme.primary)
                Text(stringResource(R.string.created, session.created_at))
                Text(stringResource(R.string.last_seen, session.last_seen_at))
                Text(stringResource(R.string.expires, session.expires_at))
                OutlinedButton(onClick = { revoking = session }, enabled = !vm.busy) { Text(stringResource(R.string.revoke)) }
            } }
            More(vm.sessions.next_cursor, vm.busy) { vm.load(true) }
        }
        else -> Unit
    }
    revoking?.let { session -> AlertDialog(onDismissRequest = { revoking = null }, title = { Text(stringResource(R.string.revoke_question)) }, text = { Text(stringResource(R.string.revoke_body)) },
        confirmButton = { TextButton(onClick = { revoking = null; vm.revoke(session) }) { Text(stringResource(R.string.confirm)) } },
        dismissButton = { TextButton(onClick = { revoking = null }) { Text(stringResource(R.string.cancel)) } }) }
}
