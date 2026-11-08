# Roadmap — proposed MVP và deferred

Nguồn MVP hiện hành: [API](api/proposed-api.md), [architecture](architecture/system-overview.md),
[database](database/schema.md). Backend MVP đã có PostgreSQL integration/local runtime smoke ngày 2026-11-09; không coi roadmap là
bằng chứng chạy thực tế. [Hiện trạng BE](backend/current-state.md).

## Proposed MVP tiếp theo

1. Bộ PostgreSQL integration/migration và SMTP sink đã đạt; tiếp tục SMTP thật, browser HTTPS, origins,
   retention/load và vận hành. Không coi local smoke là xác nhận production hoặc phần cứng.
2. Chốt radar/IMU/wiring/protocol, decoder fixtures, baseline UNKNOWN-aware; BLE navigation/
   config ACK/status; test mất Internet/auth/BLE không ngừng core warnings.
3. Thu session có provenance, radar+IMU alignment, đánh giá fused features và ML nhẹ so
   baseline. Duyệt policy đổi hướng và bị đâm phía sau khi đi thẳng trước tuyên bố chức năng.

## Deferred — ngoài MVP

- Lịch sử hành trình, GPS coordinate recording/upload, route storage, trip statistics,
  trips/trip_points tables, REST /trips, Room/background trip sync. Tài liệu cũ giữ tại
  [trip-recording-sync](flows/trip-recording-sync.md), được đánh dấu deferred duy nhất.
- Challenge/proof sở hữu bằng BLE và device_claim_requests; MVP chỉ serial prototype.
- OTA, payment, device sharing. Không có các endpoint/schema nghiệp vụ này trong MVP.
- AI inference REST/server không thuộc MVP; local pipeline ESP32 là ranh giới bắt buộc.

GPS phục vụ navigation vẫn thuộc MVP; không thu log vị trí/hành trình nhân danh roadmap.
Lịch sử device_bindings vẫn thuộc MVP, không bị xóa cùng trip history.

## Quyết định cần duyệt

Deployment domains, CORS/SameSite/CSRF và cookie policy; admin recovery vận hành (CLI đã có, tự khóa/khóa admin cuối bị chặn);
retention marker idempotency sau cache 7 ngày, reports 30 ngày, token/binding/config history;
timestamp tolerance reports−24h/+5min và rate limits; hardware/radar/IMU, BLE UUID/MTU/security;
brightness với cảnh báo HUD (vibration=false tắt cơ cấu rung đã chốt); fused risk policies cho
hai tình huống riêng. Đây là proposals, không cấu hình đã cài hay chứng cứ đã test.
