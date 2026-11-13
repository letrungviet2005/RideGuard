# RideGuard Android

Ứng dụng người dùng Kotlin/Jetpack Compose, Material 3 xanh rừng, gọi [OpenAPI implemented](../../contracts/openapi.yaml). Cập nhật 13/11/2026.

Đã triển khai đăng ký/đăng nhập/quên/reset/logout; hồ sơ, mật khẩu, sessions;
danh sách/chi tiết/nickname/bind/unbind/history; config CAS và xử lý 409;
phản hồi có cursor và admin reply; tổng quan từ API thật. Auth/loading tách giao diện
chính; form giữ qua rotation bằng ViewModel. Không có màn hình quản trị ADMIN.

Access token trong memory; refresh JSON mobile mã hóa AES-GCM bằng Android Keystore,
lưu trong noBackupFilesDir. Không cookie admin web hoặc plaintext credential.

**Chưa có BLE/ESP32/cảm biến/AI/GPS.** Bind là liên kết tài khoản trên server, không
phải Bluetooth. Không gửi ACK/report; save config hiện “Đã lưu trên máy chủ — chưa
đồng bộ với gương”. Không giả số liệu, trạng thái online, pin hay sensor OK.

## Build/cài nhanh

PowerShell từ gốc repository, cần JDK và Android SDK35:

```powershell
$env:JAVA_HOME=(Resolve-Path '.local/jdk21/jdk-21.0.13+9').Path
$env:ANDROID_HOME="$env:LOCALAPPDATA/Android/Sdk"
.\apps\mobile\gradlew.bat -p apps/mobile :app:assembleDebug :app:lintDebug :app:testDebugUnitTest '-Prideguard.apiBaseUrl=http://192.168.1.68:8080'
& "$env:ANDROID_HOME/platform-tools/adb.exe" devices
& "$env:ANDROID_HOME/platform-tools/adb.exe" install -r apps/mobile/app/build/outputs/apk/debug/app-debug.apk
```

Thay IP theo máy BE. APK: `apps/mobile/app/build/outputs/apk/debug/app-debug.apk`;
package `com.rideguard.mobile.debug`, min Android8/API26, target35.
Không truyền API property thì mặc định emulator `http://10.0.2.2:8080`.
Đặt tham số -P có dấu chấm trong dấu nháy trên PowerShell.

Gradle8.11.1, AGP8.9.2, Kotlin/Compose plugin2.1.20, JVM target17; build bằng JDK21.
Đây là bộ đã build đạt. Hiện URL wrapper lại trỏ9.7.1 trong khi checksum của8.11.1; phải đồng bộ trước build tiếp. Debug chỉ cho HTTP tới đúng host cấu hình;
release bắt buộc HTTPS, mặc định api.invalid, cần endpoint/signing riêng trước phát hành.

## Tài liệu và bằng chứng

- [BE, API URL, Wi-Fi/firewall và cài APK](../../docs/mobile/local-setup.md).
- [Kiến trúc và quản lý phiên](../../docs/mobile/implementation.md).
- [Kết quả kiểm chứng và giới hạn](../../docs/mobile/verification-2026-11-13.md).

17 test JVM fixture và 1 smoke client Kotlin với BE thật đạt. Sau lượt đó đã cài và mở APK trên emulator thành công; chưa chạy UI/Keystore instrumentation hoặc toàn luồng UI–BE. Không suy ra kiểm chứng điện thoại thật từ emulator/JVM smoke. Xem [tiến độ hiện tại](../../docs/tien-do-du-an.md).
