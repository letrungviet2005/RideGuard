# Offline trip recording and sync — proposed

User explicitly starts/stops recording. No automatic tracking on launch or login. GPS
supports navigation and trip history, not primary turn-warning detection.

## Local Room design

Future `shared/storage` contains Room entities `LocalTrip` (UUID, owner account ID,
optional device ID, started/ended_at, state, version), `LocalTripPoint` (trip UUID,
sequence_number composite PK, recorded_at, lat/lon/accuracy, nullable speed/bearing),
and `SyncOperation` (operation UUID, account ID, trip UUID, type, attempt count,
next_attempt_at, payload reference, state). Persist points and queue work atomically.
No Room dependency/entities yet because recording is not implemented.

Room is source of truth while offline. Assign trip UUID before first location, monotonically
increment sequence within a transaction, preserve gaps/drops. Queue CREATE → POINT_BATCH
→ COMPLETE; upload <=500 points and <=256 KiB per batch. Advance acknowledged checkpoint
only after server confirmation. WorkManager retries under network constraint/backoff/jitter;
operations survive process death. Never move old-account queued data to a newly logged-in
user; pause on logout, require original account or explicit local deletion.

Exact trip/sequence retries deduplicate; conflicting payload returns 409 and requires
resolution. Complete waits for all accepted points or explicitly resolved loss; rename uses
optimistic version. Offline delete creates tombstone, cancels pending upload and queues
server deletion so an old retry cannot recreate it. Server cascades points. Privacy retention,
local encryption and backup policy must be finalized; current app disables Android backup.

## Android permissions/lifecycle

Request foreground coarse/fine location only when user starts relevant feature. Explain
approximate-only accuracy; deny/removed permission means unavailable, never fabricated
coordinates. Foreground location service with persistent recording notification supports
user-started recording when UI goes to background; declare service type/location permission
and applicable foreground-service permissions when implementing. Ask notification permission
where required and handle rejection without claiming invisible background recording.
Background-location permission is a separate justified flow only if the finalized use case
requires it; not requested by this foundation. Respect platform foreground-service launch
restrictions, battery limits and permission revocation. BLE scan/connect permissions are
independent and requested only for BLE operations. Current app requests INTERNET only.

On process death/reboot, recover an interrupted trip and ask user how to resume/end;
do not invent missing points or silently resume hidden collection. Show loss/poor accuracy.
Use observed accuracy, plausibility/speed and time-gap filters before estimating distance;
keep missing speed/bearing null, never zero. Do not blindly sum stationary GPS jitter.

Data stays private to owner, including for admin; unbinding device does not transfer trips.
See [proposed routes](../api/proposed-api.md) for cursor, batch, retry and ownership semantics.
