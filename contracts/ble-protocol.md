# BLE protocol — proposed logical v0.2, not implemented

No active GATT service. Keep this contract separate from REST. UUIDs, pairing/link security,
MTU, byte order, framing and bounded payload/retry limits need approval before firmware
writes. No ownership challenge/proof, OTA, trip history or raw cloud stream in MVP. BLE
pairing/link security is distinct from deferred backend possession proof. Internet/login
state never gates ESP32 radar + IMU → risk → HUD/vibration.

## Logical messages

Every envelope has protocol_version="0.2", message_id (UUID) and type. Wire encoding is
not finalized; JSON here is a logical model, not a claim that an envelope fits one packet.
Handshake exchanges compatible versions/capabilities and boot_id; unsupported versions
reject explicitly without applying partial settings. Receive/reassemble length limits and
timeouts are mandatory when implemented.

- Mobile → ESP32 navigation: cue (enum to approve), distance_m nullable nonnegative,
  ttl_ms positive bounded (maximum to approve), navigation_sequence and navigation_session_id.
  No GPS coordinate/history payload. TTL starts on ESP32 monotonic receipt; expired/disconnected
  navigation becomes unavailable, never SAFE. Sender must discard stale queued cues before send;
  local receive TTL alone cannot detect time already spent in the phone queue. Deduplicate/reject
  out-of-order sequence within session; new navigation session after reconnect.
- Mobile → ESP32 configuration: command_id, binding_id, schema_version=1, desired_version,
  full settings from [config schema](device-config.schema.json). Cùng binding không apply version thấp hơn
  version đang dùng; cùng version/cùng nội dung ACK lại; cùng version/khác nội dung lỗi.
  Binding mới thiết lập riêng, không so version giữa hai binding. Đây là hợp đồng, chưa có firmware code. Persisted
  binding-context reset on rebind and pairing authorization require approval; they are not
  cryptographic proof of ownership in this MVP.
- ESP32 → mobile config ACK: command_id, binding_id, reported_applied_version only after real
  apply. A rejection has an explicit reason/status and does not advance applied. Duplicate
  command returns prior result; old ACK cannot lower applied. Reconnect queries binding/applied
  before retry. Mobile REST ACK carries binding_id/reported_applied_version; server confirms
  issued version, but this remains a mobile assertion, not hardware attestation.
  Backend monotonic ACK không chứng minh firmware đã chống rollback.
- ESP32 → mobile diagnostics: firmware_version nullable, radar/imu/display/vibration status
  OK/ERROR/UNAVAILABLE/UNKNOWN, battery_percent integer 0–100 or null. Do not infer OK from BLE
  connected; unknown/unavailable/fault/stale internal radar samples do not map to OK. The
  normalized radar schema uses lower-case sample-quality states, distinct from report status.
- Optional ESP32 → mobile risk display: risk_state including UNKNOWN, scenario type and
  local observation age. Warning types/thresholds are not approved; app display cannot
  replace local HUD/rung or declare a missing signal SAFE.

Settings display_brightness 0–100/vibration_enabled are logical preferences, no guessed GPIO
or PWM. vibration_enabled=false tắt cơ cấu rung; HUD và xử lý nguy cơ vẫn hoạt động.
Không master-disable hoặc threshold AI. Low-brightness policy cần duyệt. Mobile cannot edit config offline; save backend first, BLE apply,
then REST ACK. No BLE → pending, not applied. See [flow](../docs/flows/configuration-sync.md).

## Clock ownership

ESP32 samples use boot_id + ESP32 monotonic_ms (wrap/reset handled explicitly); mobile queues
and RTT use phone monotonic clock. Navigation ttl_ms is a duration, not an absolute UTC
deadline. Diagnostics may include ESP32 age_ms/boot_id; mobile creates observed_at in its UTC
clock only for a fresh observation, never relabels stale cached telemetry. Server sets
received_at. Timestamp tolerance/order is [documented separately](../docs/flows/device-status-reporting.md).
Never subtract timestamps from phone/ESP32/server without a synchronization/error model to
claim latency. No radar pinout/protocol or ability to pass through mirror coating is assumed.
