# Hợp đồng RideGuard

- [openapi.yaml](openapi.yaml): hợp đồng chính v0.4.0 gồm 3 health và 31 API MVP.
  Ngày 2026-11-09: 15 integration test đạt, runtime smoke bao phủ 31 nghiệp vụ; từng operation ghi status đã thử.
- [openapi.proposed.yaml](openapi.proposed.yaml): bản đối chiếu riêng 31 API MVP, đồng bộ hợp đồng chính;
  `x-status: integration-and-runtime-smoke-tested`, không phải backlog chưa triển khai hay verified mọi nhánh.
- [device-config.schema.json](device-config.schema.json), [device-report.schema.json](device-report.schema.json):
  cấu trúc backend có code/validation và REST smoke; không chứng minh BLE/firmware apply.
- [ble-protocol.md](ble-protocol.md): logical v0.2, chưa GATT/firmware apply. Anti-rollback mới là yêu cầu hợp đồng.
- [radar-normalized.schema.json](radar-normalized.schema.json), [session.schema.json](session.schema.json):
  proposed driver/research provenance; không phải REST raw sensor hoặc lịch sử hành trình.
- [ADR0003](../docs/decisions/0003-backend-mvp.md), [API](../docs/api/proposed-api.md), [roadmap](../docs/roadmap.md).

OpenAPI3.1/JSON Schema2020-12. Ví dụ tổng hợp, không credential hoặc chứng cứ phần cứng.
Thiếu dữ liệu dùng null; config versions trong miền số nguyên chính xác JavaScript.
Chạy `scripts/.venv/Scripts/python.exe scripts/check_contracts.py` (POSIX dùng bin/python).
Checker offline không chạy DB/backend và không xác nhận transaction/concurrency/SMTP.
[Bằng chứng thực tế và giới hạn](../docs/testing/backend-verification-2026-11-09.md).
