# REST MVP — đã có integration và runtime smoke

Nguồn chuẩn: [openapi.yaml](../../contracts/openapi.yaml), 31 nghiệp vụ cùng 3 health;
[bản MVP đối chiếu](../../contracts/openapi.proposed.yaml) giữ riêng 31 operations.
Ngày 2026-11-09 đã chạy integration PostgreSQL và HTTP smoke; trạng thái
`integration-and-runtime-smoke-tested` ghi phạm vi/status thực tế, không mọi nhánh lỗi. Ví dụ là minh họa,
không là dữ liệu thật/kiểm chứng phần cứng. [ADR0003](../decisions/0003-backend-mvp.md),
[runbook](../backend/runbook.md), [verification](../testing/verification-status.md).

## Quy ước

Base `/api/v1`, JSON UTF-8, snake_case, UUID, RFC 3339 UTC hậu tố Z. Null khi chưa có dữ
liệu, không dùng 0 thay thiếu. Schema xác định required/nullable/readOnly/writeOnly;
unknown request properties bị từ chối. Chuẩn hóa các chuỗi được quy định trước validation;
password không bao giờ trim/normalize/truncate. Không nhận user_id/role/status ở self-service.

Đối tượng trả trực tiếp; list `{items,next_cursor}`, default 20 / max 100. Cursor kiểm tra tính
toàn vẹn, gắn caller/route/filter/sort; sai/giả mạo trả 400. Sort thiết bị/binding theo
bound_at DESC và binding UUID DESC; list còn lại created_at DESC/id DESC, session dùng
family_id ổn định. Không offset; không cam kết snapshot isolation giữa nhiều trang.
Body mutation tối đa 32 KiB decoded UTF-8; quá mức 413. Rate limit per-IP/process cấu hình được, mặc định120/phút; không là limiter phân tán.

Error hiện tại `{status,code,message,request_id}`: request_id UUID do server sinh. 400
validation/cursor;401 token/session bị thu hồi/hết hạn/account khóa;403 thiếu ADMIN hoặc
Origin/CSRF sai;404 tài nguyên riêng tư không có/không thuộc caller;409 conflict;413 size;
429 Retry-After;500 lỗi chung;503 tạm thời. Không lộ SQL/stack trace/chủ khác. Error code hiện có request_id server sinh, header X-Request-ID; không log body auth/secret.

## API MVP (mọi path có tiền tố /api/v1)

- Auth: POST `/auth/register`, `/auth/login`, `/auth/refresh`, `/auth/logout`,
  `/auth/forgot-password`, `/auth/reset-password`.
- Self: GET/PATCH `/users/me`; POST `/users/me/change-password`; GET `/users/me/sessions`;
  DELETE `/users/me/sessions/{session_id}`.
- Devices: GET `/devices`; GET/PATCH `/devices/{device_id}`; POST `/devices/bind`;
  DELETE `/devices/{device_id}/binding`; GET `/device-bindings`.
- Config: GET/PATCH `/devices/{device_id}/config`; POST `/devices/{device_id}/config/ack`.
- Reports: POST `/devices/{device_id}/reports`.
- Feedback: GET/POST `/feedbacks`; GET `/feedbacks/{feedback_id}`.
- Admin: GET `/admin/users`; PATCH `/admin/users/{user_id}/status`; GET/POST `/admin/devices`;
  PATCH `/admin/devices/{device_id}/status`; GET `/admin/feedbacks`;
  PATCH `/admin/feedbacks/{feedback_id}`.

## Auth, account và transport

full_name 1–100 sau trim; email tối đa 254, trim/lowercase toàn bộ, không provider-specific
dot/plus rewriting; password 12–128 Unicode characters nguyên trạng. Register gán USER/ACTIVE,
201 User, không auto-login; duplicate email 409 EMAIL_UNAVAILABLE. MVP không đổi email.
GET sessions không trả token/hash; session_id là family UUID ổn định qua refresh rotation.
Reset token một lần 15 phút; forgot-password 202 chung; reset revoke tất cả phiên; đổi mật
khẩu với current_password revoke các phiên khác. Không có admin/password mặc định.

JWT access 15 phút, refresh family tối đa 30 ngày từ login, không sliding. Kiểm
tra session/account mỗi request, không chỉ chữ ký JWT. X-Client-Type=mobile/admin_web bắt
buộc ở login/refresh/logout. Mobile nhận access_token/refresh_token/token_type/expires_in/user;
refresh/logout body có refresh_token, không dùng cookie. Web trả cùng thông tin trừ refresh
token; refresh qua cookie HttpOnly/Secure, body `{}`, access giữ memory, không localStorage.
Web branch cần ADMIN; header chọn transport không tự cấp quyền.

OpenAPI có oneOf/examples hai client và Set-Cookie. Refresh/logout `{}` security alternative
chỉ là biểu diễn **mobile body-auth** mà OpenAPI không có securityScheme tương ứng, không
cho anonymous access; xem x-authentication-required/x-token-transport. CORS/SameSite/CSRF
đã có code và HTTP slice tests; exact origins do môi trường cấp, triển khai thật cần kiểm chứng. [Authentication flow](../flows/authentication.md). Admin tự khóa/khóa admin ACTIVE cuối bị409.

## Devices, configuration, reporting

Serial preregistered, canonical trim/uppercase ASCII,1–128 A–Z/0–9/gạch ngang, bắt đầu chữ/số;
không mật khẩu/proof. Bind request serial_number + nickname optional, Idempotency-Key UUID.
ACTIVE chưa owner 201; đã thuộc caller/replay hợp lệ 200, nickname không tự đổi; owner khác
409 DEVICE_UNAVAILABLE; serial lạ/DISABLED/RETIRED 404 DEVICE_NOT_AVAILABLE. Replay binding
đã đóng 409 BINDING_NO_LONGER_ACTIVE, không bind lại. Transaction/row lock/partial unique.
Unbind giữ lịch sử; query expected_binding_id UUID bắt buộc. Chỉ đóng binding đúng
caller/device; retry bản đã đóng204 và không tác động binding mới. Sai caller/device404. PATCH chỉ nickname 1–60 sau trim hoặc null để xóa. Device DTO có id/binding_id/
serial_number/nickname/model/hardware_revision/status/latest_report. [Binding flow](../flows/device-binding.md).

Config theo binding, schema_version 1, desired bắt đầu 1/applied=null; PATCH expected_version
và settings không rỗng, không If-Match. Omitted giữ nguyên, null/unknown bị từ chối.
brightness integer 0–100/vibration boolean; không ngưỡng risk/master disable. vibration_enabled=false
tắt cơ cấu rung; HUD và xử lý nguy cơ vẫn hoạt động. Không chỉnh threshold AI/master-disable.
Wrong version 409 VERSION_CONFLICT. ACK binding_id/applied phải là active owned binding và
issued version<=desired; ACK cũ/lặp thành công, không giảm applied. ACK chỉ mobile báo lại,
không attestation. Không sửa offline; server lưu nhưng chưa BLE ACK → chờ đồng bộ.
[Configuration flow](../flows/configuration-sync.md).

Report gửi binding_id/observed_at/firmware_version/radar_status/imu_status/display_status/
vibration_status/battery_percent; required, firmware/pin unknown = null. Enum OK/ERROR/
UNAVAILABLE/UNKNOWN; firmware tối đa 64; pin integer 0–100 hoặc null. observed_at trong
[received_at−24h, received_at+5min], ngoài 400 OBSERVED_AT_OUT_OF_RANGE; cũ/bằng latest→200
accepted=false, mới→true. received_at server sinh. Không online realtime, không raw
radar/IMU/GPS, không suy OK từ BLE. [Status flow](../flows/device-status-reporting.md).

## Feedback/admin

Create feedback yêu cầu key; device_id nullable nhưng phải owned lúc tạo nếu có, category
BUG/SUGGESTION/OTHER, content 1–5000 trim, không attachment. Response id/device_id/category/
content/status/admin_response/created_at/responded_at; hai trường response/time nullable.
User chỉ xem own feedback kể cả sau unbind, không tự đặt status/response. OPEN/IN_PROGRESS/
RESOLVED; admin có thể reopen. Admin patch chỉ status/response; omitted giữ nguyên, null
bị từ chối, server đặt responded_at khi response đổi. Self lists filter status/category/device_id;
admin bổ sung user_id. Không chuyển tác giả/device/context.

ADMIN bắt buộc backend. Users filter q/name/email,role,status; khóa account revoke mọi phiên,
unlock không hồi sinh. Device register serial/model/hardware_revision optional nullable,
status mặc địnhACTIVE; trùng serial 409. Device filters q/serial/model,status; status
ACTIVE↔DISABLED, RETIRED terminal, reversal 409 INVALID_STATUS_TRANSITION. Registry status
không tắt ESP32. Không chuyển owner hoặc API location/trip/challenge/inference.

## Idempotency và retry

Bind/create feedback: scope user+method+route+UUID key, hash canonical normalized validated
payload. Same key/payload không tạo mới; khác 409 IDEMPOTENCY_CONFLICT; in-progress 409
IDEMPOTENCY_IN_PROGRESS. Unique scope + resource writes + success record cùng transaction.
Replay 200 snapshot đã ghi sau kiểm tra lại quyền/trạng thái; GET để lấy dữ liệu mới nhất.
Feedback replay kiểm tra own feedback, không đòi binding cũ còn active.

Full response cache 7 ngày sau thành công. Để retry bind cũ không tạo lại sau expiry, giữ
minimal key/hash/resource tombstone trong đời binding/feedback; expired key 409
IDEMPOTENCY_KEY_EXPIRED, riêng binding đóng ưu tiên BINDING_NO_LONGER_ACTIVE. Không xóa marker
rồi coi retry như thao tác mới. Code giữ marker, cleanup chỉ xóa response hết7 ngày; account erasure vẫn cần chính sách. DB constraints vẫn bắt buộc.

Retry bounded/backoff/jitter cho network/429/503/5xx tạm thời, tôn trọng Retry-After, cùng
key cho cùng thao tác. Refresh single-flight một lần cho chuỗi access hết hạn; session
revoked/locked thì login lại. Không loop 400/403/409, không tự thay expected_version để
overwrite config. Không queue đồng bộ hành trình MVP. [DB](../database/schema.md),
[roadmap deferred](../roadmap.md).
