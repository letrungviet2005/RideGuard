# Local PostgreSQL

Copy `.env.example` to `.env`, choose a local password, then from repository root:

```sh
docker compose --env-file infra/.env -f infra/compose.yaml up -d --wait
docker compose --env-file infra/.env -f infra/compose.yaml ps
docker compose --env-file infra/.env -f infra/compose.yaml stop
```

Compose only runs PostgreSQL 17.11 with healthcheck and named volume. Published port binds
127.0.0.1. Backend outside Docker uses localhost:5432 (or chosen POSTGRES_PORT). A future
backend container in this network uses postgres:5432; localhost there means the container
itself. Mobile emulator reaches host backend via 10.0.2.2, not Docker database.
Changing password variables does not reset an initialized DB user's password. Do not use
`down -v` or remove the volume to fix configuration problems. No production deployment.
