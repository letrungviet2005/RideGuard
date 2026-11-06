# ADR 0001 — independent foundations in one repository

Status: accepted for bootstrap, 2026-11-06.

Use one root Git repository, manifests and build outputs per component, no submodules,
no shared node_modules or Python environment. Keep existing STL source assets untouched.
Feature packages grow when code exists; responsibilities are documented before creating
empty layers. Shared contracts have one canonical location.

Safety pipeline resides on ESP32; offline/mobile/backend outages never imply safe road
conditions. Hardware stubs explicitly unavailable. AI is offline research until evidence,
resource budget and rule-based baseline exist. Only health API implemented now.

Business DB is a nine-table proposal; foundation migration creates schema only. Readiness
includes database, liveness excludes it. All non-health backend requests denied until auth
is deliberately implemented. No default account and no JWT scaffolding.

Versions and primary compatibility sources: [toolchain.md](../development/toolchain.md).
Unresolved: radar choice and exact serial protocol revision, IMU module, pin map/power,
radar mounting/transmission through materials, risk thresholds, stable target tracking,
BLE UUID/security/MTU, auth strategy, privacy retention, backend deployment URLs.
