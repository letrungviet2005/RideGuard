# Canonical contracts

- `openapi.yaml`: only the three implemented health endpoints. Business routes are
  [proposed](../docs/api/proposed-api.md), not a second OpenAPI.
- `ble-protocol.md`: proposed logical protocol; no active GATT implementation.
- `radar-normalized.schema.json`: normalized sensor metadata and optional measurements.
- `session.schema.json`: provenance, collection session and partition metadata.
- `device-config.schema.json`: versioned binding-scoped desired/applied configuration.

JSON Schemas use draft 2020-12. Contract version 1 is a foundation, not a validated
hardware protocol. Missing measurements are null, never fabricated zero. Schema changes
must be reviewed by all consumers, and incompatible changes require a version change.
Run `python scripts/check_contracts.py` with `scripts/requirements.txt` installed.
