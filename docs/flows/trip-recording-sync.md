# Trip recording/sync — deferred, ngoài MVP

Thiết kế hành trình trước đây đã được rút khỏi MVP bởi [ADR0002](../decisions/0002-mvp-scope.md).
File giữ ở đường dẫn cũ để không làm hỏng liên kết; đây là trang đánh dấu deferred, không
phải luồng hiện hành. [Roadmap](../roadmap.md) là điểm vào duy nhất cho phạm vi tương lai.

Không triển khai Room trip entities/queue, background trip sync, ghi/đồng bộ tọa độ GPS,
tuyến đường/thống kê, bảng trips/trip_points hoặc REST /trips trong MVP. Nếu đưa lại sau
này phải thiết kế lại consent/privacy/retention/permissions/idempotency và review độc lập;
không dùng bản thiết kế cũ làm hợp đồng hiện hành.

GPS phục vụ điều hướng vẫn MVP: mobile gửi cue/distance/TTL qua BLE mà không lưu hành
trình lên backend. Device binding history vẫn giữ bound_at/unbound_at và không liên quan
đến quyết định deferred này. Không tính năng ghi hành trình đã được triển khai/xóa trong
nhiệm vụ tài liệu; source UI cũ có nhắc hành trình được ghi nhận là lệch copy, chưa sửa code.
