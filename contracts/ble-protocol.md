# BLE protocol — proposed v0.1, not implemented

This is a design contract, not an available GATT service. UUIDs, authentication,
MTU, payload limits, byte order and retry timers must be ratified before implementation.
BLE is separate from REST. Internet/account state never gates radar, HUD or vibration.

Planned logical messages: capabilities/version handshake, navigation cue with expiry,
configuration request, applied/rejected acknowledgement, sensor diagnostics.
No raw radar cloud stream and no OTA. Navigation must expire to unavailable if the phone
disconnects. A stale cue must not imply that the road is safe.

Configuration envelope: protocol_version, schema_version, binding-scoped config ID,
desired_version, command_id, payload. ESP32 validates all fields and compatibility,
then acknowledges command_id and reported_applied_version only after actual application.
Duplicate command IDs replay the acknowledgement; rejected/unsupported commands do not
advance the applied version. Mobile records acknowledgement before reporting to REST.
Reconnection reads applied version and retries bounded, idempotent commands.
Transfers must be bounded, framed, length-checked and reassembled with timeout; no
assumption that a JSON message fits one BLE packet. Pairing/security remains a blocking
design decision before accepting configuration writes on hardware.

Measure round-trip latency on a single monotonic clock. Never subtract phone/server/ESP32
timestamps unless clock synchronization and uncertainty are explicitly accounted for.
