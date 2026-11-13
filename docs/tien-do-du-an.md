# Tiến độ và bàn giao RideGuard

Cập nhật **13/11/2026** từ source và bằng chứng kiểm chứng đã ghi nhận. Lần cập nhật tài liệu này không chạy lại ứng dụng, test suite hoặc truy vấn database. Đây là điểm đọc đầu tiên để tiếp tục công việc; [mục lục tài liệu](README.md) chỉ dẫn phần chuyên sâu.

## 1. Hiện trạng ba ứng dụng

**Backend MVP đã có nghiệp vụ và kiểm chứng PostgreSQL; web quản trị đã có UI được bảo vệ theo phiên; Android đã có ứng dụng REST người dùng, không còn chỉ là foundation.** Chưa có sản phẩm gương tích hợp cảm biến/BLE hoàn chỉnh.

### Backend — `apps/backend`

- Java 21, Spring Boot 3.5.16, Maven 3.9.11, JDBC, PostgreSQL, Flyway V1/V2 với 10 bảng nghiệp vụ. Có 31 operations nghiệp vụ và 3 health endpoint trong [OpenAPI implemented](../contracts/openapi.yaml).
- Có auth, refresh rotation/replay/revoke, profile/password/reset SMTP, phiên, quyền USER/ADMIN và khóa tài khoản; CLI tạo ADMIN.
- Có registry, binding/history/nickname, unbind với `expected_binding_id`, idempotency, config CAS/issued version/ACK, reports theo thời gian, feedback và admin reply.
- Máy local dùng PostgreSQL Windows **18.6**, `127.0.0.1:5433`, database/user `rideguard`; bảng nghiệp vụ trong `rideguard`, Flyway history trong `public`. Không cần Docker cho cách chạy local này.
- Secret nằm trong file riêng bị gitignore; ADMIN đã tồn tại theo lần kiểm tra trước. Tài khoản DB khác tài khoản web, không có mật khẩu ADMIN mặc định.

Bằng chứng: ngày 09/11, **27 test độc lập + 15 integration đạt, 0 failure/error/skipped** trên PostgreSQL 17.11; 69 HTTP request có assertion, CLI ADMIN, restart và SMTP sink đã thử. Ngày 12/11, PostgreSQL 18 local chạy migration/readiness/restart thành công; chưa chạy lại toàn bộ integration suite trên 18. SMTP sink không chứng minh gửi được vào hộp thư thật.

Nguồn: [backend README](../apps/backend/README.md), [báo cáo 09/11](testing/backend-verification-2026-11-09.md), [PostgreSQL local](backend/postgresql-local.md).

### Web quản trị — `apps/admin-web`

- React/TypeScript/Vite; quản lý users/khóa tài khoản, registry/trạng thái thiết bị, feedback/reply, profile/password/sessions và health.
- `AuthLayout` dành cho đăng nhập/quên/reset mật khẩu. Chỉ render `AdminLayout` khi xác nhận ADMIN; khôi phục phiên hiện loading trung tính, bảo vệ deep link và return URL nội bộ. USER bị chặn khỏi admin.
- Access token trong memory; refresh cookie HttpOnly, single-flight; logout hoặc credential bị từ chối xóa dữ liệu phiên. Lỗi mạng có xử lý riêng. Development Lab có phiên mobile riêng và không có trong production.
- Lab hỗ trợ thử REST USER, ACK/report thủ công; các dữ liệu này không chứng minh gương thật đã hoạt động.

Bằng chứng ngày 13/11: **18 client test + 15 Chrome fixture test đạt**, typecheck/lint/build và checker bao phủ 34 operations đạt. Browser với BE thật mới xác nhận **anonymous refresh 401 → deep link quay về login, không sidebar**. Chưa hoàn tất browser thật ADMIN login/rotation/logout; fixture không thay thế bằng chứng cookie và quyền trên server thật. Health cross-origin còn phụ thuộc CORS của `/actuator/**`.

Nguồn: [layout và phiên](frontend/authentication-layout.md), [kiểm chứng FE](frontend/verification-status.md), [báo cáo 13/11](testing/frontend-demo-2026-11-13.md).

### Android — `apps/mobile`

- Kotlin/Compose Material3, auth và 4 tab người dùng, 15 trạng thái màn hình; client/DTO, repository, ViewModel và vault Android Keystore/AES-GCM.
- Đã nối **22 operations nghiệp vụ**: auth/reset, profile/password/sessions, danh sách/chi tiết/đổi tên/bind/unbind/history thiết bị, config CAS, feedback. Access token trong memory, refresh JSON được lưu mã hóa, refresh single-flight.
- Trong 34 operations tổng: 22 dành cho người dùng đã nối; 2 ACK/report chưa gọi do chưa có BLE thật; 7 admin không thuộc app USER; 3 health chưa tích hợp. Số endpoint không tương đương số màn hình.
- Config hiện chỉ có brightness và vibration theo hợp đồng. Lưu config lên server chưa có nghĩa phần cứng đã áp dụng; không giả ACK, pin, telemetry hoặc trạng thái cảm biến.
- Còn thiếu một số filter API, thông tin report/feedback/hardware trong UI và diagnostics. Dashboard đếm phần dữ liệu đã tải, không phải tổng toàn hệ thống. Chi tiết ở [đối chiếu API mobile](mobile/api-coverage-review-2026-11-13.md).
- Chưa có BLE, điều hướng GPS, driver/cảm biến, AI hoặc cảnh báo ESP32. Bind serial là liên kết tài khoản trên server, không phải kết nối Bluetooth.

Bằng chứng: **17 JVM fixture test + 1 Kotlin HTTP smoke với BE thật đạt**; smoke có auth/profile/password/sessions/feedback/refresh/logout, chưa đủ bind/config/unbind thật. Ba instrumentation test đã compile nhưng chưa chạy. Build/lint đạt với 7 cảnh báo dependency trong lượt ghi nhận. Sau đó APK đã cài và mở thành công trên emulator; chưa có bằng chứng toàn luồng UI–BE.

**Vướng hiện tại:** wrapper trên đĩa đang trỏ **Gradle 9.7.1** nhưng `distributionSha256Sum` vẫn của **8.11.1**, gây lỗi verification tải distribution. Bộ đã build đạt là Gradle 8.11.1 / AGP 8.9.2 / Kotlin 2.1.20, JDK 21, JVM target 17. Lần dọn docs này giữ nguyên thay đổi wrapper của người dùng. Cần thống nhất URL/checksum trước build tiếp, không bỏ kiểm tra checksum.

APK debug ở `apps/mobile/app/build/outputs/apk/debug/app-debug.apk`; BuildConfig được đọc trong lượt rà này dùng `http://10.0.2.2:8080` cho emulator. APK/hash LAN trong báo cáo ban đầu là artifact lịch sử, không suy ra hash của APK mới.

Nguồn: [Android README](../apps/mobile/README.md), [thiết kế](mobile/implementation.md), [kiểm chứng](mobile/verification-2026-11-13.md).

## 2. Dữ liệu local và cách chạy

- [Hướng dẫn chạy ba ứng dụng](huong-dan-chay-du-an.md) là đầu mối lệnh PowerShell. BE 8080, web 5173, PostgreSQL 5433; emulator gọi máy host qua `10.0.2.2`. Không cam kết các process hiện vẫn chạy.
- [Seed demo](backend/demo-data.md) đã có script riêng, chạy lại không ghi đè dữ liệu cũ: 2 USER, 5 serial, 3 feedback, binding/config hợp lệ. **Chưa có bằng chứng seed đầy đủ đã chạy hoặc chạy lại trên DB**; lần trước thiếu hai mật khẩu demo. Không đọc lại secret trong lần cập nhật docs này.
- Một serial riêng **`RG-DEMO-ABEA7C`** đã được tạo thủ công ACTIVE để thử liên kết. Lúc tạo chưa bind; không truy vấn lại nên không cam kết còn trống. Không có ACK/report/config giả được tạo kèm. Đây không phải kết quả chạy full seed.
- Tài khoản USER do HTTP smoke tạo dùng password tạm trong memory, không phải tài khoản demo để đăng nhập lại. Cấp password demo riêng theo hướng dẫn, không đưa vào Git hoặc báo cáo.

## 3. Công việc tiếp theo theo thứ tự

1. Đọc git status và hướng dẫn trong repository trước sửa; giữ thay đổi sẵn có. Giải quyết cặp Gradle URL/checksum, rồi build/cài APK trên emulator đang dùng.
2. Xác nhận luồng Android thật: đăng ký/đăng nhập → bind serial còn trống → sửa config → feedback → unbind → kiểm tra quyền cũ; ghi evidence riêng. Chưa có BLE thì giữ applied_version null.
3. Hoàn tất web ADMIN với BE thật: login/cookie, reload/refresh, logout/credential hết hiệu lực, phân quyền và dữ liệu phiên cũ. Chạy seed sau khi được cấp credential, chạy lại kiểm tra không reset dữ liệu.
4. Bổ sung UI Android còn thiếu theo báo cáo đối chiếu API; không thêm API hoặc giả dữ liệu để làm màn hình phong phú hơn. Kiểm chứng instrumentation/Keystore/lifecycle.
5. Chốt phần cứng và BLE, làm driver radar/IMU, config apply/ACK thực rồi reports; triển khai điều hướng GPS. Kiểm tra mất mạng/BLE, reboot và đổi binding.
6. Đánh giá cảnh báo local/AI bằng dữ liệu thật; kiểm thử PostgreSQL 18 đầy đủ, SMTP nhà cung cấp, HTTPS/cookie, tải/retention, backup/restore trước production.

Chưa có phân công hoặc deadline được xác nhận. Không cộng test từ nhiều lượt thành một lần full-suite mới; có source/CI workflow không có nghĩa runtime/hosted CI đã đạt.

## 4. Ranh giới sản phẩm và tài liệu

Firmware vẫn là khung build/driver chưa hoàn thiện; AI có công cụ chia session, chưa có model được đánh giá; phần cứng chưa có nghiệm thu tích hợp. Cảnh báo cốt lõi dự kiến chạy local ESP32, dữ liệu thiếu phải UNKNOWN/unavailable. Không tuyên bố an toàn từ test REST.

Trips/GPS recording/upload/history/statistics, OTA, payment, sharing và BLE ownership challenge ngoài MVP. GPS navigation và lịch sử binding vẫn thuộc MVP. Xem [roadmap](roadmap.md) và [kiến trúc](architecture/system-overview.md).

`contracts/openapi.yaml` mô tả API implemented; proposed và flow không phải bằng chứng triển khai mọi client. Báo cáo có ngày giữ lịch sử; tài liệu này ghi hiện trạng mới nhất và giới hạn. Mỗi lần tiếp tục cần đối chiếu source, ghi rõ “có code / test đạt / runtime đạt / phần cứng đạt”.
