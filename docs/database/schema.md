# Thiết kế database — PROPOSED

Chưa tạo entity/migration nghiệp vụ. Migration V1 chỉ tạo schema `rideguard`;
Flyway lưu lịch sử ở `public.flyway_schema_history`. Các bảng nghiệp vụ tương lai
đặt trong `rideguard`. UUID là khóa chính; thời gian dùng `timestamptz` UTC.
Mọi cột dưới đây NOT NULL trừ khi ghi nullable. Không dùng 0 thay cho dữ liệu thiếu.

## 1. users

`id uuid PK`, `email varchar(254)`, `password_hash text`, `role varchar(10)` default
USER, `status varchar(10)` default ACTIVE, `created_at`, `updated_at timestamptz`.
Unique index trên `lower(email)` sau chuẩn hóa; CHECK email không rỗng, role IN
(USER, ADMIN), status IN (ACTIVE, LOCKED). Server gán USER khi đăng ký, không nhận role
từ client. Password dùng adaptive hash (Argon2id với tham số đo trên server; không
SHA256 thuần). Chính sách xóa tài khoản/anonymization cần chốt trước triển khai.

## 2. auth_sessions

`id uuid PK`, `user_id uuid FK users ON DELETE CASCADE`, `refresh_token_hash text UNIQUE`,
`created_at`, `expires_at timestamptz`, `revoked_at timestamptz nullable`,
`replaced_by_session_id uuid nullable FK auth_sessions ON DELETE SET NULL`.
CHECK expires_at > created_at, revoked_at >= created_at nếu có, không tự thay thế chính nó.
Index (user_id, expires_at), partial index user_id WHERE revoked_at IS NULL.
Token ngẫu nhiên entropy cao, chỉ lưu digest/HMAC phù hợp; rotate atomically, phát hiện
tái dùng và thu hồi token family theo chính sách auth. Trạng thái active/revoked/expired
suy ra từ thời điểm, không lưu cờ mâu thuẫn.

## 3. password_reset_tokens

`id uuid PK`, `user_id uuid FK users ON DELETE CASCADE`, `token_hash text UNIQUE`,
`created_at`, `expires_at timestamptz`, `used_at timestamptz nullable`.
CHECK expires_at > created_at, used_at >= created_at. Index (user_id, expires_at).
Token entropy cao, digest/HMAC; chỉ sử dụng một lần trong transaction, phản hồi không
tiết lộ email tồn tại, không ghi token vào log. Dọn token hết hạn theo retention.

## 4. devices

`id uuid PK`, `serial varchar(128) UNIQUE`, `radar_model varchar(16) nullable`,
`status varchar(16)` default ACTIVE, `created_at timestamptz`.
CHECK serial trim không rỗng; serial chuẩn hóa có quy tắc duy nhất trước ghi;
radar_model IN (LD2451, LD2417) nếu có; status IN (ACTIVE, DISABLED, RETIRED).
Không tự thêm thiết bị chỉ vì client gửi serial lạ. Không xóa cứng thiết bị còn lịch sử binding.

## 5. device_bindings

`id uuid PK`, `device_id uuid FK devices ON DELETE RESTRICT`,
`user_id uuid FK users ON DELETE RESTRICT`, `bound_at timestamptz`,
`unbound_at timestamptz nullable`. CHECK unbound_at >= bound_at.
UNIQUE partial index (device_id) WHERE unbound_at IS NULL đảm bảo một chủ đang hoạt động.
Index (user_id, bound_at DESC, id), (device_id, bound_at DESC).
Một user có nhiều binding; trạng thái active/unbound suy ra từ unbound_at.
Bind phải transaction + khóa bản ghi device + unique constraint để chống race.
Unbind chỉ đóng binding của user hiện tại. Không ghi đè chủ sở hữu.
MVP serial không chứng minh sở hữu vật lý: có nguy cơ đoán/lộ serial, chiếm trước;
rate-limit, audit, quy trình hỗ trợ tranh chấp; không có bảng device_claim_requests.

## 6. device_configs

`id uuid PK`, `binding_id uuid UNIQUE FK device_bindings ON DELETE RESTRICT`,
`schema_version integer`, `desired_version bigint`, `reported_applied_version bigint nullable`,
`settings jsonb`, `updated_at timestamptz`, `reported_at timestamptz nullable`.
CHECK schema_version > 0, desired_version >= 1; applied >= 1 AND applied <= desired
nếu có; jsonb_typeof(settings) = 'object'; reported_at null iff applied null.
Application validates [schema](../../contracts/device-config.schema.json), semantic
compatibility and monotonic ACK for the same binding. Version grows atomically via
optimistic concurrency. New owner gets a NEW config with no inherited settings/ACK.
Saving desired is not proof that hardware applied it. Firmware settings remain empty
until validated hardware parameters and limits are defined.

## 7. feedbacks

`id uuid PK`, `user_id uuid FK users ON DELETE RESTRICT`,
`binding_id uuid nullable FK device_bindings ON DELETE RESTRICT`, `body text`,
`status varchar(16)` default OPEN, `created_at`, `updated_at timestamptz`.
CHECK length(trim(body)) BETWEEN 1 AND 5000; status IN (OPEN, IN_REVIEW, RESOLVED, CLOSED).
Index (user_id, created_at DESC, id), (status, created_at, id).
Service verifies binding belongs to feedback author (FK alone cannot ensure this).
Feedback never automatically attaches GPS or raw radar. Retention and deletion need policy.

## 8. trips

`id uuid PK` (mobile may generate offline), `user_id uuid FK users ON DELETE RESTRICT`,
`device_id uuid nullable FK devices ON DELETE SET NULL`, `title varchar(160) nullable`,
`status varchar(16)` default RECORDING, `started_at timestamptz`,
`ended_at timestamptz nullable`, `distance_estimate_m double precision nullable`,
`created_at`, `updated_at timestamptz`, `version bigint` default 1.
CHECK status IN (RECORDING, COMPLETED, ABORTED), version >= 1; ended_at >= started_at;
RECORDING requires ended_at null; terminal states require ended_at;
distance finite and >= 0 if present. Index (user_id, started_at DESC, id), device_id.
Owner immutable. Optional device association must be authorized at creation/sync;
offline uploads after unbind require binding history matching user and recording time,
not current ownership alone. Ambiguous cases reject device association or require user
confirmation to upload without device. Trip remains owned by creator after unbinding.
Distance is an estimate after accuracy/outlier/time-gap filtering; not raw GPS jitter sum.

## 9. trip_points

`trip_id uuid FK trips ON DELETE CASCADE`, `sequence_number bigint`,
`recorded_at timestamptz`, `latitude double precision`, `longitude double precision`,
`accuracy_m double precision`, `speed_mps double precision nullable`,
`bearing_deg double precision nullable`. PK (trip_id, sequence_number) supplies UNIQUE
(trip_id, sequence_number). CHECK sequence_number >= 0, latitude [-90,90], longitude
[-180,180], accuracy_m >= 0, speed_mps >= 0 if present, bearing_deg [0,360) if present;
all numbers must be finite (explicitly reject NaN/infinity at API and DB boundary).
Index (trip_id, recorded_at, sequence_number) for ordered temporal reads.
Identical retry is accepted, same sequence with different payload is 409; do not overwrite.
Sequence may have gaps; loss is explicit. recorded_at order inconsistencies are flagged,
not silently rewritten. Batch limits and ownership enforced before insert transaction.

## Privacy and integrity

Every trip/point query is scoped to authenticated user, including UUID lookups, batch,
rename, complete and delete. ADMIN is not an override to view personal locations.
Deleting a trip cascades points; backups follow documented retention, not instant erasure.
User account deletion and legal retention remain design decisions, not implemented actions.
Index FKs used for joins/deletion; bounds and finite values must be covered by migration
tests when these proposed tables are implemented. No business table exists yet.
