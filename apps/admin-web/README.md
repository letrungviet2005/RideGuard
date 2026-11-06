# RideGuard admin web

React/TypeScript/Vite foundation with browser router, layout and `src/shared/api.ts`.
Current pages explain platform scope, without invented users, metrics or device connections.
Planned feature folders: auth, users, devices, feedback; create them with actual behavior.
**Implemented:** foundation only. **Proposed MVP:** these four features; no location/trip API.

Node 24.19.0 recommended; dependencies exactly pinned in package.json/package-lock.json.
From this directory:

```sh
npm ci
npm run dev
npm run typecheck
npm run lint
npm run build
npm run preview
```

Copy `.env.example` to `.env` for local configuration. `VITE_API_BASE_URL` is a PUBLIC
build-time value bundled into JavaScript; changing deployment environment alone does not
change an existing dist. Never store secrets in VITE_* or browser code. Current foundation
does not call unimplemented APIs. Backend intentionally has no broad CORS allowance.
Future auth integration must configure exact allowed origin or same-origin reverse proxy.
Deploy dist independently with HTTPS and SPA fallback to index.html; no deployment configured.

[Proposed OpenAPI](../../contracts/openapi.proposed.yaml) requires backend ADMIN checks.
Web login/refresh returns access token in JSON, refresh only via HttpOnly/Secure cookie;
refresh/logout body is {}, not a mandatory body refresh_token. Keep access in memory, not
long-lived localStorage. X-Client-Type=admin_web selects transport, not authorization.
Origin allowlist/SameSite/CSRF deployment policy is still proposed; see
[authentication](../../docs/flows/authentication.md). No owner reassignment controls or
fake dashboards. Server device status does not disable ESP32 safety functions.
