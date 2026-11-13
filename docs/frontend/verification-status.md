# Kiểm chứng frontend RideGuard

## Cập nhật ngày 2026-11-13

Layout/route guard mới: 18 client tests và 15 Chrome fixture tests đạt; typecheck, lint và build đạt. Chrome với BE thật mới xác nhận anonymous refresh 401 và deep link về login; chưa kiểm chứng login/rotation/logout ADMIN thật. Seed demo có script và unit test nhưng chưa thực thi do thiếu hai mật khẩu USER tự cấp. Xem [báo cáo chi tiết](../testing/frontend-demo-2026-11-13.md).


## Cập nhật giao diện ngày 2026-11-12

Đã chỉnh bố cục đăng nhập hai cột, sidebar có biểu tượng, màu xanh rừng và xanh nhạt,
form/bảng/modal và responsive. Đã xem ảnh desktop và mobile 390px; sửa skip-link ẩn
để không đè nội dung khi chụp trang dài. Giữ nguyên các luồng API.

Kiểm tra lần này: `npm run typecheck`, `npm run lint` đạt; `npm run test:ui` build
thành công và **8/8 test Chrome đạt, 0 lỗi, 0 skipped** (11,8 giây lượt cuối).
Các luồng Playwright vẫn dùng fixture. BE đã package bằng `-DskipTests`; đây là build,
không phải chạy lại backend tests. FE dev đã trả HTTP 200 tại `127.0.0.1:5173`.
Hướng dẫn vận hành chung: [khởi chạy dự án](../huong-dan-chay-du-an.md).

Phần bên dưới là báo cáo ngày 2026-11-09, giữ nguyên phạm vi/bằng chứng lịch sử.

Ngày 2026-11-09, trên Windows/PowerShell, Node.js 24.19.0, npm 11.17.0, Chrome headless cài trên máy. Phạm vi thay đổi: `apps/admin-web` và `docs/frontend`; không sửa backend, mobile, firmware hoặc hợp đồng API.

## Kết quả thực tế

Lệnh chạy tại `apps/admin-web`, lần cuối sau sửa code/layout:

- `npm run api:check`: exit 0, DTO/routes khớp **34 operations** hợp đồng implemented.
- `npm run typecheck`: exit 0, TypeScript strict cho app và tests.
- `npm run lint`: exit 0, không lỗi/cảnh báo ESLint. Không đồng nghĩa đã chứng minh không có memory leak.
- `npm test`: **16 chạy, 16 đạt, 0 lỗi, 0 skipped, 0 cancelled**.
- `$env:VITE_ENABLE_MOBILE_LAB='true'`; `$env:VITE_API_BASE_URL='http://127.0.0.1:8080'`; `npm run test:ui`: build thành công, **8 chạy, 8 đạt, 0 lỗi, 0 skipped, 0 flaky** (12,0 giây).
- Build trong `test:ui`: 54 modules; JS 289,28 kB (gzip 91,68 kB), CSS 3,81 kB. Test production kiểm tra không có route Lab, nút đăng ký USER, ACK/report hoặc chunk Lab dù build flag bật.
- `npm install` khi bổ sung dev dependencies: thành công, audit không báo vulnerability. Lượt kiểm tra lại `npm ci` bị **EPERM unlink esbuild.exe** do dev server của người dùng đang giữ file. Đã khôi phục bằng `npm install --ignore-scripts` (exit 0, 0 vulnerabilities; còn cảnh báo cleanup file đang dùng). Sau khôi phục, toàn bộ kiểm tra phía trên đều đạt. **Không ghi nhận clean install `npm ci` đạt**; chạy lại khi đã dừng dev server của chính mình. Không dừng server người dùng.

Report Playwright tại `apps/admin-web/test-results/report.json` (gitignored). Attachment `api-operations` của các test đạt ghi nhận request method/path từ UI tới fixture: hợp **34/34 operation IDs**, gồm 31 nghiệp vụ và 3 health. Đây là bao phủ thao tác gọi API trên UI, **không phải bao phủ mọi nhánh nghiệp vụ hay kiểm chứng BE thật**. Không đưa token/body vào attachment.

Đã xem ảnh `test-results/admin-production.png` và `test-results/lab-mobile.png`: form/sidebar desktop và màn hình 390px hiển thị được; bảng rộng cuộn ngang trong panel, không ép chữ thành từng ký tự. Đây là kiểm tra hình ảnh hai trạng thái, không phải kiểm thử mọi thiết bị.

## Test đã thực sự chạy

Checker độc lập chạy từ gốc repo: `.\scripts\.venv\Scripts\python.exe scripts/check_contracts.py` đạt (434 examples, 145 local links, 32 negative cases, 65 operations tổng implemented/proposed; 31 backend route mappings tĩnh). `git diff --check` exit 0, chỉ cảnh báo chuẩn hóa CRLF/LF. Các kiểm tra này không chạy backend.

`tests/client.test.ts`: 16 test độc lập với transport giả lập:

1. HTTP 204 không parse JSON, mobile không cookie.
2. Nhiều 401 đồng thời chỉ một refresh.
3. 401 trễ dùng token mới, không rotate thêm.
4. Refresh lỗi xóa phiên, không thử lại credential cũ.
5. 401 lần hai kết thúc phiên.
6. Logout xóa ngay, chờ refresh để revoke credential đã rotate.
7. Response riêng tư phiên cũ không ghi vào tài khoản mới.
8. Abort được tôn trọng ngay cả khi transport trả muộn.
9. Admin auth dùng CSRF/cookie và yêu cầu ADMIN.
10. USER không gọi route admin, public 401 không kích hoạt refresh.
11. Idempotency key giữ khi retry cùng payload, đổi khi payload/thao tác đổi.
12. Mutation lỗi mạng không tự gửi lại.
13. Unbind gửi chính xác expected_binding_id và đọc error envelope.
14. Health không token/cookie, không theo redirect mang credential.
15. Kiểm tra base URL.
16. Pin null là chưa có dữ liệu, 0 vẫn là 0%.

`tests/browser/flows.spec.ts`: 7 test Chrome development với `page.route` fixtures:

1. Users, cursor/filter reset, registry, xác nhận/hủy RETIRED.
2. Profile, đổi mật khẩu, sessions/revoke, admin feedback, refresh, ba health và logout.
3. Bind, nickname, config CAS, ACK/report thủ công, feedback/detail, unbind đúng snapshot, lịch sử binding.
4. 409 giữ draft, tải bản mới để đối chiếu, không tự PATCH/ACK.
5. Đăng ký USER, forgot/reset với token opaque, cách ly phiên admin/mobile.
6. Layout mobile, logout xóa dữ liệu riêng tư, không lưu token vào web storage.
7. Lỗi mạng hiện lỗi rõ, không có fallback dữ liệu giả.

`tests/browser/production.spec.ts`: 1 test build production loại route/chunk/nút Lab, kể cả cờ dev được bật lúc build.

## Lỗi đã sửa trong nhiệm vụ

- Native `fetch` được giữ như method của ApiClient gây `Illegal invocation` trong Chrome: bọc lời gọi để giữ đúng cách gọi native. Node mock không phát hiện lỗi này; các luồng Chrome chạy lại đạt.
- Label form có hint khiến accessible name không ổn định: label rõ ràng, hint liên kết bằng `aria-describedby`.
- DTO generator mặc định biến field có default thành required: dùng `defaultNonNullable: false` để khớp request contract.
- Điều phối auth queue và epoch/abort để response cũ không phục hồi phiên đã logout/đổi tài khoản; giữ refresh single-flight và không lặp mutation khi mạng lỗi.
- Bảng màn hình nhỏ có min-width trong vùng cuộn ngang.
- Tài liệu sửa lệnh CLI ADMIN sang profile thật, bổ sung local profile/cookie/CORS, phân biệt same-site và same-origin.

## Chưa kiểm chứng

Trong nhiệm vụ FE, `docker info` thất bại do không tìm thấy pipe `dockerDesktopLinuxEngine`; cổng 8080/5432 không có listener ở thời điểm kiểm tra. Không tự bật Docker Desktop, không khởi động BE/DB hoặc tạo ADMIN thật. Dev server FE mở từ terminal VS Code của người dùng được giữ nguyên. Server Playwright riêng chỉ dùng port 5176/5177.

Chưa kiểm chứng FE với PostgreSQL/BE thật: đăng nhập/cookie/Origin/CSRF thực, lỗi ownership/constraint/concurrency phía server, SMTP reset delivery, HTTPS/SameSite đa trình duyệt. Các kết quả kiểm chứng backend trước thuộc nhiệm vụ khác, không được dùng thay bằng chứng tích hợp FE. Chưa kiểm chứng ESP32/BLE; ACK/report chỉ là client mô phỏng REST.

Health UI gọi đúng routes nhưng backend hiện chỉ có CORS `/api/**`: cross-origin `/actuator/**` có thể bị chặn. Dùng readiness trực tiếp/PowerShell hoặc reverse proxy cùng origin; không tắt bảo mật trình duyệt hay sửa backend trong nhiệm vụ FE.

## Bước tiếp theo

Theo [hướng dẫn khởi chạy](../huong-dan-chay-du-an.md): bật Docker Engine hoặc chuẩn bị PostgreSQL, nạp secret backend, tạo ADMIN cá nhân bằng CLI tương tác rồi chạy BE. Đăng nhập FE bằng email/password vừa tạo. Không có tài khoản mặc định; fixture không phải tài khoản thật. Sau đó chạy smoke thật từ đăng nhập ADMIN → registry → USER Lab bind/config/ACK/report/feedback → admin reply → unbind, kiểm tra lại quyền cũ và cookie refresh/reload.

[Bảng đối chiếu 34 API](api-coverage.md) ghi màn hình và bằng chứng fixture; không đánh dấu toàn bộ API là live verified.
