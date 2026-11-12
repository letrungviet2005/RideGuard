# Hướng dẫn khởi chạy RideGuard trên Windows

Áp dụng cho web quản trị và backend chạy local. Máy hiện tại có PostgreSQL 18 cài trực tiếp, dịch vụ `postgresql-x64-18`, cổng **5433**. Không cần Docker cho quy trình này. Tất cả lệnh bắt đầu từ thư mục gốc RideGuard trong PowerShell.

## 1. Cần chuẩn bị những gì?

**Trạng thái máy ngày 2026-11-12:** đã tạo user/database `rideguard`, lưu mật khẩu ứng dụng
riêng trong `apps/backend/.env`, chạy thành công migration V1/V2 và nhận readiness `UP`.
FE đã trả HTTP 200 ở cổng 5173. Có thể bỏ qua bước tạo database trên máy này; vẫn cần tạo
tài khoản web ADMIN ở mục 3. Đây là trạng thái lúc kiểm tra, không bảo đảm process còn chạy
sau khi đóng terminal/khởi động lại máy.

- PostgreSQL đang chạy; có database `rideguard` và user `rideguard` với mật khẩu bạn quản lý.
- JDK 21; launcher tự tìm bản `.local/jdk21/jdk-21.0.13+9` trong repo nếu chưa có `JAVA_HOME`. Máy khác đặt `$env:JAVA_HOME='C:\duong-dan\jdk-21'`.
- Node.js 24+ và npm để chạy FE.
- Maven Wrapper đã có trong repository; lần đầu build có thể cần tải dependencies.

Kiểm tra database server:

```powershell
Get-Service postgresql-x64-18
& 'C:\Program Files\PostgreSQL\18\bin\pg_isready.exe' -h 127.0.0.1 -p 5433
```

Kết quả mong đợi: service `Running`, server `accepting connections`. Điều này chỉ xác nhận server nhận kết nối; chưa xác nhận user/password/database của RideGuard.

Nếu chưa tạo database/user, thực hiện mục 1 trong [hướng dẫn PostgreSQL local](backend/postgresql-local.md). Không dùng tài khoản quản trị PostgreSQL làm tài khoản web.

## 2. Cấu hình BE

Mở `apps/backend/.env` bằng editor. File riêng này bị gitignore; không chia sẻ hoặc commit. Cấu hình local:

```dotenv
DB_URL=jdbc:postgresql://127.0.0.1:5433/rideguard
DB_USER=rideguard
DB_PASSWORD=
PORT=8080
SPRING_PROFILES_ACTIVE=local
ADMIN_ALLOWED_ORIGINS=http://127.0.0.1:5173
COOKIE_SECURE=false
COOKIE_SAME_SITE=Lax
SPRING_FLYWAY_DEFAULT_SCHEMA=public
```

Điền mật khẩu thật của PostgreSQL user `rideguard` vào `DB_PASSWORD`. Nếu để trống, script sẽ hỏi mật khẩu ẩn trong terminal. Không gửi mật khẩu trong chat. Giữ hai khóa `JWT_SIGNING_KEY` và `CURSOR_SIGNING_KEY` đã được sinh trong file; không đổi lại mỗi lần chạy. Máy mới xem cách sinh khóa tại [runbook BE](backend/runbook.md), mục 3.

File dùng định dạng `NAME=value`, không thêm dấu nháy hoặc `export`. Launcher đọc file vào môi trường process; Spring Boot không tự nạp `.env`. Cấu hình cookie HTTP chỉ dành cho local.

## 3. Tạo tài khoản đăng nhập web một lần

Chạy trong terminal tương tác:

```powershell
.\scripts\run-backend-local.ps1 -ProvisionAdmin
```

Script build backend rồi hỏi **email, họ tên và mật khẩu ADMIN**. Nhớ thông tin vừa nhập: đây là tài khoản đăng nhập FE. Nếu script hỏi mật khẩu database trước, nhập mật khẩu PostgreSQL của user `rideguard`, không phải mật khẩu ADMIN.

Không có ADMIN mặc định. Không dùng tài khoản fixture trong test. Nếu đã có ADMIN thì bỏ qua bước tạo; CLI không tự đổi quyền email đã tồn tại.

## 4. Chạy BE — terminal thứ nhất

Mật khẩu ADMIN cần **12–128 ký tự Unicode**. Nếu gặp `INVALID_REQUEST` tại
`Json.password` khi dùng bản CLI cũ, chạy lại lệnh tạo ADMIN và chọn mật khẩu đúng
độ dài. CLI hiện tại kiểm tra và hỏi lại trước khi tạo tài khoản.

```powershell
.\scripts\run-backend-local.ps1
```

Giữ terminal mở. Backend dùng Maven Wrapper chạy Spring Boot, kết nối PostgreSQL và tự áp dụng các migration còn thiếu. URL mặc định: **http://127.0.0.1:8080**.

Kiểm tra từ terminal khác:

```powershell
Invoke-RestMethod http://127.0.0.1:8080/actuator/health/readiness
```

Khi `status` là `UP`, backend sẵn sàng. Nếu báo lỗi mật khẩu hoặc không có database, sửa bước 1–2 rồi chạy lại. Không xóa volume/database hoặc sửa checksum migration để bỏ qua lỗi.

## 5. Chạy FE — terminal thứ hai

Cài dependency lần đầu hoặc sau thay đổi lockfile, khi dev server FE đã dừng:

```powershell
npm.cmd --prefix apps/admin-web ci
```

Khởi chạy mỗi lần:

```powershell
$env:VITE_API_BASE_URL = 'http://127.0.0.1:8080'
$env:VITE_ENABLE_MOBILE_LAB = 'true'
npm.cmd --prefix apps/admin-web run dev -- --port 5173 --strictPort
```

Mở **http://127.0.0.1:5173**. Đăng nhập bằng ADMIN ở bước 3. Dùng thống nhất `127.0.0.1` cho FE/BE để cấu hình Origin/cookie khớp nhau.

`VITE_ENABLE_MOBILE_LAB=true` mở khu thử REST cho USER. Có thể đặt `false` nếu chỉ cần quản trị. Lab bị loại khỏi bản production. Không đặt secret trong biến `VITE_*` vì chúng được công khai trong JavaScript.

## 6. Thử luồng chức năng

1. ADMIN vào **Registry thiết bị** để đăng ký serial.
2. Vào **Thử API mobile**, đăng ký USER mới và đăng nhập phiên USER riêng.
3. Liên kết serial đã đăng ký; đọc/sửa cấu hình.
4. Nếu cần, gửi ACK/report thủ công trong Lab; đây là mô phỏng REST, không kết nối phần cứng.
5. USER tạo feedback; quay về quản trị để ADMIN trả lời.

Email reset mật khẩu cần SMTP được cấu hình riêng. Nút Health trong FE có thể bị giới hạn CORS cho `/actuator/**`; kiểm tra readiness bằng PowerShell như bước 4.

## 7. Dừng và chạy lại

Nhấn `Ctrl+C` tại từng terminal FE và BE. PostgreSQL là Windows Service và có thể tiếp tục chạy để phục vụ các ứng dụng khác. Lần sau chỉ cần bước 4 và 5; không tạo lại database, ADMIN hay signing keys.

Nếu cổng đã có server đang chạy, sử dụng server đó hoặc dừng đúng terminal sở hữu trước khi khởi động lần nữa. Kiểm tra:

```powershell
Get-NetTCPConnection -State Listen -ErrorAction SilentlyContinue |
  Where-Object LocalPort -in 5433,8080,5173 |
  Select-Object LocalAddress,LocalPort,OwningProcess
```

## 8. Các lỗi thường gặp

- **`password authentication failed`:** kiểm tra `DB_USER` và `DB_PASSWORD`, không nhầm mật khẩu web.
- **`database rideguard does not exist`:** tạo database theo hướng dẫn PostgreSQL local.
- **`connection refused`:** kiểm tra PostgreSQL Windows Service, port 5433 và BE8080.
- **`NETWORK_OR_CORS` trên FE:** kiểm tra readiness, URL API và `ADMIN_ALLOWED_ORIGINS` khớp địa chỉ FE.
- **`EPERM esbuild.exe` khi `npm ci`:** dừng dev server của chính bạn bằng Ctrl+C rồi cài lại.
- **PowerShell chặn `.ps1`:** xem chính sách script trên máy; có thể chạy Maven thủ công theo [runbook BE](backend/runbook.md) sau khi nạp biến môi trường.
- **Chưa có ADMIN:** chạy bước 3; không tìm mật khẩu trong database vì password được lưu dưới dạng hash.

Chi tiết kiểm thử: [frontend](frontend/verification-status.md), [backend](testing/verification-status.md). Hướng dẫn này mô tả cách chạy; trạng thái dịch vụ thực tế được báo riêng khi thực hiện, không suy ra từ việc build thành công.

## Dữ liệu demo local (bổ sung 13/11/2026)

Điền riêng `DEMO_USER1_PASSWORD`, `DEMO_USER2_PASSWORD` trong `apps/backend/.env`
bằng editor (12–128 ký tự Unicode), rồi từ gốc repo chạy:

```powershell
.\scripts\seed-demo-local.ps1
```

Lệnh tái sử dụng ADMIN ACTIVE hiện hữu, tạo hai USER, năm thiết bị và ba feedback
nếu chưa có; chạy lại bỏ qua bản ghi cũ, không reset password/config/trạng thái.
Không tạo ACK/telemetry. Nếu có nhiều ADMIN, truyền `-AdminEmail` để chọn tài khoản.
Đây là hướng dẫn; lượt 13/11 chưa thực thi seed vì hai biến mật khẩu còn trống.
Xem [chi tiết seed](backend/demo-data.md) và [báo cáo kiểm chứng](testing/frontend-demo-2026-11-13.md).

Sau reload web tự khôi phục cookie, không cần bấm nút khôi phục. Khi chưa xác thực,
chỉ hiện login/loading; sidebar chỉ xuất hiện cho ADMIN. Mở `/devices` khi chưa
đăng nhập sẽ chuyển về login rồi trở lại trang đó sau khi đăng nhập thành công.
