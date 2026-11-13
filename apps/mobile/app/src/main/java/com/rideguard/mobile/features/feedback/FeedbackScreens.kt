package com.rideguard.mobile.features.feedback

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.rideguard.mobile.R
import com.rideguard.mobile.app.*
import com.rideguard.mobile.shared.ui.*

@Composable fun FeedbackScreens(vm: RideGuardViewModel, screen: Screen) {
    when (screen) {
        Screen.FEEDBACKS -> {
            Action(R.string.new_feedback) { vm.go(Screen.NEW_FEEDBACK) }
            if (!vm.busy && vm.error == null && vm.feedbacks.items.isEmpty()) Empty()
            vm.feedbacks.items.forEach { feedback -> Panel {
                Text(statusLabel(feedback.status), color = MaterialTheme.colorScheme.primary)
                Text(feedback.content, maxLines = 4)
                Text(stringResource(R.string.created, feedback.created_at), style = MaterialTheme.typography.bodySmall)
                Action(R.string.feedback) { vm.go(Screen.FEEDBACK, feedback.id) }
            } }
            More(vm.feedbacks.next_cursor, vm.busy) { vm.load(true) }
        }
        Screen.FEEDBACK -> vm.selectedFeedback?.let { feedback -> Panel {
            Text(statusLabel(feedback.status), style = MaterialTheme.typography.titleLarge)
            Text(feedback.content)
            Text(stringResource(R.string.created, feedback.created_at))
            HorizontalDivider()
            Text(stringResource(R.string.admin_reply), style = MaterialTheme.typography.titleMedium)
            Text(feedback.admin_response ?: stringResource(R.string.no_reply))
            feedback.responded_at?.let { Text(stringResource(R.string.replied, it)) }
        } }
        Screen.NEW_FEEDBACK -> Panel {
            Text(stringResource(R.string.category), style = MaterialTheme.typography.titleMedium)
            listOf("BUG" to R.string.bug, "SUGGESTION" to R.string.suggestion, "OTHER" to R.string.other).forEach { (value, label) ->
                FilterChip(selected = vm.value("category").ifEmpty { "SUGGESTION" } == value, onClick = { vm.set("category", value) }, label = { Text(stringResource(label)) }, enabled = !vm.busy)
            }
            Text(stringResource(R.string.feedback_device))
            FilterChip(selected = vm.value("feedbackDevice").isEmpty(), onClick = { vm.set("feedbackDevice", "") }, label = { Text(stringResource(R.string.general)) }, enabled = !vm.busy)
            vm.devices.items.forEach { device -> FilterChip(selected = vm.value("feedbackDevice") == device.id, onClick = { vm.set("feedbackDevice", device.id) }, label = { Text(device.nickname ?: device.serial_number) }, enabled = !vm.busy) }
            More(vm.devices.next_cursor, vm.busy) { vm.moreFeedbackDevices() }
            Field(vm, "content", R.string.content, multiline = true)
            Action(R.string.send, vm.busy, vm.value("content").trim().isNotEmpty()) { vm.createFeedback() }
        }
        else -> Unit
    }
}
