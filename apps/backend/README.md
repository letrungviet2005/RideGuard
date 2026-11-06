# RideGuard backend

Java 21 / Spring Boot 3.5.16 / Maven wrapper 3.9.11. Use `com.rideguard` feature packages:
auth, users, devices, trips, feedback, shared; only shared infrastructure exists now.
See [architecture](../../docs/architecture/system-overview.md) for planned responsibilities.

Start PostgreSQL using [local setup](../../docs/development/local-setup.md), export
DB_PASSWORD (and optionally DB_URL/DB_USER), then from this directory:

```sh
./mvnw spring-boot:run
./mvnw -B verify
java -jar target/backend-0.1.0-SNAPSHOT.jar
```

Windows uses `./mvnw.cmd`. Verify needs PostgreSQL: it runs startup/Flyway/API checks,
not an H2 substitute. JAR packaging is part of verify. Build artifacts stay in target/.
`.env.example` is a reference; Spring does not auto-load `.env`. No credentials are embedded.

GET `/actuator/health/liveness` checks process, `/actuator/health/readiness` includes DB.
Aggregate `/actuator/health` includes DB and public group names. No sensitive details.
Startup fails if required datasource/Flyway cannot initialize; after startup a DB outage
must make readiness 503 while liveness remains 200. Other routes deny access; CSRF is on.
Only health is public, no default user, no JWT/CRUD. `{status,code,message}` errors are
used by security and MVC validation handlers. No arbitrary CORS origins allowed yet;
choose exact admin origin when implementing auth. Feature validation uses Jakarta Bean
Validation once DTOs exist. Migration V1 creates schema only; nine business tables remain design.
