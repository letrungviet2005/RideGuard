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
