# Chạy RideGuard bằng PostgreSQL trên Windows

Ngày 2026-11-12: phát hiện dịch vụ `postgresql-x64-18` đang chạy, cài tại `C:\Program Files\PostgreSQL\18`, cấu hình port **5433**. `pg_isready` xác nhận cổng này nhận kết nối. Máy khác cần kiểm tra lại port thực tế.

Backend chạy bằng JDK 21/Maven; PostgreSQL chạy bằng Windows Service. Không cần bật Docker Desktop cho cách chạy này. PostgreSQL 18 chưa được chạy lại toàn bộ integration suite của RideGuard (bằng chứng trước dùng PostgreSQL 17.11).

## 1. Chuẩn bị database một lần

Mở PowerShell tại gốc repository. Kết nối bằng tài khoản quản trị PostgreSQL đã tạo khi cài đặt; nếu tên khác `postgres`, thay `-U` tương ứng:

```powershell
& 'C:\Program Files\PostgreSQL\18\bin\psql.exe' -X -h 127.0.0.1 -p 5433 -U postgres -d postgres -W
```

Nhập mật khẩu PostgreSQL vào prompt ẩn. Trong psql, kiểm tra trước:

```sql
SELECT rolname FROM pg_roles WHERE rolname = 'rideguard';
SELECT datname FROM pg_database WHERE datname = 'rideguard';
```

Chỉ khi role/database chưa tồn tại, tạo riêng cho dự án:

```sql
CREATE ROLE rideguard LOGIN;
\password rideguard
CREATE DATABASE rideguard OWNER rideguard;
\q
```

`\password` hỏi mật khẩu mới hai lần. Ghi nhớ mật khẩu **user rideguard** để chạy BE. Nếu đã có role/database, dùng thông tin hiện có, không chạy lại CREATE hoặc đổi password tùy tiện. Không dùng database của ứng dụng khác.

## 2. Cấu hình riêng của máy

File `apps/backend/.env` của máy này đã được chuyển sang:

```dotenv
DB_URL=jdbc:postgresql://127.0.0.1:5433/rideguard
DB_USER=rideguard
DB_PASSWORD=
SPRING_PROFILES_ACTIVE=local
ADMIN_ALLOWED_ORIGINS=http://127.0.0.1:5173
COOKIE_SECURE=false
COOKIE_SAME_SITE=Lax
SPRING_FLYWAY_DEFAULT_SCHEMA=public
```

Hai khóa `JWT_SIGNING_KEY` và `CURSOR_SIGNING_KEY` còn trống đã được sinh ngẫu nhiên độc lập vào file riêng; không in ra console, không commit. Giữ khóa ổn định giữa các lần chạy. `DB_PASSWORD` để trống thì launcher hỏi ẩn, hoặc tự điền trong `.env` bằng editor. `.env` nằm trong gitignore.

Máy mới: copy `.env.example` khi chưa có `.env`, sửa các biến trên và sinh keys theo [runbook](runbook.md) mục 3. Cấu hình HTTP/cookie này chỉ dành cho local. Spring không tự đọc `.env`; script bên dưới nạp giúp bạn.

## 3. Tạo tài khoản web ADMIN

Cần JDK 21; launcher tự tìm bản `.local/jdk21/jdk-21.0.13+9` nếu chưa đặt JAVA_HOME. Nếu dùng JDK khác vị trí, đặt `$env:JAVA_HOME` trước.

```powershell
.\scripts\run-backend-local.ps1 -ProvisionAdmin
```

Script hỏi mật khẩu DB nếu cần, build JAR rồi gọi CLI tương tác. Nhập email, họ tên và mật khẩu ADMIN. Đây là tài khoản đăng nhập web, khác tài khoản PostgreSQL. Không có ADMIN mặc định. Chỉ tạo một lần; CLI không tự nâng quyền tài khoản có sẵn.

## 4. Chạy BE mỗi lần

Mật khẩu ADMIN phải dài **12–128 ký tự Unicode**. CLI sẽ báo rõ và hỏi lại nếu nhập
ngoài giới hạn; nội dung mật khẩu không được in ra. Lỗi cũ tại `Json.password` với
`INVALID_REQUEST` có nghĩa mật khẩu chưa đạt độ dài, tài khoản chưa được tạo.

```powershell
.\scripts\run-backend-local.ps1
```

Giữ terminal mở. Flyway tự kiểm tra/ap dụng migration khi khởi động nên DB phải tồn tại và user có quyền tạo schema/bảng. Terminal khác kiểm tra:

```powershell
Invoke-RestMethod http://127.0.0.1:8080/actuator/health/readiness
```

Khi trả `UP`, mở FE tại http://127.0.0.1:5173, cấu hình FE `VITE_API_BASE_URL=http://127.0.0.1:8080`, đăng nhập bằng ADMIN đã tạo. Dừng BE bằng Ctrl+C; không cần dừng PostgreSQL đang được các ứng dụng khác sử dụng.

## 5. Nếu gặp lỗi

- `password authentication failed`: kiểm tra mật khẩu role trong `DB_USER`; mật khẩu PostgreSQL và ADMIN là hai loại khác nhau.
- `database rideguard does not exist`: tạo database theo mục 1.
- `connection refused`: kiểm tra Windows Service và port 5433, không tự chuyển về 5432.
- PowerShell chặn script: dùng terminal có chính sách cho phép script local theo cấu hình máy; không cần thay execution policy toàn máy để chạy Maven theo [runbook](runbook.md).
- Reset password qua email cần cấu hình SMTP riêng; PostgreSQL local không cung cấp SMTP.

Cập nhật ngày 2026-11-12: đã tạo role `rideguard` (không superuser, không quyền tạo role/database)
và database `rideguard` do role này sở hữu. Mật khẩu ứng dụng được sinh riêng, lưu vào `.env`
bị gitignore; không dùng lại mật khẩu quản trị `postgres`. Đã kết nối bằng role ứng dụng,
khởi động BE, kiểm tra V1/V2 thành công, 10 bảng nghiệp vụ và readiness `UP`.
FE trả HTTP 200 trên cổng 5173. Database mới chưa có ADMIN; dùng mục 3 để tạo tài khoản.
Đây là kiểm chứng khởi động local trên PostgreSQL 18; chưa chạy lại toàn bộ integration suite.

Lỗi phát hiện khi restart: search_path mặc định có `"$user"`; sau khi migration tạo schema
`rideguard` trùng tên role, default schema chuyển từ `public` sang `rideguard`.
Flyway không tìm thấy history đã nằm trong `public`. Đã đặt
`SPRING_FLYWAY_DEFAULT_SCHEMA=public` trong `.env` để giữ đúng history; không baseline,
repair, sửa migration hoặc xóa dữ liệu. Các bảng nghiệp vụ vẫn nằm trong `rideguard`.
Sau sửa cấu hình đã khởi động lại thành công, Flyway validate đủ 2 migration và readiness
trả `UP`. Flyway hiện cảnh báo PostgreSQL 18.6 mới hơn phiên bản đã được nó kiểm chứng
(17); startup local đạt nhưng chưa xác nhận toàn bộ integration trên PostgreSQL 18.
