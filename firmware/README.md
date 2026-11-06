# RideGuard firmware

PlatformIO Core 6.1.18, espressif32 6.12.0 / Arduino ESP32 2.0.17, board `nodemcu-32s`.
Create a firmware-only Python environment, install requirements.txt, then:

```sh
python -m platformio run -e ld2451
python -m platformio run -e ld2417
python -m platformio device monitor --baud 115200
```

Run in firmware/ using its environment. Both environments compile, but each binary includes
exactly one radar decoder. Default is ld2451 for compilation only, not a hardware selection.
Upload only after reviewing board, wiring and power. No upload has been performed.

USB serial startup log only; peripheral pins are unassigned and motor/radar/TFT are never
activated. Decoders explicitly return NotImplemented and reset output to unavailable/null;
no fake SAFE or fabricated data. Radar metadata keeps sensor, unit-suffixed fields,
distance definition, availability and monotonic receipt time. Target IDs are unverified hints.
**Proposed MVP:** radar + IMU joint temporal processing per target, local HUD/rung; Internet,
REST account expiry and server registry status must not disable core warnings. BLE carries
navigation cue/distance/TTL, binding-scoped config/version, ACK and hardware status metadata,
not trips or possession challenge. [BLE contract](../contracts/ble-protocol.md) is logical
proposed v0.2, not implemented. Pending config is not applied; battery remains null until
measurement hardware is verified. Vibration preference cannot suppress mandatory warnings.
Direction-change versus straight-line rear-approach policies need approval; IMU cannot
know turn intent and same-side motion alone is insufficient. See [AI design](../ai/README.md).
See [architecture](../docs/architecture/system-overview.md) for planned modules (IMU/display/
vibration/tracking/features/risk/navigation/communication/UI/power/diagnostics).

Windows paths with spaces/parentheses can break PlatformIO postinstall scripts. Use an
unused drive mapping to the SAME workspace, e.g. `subst R: "<repository absolute path>"`,
then `R:\firmware\.venv\Scripts\python.exe -m platformio run -d R:\firmware -e ld2451 -e ld2417`.
Remove mapping with `subst R: /D` when done. Do not replace an occupied drive. If interrupted
esptool postinstall left missing intelhex, installing this project's pinned requirements
provides the required dependency. No hardware function is verified by compilation.
