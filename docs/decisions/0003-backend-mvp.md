# ADR 0003 — triển khai backend MVP và sửa hợp đồng

Ngày: 2026-11-07. Trạng thái: đã áp dụng trong code/hợp đồng; PostgreSQL integration chưa chạy.
Yêu cầu người dùng thay thế các quyết định tương ứng trong ADR 0002 và tài liệu trước đó.

1. Serial trim, chỉ ASCII chữ/số/gạch ngang rồi uppercase; regex `^[A-Z0-9][A-Z0-9-]{0,127}$`.
2. DELETE binding bắt buộc query `expected_binding_id` UUID. Đúng caller/device: đóng binding đó,
   retry bản đã đóng 204; không ảnh hưởng binding mới kể cả cùng caller. Sai caller/device 404.
3. `vibration_enabled=false` tắt cơ cấu rung; HUD và pipeline nguy cơ vẫn chạy. Không master-disable
   hay threshold AI. Đây là hợp đồng, không sửa firmware trong nhiệm vụ backend.
4. BLE cùng binding: version thấp hơn từ chối; cùng version/cùng nội dung ACK lại; cùng version/
   khác nội dung lỗi; binding mới thiết lập riêng, không so version giữa hai binding. Backend ACK
   monotonic không là bằng chứng firmware đã chống rollback.
5. Giữ JDBC, schema-qualified `rideguard.*`; giữ V1/checksum và Flyway history mặc định hiện có.
   V2 tạo 10 bảng, constraint/index và issued snapshot. Không clean/xóa volume.
6. JWT HS256 15 phút, key base64 >=32 bytes do môi trường cấp; cursor HMAC-SHA256 dùng key độc lập.
   Argon2id salt16/hash32, parallelism1, memory65536 KiB, iterations3; cần benchmark trước production.
   Refresh token 32 random bytes, chỉ lưu SHA-256; family tối đa30 ngày, rotation/reuse dưới user lock.
7. Khóa admin được tuần tự hóa bằng advisory transaction lock; cấm tự khóa và khóa admin ACTIVE cuối.
   CLI provision-admin hỏi password ẩn, chỉ tạo account mới, không tự nâng quyền account có sẵn.
8. Web auth cookie Secure mặc định, host-only, HttpOnly, Path=/api/v1/auth, SameSite=Lax; local profile
   mới cho phép COOKIE_SECURE=false. Strict Origin allowlist + header không bí mật X-CSRF-Protection:1
   bảo vệ web auth; API nghiệp vụ chỉ Bearer, mobile không dùng cookie để authenticate.
9. Mutation body đọc tối đa32769 bytes để enforce32768 kể cả thiếu Content-Length. Chỉ JSON UTF-8;
   Content-Encoding nén bị từ chối415, không âm thầm bỏ qua giới hạn decoded body.
10. Cursor keyset có chữ ký gắn caller/route/filter; report timestamp chuẩn hóa microsecond cho PostgreSQL.
    Reports retention mặc định30 ngày, tối thiểu2 ngày để không mất watermark trong cửa sổ nhận24h.
11. Reset request202 đưa vào queue bounded trong process trước lookup; SMTP worker không log token,
    lỗi gửi rollback token mới và log chung. Queue không durable qua crash; cần outbox nếu yêu cầu
    bảo đảm giao email cao hơn. Queue full503 cho cả email có/không có account. Không tuyên bố đã gửi SMTP thật.
12. Rate limit per-IP fixed-minute trong process, bounded10000 entries, mặc định120 request/phút.
    Không tin X-Forwarded-For trực tiếp. Multi-instance cần limiter tập trung/proxy đã cấu hình tin cậy.

31 operation có code nhưng chưa có bằng chứng PostgreSQL integration nên vẫn nằm trong
`openapi.proposed.yaml`, `x-status: implemented-unverified`; không dùng unit test để promote toàn bộ API.
Các ví dụ là tổng hợp. [Verification](../testing/verification-status.md), [runbook](../backend/runbook.md).
