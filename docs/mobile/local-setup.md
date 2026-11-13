# Chạy RideGuard Android trên điện thoại

PowerShell từ **gốc repository**. Dùng PostgreSQL Windows local, không cần Docker.

## 1. Chạy BE

PostgreSQL ở `127.0.0.1:5433`, database/user rideguard, secret trong
`apps/backend/.env`. Không đưa secret DB vào Android. Xem [PostgreSQL local](../backend/postgresql-local.md).

```powershell
.\scripts\run-backend-local.ps1
Invoke-RestMethod http://127.0.0.1:8080/actuator/health/readiness
```

Giữ terminal BE mở; kiểm tra readiness từ terminal khác. Nếu port8080 đã dùng,
kiểm tra process trước, không dừng dịch vụ khác. Tạo USER qua màn hình đăng ký Android;
password PostgreSQL không phải password ứng dụng. Cần thiết bị mẫu thì xem
[seed demo](../backend/demo-data.md), không tự chạy seed mỗi lần BE khởi động.

## 2. API URL và Wi-Fi

- Emulator: `http://10.0.2.2:8080` (alias máy host).
- Điện thoại cùng Wi-Fi: IP LAN máy BE, hiện ghi nhận `http://192.168.1.68:8080`.
  localhost/127.0.0.1 trên điện thoại là điện thoại, không phải PC.
- Endpoint triển khai: HTTPS hợp lệ; không bỏ xác minh TLS.

```powershell
Get-NetIPConfiguration | Where-Object IPv4DefaultGateway
```

Mở `http://IP-PC:8080/actuator/health/readiness` trên browser điện thoại trước khi login.
PC/phone cùng mạng, không guest/client isolation. BE cần listen LAN, không ép
server.address loopback. Lượt này `[::]:8080` listen và IP LAN từ PC trả UP;
chưa kiểm chứng truy cập từ phone.

Nếu firewall chặn, PowerShell quản trị có thể thêm rule Private/LocalSubnet:

```powershell
New-NetFirewallRule -DisplayName 'RideGuard local BE 8080' -Direction Inbound -Action Allow -Protocol TCP -LocalPort 8080 -Profile Private -RemoteAddress LocalSubnet
```

Đây là hướng dẫn, chưa thực thi thay đổi firewall. Không tắt firewall, không mở port
PostgreSQL cho phone. Không mở rule Public cho mạng không tin cậy.

## 3. Build APK

Cần SDK platform35/build-tools35.0.0/platform-tools; dùng ANDROID_HOME hoặc sdk.dir
trong `apps/mobile/local.properties` gitignored. Có thể thay JAVA_HOME bằng JDK đã cài.

```powershell
$env:JAVA_HOME=(Resolve-Path '.local/jdk21/jdk-21.0.13+9').Path
$env:ANDROID_HOME="$env:LOCALAPPDATA/Android/Sdk"
# Điện thoại thật, thay IP khi đổi mạng/DHCP:
.\apps\mobile\gradlew.bat -p apps/mobile :app:assembleDebug '-Prideguard.apiBaseUrl=http://192.168.1.68:8080'
# Hoặc emulator (ghi đè APK bằng cấu hình emulator):
.\apps\mobile\gradlew.bat -p apps/mobile :app:assembleDebug '-Prideguard.apiBaseUrl=http://10.0.2.2:8080'
```

Chọn một cấu hình. APK: **`apps/mobile/app/build/outputs/apk/debug/app-debug.apk`**.
Bản bàn giao 13/11 dùng `http://192.168.1.68:8080`; host hiện trên trang login.
Không truyền property thì debug mặc định dùng 10.0.2.2. Các tham số -P có dấu chấm
phải đặt trong dấu nháy trên PowerShell để tránh bị tách argument.

Debug sinh network security config chỉ cho HTTP tới host API đã chọn, base cleartext=false.
Release không include config debug, chỉ cho HTTPS, mặc định api.invalid để không dùng
nhầm máy dev. Release cần endpoint thật và signing riêng:

```powershell
.\apps\mobile\gradlew.bat -p apps/mobile :app:assembleRelease '-Prideguard.releaseApiBaseUrl=https://api.example.com'
```

Đây là placeholder hướng dẫn, không phải endpoint đã triển khai. Không thêm secrets vào
BuildConfig. Mobile không cần cookie web/Origin/CSRF, không cần sửa CORS BE.

## 4. Cài trên điện thoại

Qua file: chép APK, mở trên Android8/API26 trở lên, cho phép cài từ nguồn đang dùng
khi hệ điều hành yêu cầu. Đây là debug APK, không phải production release.

Qua USB: bật Developer options/USB debugging, xác nhận RSA trên điện thoại:

```powershell
& "$env:ANDROID_HOME/platform-tools/adb.exe" devices
& "$env:ANDROID_HOME/platform-tools/adb.exe" install -r apps/mobile/app/build/outputs/apk/debug/app-debug.apk
& "$env:ANDROID_HOME/platform-tools/adb.exe" shell am start -n com.rideguard.mobile.debug/com.rideguard.mobile.MainActivity
```

Nếu unauthorized thì xác nhận RSA. Không có device không đồng nghĩa cài đã đạt.
Không tự uninstall/xóa dữ liệu để chữa signature mismatch; xác định bản cũ và tác động
mất phiên trước khi người dùng quyết định gỡ.

## 5. Kiểm thử lại

```powershell
.\apps\mobile\gradlew.bat -p apps/mobile :app:testDebugUnitTest :app:lintDebug '-Prideguard.apiBaseUrl=http://192.168.1.68:8080'
.\apps\mobile\gradlew.bat -p apps/mobile :app:assembleDebugAndroidTest '-Prideguard.apiBaseUrl=http://192.168.1.68:8080'
# Cần thiết bị; fixture/vault test thay hoặc xóa phiên local của ứng dụng debug test:
.\apps\mobile\gradlew.bat -p apps/mobile :app:connectedDebugAndroidTest '-Prideguard.apiBaseUrl=http://192.168.1.68:8080'
```

JVM smoke với BE8080 thật tạo USER/feedback tiền tố test, password sinh tạm trong memory,
không log/lưu secret. Không tự xóa các bản ghi DB sau test:

```powershell
.\apps\mobile\gradlew.bat -p apps/mobile :app:testDebugUnitTest '-Prideguard.liveSmoke=true'
# Chỉ khi serial demo ACTIVE, chưa có owner, được dành riêng cho test:
.\apps\mobile\gradlew.bat -p apps/mobile :app:testDebugUnitTest '-Prideguard.liveSmoke=true' '-Prideguard.liveSerial=RG-DEMO-003'
```

Không truyền serial đang dùng. Test không tạo registry, chỉ unbind binding nó vừa tạo.
Thiếu serial thì ghi rõ device flow chưa chạy. SMTP/reset, phone UI, Keystore và
LAN/firewall phải nghiệm thu riêng. Xem [kết quả thực tế](verification-2026-11-13.md).
