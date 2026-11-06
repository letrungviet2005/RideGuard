# Authentication — proposed

No login, JWT, seeded accounts or business endpoints exist. Current backend allows GET
health only and denies all other requests. CSRF remains enabled; do not disable it just
to make future cookie-authenticated mutations work without protection.

Registration validates normalized email and password; server assigns USER/ACTIVE and
stores an adaptive password hash. Reject client-supplied role. Login returns a generic
failure for bad credentials/locked user. Rate limits and recovery avoid enumeration.
Refresh token rotation and single-use reset consume hashed token records transactionally;
replay revokes the affected session family. Logout revokes server session; mobile clears
local auth material. Token storage, access-token format/lifetime and admin session policy
must be decided before implementation. Mobile secrets use Android Keystore-backed storage;
web session design should avoid exposing long-lived tokens to JavaScript.

Never log passwords/tokens, embed secrets in APK/Vite variables, or couple warning
availability to authentication. Account loss does not disable local ESP32 hazard processing.
