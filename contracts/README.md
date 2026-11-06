# Canonical contracts — implemented / proposed / deferred

- [openapi.yaml](openapi.yaml): **implemented**, three health endpoints only; unchanged by
  MVP documentation update. Current API error has no request_id.
- [openapi.proposed.yaml](openapi.proposed.yaml): **proposed MVP**, full business DTOs,
  examples, errors and security/transport; not available in current backend. One canonical
  OpenAPI per status. [Explanation](../docs/api/proposed-api.md) links here instead of copying DTOs.
- [ble-protocol.md](ble-protocol.md): **proposed** versioned logical protocol, no active GATT.
- [device-config.schema.json](device-config.schema.json): **proposed** full config snapshot;
  OpenAPI settings patch reuses property definitions, ACK is a separate request.
- [device-report.schema.json](device-report.schema.json): **proposed** REST v1 metadata request,
  no schema_version field required in body; route defines API version. No raw sensor/GPS payload.
- [radar-normalized.schema.json](radar-normalized.schema.json): **proposed** measurement
  representation, optional values and sensor units; driver stubs do not implement serialization.
- [session.schema.json](session.schema.json): **proposed research** collection-session
  provenance, not auth sessions or trips. Clock/IMU metadata support future fused features.
- [Roadmap](../docs/roadmap.md): **deferred**, no trip/challenge/OTA/inference API in MVP.

JSON Schema draft 2020-12, OpenAPI 3.1.0. Current proposed version 0.2.0; config schema v1
was unpublished scaffolding and now defines two settings (old empty-settings shape is no
longer a valid proposed snapshot). No deployed contract consumer is claimed. Review future
breaking changes and version them before release. Examples are synthetic, never hardware evidence.
Numeric missing values are null, never fabricated zero. Config versions fit exact JS integers.

Run existing `scripts/.venv/Scripts/python.exe scripts/check_contracts.py` on Windows
(`scripts/.venv/bin/python` on POSIX). No Docker/backend/network needed. The checker resolves
local $refs, validates both specs/examples and guards MVP exclusions. Do not start the
stack or install system software for a docs-only review; report missing dependencies instead.
