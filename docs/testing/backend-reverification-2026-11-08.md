# Kiểm chứng lại backend — 2026-11-08

Kết quả lần chạy kết thúc lúc **18:49:47 +07:00**: Maven `verify` exit 0,
**27 test thực thi đạt, 0 failure/error; 15 integration test skipped, 0 integration test thực thi**.
Không suy ra backend đạt toàn bộ từ BUILD SUCCESS. Nguồn số liệu là XML Surefire/Failsafe của lần chạy này.

Báo cáo `FoundationTest` cũ từ 2026-11-06 còn trong `target/surefire-reports` không được tính:
class đó đã đổi thành FoundationIT, không thuộc lượt chạy hiện tại. Đối chiếu tên class trong
mã nguồn và thời điểm XML, không cộng tất cả file lịch sử trong target.

Đã chạy lại `scripts/check_contracts.py`: 434 example validations, 32 negative cases,
134 liên kết Markdown, 34 operations (3 health + 31 nghiệp vụ), 31 mapping controller khớp hợp đồng.
`git diff --check` đạt. Đây là kiểm tra tĩnh, không nâng trạng thái API thành runtime verified.

## Môi trường và lệnh đã chạy

Không tìm thấy AGENTS.md trong repository hoặc các thư mục tổ tiên đã kiểm tra.
Docker CLI có mặt nhưng `docker info` không kết nối được named pipe `dockerDesktopLinuxEngine`.
Không thấy service PostgreSQL, lệnh psql/pg_ctl/postgres trên PATH, thư mục cài PostgreSQL tiêu chuẩn
hoặc listener TCP 5432/5433. Đây là phạm vi dò tìm local, không khẳng định không có server từ xa.
Chưa có thông tin kết nối database test riêng. Không đọc/xuất `.env`, không bật Docker Desktop,
không cài PostgreSQL hay dùng database phát triển. Không cần sinh secret vì chưa tạo dịch vụ test.

PowerShell từ gốc repository, dùng JDK portable sẵn có:

```powershell
$env:JAVA_HOME = (Resolve-Path '.local/jdk21/jdk-21.0.13+9').Path
$env:DEBUG = 'false'
.\apps\backend\mvnw.cmd -f apps/backend/pom.xml -B -ntp verify
```

POM không yêu cầu Maven profile integration. Surefire chạy `*Test` trong phase `test`;
Failsafe chạy `*IT` trong `integration-test` và kiểm kết quả trong `verify`.
FoundationIT/MvpIT cần TEST_DB_URL; MigrationIT cần TEST_MIGRATION_DB_URL riêng và chưa có history.
Cả hai cần TEST_DB_USER/TEST_DB_PASSWORD. Database phải kết thúc `_test`, URL không có query parameters.

## Danh sách test chính xác

### ContractRulesTest — 9 test

- `roleInjectionAndWrongTypesRejected` — PASS.
- `tokenExpiryAndSignature` — PASS.
- `serialAsciiOnly` — PASS.
- `requiredNullableFieldsRemainRequired` — PASS.
- `cursorsBindCallerFilterAndRejectTampering` — PASS.
- `settingsRejectUnknownNullFractionAndEmpty` — PASS.
- `passwordsCountUnicodeWithoutTrim` — PASS.
- `canonicalizationAndPasswordPreservation` — PASS.
- `responseDtosPreserveSnakeCaseNullsAndUtc` — PASS.

### SecurityHttpTest — 9 test

- `webTransportHidesRefresh` — PASS.
- `invalidCorsPreflightHasErrorEnvelope` — PASS.
- `preflightAndWrongMedia` — PASS.
- `publicRegisterAndUnknownFields` — PASS.
- `cookieCannotAuthenticateMobile` — PASS.
- `unauthorizedAndRoleEnforced` — PASS.
- `actualBodyLimitWithoutLength` — PASS.
- `methodAndValidationNot500` — PASS.
- `webRequiresOriginAndCsrf` — PASS.

### ServiceRulesTest — 8 test

- `staleDeleteOnlyTouchesExpectedBinding` — PASS.
- `lastAdminAndSelfLockProtected` — PASS.
- `retiredDeviceCannotBecomeActive` — PASS.
- `reuseCommitsRevocationBeforeReturningUnauthorized` — PASS.
- `configCasPreventsWriteAndAckCannotInventVersion` — PASS.
- `foreignBindingCannotBeClosed` — PASS.
- `realArgon2PreservesPasswordAndUsesRandomSalt` — PASS.
- `clockToleranceAndStaleReportsUseFixedClock` — PASS.

### ValidationTest — 1 test

- `invalidAndMalformedBodiesReturnRedactedErrors` — PASS.

### FoundationIT — 2 test

- `protectedApisRequireBearer` — SKIPPED.
- `probesAreMinimalAndDatabaseIsReady` — SKIPPED.

### MigrationIT — 1 test

- `upgradesFoundationWithoutChangingV1OrMovingHistory` — SKIPPED.

### MvpIT — 12 test

- `feedbackRetainsAuthorAndAdminDoesNotEditContent` — SKIPPED.
- `refreshReplayRevokesFamilyAndLogoutRetries` — SKIPPED.
- `registrationProfileAndValidation` — SKIPPED.
- `configCasIssuedAckAndReportOrdering` — SKIPPED.
- `bindRacePrivacyAndStaleUnbind` — SKIPPED.
- `sessionsLogicalAndChangePasswordRevokesOthers` — SKIPPED.
- `keysetPaginationAndTerminalRegistry` — SKIPPED.
- `concurrentRefreshOnlyOneRotation` — SKIPPED.
- `idempotencyConflictExpiryAndClosedBinding` — SKIPPED.
- `migrationAndConstraints` — SKIPPED.
- `lockUnlockDoesNotRestoreSessionAndSelfLockProtected` — SKIPPED.
- `resetOneTimeRevokesSessionsWithoutTokenResponse` — SKIPPED.

## Giới hạn bằng chứng và việc chưa thực hiện

SecurityHttpTest dùng MVC slice với auth/service mock; ServiceRulesTest dùng repository/transaction mock.
Chúng không chứng minh SQL, commit/rollback hay race trên PostgreSQL. MvpIT có SMTP adapter mock
và tạo admin bằng repository fixture; ngay cả khi test này đạt cũng chưa thay thế CLI provisioning hoặc SMTP sink.

Chưa chạy V1/V2 trên PostgreSQL mới, upgrade/restart/checksum thực tế, FK/constraints/rollback,
bind/refresh đồng thời, CAS/ACK/report ordering hoặc ownership qua full runtime.
Chưa khởi động HTTP server; cả 10 bước runtime yêu cầu (health, USER, CLI ADMIN, registry, bind,
config/ACK, reports, feedback, unbind/quyền cũ, web cookie/Origin/CSRF) đều **chưa thực hiện**.
SMTP sink và gửi email thật đều chưa kiểm chứng. ACK/report thủ công sau này chỉ là mô phỏng REST,
không phải bằng chứng ESP32/BLE. Không đổi OpenAPI sang verified.

Không phát hiện failure trong các test đã thực thi; không có sửa lỗi production được tuyên bố trong lượt này.
Không sửa/bỏ assertion, disable test, thay PostgreSQL bằng H2/mock hoặc sửa migration.
Đã sửa ví dụ JAVA_HOME bị sai đường dẫn và bổ sung quy trình database test riêng trong runbook.

## Migration giữ nguyên trong lượt kiểm chứng

SHA-256 của file hiện tại (không phải Flyway checksum trong database):

- `V1__foundation.sql`: `a3f392bbdccffe17a2d54eeea74e63e60be9a473e00e62b9c182cf194ff4fa00`.
- `V2__mvp.sql`: `e761996c16daba0fb9be6cdc326c4dd6d88e6490e1310c252f17e60042c1e046`.

Không biết V2 đã áp dụng ở môi trường ngoài hay chưa; giữ nguyên V1/V2.
Nếu cần sửa schema sau khi có bằng chứng lỗi, dùng migration bổ sung thay vì đổi checksum cũ.

## Tiếp tục và dọn môi trường

Người vận hành cần mở Docker Desktop và đợi Engine sẵn sàng, hoặc cung cấp PostgreSQL test riêng.
Xem [runbook](../backend/runbook.md), mục 4a cho lệnh tạo hai DB độc lập và chạy đủ lifecycle.
Sau đó thực hiện CLI admin và [luồng runtime](../backend/api-examples.md) trên DB test, ghi status/assertion
thực tế từng bước; không in token, password hay nội dung thư reset vào log.

Lượt này chỉ tạo tiến trình Maven/Java test; lệnh đã kết thúc. Không tạo backend, container, SMTP sink
hoặc PostgreSQL process nên không có dịch vụ test để dừng. Không dừng dịch vụ tồn tại trước đó.
Không sửa FE/mobile trong lượt này, không commit/push/deploy.
