# RideGuard Admin Web

Cập nhật 13/11: [AuthLayout/AdminLayout và khôi phục phiên](../../docs/frontend/authentication-layout.md),
[seed demo local](../../docs/backend/demo-data.md),
[kết quả kiểm thử và giới hạn](../../docs/testing/frontend-demo-2026-11-13.md).
Web tự khôi phục cookie khi tải trang; chỉ render sidebar sau khi xác nhận ADMIN.

React + TypeScript + Vite, kết nối API thật theo `contracts/openapi.yaml`. Có giao diện cho 31 operations nghiệp vụ và 3 health; không tự chuyển sang dữ liệu giả khi backend lỗi.

- Web ADMIN: người dùng, registry thiết bị, feedback, hồ sơ/mật khẩu, phiên đăng nhập và health.
- Lab development: đăng ký USER, gương cá nhân, lịch sử binding, config/CAS, ACK/report mô phỏng, feedback. Nhãn **“Thử API mobile — không kết nối thiết bị”**. Không kiểm chứng BLE/ESP32. Route/module Lab bị loại khỏi production.
- Phiên admin và lab độc lập, token chỉ ở memory. Admin refresh bằng cookie HttpOnly, auth gửi `credentials: include` và CSRF header; CRUD dùng Bearer. Mobile refresh bằng JSON, không cookie. Không lưu token vào localStorage/sessionStorage.

## Chạy FE

PowerShell từ gốc repository, Node.js 24+:

```powershell
Set-Location apps/admin-web
npm ci
if (!(Test-Path .env)) { Copy-Item .env.example .env }
$env:VITE_API_BASE_URL = 'http://127.0.0.1:8080'
$env:VITE_ENABLE_MOBILE_LAB = 'true' # Chỉ khi cần thử API mobile
npm run dev -- --port 5173 --strictPort
```

Mở **http://127.0.0.1:5173**. Dùng `Ctrl+C` để dừng. Biến `VITE_*` công khai trong bundle, tuyệt đối không chứa secret. `.env` không được commit. Base URL chỉ là origin HTTP(S), không thêm `/api`, query hoặc credentials. Nếu Windows báo EPERM với esbuild khi `npm ci`, dừng dev server của chính bạn trước khi cài lại.

## Tài khoản đăng nhập và backend

**Không có tài khoản ADMIN mặc định.** Tài khoản fixture trong test không tồn tại trong database thật; tài khoản của đợt kiểm chứng backend trước thuộc database tạm và không có mật khẩu dùng lại. Nhiệm vụ FE này chưa xác định được tài khoản trong DB phát triển vì Docker Engine/backend không hoạt động.

Làm theo [runbook frontend](../../docs/frontend/runbook.md) mục 2 để chuẩn bị PostgreSQL, nạp biến backend, chạy CLI tương tác tạo ADMIN rồi khởi chạy BE. Đăng nhập bằng email/mật khẩu **do bạn nhập khi tạo ADMIN**. CLI dùng profile `provision-admin`, không có tùy chọn `--admin-email`.

Để HTTP local hoạt động, backend cần profile `local`, `COOKIE_SECURE=false`, `COOKIE_SAME_SITE=Lax`, `ADMIN_ALLOWED_ORIGINS=http://127.0.0.1:5173`. Dùng thống nhất host `127.0.0.1` cho FE/BE; khác port là cross-origin nhưng vẫn same-site nếu cùng scheme/host. Không áp dụng cấu hình cookie HTTP này cho production.

## Kiểm tra

Tại `apps/admin-web`:

```powershell
npm run api:check
npm run typecheck
npm run lint
npm test
$env:VITE_ENABLE_MOBILE_LAB = 'true'
$env:VITE_API_BASE_URL = 'http://127.0.0.1:8080'
npm run test:ui
```

`test:ui` build production và chạy Chrome headless: 7 luồng development dùng fixture mạng, 1 kiểm tra production không mở Lab dù cờ bật. Cần Chrome cài sẵn; có thể chọn Edge bằng `$env:PLAYWRIGHT_CHANNEL='msedge'`. Không phải integration test backend. `npm run build` tạo `dist/`; `npm run preview` xem tại port 4173 (nếu kết nối BE cần thêm origin tương ứng). Hosting production cần fallback SPA về `index.html` và HTTPS.

DTO/routes sinh từ hợp đồng bằng `npm run api:generate`; `api:check` phát hiện drift. Sau thay đổi hợp đồng cần rà lại UI, không chỉ sinh types.

## Tài liệu bàn giao

- [Runbook: lệnh chạy, tài khoản, xử lý lỗi](../../docs/frontend/runbook.md)
- [Đối chiếu từng API](../../docs/frontend/api-coverage.md)
- [Kết quả kiểm chứng và giới hạn](../../docs/frontend/verification-status.md)

Client xử lý 204, refresh single-flight, tối đa một retry sau 401, chặn response của phiên cũ và hủy request khi đổi bộ lọc. Mutation lỗi mạng không tự gửi lại; bind/feedback giữ idempotency key khi thử lại cùng payload trong form hiện tại. Key mất khi đóng/tải lại trang: kiểm tra trạng thái server trước khi tạo thao tác mới. Config 409 giữ bản nháp để đối chiếu, không tự ghi đè; unbind gửi snapshot `expected_binding_id` đã xác nhận.
