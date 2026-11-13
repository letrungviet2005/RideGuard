# Đối chiếu chức năng Android với API implemented

Rà soát ngày 13/11/2026, sau lần cài/mở app trên emulator. Chỉ đọc code và lập báo cáo,
không sửa chức năng, không chạy lại test hay thay đổi dữ liệu DB trong lượt rà này.

## Kết luận

Mobile đã có đường gọi cho **22 operations REST người dùng**, nhưng mới là bản UI
quản lý tài khoản/thiết bị/cấu hình/phản hồi cơ bản. Không nên gọi là đã tận dụng đầy đủ
API hoặc hoàn thiện sản phẩm gương. Có endpoint được gọi không có nghĩa là mọi field,
filter, nhánh lỗi và luồng UI đã được triển khai/kiểm chứng.

OpenAPI implemented có **34 operations** (đếm method + path):

- 22 operations đã nối vào client/ViewModel/UI Android, trong đó refresh chạy nội bộ.
- 2 operations ACK/report chưa gọi vì chưa có BLE và dữ liệu phần cứng xác thực.
- 7 operations ADMIN thuộc web quản trị, không thuộc app người dùng đã yêu cầu.
- 3 health operations chưa có màn chẩn đoán trên Android.

4 tab không tương đương 4 chức năng: app có 15 trạng thái màn hình trong enum Screen;
nhiều chức năng nằm trong chi tiết thiết bị hoặc Tài khoản. Tuy nhiên giao diện còn
đơn giản, khám phá tính năng và trình bày dữ liệu còn hạn chế.

## API đã nối và nơi thao tác

Các path dưới đây có tiền tố `/api/v1` trừ health. Đã đối chiếu cả repository/client,
ViewModel và composable; đây là bao phủ mã nguồn, không phải 22 operations đều đã
được chạy trên emulator với BE thật.

### Xác thực — 6

- `POST /auth/register`: Tạo tài khoản → về login, không tự đăng nhập.
- `POST /auth/login`: Đăng nhập.
- `POST /auth/refresh`: tự khôi phục phiên/refresh sau401, không phải một màn riêng.
- `POST /auth/logout`: Tài khoản → Đăng xuất.
- `POST /auth/forgot-password`: Login → Quên mật khẩu.
- `POST /auth/reset-password`: Quên mật khẩu → Đặt lại mật khẩu, nhập mã từ email.

Nguồn: `ApiClient.kt`, `RideGuardRepository.kt`, `AuthScreen.kt`, `RideGuardViewModel.kt`.
Mobile dùng X-Client-Type mobile và JSON refresh, không cookie admin.

### Hồ sơ và phiên — 5

- `GET /users/me`: xem email/họ tên; cũng dùng ở Tổng quan.
- `PATCH /users/me`: sửa họ tên. API chỉ cho sửa họ tên, không đổi email/avatar.
- `POST /users/me/change-password`: Tài khoản → Đổi mật khẩu.
- `GET /users/me/sessions`: danh sách phiên, thông tin hiện tại và tải thêm.
- `DELETE /users/me/sessions/{session_id}`: xác nhận thu hồi phiên; phiên hiện tại về login.

Nguồn: repository, ViewModel và `AccountScreens.kt`.

### Thiết bị và cấu hình — 8

- `GET /devices`: Thiết bị → danh sách gương đang liên kết, tải thêm cursor.
- `GET /devices/{device_id}`: nút Chi tiết gương.
- `PATCH /devices/{device_id}`: lưu nickname trong chi tiết.
- `POST /devices/bind`: Thiết bị → Liên kết gương, serial đã có trong registry.
- `DELETE /devices/{device_id}/binding`: chi tiết → hủy liên kết có xác nhận,
  gửi expected_binding_id từ response.
- `GET /device-bindings`: Thiết bị → Lịch sử liên kết, có tải thêm.
- `GET /devices/{device_id}/config`: chi tiết → cấu hình/version; Tổng quan dùng để tính pending.
- `PATCH /devices/{device_id}/config`: chỉnh độ sáng/rung, expected_version;409 đọc lại,
  giữ draft và yêu cầu chọn bản server trước khi chỉnh/lưu tiếp.

Nguồn: repository, ViewModel, `DeviceScreens.kt`. Bind không phải kết nối Bluetooth.

### Phản hồi — 3

- `POST /feedbacks`: Phản hồi → Gửi phản hồi, category/content/device tùy chọn;
  Idempotency-Key giữ theo draft cho retry.
- `GET /feedbacks`: danh sách, tải thêm cursor; Tổng quan cũng sử dụng trang đầu.
- `GET /feedbacks/{feedback_id}`: chi tiết, trạng thái và trả lời ADMIN.

Nguồn: repository, ViewModel, `FeedbackScreens.kt`.

## Chức năng API đã hỗ trợ nhưng mobile chưa khai thác hết

1. **Thiếu bộ lọc danh sách.** Repository page chỉ gửi limit20/cursor:
   - Devices: chưa gửi filter `status` ACTIVE/DISABLED/RETIRED.
   - Binding history: chưa gửi `status` ACTIVE/UNBOUND và `device_id`.
   - Feedback: chưa gửi `status`, `category`, `device_id`.
   Các filter có trong OpenAPI nhưng UI chưa có điều khiển tương ứng.
2. **Trạng thái phần cứng hiển thị thiếu.** DeviceReport API có firmware_version,
   radar_status, imu_status, display_status, vibration_status, battery_percent,
   observed_at, received_at và binding_id. DTO Report hiện chỉ đọc một phần;
   UI chỉ hiển thị radar/IMU/pin và thời điểm/độ cũ observed_at. Chưa hiện firmware,
   trạng thái màn hình/rung, received_at. Không cần tạo report giả để bổ sung hiển thị.
3. **Chi tiết phản hồi còn sơ sài.** DTO đã có category/device_id nhưng màn detail
   chưa hiện loại và thiết bị liên quan; hiện content/status/timestamp/admin_response.
4. **Thông tin thiết bị chưa đầy đủ.** Device DTO có hardware_revision nhưng chi tiết
   chưa hiện; Binding DTO không giữ model/hardware_revision dù response API có.
5. **Tổng quan mới là các dòng số liệu.** Dùng trang đầu20 records, ghi rõ không phải
   tổng toàn tài khoản; chưa gom tất cả trang. Chưa có summary endpoint trong hợp đồng.
   Muốn tổng chính xác phải paginate hết hoặc bổ sung API theo quyết định mới.
6. **Health chưa được tích hợp.** Chưa có màn kiểm tra kết nối/API readiness giúp người
   dùng phân biệt lỗi địa chỉ server và lỗi nghiệp vụ.
7. **Khả năng khám phá còn hạn chế.** Sessions/change-password trong Tài khoản;
   nickname/config/unbind trong Chi tiết gương; ít mục hiện ngay ở tab chính.
   Có thể cải thiện bố cục và nhãn mà không thêm API.

Các mục này là thiếu sót thực tế của client/giao diện, không nên giải thích tất cả
bằng lý do “ngoài phạm vi”. Không tự đổi backend để che thiếu sót UI.

## Vì sao cấu hình gương chỉ có hai mục?

`contracts/device-config.schema.json` định nghĩa settings chỉ có:

- `display_brightness`: số nguyên0–100.
- `vibration_enabled`: boolean.

Settings không cho additionalProperties; ConfigPatchRequest chỉ cho expected_version
và settings. Desired/applied là dữ liệu version, không phải các nút cài đặt mới.
API hiện không có ngưỡng AI, độ nhạy radar, khoảng cách cảnh báo, âm lượng hay master-disable.
Muốn thêm phải chốt nghiệp vụ và thay hợp đồng/BE/firmware, không chỉ thêm nút Android.

## 12 operations chưa đưa vào mobile

### Cần BLE/dữ liệu thực — 2

- `POST /devices/{device_id}/config/ack`: chỉ gửi sau khi gương thực sự xác nhận apply.
- `POST /devices/{device_id}/reports`: gửi quan sát phần cứng thật.

Chưa có BLE/ESP32 nên cố ý không gọi; đây là phần tích hợp còn thiếu của sản phẩm,
không phải thêm hai nút gửi request thủ công là hoàn thành.

### ADMIN — 7

- `GET /admin/users`
- `PATCH /admin/users/{user_id}/status`
- `POST /admin/devices`
- `GET /admin/devices`
- `PATCH /admin/devices/{device_id}/status`
- `GET /admin/feedbacks`
- `PATCH /admin/feedbacks/{feedback_id}`

Quản lý user/khóa tài khoản, đăng ký serial/trạng thái registry, xử lý phản hồi thuộc
web quản trị. App người dùng không được tự tạo serial hoặc trả lời với quyền ADMIN.

### Health — 3

- `GET /actuator/health`
- `GET /actuator/health/liveness`
- `GET /actuator/health/readiness`

Có thể dùng cho màn chẩn đoán tùy chọn, không phải nghiệp vụ gương mới.

## Bằng chứng và giới hạn

- Báo cáo triển khai trước ghi17 JVM fixture tests và1 Kotlin HTTP smoke với BE thật đạt.
  Không suy ra tất cả màn hình đã được thử end-to-end.
- Sau đó đã cài APK emulator10.0.2.2 thành công (`Installed on 1 device`),
  mở MainActivity trả Status:ok; BE readiness UP. Đây mới là install/launch.
- Ba instrumentation tests trước đó mới compile; lượt rà này không chạy chúng.
- Serial RG-DEMO-ABEA7C đã tạo riêng để người dùng thử. Không coi việc tạo serial
  là bằng chứng bind/config/unbind đã thành công trên UI; lượt này không truy vấn/chỉnh binding.
- Forgot/reset email, Keystore thật và toàn bộ luồng emulator–BE còn cần nghiệm thu.

## Thứ tự hoàn thiện đề xuất

1. Chạy thử UI thật trên emulator với serial demo: bind → nickname → config →
   feedback → unbind → history, cùng restore/logout và kiểm tra lỗi.
2. Thêm filters đã có trong hợp đồng; hiển thị đủ feedback/device/report fields.
3. Cải thiện Tổng quan và lối vào chức năng; phân biệt trạng thái tài khoản/binding/
   pending sync với trạng thái phần cứng, không tự báo online.
4. Thêm chẩn đoán kết nối và hoàn thiện UX validation/error/retry.
5. Triển khai BLE/protocol/ESP32 riêng, sau đó mới nối ACK/report thật.

Nguồn chính: [OpenAPI implemented](../../contracts/openapi.yaml),
[schema config](../../contracts/device-config.schema.json),
[repository Android](../../apps/mobile/app/src/main/java/com/rideguard/mobile/data/RideGuardRepository.kt),
[ViewModel](../../apps/mobile/app/src/main/java/com/rideguard/mobile/app/RideGuardViewModel.kt),
[báo cáo triển khai](verification-2026-11-13.md).
