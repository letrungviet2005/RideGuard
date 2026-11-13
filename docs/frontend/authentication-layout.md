# Layout và phiên đăng nhập web

Cập nhật 13/11/2026. Các URL API vẫn theo `contracts/openapi.yaml`.

## Trước và sau

Trước: `Shell` bao quanh cả form đăng nhập, nên người chưa đăng nhập vẫn thấy sidebar
và thông tin phiên. Chưa có route login riêng hoặc tự khôi phục cookie khi tải trang.

Sau: `AuthLayout` chứa đăng nhập/quên/reset mật khẩu, nhận diện thương hiệu và link Lab
chỉ khi development được bật. Không render menu quản trị hay thông tin tài khoản.
`AdminLayout` chỉ render khi có user role ADMIN và bước khôi phục đã hoàn tất.
Lab sử dụng client/provider mobile riêng; đăng nhập hoặc logout Lab không thay phiên ADMIN.

## Routes và khôi phục phiên

- `/login`: đăng nhập. `/forgot-password`, `/reset-password`: phục hồi mật khẩu trong AuthLayout.
- `/`, `/devices`, `/feedback`, `/profile`, `/sessions`, `/health`: các trang ADMIN được bảo vệ.
- Tải trang lần đầu: màn hình trung tính “Đang xác minh phiên đăng nhập…”, gọi refresh cookie
  đúng một lần mỗi instance client (kể cả React StrictMode), không render dữ liệu hoặc sidebar.
- Refresh thành công và role ADMIN: vào trang đang yêu cầu. Không có cookie/401: về login.
  USER/403: báo không đủ quyền, không render khu quản trị.
- Deep link được chuyển tới `/login?returnTo=...`. Return URL chỉ chấp nhận danh sách
  đường dẫn ADMIN cố định; URL ngoài, `//host`, backslash, encoded path hoặc auth/Lab route
  đều rơi về `/`. Không dùng URL do người dùng nhập để gọi API hay điều hướng ngoài site.
- Đăng nhập thành công trở lại đường dẫn đã kiểm tra; mặc định `/`.

## Logout, lỗi mạng và dữ liệu phiên cũ

Logout xóa access token và unmount khu quản trị ngay; epoch đổi, request cũ bị abort,
state form/bảng theo tài khoản bị hủy. Sau đó gọi logout backend qua cookie/CSRF như trước.
Refresh bị từ chối hoặc lần retry API thứ hai vẫn 401 cũng kết thúc phiên và trở về login.

Lỗi mạng/5xx khi refresh được phân biệt bằng thông báo “chưa xác minh được phiên”, không
coi là mật khẩu sai. Client xóa dữ liệu riêng tư và không tự gửi lại refresh vì kết quả
rotation có thể chưa rõ; người dùng có thể đăng nhập lại khi kết nối phục hồi. Lỗi mạng
của CRUD thông thường không tự chuyển thành lỗi credential. Không có vòng retry/redirect.

Access token chỉ ở memory. Refresh admin là cookie HttpOnly, gửi `credentials: include`
và `X-CSRF-Protection: 1`; trình duyệt tự đặt Origin. CRUD gửi Bearer. Không thêm token
vào localStorage/sessionStorage, không thay CORS/CSRF hoặc hợp đồng backend.

## Chạy và kiểm tra

Từ gốc repository:

```powershell
.\scripts\run-backend-local.ps1
```

Terminal khác:

```powershell
$env:VITE_API_BASE_URL='http://127.0.0.1:8080'
$env:VITE_ENABLE_MOBILE_LAB='true'
npm.cmd --prefix apps/admin-web run dev -- --port 5173 --strictPort
```

FE tại http://127.0.0.1:5173. `npm run typecheck`, `npm run lint`, `npm test`,
`npm run test:ui` chạy tại `apps/admin-web`. Các test trong `tests/browser` dùng fixture,
bao gồm anonymous/deep link, pending restoration, ADMIN, USER, logout, expired session,
network failure, return URL và production không có Lab.

Để kiểm tra Chrome với BE thật, cấp riêng `DEMO_ADMIN_EMAIL` và `DEMO_ADMIN_PASSWORD`
trong môi trường process rồi chạy từ gốc repo (FE5173 và BE8080 phải đang chạy):

```powershell
node apps/admin-web/scripts/live-auth-smoke.mjs
```

Chỉ kiểm tra deep link anonymous với BE thật, không cần credential:

```powershell
node apps/admin-web/scripts/live-auth-smoke.mjs --anonymous
```

Chế độ này yêu cầu refresh trả 401 và login không có sidebar; không thử login/rotation/logout.

Script này không dùng route fixture: thử login, cookie HttpOnly, reload/rotation và logout,
không ghi token/body, trace hoặc storageState. Thiếu credentials thì exit 2, không ghi PASS.
Nó dùng browser context riêng, logout phiên test của chính mình; không revoke các phiên khác.
Biến trong `.env` không tự nạp vào Node; nạp bằng loader tin cậy như runbook, không in giá trị.

Xem [dữ liệu demo](../backend/demo-data.md) và [báo cáo kiểm chứng](../testing/frontend-demo-2026-11-13.md).

## Kiểm tra web và xử lý lỗi


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
- `npm test`: 18 unit/client tests với transport giả lập, không phải integration PostgreSQL.
- `test:ui`: build, 14 test Chrome development fixture và 1 kiểm tra production. Cần Chrome hoặc `$env:PLAYWRIGHT_CHANNEL='msedge'`. Server test riêng port 5176/5177, không dùng lại server có sẵn; tự dừng sau test. Report `test-results/report.json`, ảnh `test-results/*.png` bị gitignore. Fixtures chỉ trong test, không có fallback production.
- `npm run build`, `npm run preview`: output `dist/`, preview port 4173. Nếu thử BE qua preview thêm chính xác origin vào allowlist. Production cần HTTPS, cookie Secure và SPA fallback về `index.html`; Lab không có route/module trong bundle.
- Windows EPERM khi `npm ci` đụng `esbuild.exe`: dừng dev server của chính bạn bằng Ctrl+C trước khi cài; không dừng mọi tiến trình Node hoặc server người khác.

Từ gốc repository, checker độc lập:

```powershell
.\scripts\.venv\Scripts\python.exe scripts/check_contracts.py
git diff --check
```

### Xử lý lỗi và giới hạn

- **Không kết nối được DB:** môi trường hiện tại dùng PostgreSQL Windows port 5433; kiểm tra service và cấu hình theo hướng dẫn chung. Docker chỉ cần khi chủ động chọn môi trường Docker riêng. Không coi fixture là BE đạt.
- **NETWORK_OR_CORS:** kiểm tra BE8080, origin FE đúng allowlist. Cùng `http://127.0.0.1` khác port là same-site nhưng cross-origin nên vẫn cần CORS/credentials; `localhost` và `127.0.0.1` không hoán đổi. Trình duyệt tự đặt Origin, FE không giả lập header này. Không dùng `no-cors`.
- **Health trong FE lỗi nhưng mở URL trực tiếp được:** backend hiện chỉ cấu hình CORS cho `/api/**`, không cho `/actuator/**`. Dùng `Invoke-RestMethod http://127.0.0.1:8080/actuator/health/readiness` hoặc reverse proxy cùng origin. FE không sửa BE để bỏ giới hạn này.
- **Cookie local:** profile `local`, `COOKIE_SECURE=false`, SameSite Lax và cùng host/scheme. Production phải HTTPS/Secure; chưa kiểm chứng cookie thật đa trình duyệt trong nhiệm vụ này.
- **401:** single-flight refresh, mỗi request tối đa một retry xác thực; refresh lỗi thì xóa phiên, không lặp credential cũ. Lỗi mạng mutation không tự gửi lại. Bind/feedback giữ idempotency key cho cùng payload trong form đang mở; reload làm mất key, cần đối chiếu server trước thao tác mới.
- **409 config:** giữ bản nháp, hiển thị bản server để so sánh; tải lại/bỏ nháp rõ ràng trước lần lưu tiếp. Không tự ghi đè/ACK.
- **Unbind:** hộp xác nhận giữ `expected_binding_id` từ dữ liệu đã xem; lỗi stale binding phải tải lại, không ép retry bằng binding mới.
- **ADMIN_SELF_LOCK / LAST_ADMIN / 403:** hiển thị lỗi và `request_id` từ BE; UI không bỏ kiểm tra quyền backend.

Xem [bao phủ từng API](api-coverage.md) và [kết quả thực tế](verification-status.md). Đã có smoke anonymous với BE thật; login/rotation/logout ADMIN thật, email ngoài, HTTPS cookie và phần cứng vẫn chưa có bằng chứng nghiệm thu đầy đủ.

Cách chạy BE/FE, tạo ADMIN và cấu hình PostgreSQL: [hướng dẫn chung](../huong-dan-chay-du-an.md).
