# Configuration sync — proposed

Canonical envelope: [device-config.schema.json](../../contracts/device-config.schema.json).
Server validates schema and active binding; compare-and-set increments desired_version.
At this stage settings is deliberately empty, rejecting guessed hardware tuning values.
Future schema must define safe ranges with hardware evidence.

Mobile fetches desired configuration, validates compatibility/capabilities, then sends an
idempotent BLE command. ESP32 validates and applies before emitting ACK. Mobile reports
binding ID, command ID and applied version after that ACK. Server checks owner/binding,
version bounds and monotonicity. reported_applied_version stays null until first real ACK.
Server write success never means applied. Disconnect/timeouts keep pending state, and
reconnect reconciles actual device version. Old-binding ACK cannot update new-owner config.
Concurrent edits use expected version; stale updates get conflict. Rejection preserves
previous applied config and records reason without leaking secrets.

Measure durations with one monotonic clock; timestamps across unsynchronized devices
cannot be subtracted as latency. Version/state is authoritative for synchronization.
