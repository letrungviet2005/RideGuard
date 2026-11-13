# Kiểm chứng Android REST MVP — 13/11/2026

Phạm vi: `apps/mobile`, README và tài liệu mobile. Đọc git status trước sửa;
không tìm thấy AGENTS.md trong repo hoặc các thư mục tổ tiên đã kiểm tra.
Không sửa backend, V1/V2, firmware/CAD hoặc hợp đồng implemented; không push/deploy.

## Những gì đã viết

Ứng dụng Compose Material3 có auth/restoration riêng và 4 tab người dùng.
Repository triển khai 22 operations REST: auth, profile/password/sessions,
devices/binding/history/config và feedback. Không có admin, ACK/report hay BLE/GPS.
Mã được tách client/DTO, vault, repository, ViewModel, composable; strings tiếng Việt.
Xem [thiết kế và giới hạn](implementation.md), [hướng dẫn chạy](local-setup.md).

## Công cụ và các lỗi đã xử lý

- AGP8.9.2/Kotlin2.1.20 được giữ; wrapper trước đó trỏ9.7.1 nhưng checksum là8.11.1.
  Đưa URL về8.11.1, giữ checksum đã đối chiếu endpoint SHA256 chính thức. Đây là bản
  Gradle theo [bảng AGP8.9](https://developer.android.com/build/releases/agp-8-9-0-release-notes).
  Build thực tế xác nhận bộ cấu hình này dùng được với JDK21.0.13, SDK/build-tools35.
- Lượt đầu lỗi Kotlin DSL vì `java.net.URI` bị accessor `java` che; sửa import URI.
- Lượt gọi live smoke chưa quote -P bị PowerShell tách `.liveSmoke=true`; chạy lại
  với argument trong dấu nháy đạt. Các lệnh tài liệu đã quote.
- Sửa thời điểm publish login state để lỗi login không bị mất do ViewModel tự hủy job;
  thêm test navigation/data purge. Tổng quan chỉ hiển thị số sau API hoàn tất.
- Sửa resource trùng private resource Compose, tên composable trả string và strings
  chưa dùng. Không disable lint/assertion để đạt.

## Build/lint/test đã chạy

PowerShell từ gốc repo:

```powershell
$env:JAVA_HOME=(Resolve-Path '.local/jdk21/jdk-21.0.13+9').Path
$env:ANDROID_HOME="$env:LOCALAPPDATA/Android/Sdk"
.\apps\mobile\gradlew.bat -p apps/mobile :app:assembleDebug :app:lintDebug :app:testDebugUnitTest :app:assembleDebugAndroidTest '-Prideguard.apiBaseUrl=http://192.168.1.68:8080' --console=plain
```

- `assembleDebug`: đạt, có APK debug ký bằng debug key.
- `lintDebug`: đạt, **0 errors, 7 warnings GradleDependency** (có phiên bản mới hơn);
  không phải chứng nhận mọi vấn đề UX/security đã hết.
- JVM fixture: **17 tests chạy, 17 đạt, 0 failure/error/skipped**:
  - ApiClientTest:10 — restore/no token, JSON mobile/no cookie, concurrent401 một refresh,
    retry key/body,401 lần2, credential rejection,503 refresh không replay, logout204,
    error envelope/403 không refresh, response tài khoản cũ, login lỗi.
  - RepositoryTest:5 — route guard, stable keys, nullable config/feedback/UTC,
    unbind precondition204, cursor encoding/config409 không retry/ACK.
  - ViewModelTest:2 — login/draft/navigation/logout purge và409 giữ draft/read server/chọn lại.
- `assembleDebugAndroidTest`: biên dịch thành công **3 tests** (NavigationTest:2,
  TokenVaultTest:1). **Chưa chạy trên thiết bị**, không cộng vào17 tests đạt.
- `processReleaseMainManifest`: đạt; release không có debug network config.
- Thử cấu hình release HTTP bị từ chối đúng với `Release requires HTTPS`;
  đây là negative check chủ động, không phải lỗi build APK còn tồn tại.

Report JVM/lint mặc định ở `apps/mobile/app/build/test-results/testDebugUnitTest/`
và `apps/mobile/app/build/reports/`, là output gitignored. Log phiên này ở
`.local/mobile-final.log`. Không suy ra phone UI/Keystore từ test JVM.

## Kotlin client với BE thật

PostgreSQL Windows18.6 đã có, không chuyển Docker. Nhiệm vụ khởi động BE bằng
`.\scripts\run-backend-local.ps1`; Flyway validate V1/V2, không migration mới.
Readiness tại loopback và LAN192.168.1.68 từ PC đều UP. Flyway vẫn cảnh báo PG18 mới
hơn phiên bản được nó kiểm chứng (17); không tuyên bố đã kiểm chứng toàn bộ PG18.

```powershell
.\apps\mobile\gradlew.bat -p apps/mobile :app:testDebugUnitTest '-Prideguard.liveSmoke=true' --console=plain
```

**1 smoke test chạy, 1 đạt, 0 failure/error/skipped**, dùng ApiClient/Repository thật
trên JVM và HTTP tới BE8080; không mock HTTP nhưng dùng MemoryStore thay Android Keystore.
Đã kiểm tra đăng ký không tự login → login → profile/rename → sessions → change password
→ feedback create/replay cùng key/detail/list → history rỗng → restore/rotate → logout
→ login bằng password mới → revoke current session và clear identity.

Đã tạo **1 USER test và1 feedback chung** với email
`mobile-smoke-756d89cf-1b70-435d-aba5-330e05428514@example.invalid`, nội dung feedback có
tiền tố `[ANDROID REST SMOKE]`. Password sinh ngẫu nhiên trong memory, không lưu/log;
không phải tài khoản demo để người dùng đăng nhập. Session test đã logout/revoke.
Không xóa user/feedback hoặc dữ liệu thật. Bản evidence live được giữ gitignored ở
`.local/mobile-evidence/live.xml` để lượt fixture sau không ghi đè bằng chứng.

Đã đọc registry local: **0 serial RG-DEMO-***. Không có serial được dành riêng cho test,
nên **bind/config/unbind/history có bản ghi thật chưa chạy**. Không tự tạo serial hoặc
dùng gương của người khác; fixture không thay thế kiểm chứng ownership/transaction thật.
Có tham số liveSerial cho lần kiểm tra sau, xem setup. Không gửi ACK/report.

## APK và điện thoại

Artifact: **`apps/mobile/app/build/outputs/apk/debug/app-debug.apk`**.
ApplicationId `com.rideguard.mobile.debug`, minSdk26, target35. Bản bàn giao dùng
`http://192.168.1.68:8080`; thay mạng/IP phải build lại hoặc dùng HTTPS phù hợp.
Network security config sinh ra chỉ allow HTTP cho192.168.1.68, base deny cleartext.
Đã xác nhận BuildConfig URL và chữ ký APK bằng apksigner.

APK cuối có **22.371.964 bytes**, SHA256
`4A05963C71BDA187FC6BDFCE5F91EE42C4D5475ED322BEA91AB76A4BCBEA26BD`;
apksigner verify đạt bằng APK Signature Scheme v2. Bản sao cùng nội dung ở
`.local/mobile-artifacts/rideguard-android-debug.apk` (gitignored).
AAPT xác nhận quyền INTERNET và permission receiver nội bộ của AndroidX;
không có Bluetooth/location. Contract checker và `git diff --check` đạt;
checker kiểm tra202 local Markdown links,434 examples,32 negative cases.

`adb devices`: không có thiết bị. `emulator -list-avds`: không có AVD.
**Chưa cài/mở app trên phone/emulator, chưa chạy3 instrumentation tests, chưa xem
giao diện thực tế/keyboard/system bars/rotation hoặc Keystore thật.** Lệnh install
và connectedDebugAndroidTest trong setup là hướng dẫn, không phải kết quả đã đạt.
Không thay firewall. Truy cập LAN từ PC không chứng minh truy cập từ điện thoại.

## Phần cần kiểm chứng tiếp

1. Cài APK trên Android8+ cùng Wi-Fi; xác nhận login/reopen/logout, bàn phím,
   màn hình nhỏ, Back và rotation; chạy UI/Keystore instrumentation trên thiết bị test.
2. Chuẩn bị serial demo ACTIVE chưa bind, chạy luồng bind/config409/unbind/history thật.
3. Kiểm tra SMTP/email reset end-to-end; lần này chỉ có implementation UI/request.
4. HTTPS/TLS endpoint thật, mất mạng khi rotate, process death và restore trên Android.

BLE/ESP32/cảm biến/AI/GPS không thuộc lần này. Không gọi các request REST là kiểm chứng gương.

Đã dừng BE do lượt kiểm thử này khởi động sau khi hoàn tất. PostgreSQL có sẵn vẫn
chạy; không dừng dịch vụ khác. Khi cài APK để thử lại, khởi động BE theo local-setup.
