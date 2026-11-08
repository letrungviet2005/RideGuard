# Device binding — code MVP, PostgreSQL integration chưa kiểm chứng

[REST](../../contracts/openapi.proposed.yaml), [DB](../database/schema.md). Serial-only cho
nguyên mẫu; không proof sở hữu vật lý, không challenge BLE trong MVP.

1. Authenticated ACTIVE caller gửi serial_number/nickname optional, key UUID. Normalize
   serial trim/uppercase ASCII trước validate/hash; nickname trim 1–60 hoặc null.
2. Transaction unique idempotency scope, khóa device row. Bind/unbind/admin status dùng
   cùng thứ tự lock. Serial lạ/DISABLED/RETIRED 404 DEVICE_NOT_AVAILABLE; ACTIVE owner
   khác 409 DEVICE_UNAVAILABLE không tiết lộ chủ; own 200 không đổi nickname/config;
   ACTIVE chưa owner 201 binding mới.
3. Partial unique device_id WHERE unbound_at IS NULL chống race. Tạo config v1 riêng
   (defaults brightness 70/vibration=true), issued-version row và idempotency
   result atomically; applied=null. Defaults là preference thiết kế, chưa đo phần cứng.

Replay kiểm tra caller và binding gốc còn active; binding đã đóng 409
BINDING_NO_LONGER_ACTIVE, dù device trống hoặc caller đã bind mới. Active replay 200
snapshot gốc; khác payload 409. Cache 7 ngày, minimal tombstone trong đời binding để không
tạo lại sau expiry; xem [idempotency](../api/proposed-api.md).

Unbind bắt buộc query expected_binding_id UUID. Khóa device rồi binding được chỉ định;
đúng device/caller thì đóng nếu active, nếu đã đóng trả204. Không đụng binding mới kể cả
cùng caller. Binding không tồn tại/foreign/sai device404. Giữ lịch sử, không delete row.
[ADR0003](../decisions/0003-backend-mvp.md) thay giới hạn stale DELETE trong thiết kế cũ.

GET device-bindings chỉ lịch sử caller, không tên/ID chủ tiếp theo. Config/reports cũ
không chuyển chủ mới; feedback giữ tác giả sau unbind. Registry status không tắt cảnh báo.
Serial có thể lộ/đoán/preclaim; rate limits và quy trình hỗ trợ tranh chấp cần thiết,
không dùng giới hạn này để tự thêm hardware authentication.
