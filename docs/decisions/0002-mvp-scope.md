# ADR0002 — thống nhất MVP không lưu hành trình

Ghi chú lịch sử: phần backend/hợp đồng bị thay thế tương ứng bởi [ADR0003](0003-backend-mvp.md);
không dùng mô tả “chưa có code” dưới đây làm hiện trạng mới.

Status: **accepted scope / proposed contracts**, 2026-11-07. Thay thế phần phạm vi database,
trips/Room trong [ADR0001](0001-foundation.md), giữ nguyên monorepo và đường cảnh báo local.

Giữ account/session USER/ADMIN; serial-only bind và ownership history; binding-scoped
config; mobile reports; feedback/admin; BLE/navigation và radar+IMU risk trên ESP32.
Đưa trip/GPS recording/route/stats/background sync, possession challenge, OTA/payment/
sharing ra [deferred roadmap](../roadmap.md). GPS navigation và binding history vẫn MVP.

Một nguồn mỗi trạng thái: contracts/openapi.yaml chỉ implemented health; contracts/
openapi.proposed.yaml MVP tương lai; docs/api/proposed-api.md giải thích, không lặp nguồn
DTO. Bảy business tables + ba technical (idempotency_records/device_reports/
device_config_versions), tổng 10 proposed. Chưa thêm migration/entity/controller.

Token transport: mobile body refresh, web HttpOnly/Secure cookie; access 15 phút,
family 30 ngày max, rotation/revocation. Origin/SameSite/CSRF/provisioning còn cần duyệt.
Config expected_version CAS, issued version FK, monotonic ACK; report untrusted mobile
clock với explicit tolerance, không online/attestation. Ràng buộc idempotency replay
binding đóng yêu cầu giữ minimal key tombstone sau response cache 7 ngày; retention cần
duyệt, không giả vờ xóa toàn key vẫn phát hiện được retry cũ. Stale unbind cùng user rebind
là giới hạn đã ghi rõ, không tự thêm precondition/API ngoài phạm vi.

Không thay đổi code security/app/firmware trong nhiệm vụ này. Foundation source vẫn có
copy màn hình nhắc trips; các README ghi rõ đó không phải scope hiện hành. Không có bảng/
migration trip cũ cần xóa. Không chạy Docker/dịch vụ/migration; chỉ static contract/docs checks.
