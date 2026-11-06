# Contributing

Create short task branches from `main`; keep each change reviewable. Submit a pull request
with purpose, behavior change and actual test evidence. At least one other team member
reviews; all affected checks pass before merge. Configure branch protection after adding
the remote; no remote or protection has been created by bootstrap.

Do not commit secrets, location history, real radar logs with personal data, large datasets
or models. Keep local `.env` ignored. Existing STL files are preserved; verify their provenance
and license before redistribution. Do not invent GitHub usernames in CODEOWNERS; add real
team owners once accounts and module responsibilities are agreed.

Version REST, BLE, configuration schema and model/feature formats compatibly. Contract
changes require consumer review and checks. Distinguish implemented versus proposed APIs.
Do not bypass security checks to unblock UI or label missing sensor data SAFE.
Run [component commands](docs/development/local-setup.md) before merge; record unavailable
checks and their reason instead of claiming a pass. No automatic deployment is configured.
