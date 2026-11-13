# RideGuard

Monorepo cho gương chiếu hậu thông minh xe máy. **Backend MVP, web quản trị và Android REST đã có mã triển khai; tích hợp BLE/cảm biến/cảnh báo ESP32 chưa hoàn thành.**

- [Tiến độ hiện tại và việc tiếp theo](docs/tien-do-du-an.md)
- [Khởi chạy ba ứng dụng trên Windows](docs/huong-dan-chay-du-an.md)
- [Mục lục tài liệu và thứ tự đọc](docs/README.md)

## Các thành phần

- [apps/backend](apps/backend/README.md): Java 21 / Spring Boot / JDBC / PostgreSQL, API MVP và CLI ADMIN.
- [apps/admin-web](apps/admin-web/README.md): React / TypeScript / Vite, quản trị và development Lab REST.
- [apps/mobile](apps/mobile/README.md): Kotlin / Compose, tài khoản, thiết bị, cấu hình và feedback qua REST.
- [firmware](firmware/README.md), [ai](ai/README.md), [hardware](hardware/README.md), `models/`: nền tảng thiết bị, dữ liệu và cơ khí đang phát triển.
- `contracts/`: OpenAPI implemented/proposed, BLE và JSON Schemas; `infra/`: PostgreSQL Docker tùy chọn; `scripts/`: launcher/checker/seed; `docs/`: hướng dẫn và bằng chứng.

## Đọc trước khi làm tiếp

Máy local dùng PostgreSQL Windows tại **127.0.0.1:5433**, BE **8080**, web **5173**. Không cần Docker cho môi trường này. Secret trong file riêng bị gitignore, không có mật khẩu đăng nhập mặc định. Hướng dẫn chạy không cam kết các dịch vụ hiện đang hoạt động.

**Android hiện có vướng wrapper:** URL Gradle 9.7.1 không khớp checksum 8.11.1 đang ghi. Bộ build đã kiểm chứng dùng 8.11.1; xem báo cáo tiến độ trước khi build lại. Không bỏ checksum để chạy tiếp.

Cảnh báo radar + IMU dự kiến xử lý local ESP32, không phụ thuộc tài khoản/Internet. Chưa có bằng chứng hệ thống gương hoàn chỉnh hoặc đánh giá an toàn. GPS navigation thuộc MVP; ghi/lưu lịch sử hành trình, OTA, payment và sharing ngoài MVP.

Xem [kiến trúc](docs/architecture/system-overview.md), [roadmap](docs/roadmap.md), [OpenAPI implemented](contracts/openapi.yaml) và [CONTRIBUTING](CONTRIBUTING.md). Báo cáo kiểm thử phân biệt fixture, PostgreSQL integration, HTTP/browser runtime và phần cứng; không suy ra tất cả đã đạt từ BUILD SUCCESS.
