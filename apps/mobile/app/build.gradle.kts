plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

fun apiUrl(property: String, fallback: String): String {
    val value = providers.gradleProperty(property).orElse(fallback).get()
    require(value.matches(Regex("https?://[A-Za-z0-9.\\-:/]+"))) { "Invalid API base URL" }
    return "\"$value\""
}

android {
    namespace = "com.rideguard.mobile"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.rideguard.mobile"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"
    }
    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            buildConfigField("String", "API_BASE_URL", apiUrl("rideguard.apiBaseUrl", "http://10.0.2.2:8080"))
        }
        release {
            buildConfigField("String", "API_BASE_URL", apiUrl("rideguard.releaseApiBaseUrl", "https://api.invalid"))
        }
    }
    buildFeatures { compose = true; buildConfig = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2025.04.01"))
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.navigation:navigation-compose:2.8.9")
}
