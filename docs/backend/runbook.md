# Runbook backend RideGuard

Ngày 2026-11-09 đã chạy PostgreSQL integration, CLI admin, HTTP runtime và SMTP sink.
[Báo cáo thực tế](../testing/backend-verification-2026-11-09.md) ghi lệnh và phạm vi kiểm chứng.
Các ví dụ bên dưới là quy trình để chạy lại; SMTP nhà cung cấp, browser HTTPS và phần cứng chưa kiểm chứng.

## 1. Prerequisites

JDK21, Git, Maven Wrapper3.9.11 (đã có), PostgreSQL17.11. Spring Boot3.5.16.
Wrapper/dependency cần mạng nếu cache chưa có. Docker Desktop/Engine + Compose v2 chỉ cần nếu chọn
chạy PostgreSQL bằng container; backend chạy bằng Java ngoài Docker. Không bắt buộc Docker khi đã
có PostgreSQL bản cài trực tiếp. Không tự bật Docker Desktop hoặc dừng container khác.

Tất cả lệnh PowerShell/POSIX bên dưới tính từ **gốc repository**, trừ khi ghi khác.

## 2. Chuẩn bị PostgreSQL

PowerShell, chỉ copy nếu file chưa tồn tại:

```powershell
if (!(Test-Path infra/.env)) { Copy-Item infra/.env.example infra/.env }
if (!(Test-Path apps/backend/.env)) { Copy-Item apps/backend/.env.example apps/backend/.env }
# Sửa các file riêng bằng editor; không commit secret, không ghi đè .env đã có.
docker compose --env-file infra/.env -f infra/compose.yaml up -d --wait
```

POSIX:

```sh
test -e infra/.env || cp infra/.env.example infra/.env
test -e apps/backend/.env || cp apps/backend/.env.example apps/backend/.env
docker compose --env-file infra/.env -f infra/compose.yaml up -d --wait
```

Compose dùng image postgres:17.11-alpine, volume postgres_data, port localhost5432 mặc định.
POSTGRES_PASSWORD phải được cấp; DB_PASSWORD của backend phải khớp account thực trong DB.
Đổi password trong `.env` không thay password của volume đã khởi tạo.

Nếu dùng bản cài PostgreSQL trực tiếp, DBA tạo role/database riêng (không dùng DB của dự án khác):

```sql
CREATE ROLE rideguard LOGIN;
-- Trong psql: dùng \password rideguard để nhập password ẩn, không ghi literal vào SQL.
CREATE DATABASE rideguard OWNER rideguard;
```

Chạy `password rideguard` trong psql, cấp DB_URL/USER/PASSWORD tương ứng. Role migration cần quyền
tạo schema/table/function/trigger; trước production nên tách quyền migration khỏi runtime.
Không có lệnh Flyway clean hoặc xóa database/volume trong quy trình khắc phục.

## 3. Cấu hình

Spring **không tự đọc `.env`**. File example chỉ tham khảo. Nạp vào process từ secret manager hoặc
loader dưới đây cho file tin cậy của chính bạn, định dạng mỗi dòng `NAME=value` không quote/expansion.
Loader không thực thi nội dung file và không in giá trị.

PowerShell:

```powershell
$env:JAVA_HOME = 'C:\path\to\jdk-21'
Get-Content apps/backend/.env | ForEach-Object {
  if ($_ -match '^([A-Z][A-Z0-9_]*)=(.*)$') {
    [Environment]::SetEnvironmentVariable($Matches[1], $Matches[2], 'Process')
  }
}
```

POSIX (không dùng `eval` hoặc `source` file không tin cậy):

```sh
export JAVA_HOME=/path/to/jdk-21
export PATH="$JAVA_HOME/bin:$PATH"
while IFS='=' read -r name value || [ -n "$name" ]; do
  case "$name" in ''|\#*) continue ;; *[!A-Z0-9_]*|[0-9_]*) continue ;; esac
  export "$name=$value"
done < apps/backend/.env
```

Biến bắt buộc khi chạy: DB_PASSWORD, JWT_SIGNING_KEY, CURSOR_SIGNING_KEY.
Hai signing keys là **hai giá trị random độc lập**, base64 của ít nhất32 bytes; lưu ổn định trong
secret store, không sinh lại mỗi lần restart. Thiếu/sai key làm startup fail. Không có default production.
Lệnh sinh vào biến, không in ra console (lưu vào secret store theo quy trình riêng nếu cần persistence):

```powershell
$jwtBytes = New-Object byte[] 32
$cursorBytes = New-Object byte[] 32
$rng = [Security.Cryptography.RandomNumberGenerator]::Create()
$rng.GetBytes($jwtBytes); $rng.GetBytes($cursorBytes); $rng.Dispose()
$env:JWT_SIGNING_KEY = [Convert]::ToBase64String($jwtBytes)
$env:CURSOR_SIGNING_KEY = [Convert]::ToBase64String($cursorBytes)
```

```sh
export JWT_SIGNING_KEY="$(openssl rand -base64 32)"
export CURSOR_SIGNING_KEY="$(openssl rand -base64 32)"
```

Chỉ dùng lệnh sinh khi chuẩn bị môi trường lần đầu, không đặt vào startup script.

Biến tùy chọn:

- DB_URL mặc định jdbc:postgresql://localhost:5432/rideguard; DB_USER rideguard; PORT8080.
- ADMIN_ALLOWED_ORIGINS: danh sách origin chính xác ngăn bởi dấu phẩy, không wildcard/null.
  Ngoài local profile chỉ HTTPS; rỗng thì không cho web cross-origin.
- COOKIE_SECURE=true, COOKIE_SAME_SITE=Lax. Cross-site cần None+Secure và browser hỗ trợ.
- RATE_LIMIT_PER_MINUTE=120 theo IP/process; REPORTS_RETENTION_DAYS=30, tối thiểu2.
- SMTP_HOST, SMTP_PORT (587 mặc định), SMTP_USERNAME, SMTP_PASSWORD, SMTP_FROM,
  SMTP_AUTH=true, SMTP_STARTTLS=true (enable + required); timeout5 giây.
  Muốn reset hoạt động cần SMTP thật hoặc sandbox SMTP do bạn cấu hình. Không có endpoint đọc reset token.
  Chỉ trong môi trường mail local có chủ đích mới tắt SMTP_AUTH/SMTP_STARTTLS.

SMTP queue trong memory, không durable; 202 là nhận yêu cầu, không chứng minh giao email.
SMTP failure chỉ log thông báo chung không chứa địa chỉ/token/password. Không bật logging body/auth
hoặc mail.debug trên production; cấu hình hiện khóa logger web/security ở INFO để tránh DEBUG body dump.

## 4. Build, unit test, integration test

PowerShell:

```powershell
.\apps\backend\mvnw.cmd -f apps/backend/pom.xml -B -DskipTests package
.\apps\backend\mvnw.cmd -f apps/backend/pom.xml -B test
.\apps\backend\mvnw.cmd -f apps/backend/pom.xml -B verify
```

POSIX:

```sh
bash apps/backend/mvnw -f apps/backend/pom.xml -B -DskipTests package
bash apps/backend/mvnw -f apps/backend/pom.xml -B test
bash apps/backend/mvnw -f apps/backend/pom.xml -B verify
```

`test` chỉ unit/HTTP slice, không cần DB. `verify` chạy Failsafe `*IT` khi TEST_DB_URL tồn tại.
Thiếu biến này thì integration **SKIPPED**, không phải kiểm chứng DB. Test fixture keys chỉ dùng test.
Tạo một database/role test **riêng**, tên database kết thúc `_test`, không query parameters trong URL:

```sql
CREATE ROLE rideguard_test LOGIN;
-- psql: \password rideguard_test
CREATE DATABASE rideguard_test OWNER rideguard_test;
```

```powershell
$env:TEST_DB_URL = 'jdbc:postgresql://localhost:5432/rideguard_test'
$env:TEST_DB_USER = 'rideguard_test'
# Cấp TEST_DB_PASSWORD qua secret manager/môi trường, không copy password vào command history.
.\apps\backend\mvnw.cmd -f apps/backend/pom.xml -B verify
```

```sh
export TEST_DB_URL=jdbc:postgresql://localhost:5432/rideguard_test
export TEST_DB_USER=rideguard_test
# Cấp TEST_DB_PASSWORD trước khi chạy.
bash apps/backend/mvnw -f apps/backend/pom.xml -B verify
```

Integration tự chạy migration trên test DB; tạo dữ liệu UUID riêng, không xóa DB phát triển,
không clean/truncate. Test DB có thể tích lũy dữ liệu; DBA quản lý riêng. Để kiểm thử nâng từ foundation,
tạo database riêng mới `rideguard_migration_test` với cùng role test, cấp TEST_MIGRATION_DB_URL
trỏ database đó. MigrationIT yêu cầu DB chưa có Flyway history, chạy V1 rồi V2 và kiểm checksum.
Không tự clean DB đã dùng; không đặt URL này bằng TEST_DB_URL. CI tạo hai database test riêng.

## 4a. PostgreSQL kiểm thử riêng khi Docker Engine đã sẵn sàng

Quy trình đã được dùng ngày 2026-11-09 với Docker 29.6.2/PostgreSQL 17.11; các tên/port được tạo riêng mỗi lượt.
Đợi `docker info` thành công rồi chạy từ gốc repository.
Không dùng Compose database phát triển cho các test này. Mở một PowerShell riêng dành cho test,
không bật transcript, không in biến môi trường hoặc `docker inspect` toàn bộ container.

Đoạn dưới tạo container có tên ngẫu nhiên và port localhost do Docker cấp, không mount dữ liệu có sẵn.
Password chỉ sinh trong process và truyền bằng tên biến môi trường, không xuất ra console/command argument.
Hai database khác nhau: integration dùng `rideguard_test`, upgrade dùng `rideguard_migration_test` mới.
Nếu Docker cần tải image thì sẽ tải `postgres:17.11-alpine`.

```powershell
docker info --format '{{.ServerVersion}}'
if ($LASTEXITCODE -ne 0) { throw 'Docker Engine chưa sẵn sàng; không tiếp tục.' }
$rgContainer = 'rideguard-verify-' + [guid]::NewGuid().ToString('N')
$rgBytes = New-Object byte[] 32
$rgRng = [Security.Cryptography.RandomNumberGenerator]::Create()
$rgRng.GetBytes($rgBytes); $rgRng.Dispose()
$env:POSTGRES_PASSWORD = [Convert]::ToBase64String($rgBytes)
$rgCreated = $false
try {
  docker run -d --name $rgContainer -e POSTGRES_PASSWORD `
    -e POSTGRES_USER=rideguard_test -e POSTGRES_DB=rideguard_test `
    -p 127.0.0.1::5432 postgres:17.11-alpine
  if ($LASTEXITCODE -ne 0) { throw 'Không tạo được container test.' }
  $rgCreated = $true
  $rgReady = $false
  for ($i=0; $i -lt 60; $i++) {
    docker exec $rgContainer pg_isready -h 127.0.0.1 -U rideguard_test -d rideguard_test *> $null
    if ($LASTEXITCODE -eq 0) { $rgReady = $true; break }
    Start-Sleep -Seconds 1
  }
  if (!$rgReady) { throw 'PostgreSQL test chưa ready.' }
  $rgAddress = (docker port $rgContainer 5432/tcp).Trim()
  if ($LASTEXITCODE -ne 0 -or $rgAddress -notmatch '^127\.0\.0\.1:(\d+)$') {
    throw 'Không xác định được port localhost của container test.'
  }
  $rgPort = $Matches[1]
  docker exec $rgContainer createdb -U rideguard_test rideguard_migration_test
  if ($LASTEXITCODE -ne 0) { throw 'Không tạo được migration database riêng.' }
  $env:TEST_DB_URL = "jdbc:postgresql://localhost:$rgPort/rideguard_test"
  $env:TEST_MIGRATION_DB_URL = "jdbc:postgresql://localhost:$rgPort/rideguard_migration_test"
  $env:TEST_DB_USER = 'rideguard_test'
  $env:TEST_DB_PASSWORD = $env:POSTGRES_PASSWORD
  $env:DEBUG = 'false'
  # JAVA_HOME phải trỏ JDK 21; test-only signing keys do fixture cung cấp.
  .\apps\backend\mvnw.cmd -f apps/backend/pom.xml -B -ntp verify
  if ($LASTEXITCODE -ne 0) { throw 'Maven verify thất bại; xem báo cáo, không bỏ assertion.' }
  # Phải đọc Surefire/Failsafe: kỳ vọng 27 test độc lập + 15 IT thực thi, skipped=0.
} finally {
  if ($rgCreated) { docker stop $rgContainer }
  Remove-Item Env:POSTGRES_PASSWORD,Env:TEST_DB_PASSWORD -ErrorAction SilentlyContinue
}
```

Không có Maven profile riêng cho IT. `test` chạy Surefire; `verify` chạy thêm Failsafe và kiểm kết quả.
Không chạy lại MigrationIT trên chính migration DB đã có history: tạo DB mới kết thúc `_test`
và cập nhật TEST_MIGRATION_DB_URL; không clean/repair. Đoạn trên tạo container mới mỗi lần.
Container/volume được giữ sau `stop`, không xóa dữ liệu. Chỉ dừng container có tên do đoạn lệnh tạo.
`-h 127.0.0.1` tránh nhầm temporary Unix-socket server trong lúc image init rồi restart.
Đóng PowerShell test sau khi xong để bỏ các biến tạm; không tái sử dụng password/key test cho production.

Runtime cần một database test riêng còn đang chạy và credentials đã cấp. Sau khi cấu hình DB_URL,
DB_USER, DB_PASSWORD trỏ DB đó và hai signing keys tạm theo mục 3, dùng mục 5/6 và
[API examples](api-examples.md). Không dùng TEST_DB_URL như cấu hình runtime: ứng dụng đọc DB_URL.
Chạy CLI admin trong terminal tương tác trước khi bật HTTP; nhập password ẩn.
Chạy HTTP foreground bằng Maven/JAR, Ctrl+C khi xong, sau đó dừng đúng container test đã tạo.
Muốn thử restart/checksum, dùng cùng DB/keys, dừng rồi chạy lại JAR và kiểm health/readiness cùng log Flyway;
không sửa V1/V2. Kết quả dự kiến không thay thế bằng chứng runtime thực tế.

### Smoke HTTP và SMTP sink tự động

Sau khi tạo ADMIN bằng CLI ở mục 6 trên **database runtime riêng kết thúc `_test`**, giữ DB chạy,
cấp DB_URL/DB_USER/DB_PASSWORD, JAVA_HOME, JWT_SIGNING_KEY, CURSOR_SIGNING_KEY vào PowerShell test.
Script dùng Python standard library, tự mở HTTP backend và SMTP sink loopback trên port tạm, rồi dừng chúng.
Script không tạo admin thay CLI, không quản lý/dừng PostgreSQL. Password admin nhập ẩn:

```powershell
$env:RG_ADMIN_EMAIL = Read-Host 'Email ADMIN vừa tạo bằng CLI'
$rgAdminSecret = Read-Host 'Password ADMIN test' -AsSecureString
$env:RG_ADMIN_PASSWORD = [Net.NetworkCredential]::new('', $rgAdminSecret).Password
try {
  .\scripts\.venv\Scripts\python.exe scripts/backend_runtime_smoke.py
  if ($LASTEXITCODE -ne 0) { throw 'Runtime smoke thất bại; xem assertion, không in response/token.' }
} finally {
  Remove-Item Env:RG_ADMIN_PASSWORD -ErrorAction SilentlyContinue
  $rgAdminSecret = $null
}
```

Kết quả status/assertion không chứa token nằm trong `.local/backend-runtime-results.json`.
Log server `.local/backend-runtime.log`; không bật debug HTTP/mail. Sink nhận thư trong memory, không ghi reset code.
Web cookie được gửi bằng HTTP client để thử logic server; không phải kiểm browser HTTPS/SameSite thực.
ACK/report là client REST mô phỏng. Sau smoke dừng đúng container test do bạn tạo, giữ volume.
Health tổng hợp loại MailHealthIndicator (`management.health.mail.enabled=false`): SMTP tùy chọn không
chặn API/database; theo dõi giao mail riêng, không suy ra gửi mail thành công từ health UP.

## 5. Chạy backend và migration

```powershell
.\apps\backend\mvnw.cmd -f apps/backend/pom.xml spring-boot:run
# Hoặc sau build:
java -jar apps/backend/target/backend-0.1.0-SNAPSHOT.jar
```

```sh
bash apps/backend/mvnw -f apps/backend/pom.xml spring-boot:run
# Hoặc:
java -jar apps/backend/target/backend-0.1.0-SNAPSHOT.jar
```

Flyway tự chạy khi Spring context khởi động (backend, provisioning, integration tests), không khi
chỉ package/unit test. V1 giữ nguyên. V2 tạo 10 bảng/function/trigger trong `rideguard` bằng SQL
schema-qualified; Hikari đặt session timezone UTC. Không đổi vị trí history; với datasource/search path mặc định history ở `public`.
Nếu DB cũ dùng search path khác, xác minh history trước khi chạy; không tự baseline/repair/clean.

GET /actuator/health/liveness không gồm DB; /readiness gồm DB. Startup vẫn cần datasource/Flyway.
Port mặc định8080; stop process cũ hoặc đổi PORT nếu bị chiếm.

## 6. Tạo admin chủ động và đăng ký serial

Sau build, từ terminal tương tác có console, cấp cấu hình DB/keys như trên rồi chạy:

```sh
java -jar apps/backend/target/backend-0.1.0-SNAPSHOT.jar --spring.profiles.active=provision-admin
```

Lệnh dùng được trong PowerShell/POSIX. Không mở HTTP server; hỏi email/full name và password ẩn,
tạo account ADMIN mới rồi đóng context. Không password/admin mặc định, không public role assignment,
không tự promote email có sẵn. Console IDE không hỗ trợ readPassword sẽ bị từ chối; dùng terminal thật.
Không đưa password vào command argument. Khôi phục mất admin phải qua quy trình DBA/CLI chủ động.

Login admin rồi POST /api/v1/admin/devices với serial/model/hardware_revision optional.
Xem [luồng ví dụ đầy đủ](api-examples.md). Không bind serial chưa được đăng ký trước.

## 7. Client, CORS và cookie

Android emulator gọi http://10.0.2.2:8080; máy thật dùng địa chỉ LAN của máy backend và firewall phù hợp.
Không mặc định frontend hiện tại đã tích hợp; production dùng HTTPS thực.

Web: `X-Client-Type: admin_web`, Origin đúng allowlist, `X-CSRF-Protection: 1`, Content-Type JSON
cho login/refresh/logout. Browser fetch `credentials: include`; refresh/logout body `{}`. Backend kiểm ADMIN.
Cookie HttpOnly, host-only; access token trong memory frontend, không localStorage. CRUD dùng Bearer,
không dùng refresh cookie làm quyền truy cập. Preflight cho Authorization/Content-Type/X-Client-Type/
X-CSRF-Protection/Idempotency-Key. X-Client-Type không cấp role.

Chỉ HTTP localhost development:

```powershell
$env:SPRING_PROFILES_ACTIVE='local'
$env:COOKIE_SECURE='false'
$env:ADMIN_ALLOWED_ORIGINS='http://localhost:5173'
```

```sh
export SPRING_PROFILES_ACTIVE=local COOKIE_SECURE=false
export ADMIN_ALLOWED_ORIGINS=http://localhost:5173
```

Production không chọn local, COOKIE_SECURE=true, exact HTTPS origins. Header CSRF là dấu hiệu
non-simple request cùng strict Origin, không phải secret. Mobile dùng JSON refresh và bỏ qua cookie.

## 8. Dừng và bảo toàn dữ liệu

Ctrl+C tại terminal chạy backend. Nếu dùng Compose riêng của dự án:

```sh
docker compose --env-file infra/.env -f infra/compose.yaml stop
```

Giữ named volume, không dùng down -v. Không dừng container không thuộc dự án.
Xem [troubleshooting](troubleshooting.md) khi gặp lỗi; không dùng clean/xóa volume như mặc định.
