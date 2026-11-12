# Kiểm chứng layout xác thực và seed demo — 13/11/2026

Phạm vi: frontend quản trị và CLI seed local. Đã đọc mã hiện tại, hợp đồng API,
git status và giữ các thay đổi sẵn có. Không tìm thấy AGENTS.md trong repository
hoặc các thư mục tổ tiên đã kiểm tra. Không sửa hợp đồng API, V1/V2, mobile/firmware;
không push/deploy. Workspace có nhiều thay đổi từ trước; không coi toàn bộ git diff
là thay đổi của lượt này.

## Thay đổi

- AuthLayout riêng cho login/quên/reset mật khẩu; AdminLayout chỉ render khi ADMIN
  đã được xác thực. Loading khôi phục phiên không có sidebar hay dữ liệu riêng tư.
- Deep link được bảo vệ, return URL dùng allowlist đường dẫn nội bộ. USER bị chặn.
- Khôi phục cookie một lần, giữ refresh single-flight; logout/refresh bị từ chối
  xóa token và hủy state/request của phiên trước. Lỗi mạng có thông báo riêng,
  không tự lặp refresh. Không lưu token vào browser storage.
- Lab vẫn có client mobile riêng và không mở trong production.
- Seed chỉ chạy chủ động, chặn database khác local rideguard:5433, dùng transaction
  và các service hiện có; bỏ qua dữ liệu đã tồn tại thay vì reset trạng thái.

Các file chính: `apps/admin-web/src/app/{App,AuthLayout,AdminLayout}.tsx`,
`src/app/routes.ts`, `src/shared/api/client.ts`, `src/features/auth/Auth.tsx`,
`src/features/dev-api-lab/Lab.tsx`, `src/style.css`; các test client/browser và
`scripts/live-auth-smoke.mjs` trong admin-web. Phần backend thêm
`src/main/java/com/rideguard/demo/LocalDemoSeed.java`,
`src/main/resources/application-seed-demo.yaml`,
`src/test/java/com/rideguard/demo/LocalDemoSeedTest.java`;
launcher ở `scripts/seed-demo-local.ps1` từ gốc repository.

## Kết quả thực tế

- FE typecheck, lint, API client checker (34 operations), build: đạt.
- Client tests: **18 chạy, 18 đạt, 0 failed/skipped**.
- Chrome Playwright fixture: **15 chạy, 15 đạt, 0 failed/skipped** lượt cuối.
  Gồm 7 test guard mới, 7 flow và 1 production. Lượt đầu có 1 test cũ lỗi vì tìm
  menu sau logout; cập nhật để yêu cầu menu biến mất và URL về login, giữ kiểm tra
  form đăng nhập/storage. Không bỏ assertion bảo vệ phiên.
- Guards bao phủ anonymous, restoration pending, ADMIN, USER, logout/expired
  refresh, deep link, lỗi mạng và return URL ngoài site. Test production chặn Lab.
- Đã xem ảnh login desktop và Lab mobile 390px; không thấy sidebar ở login.
- Java: lượt package chọn LocalDemoSeedTest, AdminProvisioningTest,
  ContractRulesTest có **12 tests đạt, 0 failure/error/skipped**. Sau khi bổ sung
  test rerun, chạy riêng LocalDemoSeedTest: **3 đạt, 0 failure/error/skipped**.
  Đây là test guard/marker và mock kiểm tra không gọi mutation khi bản ghi đã có;
  không phải bằng chứng seed đã chạy trên PostgreSQL. Không chạy lại toàn bộ BE suite.
- Parser PowerShell: launcher seed không có lỗi cú pháp.

## BE và trình duyệt thật

PostgreSQL Windows có sẵn tại 127.0.0.1:5433; không dùng Docker. BE profile local
khởi động được, readiness UP; Flyway xác nhận hai migration hiện có. PostgreSQL
18.6 vẫn có cảnh báo mới hơn phiên bản Flyway đã kiểm chứng (17).

Chrome với FE5173/BE8080 thật, không route fixture: POST refresh khi chưa có cookie
trả **401**, truy cập `/devices` chuyển về `/login?returnTo=%2Fdevices`, form login
hiện và sidebar không tồn tại. Smoke này đạt. Không suy ra login, refresh rotation
hoặc logout ADMIN thật đạt từ thử nghiệm anonymous.

**Chưa kiểm chứng ADMIN browser thật:** mật khẩu được cung cấp không đáp ứng giới
hạn 12–128 ký tự của hợp đồng hiện tại; không gửi login với mật khẩu đó, không đổi
mật khẩu ADMIN. `DEMO_ADMIN_PASSWORD` trong file riêng còn trống lúc kiểm tra.
Script live đầy đủ đã có, cần cấp credential ADMIN hợp lệ để chạy.

## Dữ liệu demo: chưa thực thi

Đã xác nhận DB có ADMIN hiện hữu và kiểm tra dữ liệu trước seed. Hai biến
`DEMO_USER1_PASSWORD`, `DEMO_USER2_PASSWORD` còn trống; người dùng chọn tự cấp,
nên không sinh mật khẩu thay thế và không chạy seed.

**Chưa tạo USER, thiết bị, binding, config, issued-version hay feedback demo.**
Chưa có kết quả CREATED/SKIPPED từ database; chưa kiểm chứng chạy lại hai lần hoặc
giữ dữ liệu đã sửa trên PostgreSQL. Dòng SKIPPED trong unit test dùng mock không
phải dữ liệu thật. Không tạo ACK, reports hoặc telemetry.

## Lệnh chạy và chạy lại

PowerShell từ gốc repository:

```powershell
npm.cmd --prefix apps/admin-web run typecheck
npm.cmd --prefix apps/admin-web run lint
npm.cmd --prefix apps/admin-web run api:check
npm.cmd --prefix apps/admin-web test
$env:VITE_API_BASE_URL='http://127.0.0.1:8080'
$env:VITE_ENABLE_MOBILE_LAB='true'
npm.cmd --prefix apps/admin-web run test:ui

$env:JAVA_HOME=(Resolve-Path '.local/jdk21/jdk-21.0.13+9').Path
$env:DEBUG='false'
.\apps\backend\mvnw.cmd -f apps/backend/pom.xml -B '-Dtest=LocalDemoSeedTest,AdminProvisioningTest,ContractRulesTest' package
.\apps\backend\mvnw.cmd -f apps/backend/pom.xml -B '-Dtest=LocalDemoSeedTest' test
```

BE đã được chạy bằng `.\scripts\run-backend-local.ps1`. FE dev dùng:

```powershell
npm.cmd --prefix apps/admin-web run dev -- --port 5173 --strictPort
```

Terminal khác, giữ FE/BE đang chạy:

```powershell
# Đã chạy đạt, không cần credential:
node apps/admin-web/scripts/live-auth-smoke.mjs --anonymous
# Chưa chạy: cần DEMO_ADMIN_EMAIL và DEMO_ADMIN_PASSWORD trong process environment:
node apps/admin-web/scripts/live-auth-smoke.mjs
```

Lệnh seed dưới đây **chưa chạy**. Điền hai mật khẩu USER bằng editor vào
`apps/backend/.env`, sau đó chạy hai lần để kiểm tra idempotency:

```powershell
.\scripts\seed-demo-local.ps1
.\scripts\seed-demo-local.ps1
```

Nếu thiếu biến, launcher hỏi ẩn; không đặt mật khẩu trực tiếp trong command history.
Không cần mật khẩu ADMIN cho seed. Xem [dữ liệu và quy tắc bỏ qua](../backend/demo-data.md)
và [layout/phiên đăng nhập](../frontend/authentication-layout.md).

Python hệ thống thiếu `openapi_spec_validator`; đã chuẩn bị venv riêng gitignored
theo lockfile, không đổi dependency ứng dụng:

```powershell
python -m venv .local/contracts-venv
.\.local\contracts-venv\Scripts\python.exe -m pip install -r scripts/requirements.lock
.\.local\contracts-venv\Scripts\python.exe scripts/check_contracts.py
git diff --check
```

Contract checker đạt: 34 operations implemented, 31 proposed, 31 source route
mappings, 434 examples và 32 negative cases. Đây là kiểm tra tĩnh, không thay thế
integration/runtime. `git diff --check` đạt; Git có cảnh báo chuẩn hóa CRLF/LF
trên một số file sẵn có, không có lỗi whitespace.

Cuối lượt đã dừng Java BE và Node FE do nhiệm vụ này khởi chạy. PostgreSQL có sẵn
vẫn chạy; không dừng dịch vụ khác. Để dùng lại, mở hai terminal chạy BE và FE theo
các lệnh bên trên. Không tự chạy seed khi khởi động BE.
