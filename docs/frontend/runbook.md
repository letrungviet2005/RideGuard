# Runbook frontend RideGuard

Ngày 2026-11-09. Các lệnh PowerShell dưới đây bắt đầu từ **gốc repository**, trừ phần đã `Set-Location`. FE đã kiểm tra bằng client tests và Chrome fixture; các lệnh khởi động BE/tạo ADMIN dưới đây là hướng dẫn theo code/runbook backend, **chưa chạy trong nhiệm vụ FE**.

## 1. Khởi chạy frontend

Cần Node.js 24+, npm và backend truy cập được để dùng chức năng thật.

```powershell
Set-Location apps/admin-web
npm ci
if (!(Test-Path .env)) { Copy-Item .env.example .env }
$env:VITE_API_BASE_URL = 'http://127.0.0.1:8080'
$env:VITE_ENABLE_MOBILE_LAB = 'true'
npm run dev -- --port 5173 --strictPort
```

Mở http://127.0.0.1:5173. Lab chỉ bật trong dev khi cờ `true`; mặc định tắt. Token admin/mobile nằm trong hai client memory độc lập. Không nhập refresh token vào web admin. Sau reload, web tự khôi phục phiên bằng cookie một lần và hiện loading trung tính; mobile Lab phải đăng nhập lại. Xem [layout và route guard](authentication-layout.md). Không đặt secret trong `VITE_*`.

POSIX tại `apps/admin-web`: `test -e .env || cp .env.example .env`, sửa URL/cờ rồi `npm ci`, `npm run dev -- --port 5173 --strictPort`.

## 2. Khởi chạy backend và tạo ADMIN

**Cập nhật máy hiện tại:** PostgreSQL 18 đã cài trên Windows, port 5433. Dùng
[hướng dẫn PostgreSQL local](../backend/postgresql-local.md) và
`.\scripts\run-backend-local.ps1` từ gốc repo; tạo ADMIN bằng cờ `-ProvisionAdmin`.
Với cách này bỏ qua các bước Docker/Compose bên dưới.

Không có ADMIN mặc định hay mật khẩu demo. Email/password trong `tests/browser/fixture.ts` chỉ phục vụ test, không dùng đăng nhập thật. ADMIN của lần kiểm chứng backend trước nằm trong DB tạm, mật khẩu không được lưu. Để có tài khoản cá nhân dùng lâu dài, tạo ADMIN trong **database phát triển RideGuard bạn đã chọn**, không phải DB integration.

Mở terminal PowerShell tương tác mới tại gốc repo. Xem [runbook backend](../backend/runbook.md) mục 2–3 để chuẩn bị PostgreSQL và secret. Không chạy Compose nếu đang dùng PostgreSQL riêng ở cùng port.

```powershell
docker info
# Chỉ tiếp tục Compose khi Engine hoạt động và bạn chọn PostgreSQL của repo.
if (!(Test-Path infra/.env)) { Copy-Item infra/.env.example infra/.env }
if (!(Test-Path apps/backend/.env)) { Copy-Item apps/backend/.env.example apps/backend/.env }
# Dùng editor điền secret thật vào file riêng; không để nguyên placeholder.
docker compose --env-file infra/.env -f infra/compose.yaml up -d --wait
```

`POSTGRES_PASSWORD` của Compose phải khớp password thực của user DB. Backend cần `DB_URL`, `DB_USER`, `DB_PASSWORD`, hai khóa base64 độc lập `JWT_SIGNING_KEY`, `CURSOR_SIGNING_KEY` (ít nhất 32 bytes mỗi khóa, lưu ổn định). Tham khảo cách sinh không in secret trong runbook backend. Không đổi password volume bằng cách chỉ sửa `.env`, không xóa volume để khắc phục.

Spring không tự đọc `.env`. Nạp file riêng đáng tin cậy, định dạng `NAME=value`, không quotes/expansion, không in giá trị:

```powershell
# Thay bằng JDK 21 đang cài. Repo hiện có bản local sau:
$env:JAVA_HOME = (Resolve-Path '.local/jdk21/jdk-21.0.13+9').Path
Get-Content apps/backend/.env | ForEach-Object {
  if ($_ -match '^([A-Z][A-Z0-9_]*)=(.*)$') {
    [Environment]::SetEnvironmentVariable($Matches[1], $Matches[2], 'Process')
  }
}
$env:SPRING_PROFILES_ACTIVE = 'local'
$env:COOKIE_SECURE = 'false'
$env:COOKIE_SAME_SITE = 'Lax'
$env:ADMIN_ALLOWED_ORIGINS = 'http://127.0.0.1:5173'
.\apps\backend\mvnw.cmd -f apps/backend/pom.xml -B -DskipTests package
```

Tạo ADMIN **một lần**, từ terminal có console hỗ trợ nhập password ẩn:

```powershell
& "$env:JAVA_HOME/bin/java.exe" -jar apps/backend/target/backend-0.1.0-SNAPSHOT.jar --spring.profiles.active=provision-admin,local
```

CLI hỏi email, họ tên, mật khẩu; dùng thông tin bạn vừa nhập để đăng nhập FE. `provision-admin` bật CLI, `local` cho phép cấu hình HTTP local đã nạp. CLI không mở HTTP server, không tự nâng quyền email đã tồn tại. Không truyền password trên command line. Nếu tài khoản đã có, dùng mật khẩu riêng hoặc luồng reset qua SMTP; không tìm mật khẩu trong database hash.

Sau CLI thoát, chạy BE trong cùng terminal đã nạp biến:

```powershell
& "$env:JAVA_HOME/bin/java.exe" -jar apps/backend/target/backend-0.1.0-SNAPSHOT.jar
# Hoặc thay lệnh java bằng:
# .\apps\backend\mvnw.cmd -f apps/backend/pom.xml spring-boot:run
```

Terminal khác kiểm tra:

```powershell
Invoke-RestMethod http://127.0.0.1:8080/actuator/health/readiness
```

Đăng nhập ADMIN tại FE, đăng ký serial trong Registry. Muốn thử luồng USER, mở Lab, đăng ký USER mới rồi liên kết serial đó. ACK/report gửi thủ công chỉ mô phỏng REST client, không kết nối ESP32. Feedback không có API USER trả lời trong cùng thread: đọc phản hồi admin hoặc tạo feedback mới. SMTP phải cấu hình nếu muốn nhận reset token; UI không tiết lộ token hay sự tồn tại email.

`Ctrl+C` dừng BE/FE do bạn chạy; không dừng dịch vụ khác. Backend/CLI tự chạy Flyway nên cần DB và quyền migration phù hợp. Không chạy `clean`, không sửa checksum migration.

## 3. Các kiểm tra frontend

Từ `apps/admin-web`:

```powershell
npm run api:check
npm run typecheck
npm run lint
npm test
$env:VITE_ENABLE_MOBILE_LAB = 'true'
$env:VITE_API_BASE_URL = 'http://127.0.0.1:8080'
npm run test:ui
```

- `api:check`: DTO và routes sinh từ 34 operations của hợp đồng implemented; cập nhật bằng `npm run api:generate` khi hợp đồng thay đổi.
- `typecheck`: app và test TS strict; `lint`: các rule ESLint/TypeScript đã cấu hình, không phải chứng minh không có memory leak.
- `npm test`: 16 unit/client tests với transport giả lập, không phải integration PostgreSQL.
- `test:ui`: build, 7 luồng Chrome development fixture và 1 kiểm tra production. Cần Chrome hoặc `$env:PLAYWRIGHT_CHANNEL='msedge'`. Server test riêng port 5176/5177, không dùng lại server có sẵn; tự dừng sau test. Report `test-results/report.json`, ảnh `test-results/*.png` bị gitignore. Fixtures chỉ trong test, không có fallback production.
- `npm run build`, `npm run preview`: output `dist/`, preview port 4173. Nếu thử BE qua preview thêm chính xác origin vào allowlist. Production cần HTTPS, cookie Secure và SPA fallback về `index.html`; Lab không có route/module trong bundle.
- Windows EPERM khi `npm ci` đụng `esbuild.exe`: dừng dev server của chính bạn bằng Ctrl+C trước khi cài; không dừng mọi tiến trình Node hoặc server người khác.

Từ gốc repository, checker độc lập:

```powershell
.\scripts\.venv\Scripts\python.exe scripts/check_contracts.py
git diff --check
```

## 4. Xử lý lỗi và giới hạn

- **Docker pipe không tồn tại:** mở Docker Desktop và chờ Linux Engine khởi động, chạy lại `docker info`. FE vẫn chạy được nhưng không có dữ liệu thật khi BE chưa chạy. Không coi fixture là BE đạt.
- **NETWORK_OR_CORS:** kiểm tra BE8080, origin FE đúng allowlist. Cùng `http://127.0.0.1` khác port là same-site nhưng cross-origin nên vẫn cần CORS/credentials; `localhost` và `127.0.0.1` không hoán đổi. Trình duyệt tự đặt Origin, FE không giả lập header này. Không dùng `no-cors`.
- **Health trong FE lỗi nhưng mở URL trực tiếp được:** backend hiện chỉ cấu hình CORS cho `/api/**`, không cho `/actuator/**`. Dùng lệnh readiness phía trên hoặc reverse proxy cùng origin. FE không sửa BE để bỏ giới hạn này.
- **Cookie local:** profile `local`, `COOKIE_SECURE=false`, SameSite Lax và cùng host/scheme. Production phải HTTPS/Secure; chưa kiểm chứng cookie thật đa trình duyệt trong nhiệm vụ này.
- **401:** single-flight refresh, mỗi request tối đa một retry xác thực; refresh lỗi thì xóa phiên, không lặp credential cũ. Lỗi mạng mutation không tự gửi lại. Bind/feedback giữ idempotency key cho cùng payload trong form đang mở; reload làm mất key, cần đối chiếu server trước thao tác mới.
- **409 config:** giữ bản nháp, hiển thị bản server để so sánh; tải lại/bỏ nháp rõ ràng trước lần lưu tiếp. Không tự ghi đè/ACK.
- **Unbind:** hộp xác nhận giữ `expected_binding_id` từ dữ liệu đã xem; lỗi stale binding phải tải lại, không ép retry bằng binding mới.
- **ADMIN_SELF_LOCK / LAST_ADMIN / 403:** hiển thị lỗi và `request_id` từ BE; UI không bỏ kiểm tra quyền backend.

Xem [bao phủ từng API](api-coverage.md) và [kết quả thực tế](verification-status.md). Chưa kiểm chứng FE với DB/BE thật, email thật, HTTPS cookie hoặc BLE/firmware trong nhiệm vụ này.
