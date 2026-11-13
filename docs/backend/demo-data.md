# Dữ liệu demo local RideGuard

Lệnh seed chỉ dành cho PostgreSQL Windows `127.0.0.1:5433`, database/user `rideguard`.
Các bảng nghiệp vụ ở schema `rideguard`, Flyway history ở `public`. Không cần Docker.
Seed chạy chủ động bằng profile `local,seed-demo`, không mở HTTP server và không chạy
khi khởi động BE bằng profile `local` thông thường.

## Dữ liệu sẽ tạo

- `demo.user1@rideguard.example`, `demo.user2@rideguard.example`: vai trò USER.
- `RG-DEMO-001`, `RG-DEMO-002`: ACTIVE, mỗi thiết bị liên kết với một USER demo.
- `RG-DEMO-003`: ACTIVE, chưa liên kết.
- `RG-DEMO-004`: DISABLED, chưa liên kết.
- `RG-DEMO-005`: RETIRED, chưa liên kết.
- Hai binding mới có config mặc định hợp lệ: brightness 70, vibration_enabled true,
  desired_version 1 và issued-version 1, `reported_applied_version=null`.
- Ba feedback chung có nội dung bắt đầu `[DEMO]`: OPEN, IN_PROGRESS, RESOLVED.
  Hai trường hợp sau có phản hồi của ADMIN hiện hữu.

Đây là dữ liệu giao diện. Không gửi ACK, không tạo device_reports, pin hoặc telemetry.
Không giả lập gương đã áp dụng cấu hình. Các trạng thái ban đầu trên chỉ được đặt
khi tạo mới; nếu người dùng đã sửa dữ liệu, chạy lại giữ nguyên dữ liệu đó.

## Cấp mật khẩu và chạy

Từ gốc repository, sau khi cấu hình `apps/backend/.env` theo [PostgreSQL local](postgresql-local.md):

```powershell
.\scripts\seed-demo-local.ps1
```

Nếu thiếu mật khẩu, lệnh hỏi ẩn trong terminal. Có thể cấp riêng qua các biến môi trường
`DEMO_USER1_PASSWORD`, `DEMO_USER2_PASSWORD` hoặc điền chúng vào `.env` bằng editor.
Mỗi mật khẩu phải có 12–128 ký tự Unicode; không điền secret vào command history,
tài liệu, Git hoặc log. Seed dùng PasswordEncoder và AuthRepository giống CLI ADMIN;
không lưu plaintext trong DB và không đổi mật khẩu tài khoản đã tồn tại.

Nếu DB có đúng một ADMIN ACTIVE, seed dùng ADMIN đó để ghi nhận phản hồi.
Nếu có nhiều ADMIN, chọn email hiện hữu:

```powershell
.\scripts\seed-demo-local.ps1 -AdminEmail 'email-admin-cua-ban'
```

Seed không cần mật khẩu ADMIN vì đây là CLI có quyền truy cập DB local, không phải
API đăng nhập; không tạo token/session ADMIN. Nếu chưa có ADMIN, tạo bằng CLI hiện tại:

```powershell
.\scripts\run-backend-local.ps1 -ProvisionAdmin
```

Để chạy lại seed, dùng cùng lệnh. Không cần dừng BE hoặc PostgreSQL. Launcher build JAR
trước khi chạy, tự nạp `.env`, kiểm tra đúng URL/user, không in secret.

## Cách tránh ghi đè và dữ liệu trùng

- Toàn bộ seed là một transaction; advisory lock tuần tự hóa các lần seed đồng thời.
  Lỗi trong bất kỳ bước nào làm rollback các thay đổi của lượt đó.
- USER demo dùng UUID ổn định trong namespace riêng. Email/ID trùng với tài khoản không
  khớp, role thay đổi hoặc account khóa: bỏ qua user và các hành động phụ thuộc. Không promote,
  unlock, đổi tên, reset password hoặc thay thế tài khoản.
- Serial đã có: bỏ qua toàn bộ thiết bị, không đổi trạng thái, bind lại, unbind, đổi owner,
  nickname hay config. Chỉ thiết bị vừa tạo trong lượt này mới được bind.
- Binding/config/issued-version được tạo qua `DevicesService.bind` và transaction hiện có.
- Feedback được tạo qua `FeedbackService`, có idempotency key ổn định. Khi marker đã tồn tại,
  bỏ qua cả nội dung, trạng thái và reply, kể cả response cache đã hết hạn.
- Trạng thái DISABLED/RETIRED và phản hồi ADMIN chỉ áp dụng cho bản ghi vừa tạo trong lượt này.
- Không reset/truncate/drop, sửa V1/V2 hoặc tự tạo dữ liệu mỗi lần server chạy.

Thông báo `CREATED`/`SKIPPED` chỉ in sau commit, không chứa password/token/hash. Nếu bị
collision hoặc dữ liệu demo đã bị sửa/khóa/xóa, seed ưu tiên giữ dữ liệu hiện hữu và có
thể không tạo đủ bộ mẫu; xem kết quả thay vì ép lại trạng thái ban đầu.

## Kiểm tra sau seed

Đăng nhập web bằng ADMIN hiện hữu để xem users, registry và feedback. Mở development Lab,
đăng nhập USER demo bằng mật khẩu đã tự cấp để xem binding/config. Chưa ACK thì applied
phải còn null. Không nhầm tài khoản PostgreSQL `rideguard` với tài khoản web.

Kết quả thực thi cụ thể và các phần chưa kiểm chứng nằm trong
[báo cáo ngày 13/11](../testing/frontend-demo-2026-11-13.md).

## Trạng thái dữ liệu được ghi nhận

Chưa có bằng chứng full seed đã chạy/chạy lại: lần chuẩn bị trước thiếu `DEMO_USER1_PASSWORD` và `DEMO_USER2_PASSWORD`; lần dọn docs không đọc lại secret hay truy vấn DB. Ba test seed là kiểm thử mock, không chứng minh đã tạo dữ liệu local.

Serial riêng `RG-DEMO-ABEA7C` đã được thêm thủ công vào registry, ACTIVE, model RideGuard Demo, hardware revision REST-TEST để thử Android. Lúc tạo chưa bind; không cam kết còn trống sau thao tác người dùng. Không tạo kèm config, ACK hoặc report. Serial này không thuộc bộ năm serial của script; không coi nó là bằng chứng full seed thành công.
