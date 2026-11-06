# Repository checks

Create a separate environment here: `python -m venv scripts/.venv`, then install
`scripts/requirements.txt`. Run `python scripts/check_contracts.py` using that environment.
Windows uses `.venv/Scripts/python.exe`; POSIX uses `.venv/bin/python`.
These checks validate schemas and examples, not hardware measurements.
