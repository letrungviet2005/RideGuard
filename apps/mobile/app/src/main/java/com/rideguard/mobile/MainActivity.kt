package com.rideguard.mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.rideguard.mobile.app.RideGuardApp
import com.rideguard.mobile.app.RideGuardViewModel
import com.rideguard.mobile.data.RideGuardRepository
import com.rideguard.mobile.shared.networking.ApiClient
import com.rideguard.mobile.shared.networking.ApiEnvironment
import com.rideguard.mobile.shared.storage.TokenVault

class MainActivity : ComponentActivity() {
    private val model: RideGuardViewModel by viewModels { object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = RideGuardViewModel(
            RideGuardRepository(ApiClient(ApiEnvironment.baseUrl, TokenVault(applicationContext)))
        ) as T
    } }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { RideGuardApp(model) }
    }
}
