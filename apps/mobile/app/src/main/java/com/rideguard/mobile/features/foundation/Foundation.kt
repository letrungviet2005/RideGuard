package com.rideguard.mobile.features.foundation

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

import androidx.compose.runtime.Composable

@Composable
fun RideGuardFoundation() {
    MaterialTheme {
        val nav = rememberNavController()
        Surface(modifier = Modifier.fillMaxSize()) {
            NavHost(navController = nav, startDestination = "foundation") {
                composable("foundation") {
                    Column(Modifier.padding(24.dp).systemBarsPadding(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text("RideGuard", style = MaterialTheme.typography.headlineLarge)
                        Text("Nền tảng ứng dụng. BLE, điều hướng và ghi hành trình chưa được triển khai.")
                        Button(onClick = { nav.navigate("scope") }) { Text("Phạm vi phát triển") }
                    }
                }
                composable("scope") {
                    Column(Modifier.padding(24.dp).systemBarsPadding(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text("Chức năng dự kiến", style = MaterialTheme.typography.headlineMedium)
                        Text("Tài khoản · Thiết bị · Điều hướng · Hành trình · Cài đặt")
                        Text("Cảnh báo nguy hiểm xử lý tại gương. Ứng dụng hiện không kết nối gương hoặc thu thập vị trí.")
                        Button(onClick = { nav.popBackStack() }) { Text("Quay lại") }
                    }
                }
            }
        }
    }
}
