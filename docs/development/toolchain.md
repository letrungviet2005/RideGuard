# Toolchain decisions

Pinned versions are in manifests/wrappers/lockfiles; do not replace with `latest`.
Reviewed official compatibility documentation on 2026-11-06:

- Java 21, Spring Boot 3.5.16, Maven wrapper 3.9.11. Boot BOM pins Spring/Flyway/PostgreSQL
  driver dependencies. [Boot requirements](https://docs.spring.io/spring-boot/3.5/system-requirements.html)
  support Java 21 and Maven >=3.6.3. Local default Java 25 is not the chosen runtime;
  verification uses portable Temurin 21.0.13 downloaded under ignored `.local/`.
- React 19.3.1, TypeScript 5.9.3, Vite 7.3.7, React plugin 5.1.0; exact dependency graph
  in admin package-lock.json. [Vite 7 requirements](https://v7.vite.dev/guide/)
  require Node 20.19+ or 22.12+; CI uses Node 24.19.0. npm resolves peer compatibility.
  This project's full lint/build toolchain declares Node >=24.0.0. ESLint 10.14.0 and
  typescript-eslint 8.72.1 are pinned. Temporary transitive overrides minimatch 11.0.1
  and esbuild 0.28.1 remove npm-audit advisories in the resolved toolchain; typecheck,
  lint and build verify compatibility. Re-evaluate/remove overrides during future updates.
- Android AGP 8.9.2, Gradle 8.11.1 checksum-pinned wrapper, Kotlin/Compose compiler 2.1.20,
  Compose BOM 2025.04.01, activity-compose 1.10.1, navigation-compose 2.8.9, minSdk 26,
  compile/targetSdk 35, build-tools 35.0.0. JDK 21 runs Gradle; bytecode target 17.
  [AGP matrix](https://developer.android.com/build/releases/agp-8-9-0-release-notes)
  specifies Gradle 8.11.1, SDK 35 and JDK minimum 17.
  [Compose compiler](https://developer.android.com/develop/ui/compose/compiler)
  uses matching Kotlin compiler plugin version. Reassess Play target requirements before release.
- PlatformIO Core 6.1.18; platformio/espressif32 6.12.0; Arduino ESP32 2.0.17
  from that platform; board `nodemcu-32s` (ESP32, 240 MHz, 4 MB flash, 320 KB RAM).
  [Board definition](https://docs.platformio.org/en/latest/boards/espressif32/nodemcu-32s.html),
  [platform release](https://github.com/platformio/platform-espressif32/releases/tag/v6.12.0).
  No peripheral libraries needed until hardware selected. Python dependencies pinned per environment.
- Python >=3.11, CI 3.11; setuptools 80.9.0/wheel 0.45.1 build backend, no AI runtime
  dependencies. [Python packaging/venv](https://packaging.python.org/en/latest/guides/installing-using-pip-and-virtual-environments/).
- PostgreSQL 17.11 Alpine Docker image, separate named volume, loopback-only published port.
  [PostgreSQL 17 documentation](https://www.postgresql.org/docs/17/).

These versions are an explicit compatibility baseline, not a claim of hardware validation
or indefinite support. Update related versions together and run affected checks.

## Python và firmware: lệnh riêng

Chuyển từ local-setup cũ; là hướng dẫn, không phải kiểm chứng lại ngày13/11. Lệnh chạy từ gốc repo.


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


## Android — lưu ý hiện trạng13/11

Bộ đã build/install đạt là Gradle8.11.1 + checksum f397b287023acdba1e9f6fc5ea72d22dd63669d59ed4a289a29b1a76eee151c6.
Lúc rà tài liệu, wrapper trên đĩa lại trỏ9.7.1 với checksum này; đây là lệch cấu hình cần xử lý trước build tiếp, không tắt kiểm tra checksum. Lượt tài liệu giữ nguyên file người dùng.
PostgreSQL17.11 ở trên là baseline test/Compose; local Windows hiện là18.6 port5433.
