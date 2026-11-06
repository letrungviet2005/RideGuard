# RideGuard AI research package

Python 3.11+, independent environment; no model training/inference or accuracy claim.
Status: session assignment **implemented**; fused risk research **proposed**; no AI REST
inference endpoint in MVP. Trip/GPS-history features are **deferred** in the [roadmap](../docs/roadmap.md).
Implemented preprocessing assigns whole sessions deterministically to train/validation/test
BEFORE any windowing. For correlated sessions from the same rider/day/route, group them
before assignment; a session hash alone does not guarantee cross-rider generalization.

```sh
python -m venv .venv
# activate .venv, then:
python -m pip install -e .
python -m unittest discover -s tests -v
python -m pip wheel --no-deps . -w dist
```

Build backend versions pinned in pyproject.toml; no runtime dependencies/lockfile needed.
`preprocessing.py`, `configs/baseline.toml` and behavioral tests exist. Planned `features`,
`training`, `evaluation`, `export` modules will be created with actual algorithms, not empty
classes. Configuration format and input session provenance live in contracts/.

Keep ignored data/raw, data/processed, data/labeled and data/simulated separate. Real and
simulated provenance persists through processing, never label simulated data as hardware
evidence. Store session/sensor/revision/clock/distance definition, label protocol and split
manifest. Split before windows and preprocessing fit; normalization fits training only.
Freeze test sessions, compare against rule-based baseline on the same sessions; report
dangerous misses, false alarms, latency (single monotonic clock), RAM and model size,
with uncertainty and operating conditions. No values have been measured yet.

Features require shared definition/version, units, missingness, window boundary and numeric
tolerance. Implement Python/C++ parity against common golden synthetic fixtures before
deployment; no current feature extractor or model exists. Large models, personal GPS,
sensitive logs and raw datasets must not enter Git. No inference server is needed now.

## Proposed radar + IMU fusion

Associate radar observations with an internal target track (sensor target IDs are unverified
hints), align IMU ego-motion over the same temporal window and construct a joint feature
vector per target. Track sensor/clock/units/axis definition, missingness and time uncertainty;
never assume LD2417 axis-Y distance equals LD2451 distance. Session schema records optional
IMU provenance; a schema field does not prove the module is installed or time-aligned.

Features relate approach side/motion to ego yaw/lateral movement where sensors actually
support those measurements. IMU does not know future turn intent; same-side motion alone
does not establish intersecting trajectories or collision. Missing/invalid/unreliably aligned
inputs produce UNKNOWN. Do not describe two independent radar/IMU models without a fusion step.

Compare a rule-based baseline with a lightweight ML candidate using identical fused inputs
and session splits. Separately label/evaluate direction-change conflicts versus straight-line
rear-approach risk; product warning policies for both scenarios are pending approval, not
implemented features. No accuracy or safe threshold claimed. Measure dangerous misses, false
alarms, latency on one clock, RAM/model size; share Python/ESP32 feature definitions/version,
window semantics and golden parity fixtures before deployment. No GPS history is required.
