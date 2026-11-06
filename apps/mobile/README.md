# RideGuard Android

Kotlin/Compose, Gradle Kotlin DSL and checked-in Gradle 8.11.1 wrapper. AGP 8.9.2,
compile/target API 35, min API 26; JDK 21 recommended. Install Android platform 35 and
build-tools 35.0.0, set ANDROID_HOME or ignored local.properties sdk.dir.

```sh
./gradlew :app:assembleDebug :app:lintDebug
./gradlew :app:installDebug
./gradlew :app:assembleRelease -Prideguard.releaseApiBaseUrl=https://your-real-api-host
```

Windows uses `./gradlew.bat`. installDebug needs an emulator/phone; release is unsigned
until a private signing setup is supplied. Debug API URL defaults to emulator host alias
http://10.0.2.2:8080; override with `-Prideguard.apiBaseUrl=http://192.168.1.10:8080` for
a LAN test device. Release uses intentionally unreachable https://api.invalid unless
configured; HTTP cleartext disabled in release. URL is public configuration, never a secret.
Cloud backup and device transfer exclusions are explicit for Android 12+; earlier Android
uses allowBackup=false. See [Android backup rules](https://developer.android.com/identity/data/autobackup).

App opens foundation and scope screens through Compose Navigation. No fake BLE connection,
trip, auth or location permission request. Feature map: auth/devices/navigation/trips/settings;
shared networking/BLE/storage/models/UI as documented in architecture. BLE will have a separate
adapter from REST. Room and sync queue design is in
[trip-recording-sync](../../docs/flows/trip-recording-sync.md). No Room implementation yet.
