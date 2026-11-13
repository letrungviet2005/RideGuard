package com.rideguard.mobile.features.devices

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.rideguard.mobile.R
import com.rideguard.mobile.app.*
import com.rideguard.mobile.shared.networking.Report
import com.rideguard.mobile.shared.ui.*
import java.time.Duration
import java.time.Instant

@Composable fun DeviceScreens(vm: RideGuardViewModel, screen: Screen) {
    when(screen) {
        Screen.DEVICES -> {
            Action(R.string.bind) { vm.go(Screen.BIND) }
            TextButton(onClick = { vm.go(Screen.HISTORY) }) { Text(stringResource(R.string.history)) }
            if (!vm.busy && vm.error == null && vm.devices.items.isEmpty()) Empty()
            vm.devices.items.forEach { device -> Panel {
                Text(device.nickname ?: device.serial_number, style = MaterialTheme.typography.titleLarge)
                Text(device.serial_number); Text(statusLabel(device.status))
                Text(device.model, style = MaterialTheme.typography.bodySmall)
                Action(R.string.device) { vm.go(Screen.DEVICE, device.id) }
            } }
            More(vm.devices.next_cursor, vm.busy) { vm.load(true) }
        }
        Screen.BIND -> Panel {
            Text(stringResource(R.string.bind_help))
            Field(vm, "serial", R.string.serial); Field(vm, "bindName", R.string.nickname)
            Action(R.string.bind, vm.busy, vm.value("serial").isNotBlank()) { vm.bind() }
        }
        Screen.HISTORY -> {
            if (!vm.busy && vm.error == null && vm.history.items.isEmpty()) Empty()
            vm.history.items.forEach { binding -> Panel {
                Text(binding.nickname ?: binding.serial_number, style = MaterialTheme.typography.titleLarge)
                Text(binding.serial_number)
                Text(stringResource(if (binding.status == "ACTIVE") R.string.binding_active else R.string.binding_closed))
                Text(stringResource(R.string.bound, binding.bound_at))
                binding.unbound_at?.let { Text(stringResource(R.string.unbound, it)) }
            } }
            More(vm.history.next_cursor, vm.busy) { vm.load(true) }
        }
        Screen.DEVICE -> DeviceDetail(vm)
        else -> Unit
    }
}

@Composable private fun DeviceDetail(vm: RideGuardViewModel) {
    var confirm by rememberSaveable { mutableStateOf(false) }
    vm.selectedDevice?.let { device ->
        Panel {
            Text(device.serial_number, style = MaterialTheme.typography.headlineSmall)
            Text(device.model); Text(statusLabel(device.status))
            Field(vm, "nickname:${device.id}", R.string.nickname)
            Action(R.string.save, vm.busy) { vm.rename() }
        }
        vm.config?.let { config -> Panel {
            Text(stringResource(R.string.config), style = MaterialTheme.typography.titleLarge)
            Text(stringResource(R.string.desired, config.desired_version))
            Text(stringResource(R.string.applied, config.reported_applied_version?.toString() ?: stringResource(R.string.unknown)))
            if (config.pending) Text(stringResource(R.string.pending_sync), color = MaterialTheme.colorScheme.primary)
            Field(vm, "brightness:${device.id}", R.string.brightness, number = true)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(stringResource(R.string.vibration), Modifier.weight(1f).padding(top = 12.dp))
                Switch(checked = vm.value("vibration:${device.id}").toBoolean(), onCheckedChange = { vm.set("vibration:${device.id}", it.toString()) }, enabled = !vm.busy)
            }
            Text(stringResource(R.string.config_note), style = MaterialTheme.typography.bodySmall)
            vm.conflictingConfig?.let { server ->
                Text(stringResource(R.string.conflict))
                Text(stringResource(R.string.server_config, server.desired_version, server.settings.display_brightness, stringResource(if (server.settings.vibration_enabled) R.string.yes else R.string.no)))
                OutlinedButton(onClick = { vm.adoptServerConfig() }) { Text(stringResource(R.string.adopt_server)) }
            }
            Action(R.string.save, vm.busy, vm.conflictingConfig == null) { vm.saveConfig() }
        } }
        Panel { ReportCard(device.latest_report) }
        OutlinedButton(onClick = { confirm = true }, enabled = !vm.busy, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.unbind)) }
    }
    if (confirm) AlertDialog(onDismissRequest = { confirm = false }, title = { Text(stringResource(R.string.confirm_unbind)) }, text = { Text(stringResource(R.string.unbind_body)) },
        confirmButton = { TextButton(onClick = { confirm = false; vm.unbind() }, enabled = !vm.busy) { Text(stringResource(R.string.confirm)) } },
        dismissButton = { TextButton(onClick = { confirm = false }) { Text(stringResource(R.string.cancel)) } })
}

@Composable private fun ReportCard(report: Report?) {
    if (report == null) Text(stringResource(R.string.report_none))
    else {
        val age = runCatching { Duration.between(Instant.parse(report.observed_at), Instant.now()).toMinutes() }.getOrNull()
        if (age == null || age < 0) Text(stringResource(R.string.report_unknown_time))
        else Text(stringResource(R.string.report_age, report.observed_at, age))
        Text(stringResource(R.string.report_values, report.radar_status, report.imu_status, report.battery_percent?.let { "$it%" } ?: stringResource(R.string.not_available)))
    }
}
