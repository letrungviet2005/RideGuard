# Device binding — proposed

1. Authenticated user submits a known device serial.
2. Normalize/validate serial, rate-limit request, lock device row in transaction.
3. Reject disabled/unknown device or another active owner; never overwrite ownership.
4. Create binding with bound_at; partial unique index is the final concurrent-write guard.
5. Create fresh binding-scoped config; do not inherit an older owner's settings or ACK.

Unbind checks owner and sets unbound_at. Existing config remains historical, no longer
writable as active. Trips stay with original user. Device reuse creates a new binding.
MVP serial entry does not prove possession: serial leakage/guessing or pre-claiming is
possible. Rate limits, audits and support dispute process are required. BLE proof of
ownership is future work, not implied. There is no device_claim_requests table.
