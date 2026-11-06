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

**Implemented:** foundation/scope screens through Compose Navigation and API environment;
no BLE/auth/location collection. Foundation screen copy still mentions trips; it is stale
scope text, not a feature or current MVP requirement. Source UI is unchanged by this docs task.

**Proposed MVP:** auth/devices/navigation/settings/feedback; shared networking/BLE/storage/
models/UI. BLE adapter is separate from REST; backend never calls REST on the phone. Mobile
calls [MVP API](../../contracts/openapi.proposed.yaml), receives refresh token in JSON and
protects it with Android Keystore-backed storage; single-flight refresh, bounded retries,
stable Idempotency-Key for bind/feedback. Config edits require Internet; after backend save,
BLE apply then REST ACK. No BLE means pending sync, not applied. See
[config flow](../../docs/flows/configuration-sync.md) and [report flow](../../docs/flows/device-status-reporting.md).

GPS remains for user-started navigation and BLE cue/distance/TTL. Future location/foreground
service permissions must match actual navigation lifecycle; denial/revocation means unavailable,
not fabricated coordinates. BLE scan/connect permissions are separate. No GPS recording/upload,
Room trip entities or background trip queue in MVP. [Old trip flow](../../docs/flows/trip-recording-sync.md)
is **deferred**. Internet/auth expiry must not stop local ESP32 warnings.
