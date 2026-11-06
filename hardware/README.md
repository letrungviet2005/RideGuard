# Hardware decisions pending

Existing user-provided STL files remain at repository root in
`espejo-retrovisor-de-motocicleta-ustom-motorcycle-side-mirror-housing-mount/` (Left/Right).
They were not moved, edited or claimed to fit the electronics. License/provenance and
mechanical suitability must be confirmed before redistribution/fabrication.

Target: NodeMCU-32S, one of HLK-LD2451/LD2417, LSM6DS3/BMI160 undecided, ST7789 240×240,
motor through driver, removable externally charged battery. No verified pin map/schematic
exists. Confirm logic voltages, motor suppression/driver current, battery protection/power
budget, connector polarity, waterproofing, vibration and mounting before enabling outputs.
Validate radar field of view and attenuation at its mounting location; do not assume it
passes through reflective mirror coating. No hardware test has been performed.
