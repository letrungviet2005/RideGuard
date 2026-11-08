# Khắc phục lỗi backend

Các bước dưới là hướng dẫn, chưa được chạy với database/SMTP thật trong lần triển khai này.
Không in biến môi trường, token hoặc body auth vào log khi điều tra.

- **Thiếu DB_PASSWORD:** Spring không tự nạp `.env`. Nạp process environment theo [runbook](runbook.md),
  kiểm tra biến có tồn tại mà không in giá trị. Không thêm password mặc định vào YAML.
- **Connection refused:** kiểm tra đúng host/port/database và PostgreSQL đã sẵn sàng. Docker chỉ là lựa chọn;
  nếu Docker daemon chưa chạy thì người vận hành mở chủ động, không tự dừng container khác. Không gọi localhost
  từ emulator (dùng10.0.2.2); backend ngoài Docker dùng port publish, không hostname service postgres.
- **Password authentication failed:** credential phải khớp account trong DB hiện có. Sửa `.env` không đổi password
  đã lưu trong volume; quản trị bằng psql/DBA, không xóa volume để tạo lại.
- **Port8080 bị chiếm:** Ctrl+C đúng backend cũ hoặc đổi PORT; không kill process không rõ nguồn.
- **Flyway checksum/migration lỗi:** giữ V1, kiểm tra history/schema/search path và migration lỗi.
  Không clean/repair/baseline tự động, không xóa database. V2 chưa chạy ở máy triển khai này; xem SQL và chạy
  integration trên DB test riêng trước. Chỉ viết migration tiếp theo khi bản cũ đã áp dụng.
- **JWT/cursor key lỗi:** base64 giải mã >=32 bytes, hai key độc lập và ổn định. Không dùng placeholder,
  không random lại khi restart; đổi JWT key làm token cũ mất hiệu lực, đổi cursor key làm cursor cũ invalid.
- **401:** thiếu/sai/hết hạn Bearer, account LOCKED hoặc family revoked/expired. JWT đúng chữ ký chưa đủ.
  Refresh reuse revoke toàn family; client phải single-flight refresh, không retry token cũ vô hạn.
- **403 web:** kiểm tra Origin chính xác (scheme/host/port), header X-CSRF-Protection:1, X-Client-Type,
  application/json, role ADMIN; không tắt CSRF toàn cục hoặc dùng wildcard credentialed CORS.
- **Cookie không lưu/gửi:** HTTPS/Secure/SameSite/credentials:include và host phải khớp. HTTP localhost
  phải chủ động chọn local profile+COOKIE_SECURE=false; production không được dùng cấu hình đó.
- **Reset202 nhưng chưa có email:** 202 chỉ nhận queue; kiểm tra SMTP host/port/TLS/auth/from và log lỗi chung.
  Fake mail adapter chỉ trong test. Queue trong memory có thể mất khi process crash; yêu cầu lại sau khi sửa SMTP.
  Không tạo endpoint/log trả token reset. SMTP thật chưa được kiểm chứng.
- **409 VERSION_CONFLICT:** GET config mới và cho người dùng quyết định, không tự đổi expected_version để overwrite.
  ACK chỉ issued version của active binding; binding mới có config riêng.
- **409 IDEMPOTENCY_KEY_EXPIRED:** cache7 ngày hết hạn, marker vẫn giữ; đọc resource trước quyết định thao tác mới.
  BINDING_NO_LONGER_ACTIVE không tự bind lại; retry unbind phải giữ expected_binding_id của lần gốc.
- **400 INVALID_CURSOR:** không sửa/chuyển cursor sang caller/route/filter khác; bắt đầu trang đầu lại.
- **413/415:** body mutation tối đa32KiB đọc thực tế, chỉ JSON; compressed Content-Encoding không hỗ trợ.
- **429:** chờ Retry-After; limiter per-IP/process, proxy/multi-instance cần cấu hình vận hành thích hợp.
- **Integration SKIPPED:** thiếu TEST_DB_URL; không được gọi là DB pass. URL phải trỏ database riêng kết thúc
  `_test`, cùng TEST_DB_USER/PASSWORD. Không dùng DB phát triển làm test.

Không mặc định Android/admin web/firmware tương thích sau khi backend đổi hợp đồng. BLE anti-rollback vẫn là
yêu cầu chưa triển khai phần cứng, không được suy ra từ backend ACK monotonic.
