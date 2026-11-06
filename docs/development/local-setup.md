# Local setup

Run from repository root unless indicated. Install versions in [toolchain.md](toolchain.md).
Only root Git repository is used; dependency environments are separate. Do not run tools
with system Java 25 for this Android Gradle version: select JDK 21.
On POSIX, invoke wrappers with `bash ./mvnw` / `bash ./gradlew` if a Windows-created checkout
has no executable bit; set executable bits when first committing from a POSIX environment.

## PowerShell quick start

```powershell
# Use your own installed JDK 21 location; ignored .local copy exists on bootstrap machine.
$env:JAVA_HOME = 'C:\path\to\jdk-21'
$env:ANDROID_HOME = "$env:LOCALAPPDATA\Android\Sdk"
if (!(Test-Path infra/.env)) { Copy-Item infra/.env.example infra/.env }
# Edit infra/.env: set POSTGRES_PASSWORD. Do not commit it.
docker compose --env-file infra/.env -f infra/compose.yaml up -d --wait
$env:DB_URL = 'jdbc:postgresql://localhost:5432/rideguard'
$env:DB_USER = 'rideguard'
$env:DB_PASSWORD = '<same local password>'
apps/backend/mvnw.cmd -f apps/backend/pom.xml -B verify
apps/backend/mvnw.cmd -f apps/backend/pom.xml spring-boot:run
```

Spring reads exported process variables, not `.env` automatically. Compose `.env` is only
for infrastructure interpolation. No secret belongs in shell history on shared machines;
use your local secret injection practice. DB port override also changes DB_URL.
Health at http://localhost:8080/actuator/health; readiness includes DB. Backend outside
Docker uses localhost, future backend in Compose network uses postgres:5432.

In another terminal:

```powershell
if (!(Test-Path apps/admin-web/.env)) { Copy-Item apps/admin-web/.env.example apps/admin-web/.env }
npm.cmd --prefix apps/admin-web ci
npm.cmd --prefix apps/admin-web run dev
# independent checks/build:
npm.cmd --prefix apps/admin-web run typecheck
npm.cmd --prefix apps/admin-web run lint
npm.cmd --prefix apps/admin-web run build
apps/mobile/gradlew.bat -p apps/mobile :app:assembleDebug :app:lintDebug
```

Admin uses localhost:5173 and public build-time VITE_API_BASE_URL. No backend data is
requested by current screens. Mobile emulator backend URL is 10.0.2.2:8080; a physical
phone needs an explicit reachable LAN host and a reviewed local firewall arrangement.
Release API URL must use your real HTTPS endpoint; secrets never belong in BuildConfig.

## Python and firmware

```powershell
py -3.11 -m venv ai/.venv
ai/.venv/Scripts/python.exe -m pip install -e ./ai
ai/.venv/Scripts/python.exe -m unittest discover -s ai/tests -v
ai/.venv/Scripts/python.exe -m pip wheel --no-deps ./ai -w ai/dist
py -3.11 -m venv scripts/.venv
scripts/.venv/Scripts/python.exe -m pip install -r scripts/requirements.lock
scripts/.venv/Scripts/python.exe scripts/check_contracts.py
py -3.11 -m venv firmware/.venv
firmware/.venv/Scripts/python.exe -m pip install -r firmware/requirements.lock
firmware/.venv/Scripts/python.exe -m platformio run -d firmware -e ld2451 -e ld2417
```

On POSIX use `python3` and `.venv/bin/python`. AI has no runtime dependencies; pyproject
pins build tooling. Firmware/scripts direct requirements and resolved requirements.lock
are scoped independently. Refresh each lock intentionally and verify both Windows/Linux CI.

On this workspace's Windows path, PlatformIO/esptool package scripts mishandle spaces and
parentheses. Use an unused drive, e.g. `subst R: "<absolute repository path>"`, then run
`R:\firmware\.venv\Scripts\python.exe -m platformio run -d R:\firmware -e ld2451 -e ld2417`.
`subst R: /D` removes only the alias after build. Android sdkmanager batch launcher may
also need this alias. No source files are moved. Install Android SDK 35/build-tools 35.0.0
through Android Studio SDK Manager (or sdkmanager after accepting applicable licenses).

## Persistence and cleanup

`docker compose --env-file infra/.env -f infra/compose.yaml stop` stops PostgreSQL while
retaining named volume. Never automatically delete volume to fix startup. A password change
in `.env` does not update an existing database user. `.local/` contains local downloaded
toolchains/logs only and is ignored. No global shared Python environment was created.
No automatic production deployment. Each component produces its own artifact.

On the bootstrap machine, ignored `infra/.env` already matches the initialized local-only
database. Preserve it; read its DB settings into your terminal instead of overwriting it
with the example. Verification servers are stopped after checks; the PostgreSQL volume
is retained. Build/reinstall after stopping the JAR/preview server to avoid Windows file locks.
