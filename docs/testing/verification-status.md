# Verification status

## 2026-11-07 — static MVP contract/document update

Status: **implemented foundation unchanged; proposed MVP contracts checked statically;
trip/possession-proof scope deferred**. This task did not run Docker, database, backend,
background services, migrations, package installation, firmware/mobile builds or deployment.
No credentials requested. Existing scripts/.venv was used.

`scripts/.venv/Scripts/python.exe scripts/check_contracts.py` checks both OpenAPI 3.1
documents, all local $refs (resolved in memory), unique operationIds, exact MVP route set,
request/response/header examples, required/nullability/bounds, readOnly/writeOnly and secret
exposure guards, auth transport declarations, four JSON Schemas and local Markdown file links.
It also rejects unknown fields, role/user_id injection, unsafe config fields, invalid sensor
statuses/values and incorrect token-body shapes. No third-party endpoint is contacted.

The initial direct validator call hit a local relative-$ref resolver limitation on Windows;
the checked-in tool now resolves local references itself, then validates the expanded spec
in memory. It does not remove references from the canonical contract or skip validation.
Final checker result: PASS, 34 operations (3 implemented + 31 proposed), 394 example
validations, 32 negative cases and 83 local Markdown file links. `git diff --check` passed.
Diff review also confirmed no changes to implemented OpenAPI, app/security/firmware source,
migration, Compose or CI workflow. No new runtime behavior is inferred from these checks.

Manual review: DB/API fields, ownership/config/report/version naming, 7 business + 3 technical
tables, explicit timestamp tolerance and retention; rg review of trip/GPS mentions with
deferred/navigation context. Implemented health OpenAPI, application/security/firmware code
and migration remain unchanged. Foundation mobile UI still mentions trips in old copy;
source changes are out of this docs task, and app README records the discrepancy.

Not run: runtime HTTP/auth/CORS/CSRF/DB concurrency tests, timestamp-to-now behavior, BLE,
hardware/ML, GitHub-hosted CI and Mermaid visual rendering. They require implementations,
services or additional tooling outside this static task. External web links/Markdown anchors
are not checked; local file targets are. Referential DB constraints are design, not migrated
or SQL-executed. Deployment domains, cookie policy, retention and risk policy remain proposed.

Current Git state has an existing commit/origin and a user-managed STL move from model/ to
models/; this task preserves it and only corrects documentation paths. No commit/push/deploy
or model-file operation was performed by this task.

## Historical bootstrap record — 2026-11-06

The following evidence belongs to the earlier foundation setup, not this static update.
Its old no-remote/initial-path observations are historical and do not describe current Git
or model layout. It is not evidence that the new proposed auth/CRUD/BLE contracts are implemented.

### Verified during bootstrap

- Root Git initialized on main, no remotes/submodules/nested source repository. Existing four
  STL files retained at original paths. New files not committed or pushed.
- Backend: Maven wrapper 3.9.11 with Temurin 21.0.13; `verify` passed, 3 tests, 0 failures.
  Spring application starts against real PostgreSQL 17.11; Flyway history confirms V1 success.
  Tests cover public minimal health, denied business/actuator requests and redacted validation/
  malformed-JSON errors. Executable JAR packaged.
- Backend runtime: HTTP readiness 200 with DB up; deliberately stopped this project's
  PostgreSQL container, observed liveness 200 and readiness 503; restored DB and readiness 200.
  Aggregate health includes public probe group names, no DB details. No default user created.
- Admin: `npm ci`, typecheck, ESLint and production build passed with final lockfile.
  `npm audit` reported 0 vulnerabilities at verification time. Preview served `/` and `/scope`
  with HTTP 200. Browser rendering/interactions have not been visually inspected.
- Firmware: both `ld2451` and `ld2417` PlatformIO environments built successfully. Board
  definition reports ESP32 240 MHz/4 MB flash/320 KB RAM. Each binary includes one decoder.
  No upload, GPIO actuation or real sensor validation performed.
- AI: editable package installation/import passed, 3 session-split tests passed, wheel built.
  No model trained, accuracy measured, feature parity or real dataset evaluation performed.
- Contracts: all three JSON Schemas validated with examples/version rejection, OpenAPI 3.1
  validation and implemented-endpoint boundary check passed.
- Mobile: Gradle wrapper with JDK 21, Android platform 35/build-tools 35.0.0; debug APK and
  lint built successfully: 0 errors, 4 warnings. Three are dependency-update notices for the
  deliberately pinned Compose BOM/activity/navigation versions; one is DataExtractionRules
  guidance for pre-Android-12 backup handling (allowBackup=false/fullBackupContent=false
  and Android 12+ exclusion rules are present; behavior still requires device testing).
  No configured AVD/attached test device used, so launch, navigation,
  BLE and permission flows have not been exercised on Android runtime.
- Compose config parsed, PostgreSQL image pulled and healthcheck passed. Python firmware/
  contract environments passed `pip check`. CI YAML and embedded Python parsed locally.

### Environment problems resolved during bootstrap

- Docker daemon initially unavailable; started installed Docker Desktop, then used this
  project's PostgreSQL container and named volume. No volume was removed.
- System JDK was 25; downloaded portable JDK 21 under ignored `.local/` for builds.
  Installed Android API 35/build-tools 35.0.0 into existing SDK; original SDK 37 retained.
- PlatformIO package-postinstall mishandled Windows spaces/parentheses. Built through a
  temporary drive alias to the same workspace; pinned intelhex dependency fixes the interrupted
  esptool setup. No source checkout copied or moved.
- Vite peer mismatch corrected; pinned compatible versions and patched transitive overrides
  as explained in toolchain.md. Clean `npm ci` succeeded after stopping preview process.
- Repackaging a running JAR on Windows locks the file; stopped the verification process and
  reran Maven successfully. Stop local servers before rebuilding/reinstalling their artifacts.

### Not verified / not implemented at bootstrap

GitHub Actions has not run remotely (no remote created). Linux CI/lockfile portability still
needs the first hosted run. No production deploy, signed release APK or end-to-end UI test.
Hardware protocols, radar mounting/attenuation, sensor timing, target stability, IMU/risk,
power/motor/HUD and BLE remain untested. Business authentication/CRUD, DB entities,
config sync, data retention and AI are designs only; Room trip sync has since become
deferred outside MVP. No claim of production/safety readiness.

Artifacts: backend `target/backend-0.1.0-SNAPSHOT.jar`, admin `dist/`, mobile
`app/build/outputs/apk/debug/app-debug.apk`, firmware `.pio/build/{ld2451,ld2417}/firmware.bin`,
AI `dist/rideguard_ai-0.1.0-py3-none-any.whl`. Build artifacts and local logs are ignored.
Detailed local logs remain in ignored `.local/`; test reports are under each build output.
