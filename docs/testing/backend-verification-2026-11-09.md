# Kiểm chứng backend RideGuard — 2026-11-09

## Kết quả thực tế

Docker Engine **29.6.2**, image **postgres:17.11-alpine**, PostgreSQL **17.11**, JDK **21.0.13**, Maven Wrapper **3.9.11**.
Không tìm thấy AGENTS.md trong repository và thư mục tổ tiên đã kiểm tra. Không đọc `.env` thực.
Dùng một container mới tên ngẫu nhiên `rideguard-verify-*`, chỉ publish localhost, không mount volume phát triển.
DB integration `rideguard_test`; upgrade lần lượt `rideguard_migration_test`, `rideguard_migration_retry_test`,
`rideguard_migration_final_test` (mỗi lần mới); runtime `rideguard_runtime_test` riêng.
Secret sinh ngẫu nhiên, lưu tạm DPAPI của Windows trong `.local`, không commit hoặc in ra; xóa file secret khi kết thúc.

Lần verify đầu: **27/27 test độc lập đạt; 14/15 integration đạt, 1 failure, 0 error, 0 skipped**.
Lần verify sau sửa health: **27/27 + 15/15 đạt, 0 failure/error/skipped**.
Lần verify cuối sau bổ sung assertion FK/rollback: **27/27 + 15/15 đạt, 0 failure/error/skipped**.
Lần cuối kết thúc **00:10:38 +07:00 ngày 2026-11-09**, BUILD SUCCESS. Không tính FoundationTest XML cũ.

## Lỗi và cách sửa

1. `pg_isready` qua Unix socket bắt trúng temporary server lúc init image; `createdb` bị ngắt khi server đó dừng.
   Đã đợi server TCP cuối cùng và tạo lại đúng database chưa được tạo. Runbook đổi sang `pg_isready -h 127.0.0.1`.
2. FoundationIT nhận 503 ở health tổng hợp: starter-mail tự thêm MailHealthIndicator, SMTP chưa có credentials.
   Đặt `management.health.mail.enabled=false`: SMTP reset bất đồng bộ/tùy chọn không chặn health API/database.
   Không sửa assertion; SMTP vẫn được kiểm riêng qua sink. Health UP không chứng minh mail đã giao.
3. Bổ sung assertion trong MvpIT hiện có: FK từ issued version đến binding từ chối orphan;
   deferred snapshot constraint lỗi tại commit làm rollback cả issued version và config desired_version.
   Sau rollback không có version 3, desired vẫn 2. Không tăng/disable số test method.

## Test suites thực thi

### ContractRulesTest: 9 PASS, 0 failure/error/skipped

- `roleInjectionAndWrongTypesRejected`.
- `tokenExpiryAndSignature`.
- `serialAsciiOnly`.
- `requiredNullableFieldsRemainRequired`.
- `cursorsBindCallerFilterAndRejectTampering`.
- `settingsRejectUnknownNullFractionAndEmpty`.
- `passwordsCountUnicodeWithoutTrim`.
- `canonicalizationAndPasswordPreservation`.
- `responseDtosPreserveSnakeCaseNullsAndUtc`.

### SecurityHttpTest: 9 PASS, 0 failure/error/skipped

- `webTransportHidesRefresh`.
- `invalidCorsPreflightHasErrorEnvelope`.
- `preflightAndWrongMedia`.
- `publicRegisterAndUnknownFields`.
- `cookieCannotAuthenticateMobile`.
- `unauthorizedAndRoleEnforced`.
- `actualBodyLimitWithoutLength`.
- `methodAndValidationNot500`.
- `webRequiresOriginAndCsrf`.

### ServiceRulesTest: 8 PASS, 0 failure/error/skipped

- `staleDeleteOnlyTouchesExpectedBinding`.
- `lastAdminAndSelfLockProtected`.
- `retiredDeviceCannotBecomeActive`.
- `reuseCommitsRevocationBeforeReturningUnauthorized`.
- `configCasPreventsWriteAndAckCannotInventVersion`.
- `foreignBindingCannotBeClosed`.
- `realArgon2PreservesPasswordAndUsesRandomSalt`.
- `clockToleranceAndStaleReportsUseFixedClock`.

### ValidationTest: 1 PASS, 0 failure/error/skipped

- `invalidAndMalformedBodiesReturnRedactedErrors`.

### FoundationIT: 2 PASS, 0 failure/error/skipped

- `protectedApisRequireBearer`.
- `probesAreMinimalAndDatabaseIsReady`.

### MigrationIT: 1 PASS, 0 failure/error/skipped

- `upgradesFoundationWithoutChangingV1OrMovingHistory`.

### MvpIT: 12 PASS, 0 failure/error/skipped

- `feedbackRetainsAuthorAndAdminDoesNotEditContent`.
- `refreshReplayRevokesFamilyAndLogoutRetries`.
- `registrationProfileAndValidation`.
- `configCasIssuedAckAndReportOrdering`.
- `bindRacePrivacyAndStaleUnbind`.
- `sessionsLogicalAndChangePasswordRevokesOthers`.
- `keysetPaginationAndTerminalRegistry`.
- `concurrentRefreshOnlyOneRotation`.
- `idempotencyConflictExpiryAndClosedBinding`.
- `migrationAndConstraints`.
- `lockUnlockDoesNotRestoreSessionAndSelfLockProtected`.
- `resetOneTimeRevokesSessionsWithoutTokenResponse`.

## Runtime và CLI

CLI thực thi JAR profile `provision-admin` trong terminal PTY, nhập email/name rồi password ẩn:
đã tạo ADMIN mới, exit 0 và tự đóng datasource/context. Không thay CLI bằng SQL/repository fixture.
Sau đó Python smoke chạy JAR HTTP thật (Tomcat), không MockMvc, dùng admin vừa tạo.
**69 HTTP request có assertion status và 89 assertion khác đạt**, bao phủ path/method của cả 31 API nghiệp vụ.
Các UUID/path là dữ liệu test. Không lưu response body/token/reset code vào báo cáo.

Đã kiểm chứng health/readiness; register/login/profile; USER bị cấm admin; admin registry; bind/replay;
config CAS; ACK tăng dần; report cũ không thay mới; feedback/admin reply; tác giả đọc feedback sau unbind;
quyền config/report/ACK cũ trả 404; stale expected_binding_id không đóng binding mới; refresh replay revoke;
lock/unlock không phục hồi token; change password/revoke session; reset một lần qua SMTP sink.
Web-admin: JSON không có refresh_token, Set-Cookie có HttpOnly/Secure/SameSite; Origin sai/thiếu CSRF 403;
cookie không xác thực mobile/CRUD; preflight đúng allowlist; cookie rotation/logout và Max-Age=0.
Đây là HTTP client gửi Cookie thủ công để kiểm server; **chưa kiểm browser thực/HTTPS/SameSite enforcement**.
Restart JAR trên cùng runtime DB và signing keys, health/login/admin list đạt; Flyway validate V1/V2 và không migrate lại.
ACK/report là mô phỏng client REST, không phải ESP32/BLE apply.

### Status đã thử theo operation

- `POST /api/v1/auth/register`: 201.
- `POST /api/v1/auth/login`: 200, 403.
- `POST /api/v1/auth/refresh`: 200, 400, 401, 403.
- `POST /api/v1/auth/logout`: 204.
- `POST /api/v1/auth/forgot-password`: 202.
- `POST /api/v1/auth/reset-password`: 204, 400.
- `GET /api/v1/users/me`: 200, 401.
- `PATCH /api/v1/users/me`: 200.
- `POST /api/v1/users/me/change-password`: 204.
- `GET /api/v1/users/me/sessions`: 200.
- `DELETE /api/v1/users/me/sessions/{session_id}`: 204.
- `GET /api/v1/devices`: 200.
- `GET /api/v1/devices/{device_id}`: 200, 404.
- `PATCH /api/v1/devices/{device_id}`: 200.
- `POST /api/v1/devices/bind`: 200, 201.
- `DELETE /api/v1/devices/{device_id}/binding`: 204.
- `GET /api/v1/device-bindings`: 200.
- `GET /api/v1/devices/{device_id}/config`: 200, 404.
- `PATCH /api/v1/devices/{device_id}/config`: 200, 409.
- `POST /api/v1/devices/{device_id}/config/ack`: 200, 404.
- `POST /api/v1/devices/{device_id}/reports`: 200, 404.
- `POST /api/v1/feedbacks`: 201.
- `GET /api/v1/feedbacks`: 200.
- `GET /api/v1/feedbacks/{feedback_id}`: 200.
- `GET /api/v1/admin/users`: 200, 403.
- `PATCH /api/v1/admin/users/{user_id}/status`: 200.
- `POST /api/v1/admin/devices`: 201.
- `GET /api/v1/admin/devices`: 200.
- `PATCH /api/v1/admin/devices/{device_id}/status`: 200.
- `GET /api/v1/admin/feedbacks`: 200.
- `PATCH /api/v1/admin/feedbacks/{feedback_id}`: 200.

Ba GET health thử 200/UP. Không tuyên bố đã thử toàn bộ error status được hợp đồng mô tả.

## SMTP

`scripts/backend_runtime_smoke.py` mở SMTP sink TCP loopback tạm bằng Python standard library.
JavaMailSender production gửi email vào sink; recipient đúng, lấy code trong memory, reset thành công và reuse bị 400.
Không trả/log token để demo. Sink tắt AUTH/TLS có chủ đích trong test; **chưa kiểm SMTP thật, TLS/auth provider, deliverability/inbox**.

## Lệnh và biến đã dùng

PowerShell từ gốc repository. Tạo container/DB theo runbook mục 4a; không dùng profile Maven riêng.
Các biến TEST_DB_PASSWORD/DB_PASSWORD, signing keys và admin password lấy từ secret tạm, không ghi giá trị bên dưới.

Lệnh Docker đã dùng, `$rgContainer` là tên duy nhất của lượt này và POSTGRES_PASSWORD được sinh random vào process:

```powershell
docker info --format '{{.ServerVersion}}'
docker run -d --name $rgContainer -e POSTGRES_PASSWORD -e POSTGRES_USER=rideguard_test -e POSTGRES_DB=rideguard_test -p 127.0.0.1::5432 postgres:17.11-alpine
docker exec $rgContainer pg_isready -h 127.0.0.1 -U rideguard_test -d rideguard_test
docker port $rgContainer 5432/tcp
docker exec $rgContainer createdb -U rideguard_test rideguard_migration_test
docker exec $rgContainer createdb -U rideguard_test rideguard_runtime_test
# Database upgrade mới cho mỗi lần verify tiếp theo, giữ database integration:
docker exec $rgContainer createdb -U rideguard_test rideguard_migration_retry_test
docker exec $rgContainer createdb -U rideguard_test rideguard_migration_final_test
```

```powershell
$env:JAVA_HOME = (Resolve-Path '.local/jdk21/jdk-21.0.13+9').Path
$env:DEBUG = 'false'
# Port thực của container lần này: 62325; lần sau lấy bằng docker port.
$env:TEST_DB_URL = 'jdbc:postgresql://localhost:62325/rideguard_test'
$env:TEST_MIGRATION_DB_URL = 'jdbc:postgresql://localhost:62325/rideguard_migration_final_test'
$env:TEST_DB_USER = 'rideguard_test'
# TEST_DB_PASSWORD đã được cấp tạm vào process.
.\apps\backend\mvnw.cmd -f apps/backend/pom.xml -B -ntp verify
# Runtime: DB_URL trỏ rideguard_runtime_test; DB_USER/DB_PASSWORD cùng role test.
# Cấp JWT_SIGNING_KEY và CURSOR_SIGNING_KEY ngẫu nhiên độc lập.
& "$env:JAVA_HOME/bin/java.exe" -jar apps/backend/target/backend-0.1.0-SNAPSHOT.jar --spring.profiles.active=provision-admin
# Nhập admin bằng console; cấp RG_ADMIN_EMAIL/RG_ADMIN_PASSWORD vào process để smoke login.
.\scripts\.venv\Scripts\python.exe scripts/backend_runtime_smoke.py
.\scripts\.venv\Scripts\python.exe scripts/check_contracts.py
git diff --check
```

Lệnh unit-only: `mvnw.cmd -f apps/backend/pom.xml -B test`; lượt này unit chạy trong lifecycle verify.
Smoke tự chọn port localhost, cấu hình SMTP sink, khởi động JAR hai lần và dừng đúng PID của nó trong finally.
Trên Windows, stop subprocess kết thúc process; đã kiểm restart và dữ liệu tồn tại, chưa đánh giá graceful shutdown khi có tải.

## Migration và giới hạn

- `V1__foundation.sql` SHA-256: `a3f392bbdccffe17a2d54eeea74e63e60be9a473e00e62b9c182cf194ff4fa00` (giữ nguyên so với báo cáo 2026-11-08).
- `V2__mvp.sql` SHA-256: `e761996c16daba0fb9be6cdc326c4dd6d88e6490e1310c252f17e60042c1e046` (giữ nguyên so với báo cáo 2026-11-08).

MigrationIT đã chạy V1 rồi V2, validate checksum V1 không đổi; 10 bảng nằm trong `rideguard`, history ở `public`.
MvpIT/full runtime kiểm các race/ownership/CAS/replay đã liệt kê, không phải chứng minh mọi interleaving hoặc tải production.
Chưa kiểm browser HTTPS, email ngoài, hardware/BLE, performance/load, hosted CI, retention scheduler dài hạn.
OpenAPI chính nay có 34 operations; từng API nghiệp vụ ghi `integration-and-runtime-smoke-tested` cùng status đã thử.
Không dùng nhãn verified tuyệt đối. OpenAPI proposed giữ bản MVP đối chiếu, không phải backlog chưa triển khai.

## Kết thúc

Contract checker đạt: 434 example validations, 32 negative cases, 136 liên kết Markdown;
34 operations trong hợp đồng chính và 31 trong bản MVP đối chiếu (65 lượt kiểm, 34 operation phân biệt).
31 mapping controller khớp; checker yêu cầu nội dung path MVP giữa hai hợp đồng giống nhau.
`git diff --check` đạt. Kiểm tra SQL runtime trả V1/V2 success và đúng 10 bảng `rideguard`.

Java runtime và SMTP sink do smoke tạo được đóng trong finally; CLI/test JVM đã thoát.
Container PostgreSQL của lượt này được stop sau kiểm tra cuối; không xóa container/volume, không dừng dịch vụ khác.
File secret DPAPI tạm được xóa. Không sửa FE/mobile/firmware trong lượt này, không commit/push/deploy.
Đã xác nhận container `rideguard-verify-8052a10004a04744ae51fc9def74bd8b` ở trạng thái `exited`.
Hai Java process có thời điểm khởi động trước lượt kiểm chứng được giữ nguyên, không dừng theo tên process.
