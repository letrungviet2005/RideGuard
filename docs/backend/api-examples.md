# Ví dụ API backend

Luồng tương ứng đã chạy bằng HTTP client trên PostgreSQL test ngày 2026-11-09;
xem [báo cáo thực tế](../testing/backend-verification-2026-11-09.md).
Các lệnh PowerShell/curl bên dưới vẫn là ví dụ hướng dẫn, không phải transcript đã chạy nguyên văn.
JSON response nếu được mô tả là minh họa. Chuẩn bị DB/keys/SMTP/admin theo [runbook](runbook.md).
Không commit file chứa token; không in `$login`, `$adminLogin` hoặc bật HTTP body logging.

## PowerShell: luồng từ đăng ký đến feedback

Chạy từ gốc repository, backend tại localhost8080. Password nhập ẩn, token lấy trực tiếp vào biến.
Email `example.invalid` là ví dụ; thay bằng địa chỉ bạn kiểm soát nếu thử SMTP reset.

```powershell
$base = 'http://localhost:8080/api/v1'
$email = Read-Host 'Email người dùng mới'
$secret = Read-Host 'Password (12–128 ký tự)' -AsSecureString
$password = [Net.NetworkCredential]::new('', $secret).Password
$user = Invoke-RestMethod -Method Post -Uri "$base/auth/register" -ContentType 'application/json' -Body (@{
  email=$email; full_name='Người dùng thử'; password=$password
} | ConvertTo-Json)
$login = Invoke-RestMethod -Method Post -Uri "$base/auth/login" -Headers @{'X-Client-Type'='mobile'} -ContentType 'application/json' -Body (@{
  email=$email; password=$password; client_name='PowerShell thử API'
} | ConvertTo-Json)
$password = $null; $secret = $null
$userHeaders = @{Authorization="Bearer $($login.access_token)"}
$profile = Invoke-RestMethod -Uri "$base/users/me" -Headers $userHeaders

# Admin đã tạo qua CLI, không đăng ký role bằng public API.
$adminEmail = Read-Host 'Email admin đã provision'
$adminSecret = Read-Host 'Password admin' -AsSecureString
$adminPassword = [Net.NetworkCredential]::new('', $adminSecret).Password
# Chọn mobile transport để thử bằng CLI: role ADMIN vẫn do backend xác minh.
$adminLogin = Invoke-RestMethod -Method Post -Uri "$base/auth/login" -Headers @{'X-Client-Type'='mobile'} -ContentType 'application/json' -Body (@{
  email=$adminEmail; password=$adminPassword; client_name='CLI admin'
} | ConvertTo-Json)
$adminPassword = $null; $adminSecret = $null
$adminHeaders = @{Authorization="Bearer $($adminLogin.access_token)"}
$serial = 'RG-' + [guid]::NewGuid().ToString().ToUpperInvariant()
$device = Invoke-RestMethod -Method Post -Uri "$base/admin/devices" -Headers $adminHeaders -ContentType 'application/json' -Body (@{
  serial_number=$serial; model='Prototype'; hardware_revision=$null
} | ConvertTo-Json)
$bindKey = [guid]::NewGuid().ToString()
$bindHeaders = @{Authorization=$userHeaders.Authorization; 'Idempotency-Key'=$bindKey}
$bound = Invoke-RestMethod -Method Post -Uri "$base/devices/bind" -Headers $bindHeaders -ContentType 'application/json' -Body (@{
  serial_number=$serial; nickname='Gương thử'
} | ConvertTo-Json)
$deviceId = $device.id
$bindingId = $bound.binding_id
$config = Invoke-RestMethod -Uri "$base/devices/$deviceId/config" -Headers $userHeaders
$updated = Invoke-RestMethod -Method Patch -Uri "$base/devices/$deviceId/config" -Headers $userHeaders -ContentType 'application/json' -Body (@{
  expected_version=$config.desired_version
  settings=@{display_brightness=80; vibration_enabled=$false}
} | ConvertTo-Json -Depth 4)

# Chỉ thử contract ACK trong môi trường test. Trong sản phẩm phải đợi BLE apply thật.
$ack = Invoke-RestMethod -Method Post -Uri "$base/devices/$deviceId/config/ack" -Headers $userHeaders -ContentType 'application/json' -Body (@{
  binding_id=$bindingId; reported_applied_version=$updated.desired_version
} | ConvertTo-Json)
$report = Invoke-RestMethod -Method Post -Uri "$base/devices/$deviceId/reports" -Headers $userHeaders -ContentType 'application/json' -Body (@{
  binding_id=$bindingId; observed_at=[DateTime]::UtcNow.ToString('o')
  firmware_version=$null; radar_status='UNKNOWN'; imu_status='UNKNOWN'
  display_status='UNKNOWN'; vibration_status='UNKNOWN'; battery_percent=$null
} | ConvertTo-Json)
$feedbackKey = [guid]::NewGuid().ToString()
$feedback = Invoke-RestMethod -Method Post -Uri "$base/feedbacks" -Headers @{
  Authorization=$userHeaders.Authorization; 'Idempotency-Key'=$feedbackKey
} -ContentType 'application/json' -Body (@{
  device_id=$deviceId; category='SUGGESTION'; content='Ví dụ thử API, không phải lỗi phần cứng thực.'
} | ConvertTo-Json)
$reply = Invoke-RestMethod -Method Patch -Uri "$base/admin/feedbacks/$($feedback.id)" -Headers $adminHeaders -ContentType 'application/json' -Body (@{
  status='IN_PROGRESS'; admin_response='Đã nhận phản hồi thử.'
} | ConvertTo-Json)
```

Config desired tăng nhưng applied chưa tăng cho đến ACK; ví dụ gửi ACK thủ công không chứng minh firmware apply.
`vibration_enabled=false` tắt cơ cấu rung theo hợp đồng, vẫn giữ HUD/risk. Report toàn UNKNOWN/null
không giả vờ đã đo phần cứng. `received_at` do backend sinh, không gọi report là online realtime.

## curl/POSIX và token trong biến

Đoạn Bash này minh họa login lấy token trực tiếp vào biến, không copy token literal vào tài liệu;
cần Python3 chỉ để tạo/đọc JSON (không dependency ngoài standard library). Password không nằm trong argv curl.

```bash
BASE=http://localhost:8080/api/v1
read -r -p 'Email: ' EMAIL
read -r -s -p 'Password: ' PASSWORD; printf '\n'
export EMAIL PASSWORD
LOGIN=$(python3 -c 'import json,os; print(json.dumps({"email":os.environ["EMAIL"],"password":os.environ["PASSWORD"]}))' |
  curl --fail-with-body -sS "$BASE/auth/login" -H 'Content-Type: application/json' -H 'X-Client-Type: mobile' --data-binary @-)
unset PASSWORD
ACCESS_TOKEN=$(printf '%s' "$LOGIN" | python3 -c 'import json,sys; print(json.load(sys.stdin)["access_token"])')
REFRESH_TOKEN=$(printf '%s' "$LOGIN" | python3 -c 'import json,sys; print(json.load(sys.stdin)["refresh_token"])')
unset LOGIN
curl --fail-with-body -sS "$BASE/users/me" -H "Authorization: Bearer $ACCESS_TOKEN"
```

Các JSON của luồng PowerShell ở trên cũng dùng được làm body curl/Postman; không đưa token vào URL.
Ví dụ PATCH config (thay DEVICE_ID, EXPECTED_VERSION bằng dữ liệu response, không đoán version):

```bash
curl --fail-with-body -sS -X PATCH "$BASE/devices/$DEVICE_ID/config" \
  -H "Authorization: Bearer $ACCESS_TOKEN" -H 'Content-Type: application/json' \
  --data "{\"expected_version\":$EXPECTED_VERSION,\"settings\":{\"vibration_enabled\":false}}"
```

## Refresh, unbind, history và lỗi dự kiến

PowerShell, giữ cùng key cho retry cùng thao tác, không tạo key mới để che timeout:

```powershell
$login = Invoke-RestMethod -Method Post -Uri "$base/auth/refresh" -Headers @{'X-Client-Type'='mobile'} -ContentType 'application/json' -Body (@{
  refresh_token=$login.refresh_token
} | ConvertTo-Json)
$userHeaders = @{Authorization="Bearer $($login.access_token)"}
# Single-flight: không dùng lại refresh token cũ sau rotation.
Invoke-RestMethod -Method Delete -Uri "$base/devices/$deviceId/binding?expected_binding_id=$bindingId" -Headers $userHeaders
$history = Invoke-RestMethod -Uri "$base/device-bindings?limit=20" -Headers $userHeaders
$ownFeedback = Invoke-RestMethod -Uri "$base/feedbacks/$($feedback.id)" -Headers $userHeaders
```

Retry DELETE cùng expected_binding_id trả204 kể cả đã rebind, không đóng binding mới.
Binding của người khác/khác device trả404. Bind replay key cũ sau unbind trả409 BINDING_NO_LONGER_ACTIVE.
Config expected_version cũ409; ACK version chưa phát hành409; report cũ/bằng trả200 accepted=false.
Feedback của tác giả vẫn đọc được sau unbind. List trả `{items,next_cursor}`; URL-encode cursor,
giữ caller/route/filter, default20/max100. Không sửa cursor hoặc dùng offset.

## Web admin transport

Browser dùng Origin thật trong allowlist, `credentials: include`, X-Client-Type:admin_web,
X-CSRF-Protection:1 và application/json. Login chỉ ADMIN; JSON không chứa refresh_token.
Refresh/logout body `{}`, credential trong cookie HttpOnly. Cookie policy xem runbook.
Không gửi cookie để authenticate mobile, không lưu access token vào localStorage.

Forgot-password: POST `{"email":"địa-chỉ-của-bạn"}` trả202 chung; lấy code từ email SMTP,
POST reset-password `{"reset_token":"<giá-trị-nhập-riêng>","new_password":"<giá-trị-nhập-riêng>"}`.
Các placeholder không phải credential thật; không lưu chúng/giá trị thật vào file chia sẻ.
SMTP sink đã nhận mail từ adapter production; SMTP nhà cung cấp/inbox thật chưa kiểm chứng.
Token reset một lần15 phút, reset revoke mọi session.
