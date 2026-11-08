# Hiện trạng backend RideGuard

Cập nhật **2026-11-09**, dựa trên mã nguồn và kiểm thử thực tế.
**31 API MVP đã có integration PostgreSQL và local runtime smoke; SMTP sink đạt, email ngoài chưa kiểm chứng.**
Không tìm thấy AGENTS.md khi bắt đầu; giữ thay đổi Gradle mobile và `.vscode` do người dùng quản lý.

## 1. Công nghệ và dependency

Java21, Spring Boot3.5.16, Maven3.9.11/Wrapper3.3.4. PostgreSQL image17.11-alpine trong Compose;
Docker chỉ là lựa chọn chạy DB. JDBC/Flyway, không JPA/Hibernate/H2. SQL/JDBC chỉ rõ `rideguard.*`.

Dependency trực tiếp trong [pom.xml](../../apps/backend/pom.xml): web (MVC/JSON/HTTP), actuator (health),
security (authorization/CSRF), validation, jdbc (datasource/pool), flyway-core và flyway-database-postgresql,
postgresql driver runtime, starter-test và security-test ở scope test. Bổ sung oauth2-resource-server
(Nimbus JWT encoder/decoder qua JOSE), starter-mail (SMTP), bcprov-jdk18on1.80 (Argon2id).
Các version không ghi riêng do Boot parent/BOM quản lý. Không tuyên bố đã audit dependency vulnerabilities.

Plugin Boot tạo executable JAR; Failsafe chạy `*IT` ở integration-test/verify. Không có Maven module JPA.

## 2. Cấu trúc thực tế và nhiệm vụ

```text
apps/backend/
├── pom.xml, mvnw, mvnw.cmd, .mvn/wrapper/maven-wrapper.properties
├── .env.example, README.md
└── src/
    ├── main/java/com/rideguard/
    │   ├── RideGuardApplication.java
    │   ├── auth/       # controller, request/response DTO, service, JDBC, JWT, SMTP, CLI
    │   ├── users/      # profile/session/admin users controller, service, JDBC, DTO
    │   ├── devices/    # registry/binding/config/report controller, service, JDBC, DTO
    │   ├── feedback/   # self/admin feedback controller, service, JDBC, DTO
    │   └── shared/     # security, errors, body limit, cursor, idempotency, SQL, cleanup
    ├── main/resources/
    │   ├── application.yaml
    │   ├── application-provision-admin.yaml
    │   └── db/migration/{V1__foundation.sql,V2__mvp.sql}
    └── test/java/com/rideguard/
        ├── ContractRulesTest.java
        ├── SecurityHttpTest.java
        ├── ServiceRulesTest.java
        ├── ValidationTest.java
        ├── PostgresIT.java
        ├── FoundationIT.java
        ├── MigrationIT.java
        └── MvpIT.java
```

Package gốc `com.rideguard`. Controller chỉ nhận/validate/map DTO và gọi service; không chứa SQL.
Các `*Repository` thực thi SQL parameter binding; dynamic SQL chỉ ghép tên cột/clause từ code whitelist,
không nối input người dùng. Services dùng `@Transactional` hoặc TransactionTemplate cho nhiều bước.
Response records trong từng feature là whitelist; auth dùng whitelist projection cho user/token transport.

- `RideGuardApplication`: bootstrap, vẫn loại default UserDetailsService account.
- Auth: `AuthController/AuthDtos/Responses`, `AuthService`, `AuthRepository`, `Actor`, `Tokens`,
  `BearerFilter`, `ResetMail/SmtpResetMail`, `AdminProvisioning`.
- Users: `UsersController/UsersService/UsersRepository`, `UserDtos`, `Responses`.
- Devices: `DevicesController/DevicesService/DevicesRepository`, `DeviceDtos`, `Responses`.
- Feedback: `FeedbackController/FeedbackService/FeedbackRepository`, `FeedbackDtos`, `Responses`.
- Shared: `AppConfig/TransactionConfig` (Clock/Argon2/Jackson/executor/transaction), `SecurityConfig`
  (route/CORS/CSRF), `HttpBoundary` (request ID/body/Origin/rate), `WebConfig` (UUID canonical),
  `ApiError/ApiException/ApiExceptionHandler/Errors`, `Json/ResponseViews`, `Sql/Listing/Cursors`,
  `Idempotency`, `Retention`.

## 3. Mã quan trọng và dữ liệu

Bản cập nhật này **không chép đầy đủ toàn bộ mã mới** vì số lớp đã tăng; liên kết trực tiếp là nguồn chuẩn,
tránh bản code nhúng bị cũ như bản foundation trước:

- [POM](../../apps/backend/pom.xml), [bootstrap](../../apps/backend/src/main/java/com/rideguard/RideGuardApplication.java).
- [Security](../../apps/backend/src/main/java/com/rideguard/shared/SecurityConfig.java),
  [HTTP boundary](../../apps/backend/src/main/java/com/rideguard/shared/HttpBoundary.java),
  [errors](../../apps/backend/src/main/java/com/rideguard/shared/ApiExceptionHandler.java).
- [Auth service](../../apps/backend/src/main/java/com/rideguard/auth/AuthService.java),
  [JWT](../../apps/backend/src/main/java/com/rideguard/auth/Tokens.java).
- [Device service](../../apps/backend/src/main/java/com/rideguard/devices/DevicesService.java),
  [idempotency](../../apps/backend/src/main/java/com/rideguard/shared/Idempotency.java).
- [Application YAML](../../apps/backend/src/main/resources/application.yaml),
  [V1](../../apps/backend/src/main/resources/db/migration/V1__foundation.sql),
  [V2](../../apps/backend/src/main/resources/db/migration/V2__mvp.sql),
  [DB mô tả](../database/schema.md) và [ERD](../database/erd.md).

V1 giữ nguyên. V2 có10 bảng: users/auth_sessions/password_reset_tokens/devices/device_bindings/
device_configs/feedbacks/idempotency_records/device_reports/device_config_versions. Unique email/serial,
partial unique active binding, config issued composite FK, snapshot trigger, immutable-version UPDATE
trigger, idempotency unique scope, report accepted-time unique. Chưa chạy migration trên DB thực tế.
Flyway history không được chuyển chỗ; public với default search path. Hikari đặt session timezone UTC.

Argon2id memory64MiB/iterations3/parallelism1/salt16/hash32; password12–128 Unicode không trim.
HS256 access15 phút, refresh random32 bytes/hash SHA256/family30 ngày, rotation nguyên tử dưới user row lock.
Reuse commit revoke trước khi trả401. Request Bearer kiểm account và active family mỗi lần.
Reset token15 phút/single-use; SMTP worker queue bounded, không token trong API/log; queue không durable.
Admin CLI không HTTP, password nhập ẩn; cấm tự khóa/khóa admin ACTIVE cuối.

Không sao chép giá trị secret. DB_PASSWORD/JWT_SIGNING_KEY/CURSOR_SIGNING_KEY/SMTP_PASSWORD chỉ là tên biến.

## 4. API và trạng thái kiểm chứng

Ba GET `/actuator/health`, `/actuator/health/liveness`, `/actuator/health/readiness` public.
Liveness chỉ process, readiness gồm DB; startup vẫn cần datasource/Flyway. Không expose actuator nhạy cảm.

31 operation MVP có code:

- Auth register/login/refresh/logout/forgot-password/reset-password.
- Users me GET/PATCH, change-password, sessions GET/DELETE.
- Devices list/get/rename/bind/unbind/history; config GET/PATCH/ACK; status report POST.
- Feedback create/list/get; admin users list/status, devices create/list/status, feedback list/patch.

Chi tiết method/path/request/response: [hợp đồng MVP](../../contracts/openapi.proposed.yaml),
[giải thích API](../api/proposed-api.md). Auth POST public ở filter nhưng refresh/logout xác thực credential
trong service; nghiệp vụ Bearer, admin role ADMIN. Ngoài `/api/v1/**` và ba health denyAll; path chưa có trong
API namespace trả404 với caller authenticated. Method không hỗ trợ405, media sai415, không catch-all thành500.

JSON snake_case/UTC/null; unknown fields bị từ chối; lỗi `{status,code,message,request_id}` và X-Request-ID.
Mutation body32KiB đọc thực, Content-Encoding nén bị415; pagination keyset/HMAC theo caller/route/filter.
Web auth strict Origin + X-CSRF-Protection:1 + HttpOnly cookie; mobile JSON refresh không dùng cookie.

Hợp đồng mới: `expected_binding_id` bắt buộc cho DELETE; serial ASCII không space; vibration=false tắt rung
nhưng HUD/risk vẫn chạy; BLE anti-rollback mới là yêu cầu chưa được firmware thực hiện. [ADR0003](../decisions/0003-backend-mvp.md).

`openapi.yaml` có 3 health và 31 nghiệp vụ, trạng thái `integration-and-runtime-smoke-tested` cho nghiệp vụ,
kèm status đã thử. Không xác nhận mọi nhánh lỗi. ValidationProbe chỉ nằm trong test.

## 5. Chạy và kiểm thử

[Runbook](runbook.md) có PostgreSQL Docker/bản cài, env loader, keys, SMTP, CLI admin, PowerShell/POSIX,
build/unit/integration/Maven/JAR, cookie policy và dừng bảo toàn volume. [Ví dụ API](api-examples.md),
[troubleshooting](troubleshooting.md). Spring không tự đọc `.env`.

Bắt buộc runtime: DB_PASSWORD, JWT_SIGNING_KEY, CURSOR_SIGNING_KEY; DB_URL/DB_USER/PORT có default.
SMTP phải cấu hình để giao reset email. Test dùng TEST_DB_URL/USER/PASSWORD riêng, database tên `_test`;
MigrationIT dùng TEST_MIGRATION_DB_URL khác và database mới. Thiếu biến thì integration SKIPPED.

Kết quả mới nhất: **27 test độc lập và 15 integration đạt, 0 skipped/failure/error**, JAR đóng gói thành công.
Đã thử CLI admin, 69 HTTP requests, SMTP sink/reset và restart cùng database; thêm assertion FK/deferred rollback.
[Bằng chứng và giới hạn](../testing/backend-verification-2026-11-09.md).
Health tổng hợp loại SMTP tùy chọn; readiness vẫn gồm DB. Health UP không chứng minh email được giao.

## 6. Còn phải xác minh trước sử dụng

- Bộ 15 PostgreSQL integration đã đạt; tiếp tục đánh giá concurrency/load và retention dài hạn trước vận hành.
- Thử SMTP thật, browser cookie/CORS và triển khai HTTPS. Chốt domains, secret rotation,
  proxy/multi-instance rate limiting, account/history retention và backup/recovery.
- Reset queue không durable và rate limiter chỉ trong process; cần outbox/limiter tập trung nếu vận hành yêu cầu.
- OpenAPI đã ghi phạm vi integration/runtime smoke, không verified tuyệt đối. Chưa chạy hosted CI.
- Không sửa Android/admin UI/firmware code, không xác nhận chúng tương thích. Không trips/GPS recording,
  BLE ownership challenge, OTA/payment/sharing hoặc AI inference server. Cảnh báo local ESP32 độc lập backend/auth.

Không commit/push/deploy hoặc thay STL/CAD. Thay đổi mobile Gradle và `.vscode` có sẵn được giữ nguyên.
