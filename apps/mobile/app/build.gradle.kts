import java.net.URI

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
}

fun apiUrl(property: String, fallback: String): String {
    val value = providers.gradleProperty(property).orElse(fallback).get()
    require(value.matches(Regex("https?://[A-Za-z0-9.\\-:/]+"))) { "Invalid API base URL" }
    if (property.contains("release")) require(value.startsWith("https://")) { "Release requires HTTPS" }
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
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
    sourceSets.getByName("debug").res.srcDir(layout.buildDirectory.dir("generated/debugNetwork/res"))
}

val debugUrl = providers.gradleProperty("rideguard.apiBaseUrl").orElse("http://10.0.2.2:8080")
val generateDebugNetwork by tasks.registering {
    inputs.property("apiUrl", debugUrl)
    val output = layout.buildDirectory.dir("generated/debugNetwork/res")
    outputs.dir(output)
    doLast {
        val uri = URI(debugUrl.get())
        require(uri.host != null && uri.userInfo == null && uri.query == null && uri.fragment == null)
        val file = output.get().file("xml/network_security_config.xml").asFile
        file.parentFile.mkdirs()
        file.writeText("""<network-security-config><base-config cleartextTrafficPermitted="false"/><domain-config cleartextTrafficPermitted="${uri.scheme == "http"}"><domain includeSubdomains="false">${uri.host}</domain></domain-config></network-security-config>""")
    }
}
tasks.matching { it.name == "preDebugBuild" }.configureEach { dependsOn(generateDebugNetwork) }

tasks.withType<Test>().configureEach {
    if (providers.gradleProperty("rideguard.liveSmoke").orNull == "true") {
        include("**/LiveBackendSmoke.class")
        systemProperty("rideguard.liveSerial", providers.gradleProperty("rideguard.liveSerial").orElse("").get())
    } else exclude("**/LiveBackendSmoke.class")
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2025.04.01"))
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.navigation:navigation-compose:2.8.9")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.1")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("androidx.compose.material:material-icons-extended")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.1")
    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
    androidTestImplementation(platform("androidx.compose:compose-bom:2025.04.01"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
