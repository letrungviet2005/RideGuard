# ERD MVP — proposed

[Schema chi tiết](schema.md): 7 bảng nghiệp vụ và 3 kỹ thuật, chưa có migration nghiệp vụ.

```mermaid
erDiagram
    users ||--o{ auth_sessions : owns_generations
    auth_sessions ||--o{ auth_sessions : family_root
    users ||--o{ password_reset_tokens : requests
    users ||--o{ device_bindings : ownership_history
    devices ||--o{ device_bindings : has
    device_bindings ||--|| device_configs : scopes
    device_bindings ||--|{ device_config_versions : issues
    device_config_versions ||--o| device_configs : desired_version
    device_config_versions o|--o| device_configs : applied_version
    device_bindings ||--o{ device_reports : observes
    users ||--o{ feedbacks : authors
    devices o|--o{ feedbacks : optional_context
    device_bindings o|--o{ feedbacks : ownership_at_creation
    users ||--o{ idempotency_records : scopes
    device_bindings o|--o{ idempotency_records : bind_result
    feedbacks o|--o{ idempotency_records : feedback_result
```

devices/configs/feedbacks không chứa ownership user override từ client. Một active binding
mỗi device qua partial unique; config/report theo binding, không chuyển dữ liệu chủ cũ.
auth_sessions là generations, API sessions dedup family. idempotency_records target đúng
một loại khi hoàn tất; ERD optional edges không thay CHECK XOR. Hai config FK là composite
(binding_id,version), chỉ applied nullable. Không có hành trình/proof tables trong MVP.
