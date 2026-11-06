# Device status reporting — proposed MVP

[Schema](../../contracts/device-report.schema.json), [REST](../../contracts/openapi.proposed.yaml).
Chưa có controller hoặc BLE telemetry. Mobile báo quan sát thực sự do ESP32 cung cấp,
không suy OK chỉ vì BLE kết nối. Tất cả trường required: binding_id, observed_at,
firmware_version nullable<=64, radar/imu/display/vibration_status OK/ERROR/UNAVAILABLE/
UNKNOWN, battery_percent integer 0–100 hoặc null. Chưa đo pin thì null. Không raw radar,
raw IMU, GPS hoặc mảng tọa độ. Body <=32 KiB.

Caller/account/session hợp lệ; binding hiện tại thuộc caller/đúng device, sai/đóng/foreign 404.
received_at server chụp khi nhận request, không lấy từ mobile. observed_at mobile UTC Z.
Đề xuất chấp nhận [received_at−24h,received_at+5min]; ngoài 400 OBSERVED_AT_OUT_OF_RANGE.
Dung sai 5 phút không chứng minh đồng bộ clock. Client lệch quá mức phải sửa clock rồi
quan sát mới, không đổi timestamp dữ liệu cũ để qua validation.

Khóa binding để serialize report/unbind; so sánh latest accepted observed_at trên binding.
Mới hơn strict→200 accepted=true; cũ/bằng (cả exact retry hoặc payload khác cùng timestamp)
→200 accepted=false, không overwrite. Lưu metadata/accepted cho báo cáo hợp lệ; GET device
chỉ latest accepted, chưa có=null. Retry trảfalse có thể do lần đầu đã thành công, không
là lỗi để retry vô hạn. received_at response là thời điểm nhận lần request này.

Proposed retention device_reports 30 ngày; dọn metadata theo chính sách đã duyệt. Sau dọn
nếu không còn latest thì null, không OK. Index binding/observed_at, partial unique cho
accepted observation. New binding không thấy report cũ. Active binding vẫn có thể báo
khi registry DISABLED/RETIRED, vì registry không điều khiển cảnh báo ESP32.

UI hiển thị thời điểm/độ cũ, không online realtime. observed_at do caller cung cấp có thể
sai/giả; không tính latency received_at−observed_at và không dùng report quyết định risk.
Đo RTT bằng một monotonic clock riêng. Rate/tần suất, timestamp tolerance, retention cần
duyệt trước triển khai. Báo cáo mobile chưa có bằng chứng mật mã phần cứng.
