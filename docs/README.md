# Tài liệu RideGuard — bắt đầu tại đây

## Thứ tự đọc để tiếp tục công việc

1. [Tiến độ và bàn giao](tien-do-du-an.md): hiện trạng backend, web, Android; bằng chứng, vướng mắc và ưu tiên tiếp theo.
2. [Khởi chạy dự án](huong-dan-chay-du-an.md): PostgreSQL local, BE, web, Android và seed.
3. README của [backend](../apps/backend/README.md), [web](../apps/admin-web/README.md), [Android](../apps/mobile/README.md): cấu trúc và tài liệu chuyên sâu từng ứng dụng.
4. [OpenAPI implemented](../contracts/openapi.yaml), [API examples](backend/api-examples.md), [database](database/schema.md), [kiến trúc hệ thống](architecture/system-overview.md): hợp đồng và ranh giới kỹ thuật.
5. [Mục lục kiểm chứng](testing/verification-status.md), [web](frontend/verification-status.md), [Android](mobile/verification-2026-11-13.md): đọc ngày, môi trường và giới hạn trước khi dùng kết quả.

## Tài liệu chuyên sâu

- Backend: [runbook](backend/runbook.md), [PostgreSQL Windows](backend/postgresql-local.md), [troubleshooting](backend/troubleshooting.md), [seed demo](backend/demo-data.md).
- Web: [layout, guard và phiên](frontend/authentication-layout.md), [bao phủ API](frontend/api-coverage.md).
- Android: [setup](mobile/local-setup.md), [implementation](mobile/implementation.md), [đối chiếu API và phần còn thiếu](mobile/api-coverage-review-2026-11-13.md).
- Phát triển: [toolchain](development/toolchain.md), [Git workflow](development/git-workflow.md), [roadmap](roadmap.md), [đóng góp](../CONTRIBUTING.md).
- Phần ngoài ba ứng dụng: [firmware](../firmware/README.md), [AI](../ai/README.md), [hardware](../hardware/README.md). Hợp đồng BLE vẫn là thiết kế cần kiểm chứng trên phần cứng.

## Cách giữ tài liệu nhất quán

Tiến độ chung chỉ cập nhật tại `docs/tien-do-du-an.md`; README dẫn tới đó, không duy trì bản tiến độ thứ hai. Hướng dẫn thao tác ở tài liệu setup/runbook; kết quả thực thi ở báo cáo có ngày. Mỗi kết quả ghi số test, môi trường, phạm vi và phần chưa kiểm chứng. Báo cáo cũ giữ nguyên ý nghĩa lịch sử, bổ sung diễn biến mới có nhãn rõ.

Ưu tiên source và hợp đồng implemented khi xác định đã có gì; dùng bằng chứng kiểm chứng để xác định đã chạy gì. Proposed/ADR/flows giải thích thiết kế, không chứng minh runtime. Không lưu secret, không lấy build thành công thay thế integration hoặc phần cứng.

## Gộp tài liệu ngày 13/11/2026

Ba file trùng lặp đã được gộp nội dung cần thiết, sửa liên kết rồi bỏ:

- `docs/backend/current-state.md` → `apps/backend/README.md` (cấu trúc/code/backend) và báo cáo tiến độ chung.
- `docs/development/local-setup.md` → hướng dẫn khởi chạy chung; phần Python/firmware chuyển sang toolchain.
- `docs/frontend/runbook.md` → hướng dẫn khởi chạy chung và tài liệu layout/phiên (checks, troubleshooting).

Giữ báo cáo kiểm thử có ngày, hợp đồng, quyết định kiến trúc, schema và flow vì chúng có giá trị đối chiếu; không xóa chỉ vì đã có báo cáo mới hơn.
