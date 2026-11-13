# Android REST MVP — cấu trúc và hành vi

Ngày 13/11/2026. Hợp đồng: `contracts/openapi.yaml`, không phải proposed.

## Các lớp

- `MainActivity`: factory ViewModel với application context, edge-to-edge và adjustResize.
- `app/RideGuardApp`: auth/loading riêng, bottom navigation Tổng quan/Thiết bị/Phản hồi/Tài khoản;
  Back về màn hình cha hoặc Tổng quan. Route luôn qua `allowedScreen`.
- `app/RideGuardViewModel`: state/form memory, coroutine viewModelScope, hủy job khi đổi trang,
  generation ngăn kết quả cũ ghi vào phiên mới. UI dùng collectAsStateWithLifecycle.
- `features/auth`, `account`, `devices`, `feedback`: composable; field/button/card chung
  trong `shared/ui`, Material3 xanh rừng; tiếng Việt ở `res/values/strings.xml`.
- `data/RideGuardRepository`: DTO typed, routes/payload snake_case, UUID, encode cursor,
  nullable fields, config CAS và expected_binding_id.
- `shared/networking/ApiClient`: OkHttp trên Dispatchers.IO, timeout hữu hạn,
  không body logging/cookie jar, không redirect/retry kết nối tự động.
- `shared/storage/TokenVault`: AES/GCM với key AndroidKeyStore, AtomicFile ciphertext
  trong noBackupFilesDir; không ghi password/access token. Backup/device transfer bị loại trừ.

Foundation cũ được thay bằng giao diện ứng dụng; không còn đưa “Hành trình” vào MVP.
Không xin quyền Bluetooth/location và không có màn hình ADMIN.

## Phiên và điều hướng

Mở app: RESTORING → refresh → AUTHENTICATED hoặc ANONYMOUS/UNAVAILABLE.
Không có refresh thì không gọi mạng. UI private chỉ render ở AUTHENTICATED.
Login/refresh/logout gửi `X-Client-Type: mobile`, refresh/logout dùng JSON refresh_token.
Đăng ký không nhận token, thành công quay về login. Tài khoản ADMIN cũng chỉ thấy
chức năng người dùng, không có thao tác quản trị.

Mutex serialize login/refresh/logout. Nhiều request 401 dùng chung rotation; request
đến sau nhận ra access token đã đổi. Chỉ retry sau 401 một lần, cùng body và key.
401 lần hai kết thúc phiên. 403 CRUD không tự refresh. Error giữ code/request_id;
204 không parse JSON. Timestamp server là RFC3339 UTC, report có thời điểm/độ cũ.

Refresh cũ bị xóa khỏi vault trước khi gửi: timeout/crash không replay token có thể đã
rotate. Lỗi credential và mạng/5xx được phân biệt. Kết quả refresh không chắc chắn
thì clear dữ liệu private và yêu cầu login lại; không loop, không có offline cache.

Logout xóa credential/identity trước request revoke. Nếu mạng ngắt, logout local
không chứng minh revoke đã tới server; phiên có thể còn đến expiry hoặc được thu hồi
từ thiết bị khác. Không tự giữ token để retry ngầm.

Epoch đổi sẽ xóa profile, lists, detail, drafts và keys; response cũ không cập nhật UI.
Form/password chỉ ở ViewModel, giữ qua rotation nhưng không qua process death.
Không ghi credential vào SavedStateHandle/Bundle/preferences.

## Thiết bị, config và phản hồi

- Bind chỉ liên kết tài khoản bằng serial đã đăng ký, không tạo serial/kết nối Bluetooth.
  Unbind có dialog và expected_binding_id từ Device response.
- Devices/history/sessions/feedback dùng cursor, tải thêm append/dedupe ID.
- Bind/feedback dùng UUID key gắn payload draft. Retry cùng nội dung giữ key trong memory;
  thay payload có chủ đích là thao tác mới. Sau process death, đối chiếu list trước khi
  làm lại thao tác chưa chắc kết quả vì draft/key không còn.
- Config PATCH dùng desired_version từ GET làm expected_version. 409 đọc lại server,
  giữ draft, khóa nút lưu đến khi người dùng chọn bản server rồi chỉnh/lưu lại.
  Không tự PATCH lần hai hoặc POST ACK. Save hiển thị chưa đồng bộ với gương.
- Feedback có device tùy chọn, category/content, detail và admin_response; vẫn đọc được
  sau unbind bằng endpoint riêng của tác giả.
- Tổng quan ghi rõ số liệu của trang đã tải. Chỉ hiển thị số sau khi API hoàn tất,
  không dùng 0 mặc định khi tải lỗi. Applied null không phải đã áp dụng.
- Không tạo report/pin/radar/online giả. Report hiện thời điểm và độ cũ nếu có.

UI/rotation/keyboard/system bars và Keystore thật còn cần instrumentation trên thiết bị.
Forgot/reset đã có UI/request, chưa xác minh gửi/nhận mail trong lượt Android.
BLE/ESP32/cảm biến/AI/GPS không thuộc lần triển khai này.

Xem [kết quả kiểm chứng](verification-2026-11-13.md).
