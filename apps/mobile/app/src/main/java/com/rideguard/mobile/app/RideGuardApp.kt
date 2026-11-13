package com.rideguard.mobile.app

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rideguard.mobile.R
import com.rideguard.mobile.features.auth.AuthScreen
import com.rideguard.mobile.features.devices.DeviceScreens
import com.rideguard.mobile.features.account.AccountScreens
import com.rideguard.mobile.features.feedback.FeedbackScreens
import com.rideguard.mobile.shared.networking.*
import com.rideguard.mobile.shared.ui.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun RideGuardApp(vm: RideGuardViewModel) {
    val identity by vm.identity.collectAsStateWithLifecycle()
    RideGuardTheme {
        if (identity.phase == Phase.RESTORING) {
            Surface(Modifier.fillMaxSize()) { Column(Modifier.safeDrawingPadding(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                CircularProgressIndicator(); Text(stringResource(R.string.restoring), Modifier.padding(24.dp))
            } }
        } else {
            val screen = allowedScreen(identity, vm.screen)
            val authenticated = identity.phase == Phase.AUTHENTICATED
            BackHandler(screen != Screen.LOGIN && screen != Screen.HOME) { vm.back() }
            Scaffold(topBar = { TopAppBar(title = { Text(if (isAuth(screen)) stringResource(R.string.app_name) else stringResource(title(screen))) }, navigationIcon = {
                if (screen !in mainTabs && screen != Screen.LOGIN) IconButton(onClick = { vm.back() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back)) }
            }) }, bottomBar = {
                if (authenticated) NavigationBar(Modifier.testTag("main-navigation")) {
                    mainTabs.forEach { tab -> NavigationBarItem(selected = screen == tab, onClick = { vm.go(tab) }, label = { Text(stringResource(title(tab))) }, icon = {
                        Icon(when(tab) { Screen.HOME -> Icons.Default.Home; Screen.DEVICES -> Icons.Default.Devices; Screen.FEEDBACKS -> Icons.AutoMirrored.Filled.Chat; else -> Icons.Default.Person }, null)
                    }) }
                }
            }) { padding ->
                key(identity.epoch, screen, vm.selectedId) {
                    Column(Modifier.fillMaxSize().padding(padding).imePadding().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        if (isAuth(screen)) {
                            Text(stringResource(R.string.tagline), style = MaterialTheme.typography.headlineMedium)
                            Text(stringResource(R.string.scope), style = MaterialTheme.typography.bodyMedium)
                            if (identity.phase == Phase.UNAVAILABLE) Text(stringResource(R.string.restore_network), color = MaterialTheme.colorScheme.error)
                            else if (identity.reason != null) ErrorText(identity.reason!!)
                        }
                        if (vm.busy) {
                            LinearProgressIndicator(Modifier.fillMaxWidth())
                            Text(stringResource(R.string.loading), style = MaterialTheme.typography.bodySmall)
                        }
                        vm.error?.let { ErrorText(it) }
                        vm.notice?.let { Notice(it) }
                        if (isAuth(screen)) AuthScreen(vm, screen)
                        else {
                            if (screen !in listOf(Screen.BIND, Screen.PASSWORD)) OutlinedButton(onClick = { vm.load() }, enabled = !vm.busy) { Text(stringResource(R.string.retry)) }
                            when(screen) {
                                Screen.HOME -> Overview(vm)
                                Screen.DEVICES, Screen.DEVICE, Screen.BIND, Screen.HISTORY -> DeviceScreens(vm, screen)
                                Screen.FEEDBACKS, Screen.FEEDBACK, Screen.NEW_FEEDBACK -> FeedbackScreens(vm, screen)
                                else -> AccountScreens(vm, screen)
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                    }
                }
            }
        }
    }
}

@Composable private fun Overview(vm: RideGuardViewModel) {
    vm.profile?.let { Text(stringResource(R.string.welcome, it.full_name), style = MaterialTheme.typography.headlineMedium) }
    Text(stringResource(R.string.scope))
    if (vm.overviewReady) Panel {
        Text(stringResource(R.string.device_count, vm.devices.items.size), style = MaterialTheme.typography.titleLarge)
        vm.pendingCount?.let { Text(stringResource(R.string.pending_count, it)) }
        Text(stringResource(R.string.feedback_count, vm.feedbacks.items.size))
        Text(stringResource(R.string.overview_note), style = MaterialTheme.typography.bodySmall)
        Action(R.string.devices) { vm.go(Screen.DEVICES) }
    }
}
