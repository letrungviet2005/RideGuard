# RideGuard

Nền tảng monorepo cho gương chiếu hậu thông minh xe máy. Đây là bộ khung phát triển,
không phải sản phẩm cảnh báo đã kiểm chứng an toàn hoặc đã thử phần cứng.

Kiến trúc **proposed MVP** đặt radar + IMU, đánh giá nguy cơ, HUD và rung trên ESP32.
Backend, Internet, tài khoản và GPS điện thoại không nằm trong đường xử lý cảnh báo.
Dữ liệu thiếu/lỗi không là SAFE. Android phụ trách BLE/điều hướng, tài khoản, thiết bị,
cấu hình và phản hồi; mobile/web chủ động gọi REST backend. GPS vẫn dùng cho điều hướng,
nhưng ghi/lưu/đồng bộ lịch sử hành trình là **deferred**, ngoài MVP.
Không có OTA, thanh toán, chia sẻ thiết bị hoặc tải toàn bộ radar lên cloud.

## Cấu trúc

```text
RideGuard/
├── apps/
│   ├── backend/       # Java 21 / Spring Boot / Maven wrapper
│   ├── admin-web/     # React / TypeScript / Vite + package-lock
│   └── mobile/        # Kotlin / Compose / Gradle wrapper
├── firmware/         # PlatformIO, chỉ một radar driver mỗi binary
├── ai/               # Python package, chia dữ liệu theo phiên
├── tools/            # Phạm vi công cụ thiết bị dự kiến
├── contracts/        # OpenAPI implemented/proposed tách riêng, BLE, JSON Schemas
├── hardware/         # Quyết định phần cứng chưa chốt
├── infra/            # PostgreSQL Compose, volume, healthcheck
├── docs/             # Kiến trúc, DB/ERD, API, flows, setup, verification, ADR
├── scripts/          # Kiểm tra hợp đồng và môi trường riêng
├── .github/          # CI theo thành phần, issue/PR templates
└── models/           # STL do người dùng quản lý, không di chuyển trong đợt tài liệu
```

Chỉ một Git repository gốc, không submodule. Repository đã có commit/remote do người dùng
thiết lập; đợt cập nhật tài liệu MVP này không commit/push/deploy hoặc thay cấu hình Git.
Không tạo thư mục rỗng cho nghiệp vụ chưa có mã. Trách nhiệm từng feature/module nằm ở
[system overview](docs/architecture/system-overview.md).

## Trạng thái

- **Implemented:** backend khởi động, PostgreSQL/Flyway migration nền tảng, health probes,
  security deny-by-default, API error/validation infrastructure và integration checks.
- **Implemented:** admin router/layout/API client/config URL; mobile Compose foundation,
  navigation và cấu hình môi trường. Không có dữ liệu hay kết nối thiết bị giả.
- **Implemented:** firmware startup log, chọn decoder khi build, dữ liệu optional/unavailable;
  Python package/chia phiên/tests; hợp đồng, CI và tài liệu nhóm.
- **Proposed MVP:** auth/users/devices/binding history/config/reports/feedback/admin, BLE,
  navigation, radar + IMU/HUD/rung/tracking/risk và nghiên cứu ML nhẹ. Database có bảy bảng
  nghiệp vụ cộng ba bảng kỹ thuật, tất cả mới là thiết kế.
- **Deferred:** hành trình/GPS recording, Room trip queue, BLE possession proof, OTA,
  payment/sharing. [Roadmap](docs/roadmap.md) phân biệt rõ với GPS navigation và lịch sử binding.

Dev bắt đầu ở [API MVP giải thích](docs/api/proposed-api.md) và
[OpenAPI proposed](contracts/openapi.proposed.yaml). [OpenAPI implemented](contracts/openapi.yaml)
chỉ health; không xem proposed là API đang chạy. Màn hình mobile foundation còn copy nhắc
hành trình của thiết kế cũ; chưa sửa source UI trong nhiệm vụ tài liệu này.

Mỗi thành phần có README riêng và build độc lập: [backend](apps/backend/README.md),
[admin](apps/admin-web/README.md), [mobile](apps/mobile/README.md),
[firmware](firmware/README.md), [AI](ai/README.md).

## Bắt đầu

Prerequisites: Git, Node 24.19.0, JDK 21, Python 3.11+, Docker Compose v2; Android SDK 35
và build-tools 35.0.0 cho mobile. Maven 3.9.11/Gradle 8.11.1 dùng wrapper. Spring Boot
3.5.16, Vite 7.3.7, PlatformIO 6.1.18/espressif32 6.12.0, PostgreSQL 17.11 được ghim rõ.
[Phiên bản và nguồn chính thức](docs/development/toolchain.md).

Lệnh dưới chạy từ thư mục thành phần; dùng môi trường Python riêng cho ai/firmware/scripts.
Windows thay `./mvnw` bằng `./mvnw.cmd`, `./gradlew` bằng `./gradlew.bat`.

```sh
# root: copy infra/.env.example to infra/.env and set a local-only password first
docker compose --env-file infra/.env -f infra/compose.yaml up -d --wait

# apps/backend (export DB_PASSWORD matching PostgreSQL first)
./mvnw spring-boot:run
./mvnw -B verify

# apps/admin-web
npm ci
npm run dev
npm run typecheck
npm run lint
npm run build

# apps/mobile (JDK 21, ANDROID_HOME configured)
./gradlew :app:assembleDebug :app:lintDebug

# firmware (activate its own .venv first)
python -m pip install -r requirements.lock
python -m platformio run -e ld2451 -e ld2417

# ai (activate its own .venv first)
python -m pip install -e .
python -m unittest discover -s tests -v
python -m pip wheel --no-deps . -w dist
```

Chi tiết PowerShell, biến môi trường, hostname, contracts check và xử lý đường dẫn Windows:
[local setup](docs/development/local-setup.md). Kết quả thực tế và giới hạn:
[verification status](docs/testing/verification-status.md).

## Giới hạn và bước tiếp theo

Không có kiểm thử trên ESP32/cảm biến/motor/TFT; không giả định radar xuyên lớp phản xạ
gương hoặc ID radar ổn định. Chưa chốt radar, IMU, pin map, nguồn, giao thức BLE/security,
deployment origins/CSRF, provisioning và retention. Token transport đã có đề xuất riêng
cho mobile/web nhưng chưa triển khai. Không có tài khoản mặc định; API nghiệp vụ bị chặn.
Vite URL là cấu hình công khai trong bundle, không chứa secret. Không có API vị trí/hành
trình trong MVP. Không commit dữ liệu lớn, GPS cá nhân hay log nhạy cảm.

Ưu tiên chốt phần cứng và tài liệu protocol, thu một phiên dữ liệu có provenance rồi
triển khai decoder + fixture tests và baseline risk xử lý UNKNOWN rõ ràng. Nhánh ứng dụng
có thể tiếp tục auth và ownership bằng migration theo thiết kế đã review, sau đó BLE,
configuration ACK/status reports và feedback. Kiểm tra tài liệu chỉ cần môi trường scripts
sẵn có, không chạy các lệnh bật stack bên trên. Xem [CONTRIBUTING](CONTRIBUTING.md),
[SECURITY](SECURITY.md), [ADR MVP](docs/decisions/0002-mvp-scope.md).
