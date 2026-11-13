# Roadmap RideGuard

Hiện trạng và thứ tự công việc cụ thể ở [tiến độ dự án](tien-do-du-an.md). Roadmap xác định phạm vi phát triển, không phải bằng chứng đã chạy. API đã triển khai ở [OpenAPI implemented](../contracts/openapi.yaml); [proposed](api/proposed-api.md) giữ ngữ cảnh thiết kế.

## Ưu tiên MVP còn lại

1. Ổn định toolchain Android và nghiệm thu luồng UI–BE thật cho Android/web; hoàn thiện các khoảng trống UI đã chỉ ra trong báo cáo đối chiếu API. Seed local và kiểm tra chạy lại khi đủ credential.
2. Chốt radar/IMU/wiring/protocol, decoder fixtures, baseline UNKNOWN-aware; triển khai BLE navigation/config apply/ACK/status. Kiểm tra mất Internet/auth/BLE không làm ngừng cảnh báo cốt lõi.
3. Thu session có provenance, đồng bộ radar–IMU, đánh giá ML nhẹ so baseline. Đo false alarms, dangerous misses, latency, RAM và nguồn trước tuyên bố hiệu quả.
4. Kiểm tra PostgreSQL 18 đầy đủ, SMTP thật, HTTPS/cookie/origins, retention/load, backup/restore và vận hành. Local smoke và SMTP sink không thay thế production validation.

## Deferred — ngoài MVP

- Trips, GPS recording/upload, route storage, trip statistics, bảng trips/trip_points, REST /trips, Room/background trip sync. [Flow cũ](flows/trip-recording-sync.md) được giữ để giải thích phần deferred.
- BLE ownership challenge/proof và device_claim_requests; MVP dùng serial prototype.
- OTA, payment và device sharing.
- AI inference REST/server; pipeline cảnh báo local ESP32 là ranh giới kiến trúc.

GPS navigation và lịch sử device_bindings vẫn thuộc MVP.

## Các quyết định còn cần chốt và kiểm chứng

Domain triển khai, HTTPS/cookie/CORS/CSRF và recovery ADMIN vận hành; retention/erasure và lịch chạy dọn dữ liệu; phần cứng, BLE UUID/MTU/security; quan hệ brightness với cảnh báo HUD và chính sách risk. Các giá trị đã xuất hiện trong API/code phải đối chiếu implementation, không mặc định tất cả còn là đề xuất. Không thay hợp đồng hoặc migration đã áp dụng chỉ bằng sửa tài liệu.
