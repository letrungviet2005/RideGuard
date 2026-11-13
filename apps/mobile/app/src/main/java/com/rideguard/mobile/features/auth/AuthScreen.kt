package com.rideguard.mobile.features.auth

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.rideguard.mobile.R
import com.rideguard.mobile.app.*
import com.rideguard.mobile.shared.networking.ApiEnvironment
import com.rideguard.mobile.shared.ui.*

@Composable fun AuthScreen(vm: RideGuardViewModel, screen: Screen) {
    Panel {
        Text(stringResource(title(screen)), style = MaterialTheme.typography.titleLarge)
        if (screen != Screen.RESET) Field(vm, "email", R.string.email)
        if (screen == Screen.REGISTER) Field(vm, "registerName", R.string.full_name)
        if (screen == Screen.RESET) Field(vm, "resetToken", R.string.reset_token, secret = true)
        if (screen != Screen.FORGOT) {
            Field(vm, "password", R.string.password, secret = true)
            Text(stringResource(R.string.password_hint), style = MaterialTheme.typography.bodySmall)
        }
        Action(title(screen), vm.busy) { vm.authenticate() }
        if (screen == Screen.LOGIN) {
            TextButton(onClick = { vm.go(Screen.REGISTER) }) { Text(stringResource(R.string.register)) }
            TextButton(onClick = { vm.go(Screen.FORGOT) }) { Text(stringResource(R.string.forgot)) }
        }
        if (screen == Screen.FORGOT) TextButton(onClick = { vm.go(Screen.RESET) }) { Text(stringResource(R.string.reset)) }
    }
    Text(stringResource(R.string.api_host, ApiEnvironment.baseUrl), style = MaterialTheme.typography.bodySmall)
}
