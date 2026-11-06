# RideGuard AI research package

Python 3.11+, independent environment; no model training/inference or accuracy claim.
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
