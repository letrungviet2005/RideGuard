# Configuration sync — backend code, BLE chưa triển khai

Trạng thái kiểm chứng theo ngày: [báo cáo chung](../tien-do-du-an.md). Nội dung flow/ERD không tự chứng minh client hoặc phần cứng đã hoạt động.

[Schema](../../contracts/device-config.schema.json), [REST](../../contracts/openapi.yaml).
Đã có backend controller/service/repository; PostgreSQL integration đã có bằng chứng; BLE apply chưa triển khai/kiểm chứng. Config gắn binding, new binding có schema_version 1,
desired_version 1, applied=null, defaults brightness 70/vibration=true.

Settings chỉ display_brightness integer 0–100, vibration_enabled boolean. vibration_enabled=false tắt cơ cấu rung; HUD và pipeline nguy cơ vẫn hoạt động.
Không có master-disable/threshold AI. Policy ánh sáng thấp cần duyệt. Giá trị brightness chưa là PWM/pin cụ thể. Không mở
ngưỡng risk hoặc master-disable.

1. Mobile online GET rồi PATCH expected_version + settings patch không rỗng. Omitted giữ
   nguyên, null/unknown reject. Không If-Match, không sửa config offline.
2. Server kiểm tra owned active binding và registry ACTIVE; CAS expected_version,
   merge/validate, tăng desired và insert immutable device_config_versions atomically.
   Wrong version 409 VERSION_CONFLICT, registry không ACTIVE 409 DEVICE_NOT_ACTIVE.
3. Server trả snapshot; mobile gửi BLE protocol/schema/binding/desired/settings. BLE
   chưa có → UI chờ đồng bộ. Server save không bằng ESP32 apply.
4. Hợp đồng ESP32: cùng binding không apply version thấp hơn; cùng version/cùng nội dung ACK lại;
   cùng version/khác nội dung lỗi. Binding mới thiết lập riêng, không so version giữa binding.
   Firmware chưa triển khai quy tắc này; backend ACK monotonic không chứng minh anti-rollback phần cứng.
   ESP32 validate compatibility và thực sự apply trước ACK binding/version/command_id;
   lỗi giữ applied cũ, không ACK giả. Core risk không phụ thuộc Internet/auth.
5. Mobile REST ACK binding_id/reported_applied_version sau BLE ACK thật.

Backend ACK xác nhận binding còn active/caller/device; sai 404. Version phải từng phát hành
trên chính binding và<=desired, sai 409 CONFIG_VERSION_NOT_ISSUED. Store max(previous,incoming),
null trước ACK đầu; ACK cũ/lặp thành công, không giảm version hay reset timestamp apply mới.
ACK cho issued config vẫn được ghi khi registry status đổi; không phát config mới. Binding
đóng không nhận ACK. Lưu versions trong đời binding, không xóa version đang tham chiếu.

Mobile report ACK chưa là attestation mật mã. New owner không kế thừa settings/version/ACK.
Mất PATCH response: GET để reconcile, không âm thầm retry với expected_version mới. Mất
REST ACK: retry cùng binding/version. Reconnect đọc applied trước resend; latency dùng
một monotonic clock, không trừ timestamp hai thiết bị chưa đồng bộ.
