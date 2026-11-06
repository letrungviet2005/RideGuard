# Kiến trúc hệ thống

ESP32 xử lý radar → tracking → features → risk → HUD/rung tại chỗ. Không có
backend, Internet hoặc đăng nhập trong đường cảnh báo. Thiếu dữ liệu, mất cảm biến,
chưa khởi tạo hoặc dữ liệu quá hạn phải là UNKNOWN/UNAVAILABLE/FAULT, không phải SAFE.
Nền tảng hiện tại chưa đánh giá nguy cơ và không điều khiển GPIO ngoại vi.

Android gửi chỉ dẫn điều hướng qua BLE và quản lý tài khoản/cấu hình/hành trình qua
REST. BLE và REST là hai adapter độc lập. GPS điện thoại dùng dẫn đường/ghi hành trình,
không là tín hiệu chính kích hoạt cảnh báo rẽ. IMU không biết trước ý định rẽ.
Backend và PostgreSQL lưu nghiệp vụ; admin web chỉ quản trị phạm vi được cấp quyền.
AI là nghiên cứu offline, chưa có mô hình, inference server hoặc số liệu độ chính xác.

## Phân chia mã nguồn khi có nghiệp vụ

- Backend `com.rideguard`: `auth`, `users`, `devices`, `trips`, `feedback`, `shared`.
  Chỉ `shared` có mã hiện tại. Controller/service/repository/DTO/entity được tạo khi
  thực sự cần; không dựng CRUD hay các lớp rỗng trước.
- Android: `features/auth`, `devices`, `navigation`, `trips`, `settings`;
  `shared/networking`, `ble`, `storage`, `models`, `ui`. Networking đã có cấu hình
  môi trường; các adapter BLE/Room chưa triển khai. Navigation nền tảng có hai màn hình.
- Admin: `features/auth`, `users`, `devices`, `feedback`; `shared/api.ts` và router/layout
  hiện có. Không dựng trang quản lý giả hay lịch sử vị trí cá nhân.
- Firmware: `drivers/radar` có giao diện và stub chọn khi build. `drivers/imu`,
  `drivers/display`, `drivers/vibration` sẽ quản lý phần cứng; `tracking` quản lý mục tiêu,
  `features` tính đặc trưng, `risk` đánh giá nguy cơ, `navigation` quản lý cue hết hạn,
  `communication` BLE, `ui` HUD, `power` pin, `diagnostics` tình trạng và log.
  Không tạo thư mục rỗng cho các phần chưa có mã.

## Ranh giới vật lý

ESP32 NodeMCU-32S, TFT ST7789 240×240, motor qua driver. Chọn LD2451 hoặc LD2417
khi build; IMU LSM6DS3/BMI160 chưa chốt. Pin tháo rời, sạc ngoài. Pin map, nguồn,
dòng motor, chống nước và vị trí anten chưa xác nhận. Không mặc định radar xuyên được
lớp phản xạ gương. ID mục tiêu radar có thể đổi; phải kiểm chứng bằng dữ liệu thật.
Không đồng nhất khoảng cách trục Y của LD2417 với khoảng cách của LD2451.

Không có OTA, thanh toán, chia sẻ thiết bị hoặc tải toàn bộ radar lên cloud trong MVP.

## Triển khai độc lập

Firmware build thành binary; mobile thành APK; admin thành static `dist/`; backend
thành executable JAR; AI thành Python wheel. Compose chỉ chạy PostgreSQL. Không có
pipeline deploy tự động. Backend cần DB và môi trường riêng; admin cần cấu hình URL
trước build và SPA rewrite về index.html trên host. Mobile không cần server để mở màn hình.
