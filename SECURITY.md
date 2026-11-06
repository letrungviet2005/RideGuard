# Security and privacy

Do not post secrets, tokens, personal location traces or exploit details in public issues.
Until a private reporting address is assigned, contact an established project maintainer
privately through your team's existing channel. No fictional security address is provided.

Backend health exposes status/probe names only. Other routes deny access. Never put secrets
in Vite variables or mobile BuildConfig. Use environment/secret manager for backend credentials.
Proposed MVP checks account/session revocation and ownership on every private route; ADMIN
is enforced by backend on admin routes. No location/trip API is in MVP (deferred roadmap).
Serial-only prototype binding does not prove possession; no BLE challenge system is added.
Mobile body refresh and web HttpOnly/Secure cookie transports are separate; exact origins,
SameSite and CSRF deployment settings must be approved before implementation. Proposed
errors add request_id; existing implemented security/error code is unchanged.

Rotate exposed credentials, revoke affected sessions, remove sensitive artifacts from
distribution/history through a coordinated process, and document remediation privately.
Do not erase database volumes as a troubleshooting shortcut. Dependency updates require
verification; this foundation is not a production safety certification.
