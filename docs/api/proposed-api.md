# REST API — PROPOSED, unavailable

Implemented API is exclusively [contracts/openapi.yaml](../../contracts/openapi.yaml).
All routes below are future design; current security denies them. No JWT/account/CRUD
implementation is implied.

## Groups

- `/api/v1/auth`: registration, login, refresh, logout, request/reset password.
- `/api/v1/users/me`: self profile; role cannot be client-selected.
- `/api/v1/devices`: owned active devices, binding history and binding-scoped configs.
- `/api/v1/feedbacks`: create/read own feedback.
- `/api/v1/trips`: private trip synchronization and reads.
- `/api/v1/admin`: authorized administration of users/devices/feedback, no default trip access.

## Binding

`POST /api/v1/devices/bind` accepts serial and an idempotency key; transaction locks device,
checks enabled/known serial, then creates active binding. Replay with same request returns
same binding; another owner gets 409 without disclosing their identity. No ownership overwrite.
`DELETE /api/v1/devices/{deviceId}/binding` closes caller's active binding; repeat for own
already-closed binding is idempotent. Foreign IDs do not reveal owner. Full semantics in
[binding flow](../flows/device-binding.md).

## Trips

- `POST /api/v1/trips`: accepts mobile UUID, started_at and optional authorized device;
  same owner/UUID/payload replay returns existing result; conflicting payload 409.
- `POST /api/v1/trips/{tripId}/points/batch`: up to 500 points and 256 KiB decoded request;
  unique sequence, transactional all-or-nothing validation/insertion; exact retry deduplicated,
  conflicting sequence payload 409, invalid point 400, oversize 413.
- `POST /api/v1/trips/{tripId}/complete`: sends ended_at, expected_last_sequence and expected
  version; complete only after acknowledged required points (explicit dropped ranges supported
  in later design). Identical retry succeeds. Conflicting completion 409. Late new points
  after completion rejected; exact duplicates may be acknowledged.
- `GET /api/v1/trips`: cursor by (started_at DESC, id), default 20/max 100.
- `GET /api/v1/trips/{tripId}`: own summary.
- `GET /api/v1/trips/{tripId}/points`: cursor sequence_number ASC, default 100/max 500.
- `PATCH /api/v1/trips/{tripId}`: rename only initially; expected version/If-Match prevents
  lost updates; no owner reassignment, computed-distance mutation or arbitrary mass assignment.
- `DELETE /api/v1/trips/{tripId}`: own trip and points atomically; repeated delete 204 for
  authorized known tombstone, otherwise 404. Offline delete tombstone prevents resurrection.

Cursor must be opaque, validated and bound to user/query filters; no unstable offset paging.
Every operation checks owner regardless of ADMIN role. Return consistent 404 for inaccessible
trip IDs to reduce enumeration. Authenticated transport HTTPS. Batch retries use bounded
exponential backoff with jitter; honor Retry-After, retry network/429/5xx, refresh on expired
auth once; 400/403/409 need resolution, not infinite retry. Idempotency records scope to
user + route + key + request hash, expire after documented retention; durable trip UUID and
point unique keys remain final duplicate protection.

Desired config update uses expected desired_version; server increments version in a transaction.
Applied ACK endpoint will accept binding_id and reported_applied_version only from authorized
mobile after device ACK, never simply copy desired_version. See configuration flow.

Errors use `{status, code, message}`; no stack traces, SQL, credentials or personal data.
Validation uses bounded strings, sizes, timestamps and finite numeric values. Rate limits and
authentication scheme remain decisions before business API launch.
