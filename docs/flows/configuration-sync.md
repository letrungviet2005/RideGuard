# Configuration sync — proposed MVP

[Schema](../../contracts/device-config.schema.json), [REST](../../contracts/openapi.proposed.yaml).
Chưa có controller/BLE apply. Config gắn binding, new binding có schema_version 1,
desired_version 1, applied=null, proposed defaults brightness 70/vibration=true.

Settings chỉ display_brightness integer 0–100, vibration_enabled boolean. Flag rung chỉ
preference tín hiệu không trọng yếu, không tắt HUD/rung cảnh báo cốt lõi; firmware ưu tiên
an toàn và ánh sáng thấp cần duyệt. Giá trị brightness chưa là PWM/pin cụ thể. Không mở
ngưỡng risk hoặc master-disable.

1. Mobile online GET rồi PATCH expected_version + settings patch không rỗng. Omitted giữ
   nguyên, null/unknown reject. Không If-Match, không sửa config offline.
2. Server kiểm tra owned active binding và registry ACTIVE; CAS expected_version,
   merge/validate, tăng desired và insert immutable device_config_versions atomically.
   Wrong version 409 VERSION_CONFLICT, registry không ACTIVE 409 DEVICE_NOT_ACTIVE.
3. Server trả snapshot; mobile gửi BLE protocol/schema/binding/desired/settings. BLE
   chưa có → UI chờ đồng bộ. Server save không bằng ESP32 apply.
4. ESP32 validate compatibility và thực sự apply trước ACK binding/version/command_id;
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
