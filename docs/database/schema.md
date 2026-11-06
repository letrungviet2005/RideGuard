# Database MVP — proposed, chưa có migration nghiệp vụ

Nguồn API: [OpenAPI proposed](../../contracts/openapi.proposed.yaml). Thiết kế gồm **7 bảng
nghiệp vụ + 3 bảng kỹ thuật = 10 bảng**, không phải chỉ bảy bảng. Migration implemented V1
chỉ tạo schema rideguard, Flyway history ở public. Không sửa/chạy migration trong cập nhật này.
Không có trips, trip_points hoặc device_claim_requests trong schema MVP; xem [deferred](../roadmap.md).

Mọi cột bên dưới NOT NULL trừ khi ghi nullable; ID UUID, timestamp timestamptz (serialize
RFC 3339 UTC Z). PK/unique tạo index tương ứng; FKs cần index truy vấn. Không dùng 0 thay null.
Các CHECK/transaction sau là đề xuất cho migration tương lai, chưa được DB thực thi.

## Bảng nghiệp vụ

### 1. users

id UUID PK; email varchar(254); full_name varchar(100); password_hash text; role varchar(10)
default USER; status varchar(10) default ACTIVE; created_at/updated_at timestamptz.
Canonical email trim/lowercase toàn bộ (không provider dot/plus), unique email; CHECK email
nonblank/max 254, full_name trim length 1–100, role USER/ADMIN, status ACTIVE/LOCKED,
updated_at>=created_at. Index(status,created_at DESC,id DESC), role nếu filter thực tế cần.
Search q tên/email case-insensitive; MVP bounded list có thể scan, chưa thêm extension
index trước khi đo. Password 12–128 ký tự nguyên trạng, Argon2id adaptive hash; DB không lưu
plaintext. Server gán USER; client không gán role; MVP không đổi email. Khóa user revoke
sessions cùng transaction; nguồn auth phải phản ánh lock ngay. Xóa account/retention cần chốt;
chưa cung cấp delete API, mặc định RESTRICT các nghiệp vụ cần lịch sử.

### 2. auth_sessions

Mỗi row là một refresh generation, không phải một thiết bị/session mới hiển thị trên UI.
id UUID PK; family_id UUID FK auth_sessions(id) ON DELETE RESTRICT DEFERRABLE INITIALLY
DEFERRED; user_id UUID FK users RESTRICT; generation integer >=0; refresh_token_hash text
UNIQUE; client_type varchar(16) mobile/admin_web; client_name varchar(100) nullable;
created_at/last_seen_at/expires_at timestamptz; consumed_at/revoked_at timestamptz nullable;
replaced_by_session_id UUID nullable FK auth_sessions RESTRICT.

Root generation 0 có family_id=id, created_at là login time, expires_at<=created_at+30 ngày;
các generation sau cùng user/family/client_type/absolute expiry. UNIQUE(family_id,generation),
partial UNIQUE(family_id) WHERE consumed_at IS NULL AND revoked_at IS NULL cho live leaf.
Index(user_id,family_id), (user_id,created_at DESC,id DESC), expires_at. CHECK expires_at>
created_at, last_seen_at>=created_at, consumed/revoked>=created_at nếu có, replacement!=id.
Service/constraint-trigger tương lai bảo đảm family root tồn tại/cùng user, generation tăng,
không cycle replacement; CHECK đơn giản không kiểm cross-row được.

Refresh transaction khóa family/root, consume leaf rồi insert generation mới/link replacement.
Giữ old hashes để phát hiện replay; rotate token cũ bị reuse → revoke mọi generation family.
Hash cho opaque high-entropy token dùng digest/HMAC phù hợp, không lưu raw. Access sid=family_id;
check root/user/active leaf mỗi request để revoke/lock có hiệu lực trước JWT expiry 15 phút.
GET sessions trả id=family_id, created_at của root, last_seen_at của current leaf,
expires_at tuyệt đối, client metadata, is_current từ access sid; không token/hash. Query
dedup family; partial index tránh hai current leaves. Revoke và rotate dùng cùng lock order.
Old generations giữ ít nhất hết lifetime family + cửa sổ vận hành đã duyệt; không xóa root
trước descendants. Không dùng cookie/response snapshot lưu refresh plaintext trong DB.

### 3. password_reset_tokens

id UUID PK; user_id UUID FK users RESTRICT; token_hash text UNIQUE; created_at/expires_at
timestamptz; used_at nullable timestamptz. CHECK expires_at>created_at và <=created_at+15 phút,
used_at>=created_at khi có; index(user_id,expires_at), expires_at. Token entropy cao, hash,
consume once transaction + update password + revoke mọi family; không unlock user. Forgot
API không tiết lộ tồn tại email. Issuing reset mới vô hiệu các unused reset cũ trong cùng
transaction (consume/invalidate theo chính sách; used_at ghi thời điểm token không còn dùng).

### 4. devices

id UUID PK; serial_number varchar(128) UNIQUE; model varchar(100); hardware_revision
varchar(64) nullable; status varchar(16) default ACTIVE; created_at/updated_at timestamptz.
CHECK serial canonical regex `^[A-Z 0-9][A-Z 0-9-]{0,127}$`, model trim length 1–100, revision
trim length 1–64 nếu có; status ACTIVE/DISABLED/RETIRED, updated_at>=created_at. Index(status,
created_at DESC,id DESC). Serial preregistered, không secret/proof. Không xóa device có binding.
ACTIVE↔DISABLED, chuyển RETIRED terminal do service enforce dưới row lock; repeat status
no-op, reversal conflict. Không chuyển owner, không lệnh tắt cảnh báo ESP32. DISABLED/RETIRED
chặn bind và config edits, không xóa binding/history hoặc chặn unbind/report/issued ACK hợp lệ.

### 5. device_bindings

id UUID PK; device_id UUID FK devices RESTRICT; user_id UUID FK users RESTRICT;
nickname varchar(60) nullable; bound_at timestamptz; unbound_at nullable timestamptz.
CHECK unbound_at>=bound_at nếu có, nickname trim 1–60 hoặc null. Partial UNIQUE(device_id)
WHERE unbound_at IS NULL; index(user_id,bound_at DESC,id DESC),(device_id,bound_at DESC,id DESC).
UNIQUE(id,device_id,user_id) cho composite FK feedback. Status ACTIVE/UNBOUND derived,
không lưu cờ gây mâu thuẫn. Nickname thuộc lần sở hữu, không devices; PATCH không đổi lịch sử
owner. Bind/unbind và registry change serialize bằng device row; report/ACK khóa binding
sau device khi cần, dùng một lock order thống nhất.

Caller có nhiều binding/devices; chỉ một active owner/device. Active own bind 200 không đổi
nickname; new 201; foreign 409; unavailable 404. Unbind ghi thời gian, không delete. Serial-only
không proof vật lý. Stale unbind cùng user sau rebind là giới hạn route MVP, xem
[flow](../flows/device-binding.md). Lịch sử liên kết giữ nguyên, không phải lịch sử hành trình.

### 6. device_configs

binding_id UUID PK/FK device_bindings RESTRICT; schema_version integer=1;
desired_version bigint>=1; reported_applied_version bigint nullable>=1;
settings jsonb; updated_at timestamptz; reported_at nullable timestamptz.
Không cần config UUID riêng vì API nhận diện theo binding. Version<=9007199254740991 để
JSON/JavaScript bảo toàn số; CHECK applied<=desired; reported_at null iff applied null.
settings object exactly display_brightness integer 0–100, vibration_enabled boolean,
không null/unknown keys; validate [schema](../../contracts/device-config.schema.json).
Không risk thresholds/master-disable. Preference rung không vô hiệu core alerts; priority
cần duyệt. New binding defaults proposed 70/true, desired 1/applied null.

Composite FK(binding_id,desired_version) và (binding_id,reported_applied_version) →
device_config_versions(binding_id,version), ON DELETE RESTRICT, DEFERRABLE INITIALLY DEFERRED;
nullable applied sử dụng MATCH SIMPLE. FK chứng minh issued version cùng binding. Snapshot
settings/schema phải bằng issued desired row: transaction service/constraint trigger, không
claim FK tự kiểm JSON equality. CAS expected_version + merge + increment + insert issued
snapshot atomic. ACK update max giữ monotonic; reported_at chỉ đổi khi applied tiến lên.
Binding inactive không nhận edit/ACK; new owner không thấy config/ACK cũ.

### 7. feedbacks

id UUID PK; user_id UUID FK users RESTRICT; device_id UUID nullable FK devices RESTRICT;
binding_id UUID nullable FK device_bindings RESTRICT; category varchar(16); content text;
status varchar(16) default OPEN; admin_response text nullable; responded_at timestamptz
nullable; responded_by UUID nullable FK users RESTRICT; created_at/updated_at timestamptz.
CHECK category BUG/SUGGESTION/OTHER; status OPEN/IN_PROGRESS/RESOLVED; trimmed content 1–5000,
admin_response trim 1–5000 nếu có; response/time/responded_by cùng null hoặc cùng nonnull;
responded_at>=created_at, updated_at>=created_at; device/binding cùng null hoặc cùng nonnull.
Composite FK(binding_id,device_id,user_id)→device_bindings(id,device_id,user_id) RESTRICT
(MATCH SIMPLE) bổ sung tính nhất quán tác giả/context. Service kiểm active ownership lúc tạo
dưới lock; sau unbind user vẫn xem own feedback, không chuyển cho chủ mới. Admin có thể
reply/reopen, không sửa user/device/content; updated/responded_at server sinh. Self DTO
không trả user_id/binding_id/responded_by; admin DTO thêm user_id/binding_id nhưng không secret.
Index(user_id,created_at DESC,id DESC),(status,created_at DESC,id DESC),(device_id), binding_id,
responded_by; category composite index chỉ thêm nếu query cần. Không attachment/GPS/raw radar.

## Bảng kỹ thuật

### 8. idempotency_records

id UUID PK; user_id UUID FK users RESTRICT; method varchar(8); route varchar(128);
idempotency_key UUID; request_hash text; resource_type varchar(16); binding_id UUID nullable
FK device_bindings RESTRICT; feedback_id UUID nullable FK feedbacks RESTRICT;
response_status smallint nullable; response_body jsonb nullable; created_at timestamptz;
completed_at/response_expires_at timestamptz nullable; state varchar(16).
UNIQUE(user_id,method,route,idempotency_key); method=POST; route chỉ /api/v1/devices/bind hoặc
/api/v1/feedbacks, resource_type BINDING/FEEDBACK tương ứng. state IN_PROGRESS/COMPLETED/
EXPIRED; CHECK final state đúng một target FK phù hợp route/type, IN_PROGRESS có thể chưa
có resource; completed_at>=created_at, expires=completed_at+7 ngày. response_status 200/201,
response_body object khi COMPLETED; EXPIRED giữ key/hash/target nhưng body null. Index
response_expires_at, binding_id, feedback_id. Không lưu credentials/token/body auth.

Canonical JSON hash sau validate/normalize (password không liên quan hai endpoint này),
unique scope bảo vệ concurrency. Reserve/write resource/result cùng transaction; lỗi
rollback không để success giả. Nếu implement IN_PROGRESS visible cần lease/recovery
và không cho double execution; baseline serialize DB transaction, lock contention có
thể 409 IDEMPOTENCY_IN_PROGRESS. Replay kiểm account/quyền/resource state trước cached body,
trả 200; key khác hash 409. Binding đóng ưu tiên 409 BINDING_NO_LONGER_ACTIVE.

Full response retention 7 ngày. **Minimal tombstone còn trong đời binding/feedback** để
key cũ không bị coi là thao tác mới sau cache expiry; hết cache 409 IDEMPOTENCY_KEY_EXPIRED,
không recreate. Chính sách erase marker cùng account/resource cần duyệt. Không thể vừa
xóa sạch key sau 7 ngày vừa hứa phát hiện mọi stale bind; thiết kế chọn giữ marker rõ ràng.

### 9. device_reports

id UUID PK; binding_id UUID FK device_bindings RESTRICT; observed_at/received_at timestamptz;
firmware_version varchar(64) nullable; radar_status/imu_status/display_status/vibration_status
varchar(16); battery_percent smallint nullable; accepted boolean. Status CHECK OK/ERROR/
UNAVAILABLE/UNKNOWN; battery 0–100 hoặc null; firmware trim 1–64 hoặc null. CHECK observed_at
BETWEEN received_at−24h AND received_at+5min (hằng số policy proposed). received_at do server,
không caller. Partial UNIQUE(binding_id,observed_at) WHERE accepted; index(binding_id,
observed_at DESC,id DESC) WHERE accepted, index(received_at), binding_id cho FK.

Binding lock serialize report/unbind; accepted true chỉ khi observed_at mới hơn latest
accepted của chính binding. Cross-row monotonic cần transaction, unique chỉ chống equal,
không tự chặn stale. Valid stale/equal lưu accepted=false, response 200 cùng received_at
của lần đó. GET own Device.latest_report chỉ latest accepted, không data binding cũ.
Retention proposed 30 ngày, không realtime online hoặc hardware attestation. Không radar/
IMU raw/GPS. Metadata firmware/pin unknown null. Index/retention cần đo tần suất trước launch.

### 10. device_config_versions

binding_id UUID FK device_bindings RESTRICT; version bigint; schema_version integer=1;
settings jsonb; issued_at timestamptz; PK(binding_id,version). version 1..9007199254740991,
settings full schema như device_configs. Rows immutable; new binding insert v1, mỗi CAS
insert version tiếp theo trong transaction cùng snapshot. Giữ trong đời binding, không
delete version đang FK tham chiếu. Issued history chứng minh ACK hợp lệ, không chứng minh
phần cứng apply; reported ACK vẫn do mobile cung cấp.

## Quyền, retention và ánh xạ API

Self queries scope authenticated user, không nhận user_id. Config/report từ active binding;
feedback từ author kể cả sau unbind; binding history chỉ các row caller. ADMIN enforced
backend cho routes admin, không nâng quyền từ client; DTO không trả password_hash/token_hash/
hardware secret. device_id là devices.id; binding_id là device_bindings.id; session_id là
auth_sessions.family_id. User/status update có dữ liệu tương ứng; report receipt maps
accepted/received_at; feedback response nullable thời điểm đúng admin_response.

Retention token family, reset hash, config versions, binding history và minimal idempotency
markers cần chính sách trước production; report 30 ngày/cache response 7 ngày là proposed.
Không xóa account/device/binding tự động vì có lịch sử. Không migration/entity nghiệp vụ
được tạo/chạy trong nhiệm vụ này. [ERD](erd.md) mô tả thiết kế, không deployed database.
