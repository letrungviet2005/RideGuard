# Tổng quan tiến độ dự án RideGuard

Cập nhật: **12/11/2026**. Phạm vi: mã nguồn trong workspace và các báo cáo kiểm chứng hiện có. Tài liệu này được lập bằng cách đọc repository; không chạy lại toàn bộ test hoặc kiểm tra phần cứng trong lần cập nhật này.

## 1. Dự án đang ở đâu?

**RideGuard đã có backend MVP và web quản trị để phát triển, kiểm thử local. Phần gương thông minh hoạt động với điện thoại và phần cứng thật chưa hoàn thành.**

- **Backend:** đã triển khai các nghiệp vụ MVP, có bằng chứng kiểm thử PostgreSQL và HTTP runtime.
- **Web quản trị:** đã có giao diện, API client và khu thử API mobile; đã kiểm thử giao diện bằng fixture, còn cần xác nhận các luồng đầy đủ với BE thật trên trình duyệt.
- **Android:** mới có khung ứng dụng và điều hướng màn hình nền tảng.
- **Firmware ESP32:** mới có khung build, startup log và interface radar; chưa đọc cảm biến hoặc điều khiển cảnh báo thật.
- **AI:** có tiện ích chia session dữ liệu; chưa có model đã train hoặc inference.
- **Phần cứng:** đang ở thiết kế/đề xuất; chưa có bằng chứng lắp ráp và vận hành hoàn chỉnh.

Không gán phần trăm hoàn thành vì chưa có danh sách công việc với trọng số thống nhất. Số API đã viết hoặc số test đạt không phản ánh đầy đủ tiến độ sản phẩm tích hợp.

## 2. Phạm vi MVP đang theo đuổi

Gương xử lý radar + IMU, đánh giá nguy cơ và phát cảnh báo HUD/rung **tại ESP32**. Điện thoại hỗ trợ tài khoản, quản lý gương, cấu hình, BLE và điều hướng GPS. Backend lưu trữ, xác thực, đồng bộ dữ liệu và phục vụ quản trị.

Yêu cầu kiến trúc: cảnh báo cốt lõi không phụ thuộc Internet hay phiên đăng nhập; dữ liệu cảm biến thiếu/không hợp lệ phải thể hiện UNKNOWN/unavailable. Đây là yêu cầu cần triển khai và kiểm thử trên thiết bị, chưa phải chức năng phần cứng đã chứng minh.

Ngoài MVP hiện tại: ghi/lưu lịch sử hành trình GPS, thống kê chuyến đi, OTA, thanh toán, chia sẻ thiết bị và chứng minh quyền sở hữu bằng BLE challenge. **Điều hướng GPS và lịch sử liên kết thiết bị vẫn thuộc MVP.**

Nguồn: [kiến trúc hệ thống](architecture/system-overview.md), [roadmap](roadmap.md), [quyết định phạm vi](decisions/0002-mvp-scope.md).

## 3. Tiến độ từng thành phần

### Backend — đã có nghiệp vụ MVP

Mã tại `apps/backend`: Java 21, Spring Boot, Maven, JDBC, PostgreSQL và Flyway.

Đã có:

- 31 operation nghiệp vụ và 3 health endpoint trong hợp đồng implemented.
- Đăng ký/đăng nhập, hash mật khẩu, access token, refresh rotation/replay, logout, quản lý phiên, đổi/reset mật khẩu.
- Quyền USER/ADMIN, kiểm tra sở hữu, khóa tài khoản, bảo vệ self-lock/last admin và CLI tạo ADMIN.
- Registry thiết bị, bind/unbind với `expected_binding_id`, nickname, lịch sử binding và idempotency.
- Config CAS/version, lịch sử version đã phát hành, ACK tăng dần và tiếp nhận report theo thứ tự thời gian.
- Feedback cá nhân và quản trị phản hồi.
- Migration V1/V2, 10 bảng nghiệp vụ, các ràng buộc SQL và transaction; SMTP adapter phục vụ reset.

Bằng chứng:

- Ngày 09/11: **27 test độc lập + 15 integration test đạt**, không failure/error/skipped ở lượt cuối trên PostgreSQL 17.11.
- Báo cáo runtime cùng ngày ghi **69 HTTP request với assertion**, bao phủ method/path của 31 API nghiệp vụ; CLI ADMIN và SMTP sink đã được thử.
- Ngày 12/11: tạo database/user riêng trên PostgreSQL Windows 18.6, V1/V2 thành công, có 10 bảng, readiness `UP`; khởi động lại thành công sau cố định Flyway history ở `public`.
- Sửa CLI tạo ADMIN để nhắc độ dài mật khẩu 12–128 ký tự Unicode và hỏi lại khi nhập sai. Lượt kiểm tra liên quan chạy **10 test đạt** (1 test provisioning mới + 9 ContractRulesTest). Không cộng lượt này thành một lần chạy lại toàn bộ suite.

Còn cần làm: integration đầy đủ trên PostgreSQL 18, smoke FE–BE thật, SMTP nhà cung cấp, HTTPS/cookie trên trình duyệt, tải/retention, backup/restore và vận hành production. Flyway hiện cảnh báo PostgreSQL 18 mới hơn phiên bản đã kiểm chứng của nó; startup đạt chưa giải quyết toàn bộ tương thích.

Nguồn: [báo cáo BE 09/11](testing/backend-verification-2026-11-09.md), [PostgreSQL local](backend/postgresql-local.md), [API implemented](../contracts/openapi.yaml).

### Web quản trị — đã có giao diện và các luồng thao tác

Mã tại `apps/admin-web`: React, TypeScript, Vite.

Đã có:

- Đăng nhập ADMIN, người dùng/khóa tài khoản, registry thiết bị/trạng thái, feedback/trả lời, hồ sơ/mật khẩu, sessions và health.
- Lab chỉ dành cho development: đăng ký USER, gương cá nhân, bind/unbind/history, config, ACK/report thủ công và feedback.
- Hai phiên ADMIN/mobile độc lập; token trong memory, admin refresh bằng cookie, mobile refresh bằng JSON.
- Xử lý 204, refresh single-flight, response cũ, phân trang cursor, idempotency và xung đột config.
- Giao diện được chỉnh ngày 12/11: sidebar có biểu tượng, tông xanh rừng, màn hình đăng nhập hai cột, form/bảng và responsive mobile.

Bằng chứng: typecheck/lint/build đạt; **16 client test** đạt ở lượt ghi nhận 09/11; **8 test Chrome** đạt ở lượt cuối 12/11. Các luồng browser đã ghi nhận gọi đủ 34 operations tới fixture. Test production xác nhận Lab không được mở trong bản build.

Giới hạn: fixture không chứng minh server thật, cookie thật hay quyền ownership phía BE. FE đã phục vụ HTTP 200 tại port 5173 nhưng chưa có báo cáo hoàn chỉnh luồng UI–BE thật. ACK/report trong Lab là mô phỏng REST, không phải kiểm chứng BLE/ESP32. Health cross-origin còn phụ thuộc CORS của `/actuator/**`.

Nguồn: [bao phủ từng API](frontend/api-coverage.md), [kiểm chứng FE](frontend/verification-status.md).

### Android — foundation, chưa triển khai nghiệp vụ

Mã tại `apps/mobile` hiện có Compose Navigation, màn hình giới thiệu/phạm vi và cấu hình API environment. Source chỉ rõ chưa kết nối gương hoặc thu vị trí.

Còn thiếu: auth/session và lưu credential an toàn; REST client; màn hình thiết bị/cấu hình/feedback; BLE scan/connect/protocol; apply config và ACK thật; GPS navigation; xử lý permissions và lifecycle.

Hai điểm cần rà trước khi bắt đầu:

- README ghi Gradle 8.11.1 nhưng `gradle-wrapper.properties` hiện trỏ **9.7.1**, còn AGP là **8.9.2**. Cần thống nhất toolchain và kiểm tra build; lần rà này không xác nhận bộ phiên bản tương thích.
- Màn hình foundation vẫn nhắc “Hành trình”, là nội dung cũ; không dùng đó làm căn cứ đưa trip recording vào MVP.

Nguồn: [README Android](../apps/mobile/README.md), source `MainActivity.kt`, `features/foundation/Foundation.kt`, `shared/networking/ApiEnvironment.kt`.

### Firmware — có khung, driver còn thiếu

Mã tại `firmware`: PlatformIO/Arduino ESP32, hai environment chọn LD2451 hoặc LD2417; mỗi binary chọn một decoder.

`main.cpp` hiện chỉ in startup log rồi yield. Decoder trả `NotImplemented` và dữ liệu unavailable. README ghi hai environment đã compile; compilation không chứng minh driver hay phần cứng hoạt động. Không có bằng chứng upload/kiểm thử thiết bị trong tài liệu đã đọc.

Còn thiếu: parser radar thật, IMU, đồng bộ thời gian, tracking, feature/risk pipeline, HUD/rung, BLE, config anti-rollback/ACK, navigation cues, quản lý nguồn và xử lý mất kết nối/cảm biến.

Nguồn: [README firmware](../firmware/README.md), `firmware/src/main.cpp`, `firmware/src/drivers/radar/`, [hợp đồng BLE đề xuất](../contracts/ble-protocol.md).

### AI và phần cứng — giai đoạn chuẩn bị

AI tại `ai`: đã có `session_split()` và test chia nguyên session ổn định vào train/validation/test trước windowing. Chưa có feature extractor, bộ dữ liệu đã xác nhận, training, evaluation, export hoặc model triển khai. Chưa có số liệu accuracy, latency hay RAM được đo.

Phần cứng tại `hardware` và model cơ khí tại `models`: có định hướng linh kiện và vỏ gương. Radar/IMU, pin map, wiring, nguồn, vị trí lắp, chống nước/rung và cơ khí còn cần chốt/kiểm tra. Chưa chứng minh linh kiện vừa vỏ hoặc cảnh báo an toàn khi vận hành.

Nguồn: [AI README](../ai/README.md), [hardware README](../hardware/README.md).

## 4. Những mốc đã đạt

- **Giai đoạn foundation:** monorepo, wrappers, hợp đồng API/BLE/schema, CI khai báo theo thành phần và tài liệu kiến trúc.
- **08/11:** backend có code MVP; 27 test độc lập đạt nhưng 15 integration còn skipped ở lần kiểm tra đó.
- **09/11:** 15 integration đã thực sự chạy đạt; HTTP runtime, CLI ADMIN, SMTP sink và restart được kiểm chứng trên môi trường test riêng.
- **09–12/11:** web quản trị và Lab được triển khai, kiểm thử fixture, sau đó chỉnh giao diện.
- **12/11:** chuyển môi trường phát triển sang PostgreSQL Windows port 5433; chạy BE/FE local; sửa Flyway default schema khi restart và thông báo độ dài mật khẩu trong CLI ADMIN.

Các mốc này có phạm vi kiểm chứng riêng, không phải một lần nghiệm thu toàn hệ thống. Có workflow CI không đồng nghĩa hosted CI đang xanh; chưa xác nhận deployment production.

## 5. Hiện có thể làm gì?

Có thể chạy BE với PostgreSQL local, tạo ADMIN bằng CLI và mở web quản trị; dùng Lab để thử các API USER bằng dữ liệu REST. Database phát triển và credentials riêng đã được chuẩn bị trên máy hiện tại.

Chưa thể xem đây là một gương cảnh báo hoàn chỉnh: chưa có luồng Android ↔ BLE ↔ ESP32 ↔ cảm biến thật đã kiểm chứng; chưa có model risk đã đánh giá.

Các địa chỉ cấu hình local: PostgreSQL `127.0.0.1:5433`, BE `http://127.0.0.1:8080`, FE `http://127.0.0.1:5173`. Đây là địa chỉ môi trường, không cam kết process luôn chạy sau khi đóng terminal. Tài khoản PostgreSQL khác tài khoản ADMIN web; tài liệu không chứa mật khẩu/token. Tình trạng ADMIN cá nhân hiện tại không được truy vấn trong lần viết báo cáo này.

Hướng dẫn thao tác: [khởi chạy dự án](huong-dan-chay-du-an.md).

## 6. Ưu tiên triển khai tiếp

1. **Chốt bản demo web–BE local:** chạy luồng thật ADMIN đăng nhập → đăng ký serial → USER bind → config → ACK/report mô phỏng → feedback → admin reply → unbind; kiểm tra refresh/reload, quyền cũ và lỗi mạng. Ghi bằng chứng riêng với fixture.
2. **Ổn định công cụ và tài liệu:** kiểm tra PostgreSQL 18/Flyway, thống nhất Gradle/AGP, sửa nội dung “foundation/chưa kiểm chứng” đã lỗi thời trong một số README. Rà thay đổi Git trước commit; workspace hiện còn nhiều file sửa/mới chưa được commit.
3. **Làm Android theo từng luồng:** networking/auth → thiết bị/binding → config/feedback; dùng hợp đồng implemented và test session/ownership ngay từ đầu.
4. **Chốt phần cứng và triển khai driver song song với Android:** chọn radar/IMU, pin map/nguồn, parser và fixture, kiểm tra cảm biến thật trước khi phát cảnh báo.
5. **Nối BLE end-to-end:** config version/binding → apply thực → ACK; status report và navigation cue/TTL. Kiểm tra mất BLE/Internet, reboot và chuyển binding.
6. **Xây dựng cảnh báo local và đánh giá:** baseline UNKNOWN-aware, radar–IMU alignment; thu session có provenance, sau đó so sánh ML với baseline trên cùng dữ liệu. Đo false alarms, dangerous misses, latency, RAM và nguồn.
7. **Chuẩn bị vận hành:** HTTPS/origin/cookie, SMTP thật, backup/restore, giám sát, tải, quản lý secret và release client/firmware.

Chưa gán deadline hoặc người phụ trách vì chưa có phân công/ước lượng được xác nhận. Với mỗi hạng mục, cập nhật riêng “đã có code”, “test đạt”, “runtime đạt” và “phần cứng đạt”.

## 7. Cách đọc các tài liệu hiện có

Ưu tiên source hiện tại và báo cáo có ngày/phạm vi rõ. Tài liệu thiết kế/flows mô tả cả hành vi dự kiến; không coi mọi bước trong flow đã được triển khai trên Android/ESP32. Một số README còn mô tả trạng thái cũ; báo cáo này chỉ ra khác biệt, không tự sửa lịch sử kiểm chứng.

- Tổng quan sản phẩm: [system overview](architecture/system-overview.md).
- Phạm vi và việc chưa làm: [roadmap](roadmap.md).
- Nghiệp vụ API: [OpenAPI implemented](../contracts/openapi.yaml), [API examples](backend/api-examples.md).
- Bằng chứng BE: [báo cáo 09/11](testing/backend-verification-2026-11-09.md), [local PostgreSQL 12/11](backend/postgresql-local.md).
- Bằng chứng FE: [verification status](frontend/verification-status.md), [API coverage](frontend/api-coverage.md).
- Chạy thử dự án: [hướng dẫn khởi chạy](huong-dan-chay-du-an.md).
