# RideGuard backend

Java 21, Spring Boot 3.5.16, Maven Wrapper 3.9.11; Spring MVC/Security/JDBC,
PostgreSQL 17.11 và Flyway. Giữ JDBC, không JPA/H2.

Đã có code cho 31 API MVP: auth/users/sessions, registry/binding/history, config/version/ACK,
reports, feedback/admin. V1 giữ nguyên; V2 thêm 10 bảng trong schema `rideguard`.
**Chưa kiểm chứng integration PostgreSQL/SMTP thật trong lần triển khai này.**
Unit/HTTP slice test không thay thế database/concurrency test.

- [Runbook](../../docs/backend/runbook.md): cấu hình, PostgreSQL, build/test/run và tạo admin.
- [Ví dụ API](../../docs/backend/api-examples.md): register đến feedback.
- [Troubleshooting](../../docs/backend/troubleshooting.md).
- [Hiện trạng](../../docs/backend/current-state.md) và [bằng chứng kiểm thử](../../docs/testing/verification-status.md).
- [Hợp đồng MVP](../../contracts/openapi.proposed.yaml): `implemented-unverified`, chưa promote sang verified contract.

Từ gốc repository (PowerShell; các lệnh hướng dẫn chưa chạy dịch vụ):

```powershell
.\apps\backend\mvnw.cmd -f apps/backend/pom.xml -B test
.\apps\backend\mvnw.cmd -f apps/backend/pom.xml -B verify
.\apps\backend\mvnw.cmd -f apps/backend/pom.xml spring-boot:run
java -jar apps/backend/target/backend-0.1.0-SNAPSHOT.jar
```

`test` chỉ unit/slice; `verify` thêm integration nếu có `TEST_DB_URL/USER/PASSWORD` riêng.
Thiếu TEST_DB_URL thì integration được ghi SKIPPED, không phải pass. Chạy dịch vụ cần DB_PASSWORD,
JWT_SIGNING_KEY, CURSOR_SIGNING_KEY; `.env` không tự nạp. Xem runbook trước khi chạy.

Health public: liveness không DB, readiness có DB. Nghiệp vụ dùng Bearer và kiểm account/session;
admin bắt buộc ADMIN. Web auth dùng strict Origin/custom CSRF header + cookie, mobile dùng JSON refresh.
Không admin/password mặc định; không token reset trong API/log. Không trips/proof/OTA/inference server.
Firmware, Android và admin UI chưa được sửa hay xác nhận tương thích.
