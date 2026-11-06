# Proposed ERD

This diagram describes the design in [schema.md](schema.md), not installed tables.

```mermaid
erDiagram
    users ||--o{ auth_sessions : owns
    users ||--o{ password_reset_tokens : requests
    users ||--o{ device_bindings : owns
    devices ||--o{ device_bindings : history
    device_bindings ||--o| device_configs : scopes
    users ||--o{ feedbacks : writes
    device_bindings o|--o{ feedbacks : context
    users ||--o{ trips : privately_owns
    devices o|--o{ trips : optional_context
    trips ||--o{ trip_points : cascades_on_delete
```

Only one active binding per device via partial unique index. Trip ownership does not
move with device ownership. Config belongs to binding, never directly to device.
