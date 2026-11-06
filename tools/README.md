# Device/data tools — planned

Future tools may collect serial logs, replay recorded radar frames and send sample BLE
commands. None is implemented now; do not simulate success with placeholder scripts.
Add each tool with its own manifest/environment as needed, strict contract validation,
bounded input handling and clear simulated/real provenance. Keep device logs and private
locations outside Git. Repository contract checks live in scripts/ instead.
