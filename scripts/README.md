# Repository checks

Create a separate environment here: `python -m venv scripts/.venv`, then install
`scripts/requirements.txt`. Run `python scripts/check_contracts.py` using that environment.
Windows uses `.venv/Scripts/python.exe`; POSIX uses `.venv/bin/python`.
Status: **implemented static tooling**, not a business API. It validates both implemented
health and proposed MVP OpenAPI, local references/examples, directionality, auth and scope
guards, JSON Schemas and local Markdown links. It does not test runtime/hardware/migrations.
For docs-only work use the existing environment without installation; if unavailable,
report the limitation. No Docker/database/background process/network is needed.
