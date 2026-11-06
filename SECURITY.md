# Security and privacy

Do not post secrets, tokens, personal location traces or exploit details in public issues.
Until a private reporting address is assigned, contact an established project maintainer
privately through your team's existing channel. No fictional security address is provided.

Backend health exposes status/probe names only. Other routes deny access. Never put secrets
in Vite variables or mobile BuildConfig. Use environment/secret manager for backend credentials.
Trip ownership must be checked on every future route; ADMIN does not automatically grant
location access. Serial-only device binding has known possession-proof limitations.

Rotate exposed credentials, revoke affected sessions, remove sensitive artifacts from
distribution/history through a coordinated process, and document remediation privately.
Do not erase database volumes as a troubleshooting shortcut. Dependency updates require
verification; this foundation is not a production safety certification.
