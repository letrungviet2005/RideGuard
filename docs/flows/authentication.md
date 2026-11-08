# Authentication — code MVP, integration PostgreSQL chưa kiểm chứng

Đã có JWT/auth controller/JDBC, không admin mặc định. Security mở từng nhóm auth/Bearer/admin;
web auth giữ strict Origin/custom CSRF. Xem [runbook](../backend/runbook.md). [Contract](../../contracts/openapi.proposed.yaml).

Register full_name 1–100 trim, email <=254 trim/lowercase toàn địa chỉ (không provider dot/plus),
password 12–128 ký tự Unicode không trim/normalize/cắt ngắn. Server gán USER/ACTIVE. Password
Argon2id salt16/hash32, parallelism1, memory65536KiB, iterations3; cần benchmark production. MVP chưa đổi email. CLI profile provision-admin tạo account ADMIN mới bằng password nhập ẩn, không credentials mặc định.

Access/JWT 15 phút, refresh family tối đa 30 ngày từ login, không kéo dài khi rotate.
Mỗi REST request kiểm tra sid/account ACTIVE và family chưa revoked; không chỉ kiểm chữ ký.
Nếu nguồn trạng thái bắt buộc không truy cập được, REST fail closed, cảnh báo ESP32 vẫn độc lập.

Refresh entropy cao chỉ lưu digest/HMAC, mỗi generation một auth_sessions row. Rotate
atomically tiêu thụ token cũ, thêm generation cùng family/absolute expiry. Reuse token đã
rotate →401 và revoke family. Client single-flight; mất response rotate có thể phải login
lại, không retry old token vô hạn. GET sessions trả một logical family, id là family UUID,
không token/hash. DELETE revoke mọi generation của own family. Logout dùng refresh credential
để không cần access còn hạn; retry credential retired còn nhận diện được 204, unknown 401.

Forgot-password202 đưa tác vụ vào queue bounded trước lookup, body chung cho email hợp lệ;
rate limit configurable, không lộ trạng thái khóa. SMTP worker gửi code, lỗi rollback token mới và log chung; queue không durable qua crash. Reset token single-use 15 phút, hash,
atomic password update+revoke tất cả family; không unlock account. Change-password cần
current_password, revoke các family khác; sai 400 CURRENT_PASSWORD_INVALID. Account lock
revoke tất cả ngay, unlock không hồi sinh. Cấm admin tự khóa và khóa admin ACTIVE cuối; advisory transaction lock bảo vệ đồng thời. Recovery qua CLI/DBA có chủ đích.

## Transport và CSRF

X-Client-Type chọn mobile/admin_web ở login/refresh/logout, không là proof client hay role.
Mobile response access_token/refresh_token/token_type=Bearer/expires_in=900/user; refresh/
logout body refresh_token, bỏ qua cookie, không phát Set-Cookie. Token Android dùng Keystore-
backed storage, không log/plaintext/backup. Bearer access cho API nghiệp vụ.

Web backend kiểm tra ADMIN; JSON không trả refresh_token. Cookie rg_refresh HttpOnly/Secure,
Path=/api/v1/auth, host-only, access ở memory không persistent localStorage. Refresh/logout
body `{}` và cookie. Logout clear cookie cùng Path/Domain policy. Token responses no-store.
OpenAPI oneOf/examples/Set-Cookie thể hiện khác biệt. Empty security alternative cho mobile
refresh/logout là body credential bắt buộc, không anonymous; xem x-token-transport.

Mặc định same-site HTTPS, SameSite=Lax; domain/topology chưa chốt. Cross-site nếu cần
SameSite=None; Secure, không giả định third-party cookies luôn hoạt động. Web login/
refresh/logout chỉ nhận application/json, Origin thuộc exact allowlist và header
X-CSRF-Protection:1. Non-simple header+preflight+strict Origin là cơ chế CSRF hiện có,
không phải token bí mật. Missing/null/untrusted Origin bị từ chối ở web branch. Không
wildcard credentialed CORS. CRUD không dùng cookie refresh để tự authenticate; dùng Bearer.
Đổi header sang mobile không cho dùng cookie: body refresh hợp lệ vẫn bắt buộc.

Cấu hình origin chính xác qua môi trường; method/header allowlist gồm Authorization,Content-Type,
X-Client-Type,X-CSRF-Protection,Idempotency-Key; xử lý preflight trước auth hợp lý. HTTP slice tests đã chạy, browser/deployment thật chưa kiểm chứng. Không token trong URL/log.
