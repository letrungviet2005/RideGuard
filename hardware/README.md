# Hardware decisions pending

Status: **proposed hardware, unverified**. Current user-managed STL files are under
`models/espejo-retrovisor-de-motocicleta-ustom-motorcycle-side-mirror-housing-mount/`
(Left/Right), relative to repository root. The user's in-progress move from model/ to
models/ is preserved; this documentation task does not move/edit those files or claim
electronics fit. License/provenance and mechanical suitability still require confirmation.

Target: NodeMCU-32S, one of HLK-LD2451/LD2417, LSM6DS3/BMI160 undecided, ST7789 240×240,
motor through driver, removable externally charged battery. No verified pin map/schematic
exists. Confirm logic voltages, motor suppression/driver current, battery protection/power
budget, connector polarity, waterproofing, vibration and mounting before enabling outputs.
Validate radar field of view and attenuation at its mounting location; do not assume it
passes through reflective mirror coating. No hardware test has been performed.
