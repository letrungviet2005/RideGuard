# Authentication — proposed MVP

Chưa có JWT/auth controller/admin mặc định. Security implemented chỉ GET health, deny
nghiệp vụ và bật CSRF; không thay code. [Contract](../../contracts/openapi.proposed.yaml).

Register full_name 1–100 trim, email <=254 trim/lowercase toàn địa chỉ (không provider dot/plus),
password 12–128 ký tự Unicode không trim/normalize/cắt ngắn. Server gán USER/ACTIVE. Password
adaptive Argon2id với tham số đo khi triển khai. MVP chưa đổi email. ADMIN provisioning/
recovery cần quyết định riêng, không credentials mặc định.

Access/JWT 15 phút, refresh family tối đa 30 ngày từ login, không kéo dài khi rotate.
Mỗi REST request kiểm tra sid/account ACTIVE và family chưa revoked; không chỉ kiểm chữ ký.
Nếu nguồn trạng thái bắt buộc không truy cập được, REST fail closed, cảnh báo ESP32 vẫn độc lập.

Refresh entropy cao chỉ lưu digest/HMAC, mỗi generation một auth_sessions row. Rotate
atomically tiêu thụ token cũ, thêm generation cùng family/absolute expiry. Reuse token đã
rotate →401 và revoke family. Client single-flight; mất response rotate có thể phải login
lại, không retry old token vô hạn. GET sessions trả một logical family, id là family UUID,
không token/hash. DELETE revoke mọi generation của own family. Logout dùng refresh credential
để không cần access còn hạn; retry credential retired còn nhận diện được 204, unknown 401.

Forgot-password 202 thông điệp/timing chung cho valid email có hoặc không có account;
rate limit configurable, không lộ trạng thái khóa. Reset token single-use 15 phút, hash,
atomic password update+revoke tất cả family; không unlock account. Change-password cần
current_password, revoke các family khác; sai 400 CURRENT_PASSWORD_INVALID. Account lock
revoke tất cả ngay, unlock không hồi sinh. Admin tự khóa được trong proposed hiện tại;
last-admin/recovery policy cần duyệt.

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

Đề xuất same-site HTTPS, SameSite=Lax; domain/topology chưa chốt. Cross-site nếu cần
SameSite=None; Secure, không giả định third-party cookies luôn hoạt động. Web login/
refresh/logout chỉ nhận application/json, Origin thuộc exact allowlist và header
X-CSRF-Protection:1. Non-simple header+preflight+strict Origin là cơ chế CSRF đề xuất,
không phải token bí mật. Missing/null/untrusted Origin bị từ chối ở web branch. Không
wildcard credentialed CORS. CRUD không dùng cookie refresh để tự authenticate; dùng Bearer.
Đổi header sang mobile không cho dùng cookie: body refresh hợp lệ vẫn bắt buộc.

Khi triển khai phải chốt origin/method/header allowlist gồm Authorization,Content-Type,
X-Client-Type,X-CSRF-Protection,Idempotency-Key; xử lý preflight trước auth hợp lý. Chưa
claim CORS/SameSite/CSRF proposed đã cấu hình an toàn. Không token trong URL/log.
