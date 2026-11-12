export interface paths {
    "/api/v1/auth/register": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        /**
         * Register USER account
         * @description Server assigns USER/ACTIVE. Duplicate normalized email: 409 EMAIL_UNAVAILABLE. Does not log in; call login. No default admin account.
         */
        post: operations["register"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/v1/auth/login": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        /**
         * Log in using client-specific token transport
         * @description Access TTL 900s; session absolute lifetime <=30 days. admin_web requires ADMIN and trusted Origin/CSRF header; creates HttpOnly Secure refresh cookie, never body refresh token. Bad credentials/locked user: generic 401.
         */
        post: operations["login"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/v1/auth/refresh": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        /**
         * Rotate refresh token
         * @description Body-auth mobile or cookie-auth admin_web, never anonymous. Atomic single-use rotation within original absolute expiry (not sliding). Replay revokes entire family. Check account/session on every use; web branch requires ADMIN.
         */
        post: operations["refresh"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/v1/auth/logout": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        /**
         * Revoke current session family
         * @description Revoke family identified by body/cookie credential; clear cookie for web. Repeated logout with same identifiable retired credential returns 204 within retention; unknown credential 401. Does not require an unexpired access token.
         */
        post: operations["logout"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/v1/auth/forgot-password": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        /**
         * Request password reset without account disclosure
         * @description Đã có code; chưa kiểm chứng SMTP thật. 202/body chung cho email hợp lệ: đưa tác vụ vào queue bounded trong process trước khi lookup account. SMTP adapter gửi reset code trong email, không API/log. Queue đầy: 503 cho mọi email; worker lỗi rollback token mới và log thông báo chung. Queue không bền qua crash; người dùng có thể yêu cầu lại.
         */
        post: operations["forgotPassword"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/v1/auth/reset-password": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        /**
         * Consume reset token and revoke all sessions
         * @description One-use hashed token, 15min expiry. Invalid/expired/used token: 400 INVALID_RESET_TOKEN. Atomic password update and revoke all families; reset does not unlock a locked account.
         */
        post: operations["resetPassword"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/v1/users/me": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /** Read own profile */
        get: operations["getProfile"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        /**
         * Update own full name only
         * @description Email, user_id, role and status are forbidden request properties.
         */
        patch: operations["updateProfile"];
        trace?: never;
    };
    "/api/v1/users/me/change-password": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        /**
         * Change password and revoke other sessions
         * @description Wrong current_password: 400 CURRENT_PASSWORD_INVALID. Revoke all other session families; current family remains within original expiry.
         */
        post: operations["changePassword"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/v1/users/me/sessions": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * List own active logical sessions without tokens
         * @description Sort (created_at DESC, family_id DESC); rotation does not create a new displayed session.
         */
        get: operations["listSessions"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/v1/users/me/sessions/{session_id}": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        post?: never;
        /**
         * Revoke own session family
         * @description session_id is stable family UUID. Unknown/foreign 404; already revoked own family 204 while retained. Revoking current family invalidates its access token immediately.
         */
        delete: operations["revokeSession"];
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/v1/devices": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * List devices with own active binding
         * @description Sort (bound_at DESC, binding_id DESC). Includes disabled/retired owned devices when no filter; does not expose other users or old-binding reports.
         */
        get: operations["listDevices"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/v1/devices/{device_id}": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /** Read own currently bound device */
        get: operations["getDevice"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        /**
         * Change current binding nickname only
         * @description String after trim 1–60 characters, null clears. No ownership, serial or status updates.
         */
        patch: operations["renameDevice"];
        trace?: never;
    };
    "/api/v1/devices/bind": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        /**
         * Bind preregistered serial
         * @description Transaction + device row lock + partial unique active binding. Unowned ACTIVE device: 201. Already owned by caller or valid replay: 200, nickname unchanged. Other owner: 409 DEVICE_UNAVAILABLE, no owner detail. Unknown/disabled/retired: 404 DEVICE_NOT_AVAILABLE. Closed original binding replay: 409 BINDING_NO_LONGER_ACTIVE, even after same user rebinds. Serial is not physical possession proof. Full cached response retention is 7 days; minimal key/hash/resource marker persists for resource lifetime to prevent stale recreation. Expired valid key: 409 IDEMPOTENCY_KEY_EXPIRED; in-progress concurrent request: 409 IDEMPOTENCY_IN_PROGRESS. See proposed-api.md for retention approval.
         */
        post: operations["bindDevice"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/v1/devices/{device_id}/binding": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        post?: never;
        /**
         * Close own binding, retain history
         * @description Đã có code; chưa kiểm chứng PostgreSQL. expected_binding_id bắt buộc. Đúng caller/device: đóng nếu active, retry binding đã đóng trả 204. Không tác động binding mới. Sai caller/device hoặc không tồn tại: 404.
         */
        delete: operations["unbindDevice"];
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/v1/device-bindings": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * List own binding history
         * @description Sort (bound_at DESC,id DESC). History is ownership history, not trip history; no subsequent owner identity.
         */
        get: operations["listBindings"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/v1/devices/{device_id}/config": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /** Read current binding desired/applied config */
        get: operations["getDeviceConfig"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        /**
         * Compare-and-set desired settings
         * @description expected_version only, no If-Match. Nonempty settings patch; absent fields unchanged, null/unknown rejected. Atomic increment + immutable issued version record. Wrong expected_version: 409 VERSION_CONFLICT; non-ACTIVE device: 409 DEVICE_NOT_ACTIVE. No offline edit, risk thresholds or master warning disable. vibration_enabled=false tắt cơ cấu rung; HUD và xử lý nguy cơ vẫn hoạt động. Không có master-disable hoặc ngưỡng AI.
         */
        patch: operations["updateDeviceConfig"];
        trace?: never;
    };
    "/api/v1/devices/{device_id}/config/ack": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        /**
         * Report BLE application ACK
         * @description Check current owned binding (foreign/closed/mismatch 404); require issued binding-scoped version <= desired (else 409 CONFIG_VERSION_NOT_ISSUED). Store max(previous, incoming); old/duplicate valid ACK succeeds, never decreases applied. Mobile assertion, no hardware cryptographic proof. ACK allowed even if server device status changed, without issuing new config.
         */
        post: operations["ackDeviceConfig"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/v1/devices/{device_id}/reports": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        /**
         * Submit bounded hardware-status metadata observed by mobile
         * @description Current owned binding only; serialize against binding. observed_at from mobile, received_at from server. Allow received_at-24h <= observed_at <= received_at+5min; outside: 400 OBSERVED_AT_OUT_OF_RANGE. Strictly newer observation accepted=true; older/equal valid observation accepted=false (200), never replaces latest. BLE link alone cannot imply OK. Not real-time online; no raw sensor/GPS upload.
         */
        post: operations["reportDeviceStatus"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/v1/feedbacks": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * List own feedback
         * @description Sort (created_at DESC,id DESC).
         */
        get: operations["listFeedback"];
        put?: never;
        /**
         * Create own feedback
         * @description device_id explicitly null or currently owned. Store binding context at creation; later unbind does not hide own feedback or transfer it. Client cannot set status/admin_response. Valid replay 200; different normalized payload 409 IDEMPOTENCY_CONFLICT. No attachments. Full cached response retention is 7 days; minimal key/hash/resource marker persists for resource lifetime to prevent stale recreation. Expired valid key: 409 IDEMPOTENCY_KEY_EXPIRED; in-progress concurrent request: 409 IDEMPOTENCY_IN_PROGRESS. See proposed-api.md for retention approval.
         */
        post: operations["createFeedback"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/v1/feedbacks/{feedback_id}": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /** Read own feedback and admin response */
        get: operations["getFeedback"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/v1/admin/users": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * List users (ADMIN required)
         * @description Sort (created_at DESC,id DESC). Never returns password/session tokens.
         */
        get: operations["adminListUsers"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/v1/admin/users/{user_id}/status": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        /**
         * Lock or unlock account (ADMIN required)
         * @description Đã có code; chưa kiểm chứng PostgreSQL. LOCKED revoke mọi phiên; ACTIVE không hồi sinh phiên. 409 ADMIN_SELF_LOCK khi tự khóa, 409 LAST_ADMIN khi khóa admin ACTIVE cuối. Khóa advisory transaction để tuần tự hóa thao tác admin.
         */
        patch: operations["adminSetUserStatus"];
        trace?: never;
    };
    "/api/v1/admin/devices": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * List registered devices (ADMIN required)
         * @description Sort (created_at DESC,id DESC). No owner transfer or location history.
         */
        get: operations["adminListDevices"];
        put?: never;
        /**
         * Preregister unique serial (ADMIN required)
         * @description Server assigns ACTIVE. Duplicate canonical serial: 409 SERIAL_ALREADY_REGISTERED; unique DB constraint. Does not create a binding. No hardware secret fields.
         */
        post: operations["adminCreateDevice"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/v1/admin/devices/{device_id}/status": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        /**
         * Change registry status (ADMIN required)
         * @description ACTIVE <-> DISABLED; either may become RETIRED; RETIRED terminal (same status no-op, reversal 409 INVALID_STATUS_TRANSITION). Binding history retained. Status gates bind/config edits only; not an ESP32 shutdown command; warnings remain local.
         */
        patch: operations["adminSetDeviceStatus"];
        trace?: never;
    };
    "/api/v1/admin/feedbacks": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * List feedback for handling (ADMIN required)
         * @description Sort (created_at DESC,id DESC). No GPS/attachments.
         */
        get: operations["adminListFeedback"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/v1/admin/feedbacks/{feedback_id}": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        /**
         * Set feedback status/response (ADMIN required)
         * @description Any OPEN/IN_PROGRESS/RESOLVED transition allowed, including reopen. Only status/admin_response writable; response timestamp is server time when response changes. Omitted fields unchanged, null rejected; empty patch rejected.
         */
        patch: operations["adminUpdateFeedback"];
        trace?: never;
    };
    "/actuator/health": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /** Aggregate health (includes database) */
        get: operations["health"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/actuator/health/liveness": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /** Process liveness (does not require database after startup) */
        get: operations["liveness"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/actuator/health/readiness": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /** Ready to serve only when application and database are available */
        get: operations["readiness"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
}
export type webhooks = Record<string, never>;
export interface components {
    schemas: {
        Error: {
            readonly status: number;
            readonly code: string;
            readonly message: string;
            /** Format: uuid */
            readonly request_id: string;
        };
        User: {
            /** Format: uuid */
            readonly id: string;
            /**
             * Format: email
             * @description Trim and lowercase entire address; no provider-specific dot/plus rewriting. MVP does not change email.
             */
            readonly email: string;
            /** @description Trim before validation; 1–100 Unicode characters. */
            readonly full_name: string;
            /** @enum {string} */
            readonly role: "USER" | "ADMIN";
            /** @enum {string} */
            readonly status: "ACTIVE" | "LOCKED";
            /**
             * Format: date-time
             * @description RFC 3339 UTC, serialized with Z suffix.
             */
            readonly created_at: string;
            /**
             * Format: date-time
             * @description RFC 3339 UTC, serialized with Z suffix.
             */
            readonly updated_at: string;
        };
        RegisterRequest: {
            /**
             * Format: email
             * @description Trim and lowercase entire address; no provider-specific dot/plus rewriting. MVP does not change email.
             */
            email: string;
            /** @description Trim before validation; 1–100 Unicode characters. */
            full_name: string;
            /** @description 12–128 Unicode characters; never trim, normalize, truncate or echo. */
            password: string;
        };
        LoginRequest: {
            /**
             * Format: email
             * @description Trim and lowercase entire address; no provider-specific dot/plus rewriting. MVP does not change email.
             */
            email: string;
            /** @description 12–128 Unicode characters; never trim, normalize, truncate or echo. */
            password: string;
            /** @default null */
            client_name?: string | null;
        };
        /** @description X-Client-Type=mobile requires refresh_token body and ignores cookies. admin_web requires {} body plus rg_refresh cookie and X-CSRF-Protection: 1; refresh_token in body rejected. */
        RefreshRequest: components["schemas"]["MobileRefreshRequest"] | components["schemas"]["WebRefreshRequest"];
        MobileRefreshRequest: {
            /** @description Opaque high-entropy credential; example values are synthetic, never valid credentials. */
            refresh_token: string;
        };
        WebRefreshRequest: Record<string, never>;
        MobileTokenResponse: {
            readonly access_token: string;
            readonly refresh_token: string;
            /** @constant */
            readonly token_type: "Bearer";
            /** @constant */
            readonly expires_in: 900;
            user: components["schemas"]["User"];
        };
        WebTokenResponse: {
            readonly access_token: string;
            /** @constant */
            readonly token_type: "Bearer";
            /** @constant */
            readonly expires_in: 900;
            user: components["schemas"]["User"];
        };
        /** @description Mobile response includes refresh_token. admin_web response excludes it; Set-Cookie carries rotated refresh token. Transport selected by X-Client-Type, never inferred from body alone. */
        TokenResponse: components["schemas"]["MobileTokenResponse"] | components["schemas"]["WebTokenResponse"];
        ForgotPasswordRequest: {
            /**
             * Format: email
             * @description Trim and lowercase entire address; no provider-specific dot/plus rewriting. MVP does not change email.
             */
            email: string;
        };
        Message: {
            readonly message: string;
        };
        ResetPasswordRequest: {
            /** @description Opaque high-entropy credential; example values are synthetic, never valid credentials. */
            reset_token: string;
            /** @description 12–128 Unicode characters; never trim, normalize, truncate or echo. */
            new_password: string;
        };
        UpdateProfileRequest: {
            /** @description Trim before validation; 1–100 Unicode characters. */
            full_name: string;
        };
        ChangePasswordRequest: {
            /** @description 12–128 Unicode characters; never trim, normalize, truncate or echo. */
            current_password: string;
            /** @description 12–128 Unicode characters; never trim, normalize, truncate or echo. */
            new_password: string;
        };
        /** @description One logical session family; id is its stable family_id across refresh rotation. No token/hash returned. Only unexpired, unrevoked sessions listed. */
        Session: {
            /** Format: uuid */
            readonly id: string;
            /** @enum {string} */
            readonly client_type: "mobile" | "admin_web";
            readonly client_name: string | null;
            /**
             * Format: date-time
             * @description RFC 3339 UTC, serialized with Z suffix.
             */
            readonly created_at: string;
            /**
             * Format: date-time
             * @description RFC 3339 UTC, serialized with Z suffix.
             */
            readonly last_seen_at: string;
            /**
             * Format: date-time
             * @description RFC 3339 UTC, serialized with Z suffix.
             */
            readonly expires_at: string;
            readonly is_current: boolean;
        };
        /** @description Latest accepted observation for current binding only; not an online indicator. */
        DeviceReport: {
            /** Format: uuid */
            readonly binding_id: string;
            /**
             * Format: date-time
             * @description RFC 3339 UTC, serialized with Z suffix.
             */
            readonly observed_at: string;
            readonly firmware_version: string | null;
            /** @enum {string} */
            readonly radar_status: "OK" | "ERROR" | "UNAVAILABLE" | "UNKNOWN";
            /** @enum {string} */
            readonly imu_status: "OK" | "ERROR" | "UNAVAILABLE" | "UNKNOWN";
            /** @enum {string} */
            readonly display_status: "OK" | "ERROR" | "UNAVAILABLE" | "UNKNOWN";
            /** @enum {string} */
            readonly vibration_status: "OK" | "ERROR" | "UNAVAILABLE" | "UNKNOWN";
            readonly battery_percent: number | null;
            /**
             * Format: date-time
             * @description RFC 3339 UTC, serialized with Z suffix.
             */
            readonly received_at: string;
        };
        Device: {
            /** Format: uuid */
            readonly id: string;
            /** Format: uuid */
            readonly binding_id: string;
            /** @description Canonical serial: trim and uppercase ASCII before validation. Not a password; preregistered by admin. */
            readonly serial_number: string;
            /** @description Trim string; null clears nickname. Stored on ownership binding. */
            readonly nickname: string | null;
            readonly model: string;
            readonly hardware_revision: string | null;
            /** @enum {string} */
            readonly status: "ACTIVE" | "DISABLED" | "RETIRED";
            readonly latest_report: components["schemas"]["DeviceReport"] | null;
        };
        BindRequest: {
            /** @description Canonical serial: trim and uppercase ASCII before validation. Not a password; preregistered by admin. */
            serial_number: string;
            /**
             * @description Trim string; null clears nickname. Stored on ownership binding.
             * @default null
             */
            nickname?: string | null;
        };
        RenameDeviceRequest: {
            /** @description Trim string; null clears nickname. Stored on ownership binding. */
            nickname: string | null;
        };
        Binding: {
            /** Format: uuid */
            readonly id: string;
            /** Format: uuid */
            readonly device_id: string;
            /** @description Canonical serial: trim and uppercase ASCII before validation. Not a password; preregistered by admin. */
            readonly serial_number: string;
            /** @description Trim string; null clears nickname. Stored on ownership binding. */
            readonly nickname: string | null;
            readonly model: string;
            readonly hardware_revision: string | null;
            /**
             * Format: date-time
             * @description RFC 3339 UTC, serialized with Z suffix.
             */
            readonly bound_at: string;
            /**
             * Format: date-time
             * @description RFC 3339 UTC, serialized with Z suffix.
             */
            readonly unbound_at: string | null;
            /** @enum {string} */
            readonly status: "ACTIVE" | "UNBOUND";
        };
        DeviceConfig: components["schemas"]["device-config.schema"];
        SettingsPatch: {
            display_brightness?: components["schemas"]["display_brightness"];
            vibration_enabled?: components["schemas"]["vibration_enabled"];
        };
        ConfigPatchRequest: {
            expected_version: number;
            settings: components["schemas"]["SettingsPatch"];
        };
        ConfigAckRequest: {
            /** Format: uuid */
            binding_id: string;
            reported_applied_version: number;
        };
        DeviceReportRequest: components["schemas"]["device-report.schema"];
        ReportReceipt: {
            readonly accepted: boolean;
            /**
             * Format: date-time
             * @description RFC 3339 UTC, serialized with Z suffix.
             */
            readonly received_at: string;
        };
        CreateFeedbackRequest: {
            /** Format: uuid */
            device_id: string | null;
            /** @enum {string} */
            category: "BUG" | "SUGGESTION" | "OTHER";
            /** @description Trim before validation; 1–5000 Unicode characters; no attachments. */
            content: string;
        };
        Feedback: {
            /** Format: uuid */
            readonly id: string;
            /** Format: uuid */
            readonly device_id: string | null;
            /** @enum {string} */
            readonly category: "BUG" | "SUGGESTION" | "OTHER";
            /** @description Trim before validation; 1–5000 Unicode characters; no attachments. */
            readonly content: string;
            /** @enum {string} */
            readonly status: "OPEN" | "IN_PROGRESS" | "RESOLVED";
            /** @description Trim before validation; 1–5000 Unicode characters; no attachments. */
            readonly admin_response: string | null;
            /**
             * Format: date-time
             * @description RFC 3339 UTC, serialized with Z suffix.
             */
            readonly created_at: string;
            /**
             * Format: date-time
             * @description RFC 3339 UTC, serialized with Z suffix.
             */
            readonly responded_at: string | null;
        };
        AdminFeedback: {
            /** Format: uuid */
            readonly id: string;
            /** Format: uuid */
            readonly device_id: string | null;
            /** @enum {string} */
            readonly category: "BUG" | "SUGGESTION" | "OTHER";
            /** @description Trim before validation; 1–5000 Unicode characters; no attachments. */
            readonly content: string;
            /** @enum {string} */
            readonly status: "OPEN" | "IN_PROGRESS" | "RESOLVED";
            /** @description Trim before validation; 1–5000 Unicode characters; no attachments. */
            readonly admin_response: string | null;
            /**
             * Format: date-time
             * @description RFC 3339 UTC, serialized with Z suffix.
             */
            readonly created_at: string;
            /**
             * Format: date-time
             * @description RFC 3339 UTC, serialized with Z suffix.
             */
            readonly responded_at: string | null;
            /** Format: uuid */
            readonly user_id: string;
            /** Format: uuid */
            readonly binding_id: string | null;
        };
        SetUserStatusRequest: {
            /** @enum {string} */
            status: "ACTIVE" | "LOCKED";
        };
        CreateDeviceRequest: {
            /** @description Canonical serial: trim and uppercase ASCII before validation. Not a password; preregistered by admin. */
            serial_number: string;
            model: string;
            /** @default null */
            hardware_revision?: string | null;
        };
        AdminDevice: {
            /** Format: uuid */
            readonly id: string;
            /** @description Canonical serial: trim and uppercase ASCII before validation. Not a password; preregistered by admin. */
            readonly serial_number: string;
            readonly model: string;
            readonly hardware_revision: string | null;
            /** @enum {string} */
            readonly status: "ACTIVE" | "DISABLED" | "RETIRED";
            /**
             * Format: date-time
             * @description RFC 3339 UTC, serialized with Z suffix.
             */
            readonly created_at: string;
            /**
             * Format: date-time
             * @description RFC 3339 UTC, serialized with Z suffix.
             */
            readonly updated_at: string;
            /** Format: uuid */
            readonly active_binding_id: string | null;
        };
        SetDeviceStatusRequest: {
            /** @enum {string} */
            status: "ACTIVE" | "DISABLED" | "RETIRED";
        };
        /** @description Omitted fields unchanged, null rejected. admin_response updates responded_at to server time. Status changes alone preserve response/timestamp. No user/device/content reassignment. */
        AdminFeedbackPatchRequest: {
            /** @enum {string} */
            status?: "OPEN" | "IN_PROGRESS" | "RESOLVED";
            /** @description Trim before validation; 1–5000 Unicode characters; no attachments. */
            admin_response?: string;
        };
        DeviceList: {
            items: components["schemas"]["Device"][];
            next_cursor: string | null;
        };
        BindingList: {
            items: components["schemas"]["Binding"][];
            next_cursor: string | null;
        };
        FeedbackList: {
            items: components["schemas"]["Feedback"][];
            next_cursor: string | null;
        };
        AdminFeedbackList: {
            items: components["schemas"]["AdminFeedback"][];
            next_cursor: string | null;
        };
        UserList: {
            items: components["schemas"]["User"][];
            next_cursor: string | null;
        };
        AdminDeviceList: {
            items: components["schemas"]["AdminDevice"][];
            next_cursor: string | null;
        };
        SessionList: {
            items: components["schemas"]["Session"][];
            next_cursor: string | null;
        };
        Health: {
            /** @enum {string} */
            status: "UP" | "DOWN" | "OUT_OF_SERVICE" | "UNKNOWN";
            /** @description Public probe group names on aggregate health only */
            groups?: ("liveness" | "readiness")[];
        };
        ApiError: {
            status: number;
            code: string;
            message: string;
            /** Format: uuid */
            request_id: string;
        };
        /**
         * Device configuration v1 — PostgreSQL integration and REST smoke tested; hardware unverified
         * @description Binding-scoped issued versions; applied <= desired. vibration_enabled=false disables the vibration actuator; HUD and risk processing remain active. No master-disable or AI threshold. Firmware anti-rollback is a contract requirement, not implemented hardware evidence.
         * @example {
         *       "binding_id": "00000000-0000-4000-8000-000000000003",
         *       "schema_version": 1,
         *       "desired_version": 2,
         *       "reported_applied_version": 1,
         *       "settings": {
         *         "display_brightness": 70,
         *         "vibration_enabled": true
         *       }
         *     }
         */
        "device-config.schema": {
            /** Format: uuid */
            readonly binding_id: string;
            /** @constant */
            readonly schema_version: 1;
            readonly desired_version: number;
            readonly reported_applied_version: number | null;
            settings: {
                display_brightness: number;
                vibration_enabled: boolean;
            };
        };
        display_brightness: number;
        vibration_enabled: boolean;
        /**
         * Device status report v1 — PostgreSQL integration and REST smoke tested; hardware unverified
         * @description REST v1 mobile report, not hardware attestation or real-time online status. All fields required; unavailable firmware/battery use null. Server receives within [received_at-24h, received_at+5min]; stale/equal observations do not replace newer ones. No radar/IMU samples or GPS.
         * @example {
         *       "binding_id": "00000000-0000-4000-8000-000000000003",
         *       "observed_at": "2026-11-06T03:00:00Z",
         *       "firmware_version": "0.1.0",
         *       "radar_status": "OK",
         *       "imu_status": "OK",
         *       "display_status": "OK",
         *       "vibration_status": "UNKNOWN",
         *       "battery_percent": null
         *     }
         */
        "device-report.schema": {
            /** Format: uuid */
            binding_id: string;
            /**
             * Format: date-time
             * @description RFC 3339 UTC, serialized with Z suffix.
             */
            observed_at: string;
            firmware_version: string | null;
            /** @enum {string} */
            radar_status: "OK" | "ERROR" | "UNAVAILABLE" | "UNKNOWN";
            /** @enum {string} */
            imu_status: "OK" | "ERROR" | "UNAVAILABLE" | "UNKNOWN";
            /** @enum {string} */
            display_status: "OK" | "ERROR" | "UNAVAILABLE" | "UNKNOWN";
            /** @enum {string} */
            vibration_status: "OK" | "ERROR" | "UNAVAILABLE" | "UNKNOWN";
            battery_percent: number | null;
        };
    };
    responses: {
        /** @description Invalid schema, unknown/null field, timestamp or cursor. */
        Error400: {
            headers: {
                [name: string]: unknown;
            };
            content: {
                /**
                 * @example {
                 *       "status": 400,
                 *       "code": "INVALID_REQUEST",
                 *       "message": "Không thể hoàn tất yêu cầu.",
                 *       "request_id": "00000000-0000-4000-8000-000000000001"
                 *     }
                 */
                "application/json": components["schemas"]["Error"];
            };
        };
        /** @description Missing/expired credential, revoked session or locked account. */
        Error401: {
            headers: {
                [name: string]: unknown;
            };
            content: {
                /**
                 * @example {
                 *       "status": 401,
                 *       "code": "UNAUTHORIZED",
                 *       "message": "Không thể hoàn tất yêu cầu.",
                 *       "request_id": "00000000-0000-4000-8000-000000000001"
                 *     }
                 */
                "application/json": components["schemas"]["Error"];
            };
        };
        /** @description Valid credential but missing role, or rejected browser Origin/CSRF. */
        Error403: {
            headers: {
                [name: string]: unknown;
            };
            content: {
                /**
                 * @example {
                 *       "status": 403,
                 *       "code": "FORBIDDEN",
                 *       "message": "Không thể hoàn tất yêu cầu.",
                 *       "request_id": "00000000-0000-4000-8000-000000000001"
                 *     }
                 */
                "application/json": components["schemas"]["Error"];
            };
        };
        /** @description Absent or inaccessible private resource; never reveal another owner. */
        Error404: {
            headers: {
                [name: string]: unknown;
            };
            content: {
                /**
                 * @example {
                 *       "status": 404,
                 *       "code": "NOT_FOUND",
                 *       "message": "Không thể hoàn tất yêu cầu.",
                 *       "request_id": "00000000-0000-4000-8000-000000000001"
                 *     }
                 */
                "application/json": components["schemas"]["Error"];
            };
        };
        /** @description State, version or idempotency conflict. */
        Error409: {
            headers: {
                [name: string]: unknown;
            };
            content: {
                /**
                 * @example {
                 *       "status": 409,
                 *       "code": "CONFLICT",
                 *       "message": "Không thể hoàn tất yêu cầu.",
                 *       "request_id": "00000000-0000-4000-8000-000000000001"
                 *     }
                 */
                "application/json": components["schemas"]["Error"];
            };
        };
        /** @description JSON request exceeds 32 KiB decoded UTF-8 bytes. */
        Error413: {
            headers: {
                [name: string]: unknown;
            };
            content: {
                /**
                 * @example {
                 *       "status": 413,
                 *       "code": "PAYLOAD_TOO_LARGE",
                 *       "message": "Không thể hoàn tất yêu cầu.",
                 *       "request_id": "00000000-0000-4000-8000-000000000001"
                 *     }
                 */
                "application/json": components["schemas"]["Error"];
            };
        };
        /** @description Configurable rate limit exceeded; retry after supplied delay. */
        Error429: {
            headers: {
                /**
                 * @description Delay in seconds (proposed policy).
                 * @example 30
                 */
                "Retry-After"?: number;
                [name: string]: unknown;
            };
            content: {
                /**
                 * @example {
                 *       "status": 429,
                 *       "code": "RATE_LIMITED",
                 *       "message": "Không thể hoàn tất yêu cầu.",
                 *       "request_id": "00000000-0000-4000-8000-000000000001"
                 *     }
                 */
                "application/json": components["schemas"]["Error"];
            };
        };
        /** @description Unexpected server error; no internal details. */
        Error500: {
            headers: {
                [name: string]: unknown;
            };
            content: {
                /**
                 * @example {
                 *       "status": 500,
                 *       "code": "INTERNAL_ERROR",
                 *       "message": "Không thể hoàn tất yêu cầu.",
                 *       "request_id": "00000000-0000-4000-8000-000000000001"
                 *     }
                 */
                "application/json": components["schemas"]["Error"];
            };
        };
        /** @description Temporary service outage; bounded retry allowed. */
        Error503: {
            headers: {
                /**
                 * @description Delay in seconds (proposed policy).
                 * @example 30
                 */
                "Retry-After"?: number;
                [name: string]: unknown;
            };
            content: {
                /**
                 * @example {
                 *       "status": 503,
                 *       "code": "SERVICE_UNAVAILABLE",
                 *       "message": "Không thể hoàn tất yêu cầu.",
                 *       "request_id": "00000000-0000-4000-8000-000000000001"
                 *     }
                 */
                "application/json": components["schemas"]["Error"];
            };
        };
        /** @description Healthy, without component details */
        Healthy: {
            headers: {
                [name: string]: unknown;
            };
            content: {
                "application/json": components["schemas"]["Health"];
            };
        };
        /** @description Not healthy or not ready; no sensitive details */
        Unhealthy: {
            headers: {
                [name: string]: unknown;
            };
            content: {
                "application/json": components["schemas"]["Health"];
            };
        };
    };
    parameters: {
        /** @example 20 */
        Limit: number;
        /**
         * @description Opaque integrity-protected cursor bound to caller, route, sort and filters. Invalid/tampered/mismatched cursor: 400.
         * @example opaque-synthetic-cursor
         */
        Cursor: string;
        /**
         * @description UUID scoped to authenticated user+method+route+key. Retain 7 days after success. Same validated/normalized payload: replay after fresh authorization/state check, no duplicate. Different payload: 409 IDEMPOTENCY_CONFLICT. Closed original binding: 409 BINDING_NO_LONGER_ACTIVE. Full response cache expires after 7 days; retain minimal key/hash/resource tombstone for the resource lifetime. After cache expiry return 409 IDEMPOTENCY_KEY_EXPIRED, with BINDING_NO_LONGER_ACTIVE taking precedence for closed binding. Never silently treat an expired key as a new operation. Same-key success replay is 200; permission/state is checked first. Canonical request hashing includes defaults for omitted optional fields (nickname=null), sorted object keys, UTF-8 and the documented string normalization.
         * @example 00000000-0000-4000-8000-000000000004
         */
        IdempotencyKey: string;
        /** @example mobile */
        ClientType: "mobile" | "admin_web";
        /**
         * @description Required for admin_web login/refresh/logout; strict Origin validation + non-simple JSON/header + credentialed exact-origin CORS. Not a secret. Deployment origins/SameSite must be approved before enabling cookies.
         * @example 1
         */
        CsrfProtection: "1";
        /** @example 00000000-0000-4000-8000-000000000002 */
        device_id: string;
        /** @example 00000000-0000-4000-8000-000000000004 */
        session_id: string;
        /** @example 00000000-0000-4000-8000-000000000001 */
        user_id: string;
        /** @example 00000000-0000-4000-8000-000000000004 */
        feedback_id: string;
    };
    requestBodies: never;
    headers: never;
    pathItems: never;
}
export type $defs = Record<string, never>;
export interface operations {
    register: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /** @description JSON UTF-8, max 32 KiB decoded body. Normalize documented strings then validate; passwords are not normalized. Unknown properties rejected. */
        requestBody: {
            content: {
                /**
                 * @example {
                 *       "email": "viet@example.invalid",
                 *       "full_name": "Việt",
                 *       "password": "Example-only-password-2026"
                 *     }
                 */
                "application/json": components["schemas"]["RegisterRequest"];
            };
        };
        responses: {
            /** @description Success */
            201: {
                headers: {
                    /**
                     * @description Server-generated request UUID
                     * @example 00000000-0000-4000-8000-000000000001
                     */
                    "X-Request-ID"?: string;
                    [name: string]: unknown;
                };
                content: {
                    /**
                     * @example {
                     *       "id": "00000000-0000-4000-8000-000000000001",
                     *       "email": "viet@example.invalid",
                     *       "full_name": "Việt",
                     *       "role": "USER",
                     *       "status": "ACTIVE",
                     *       "created_at": "2026-11-06T03:00:00Z",
                     *       "updated_at": "2026-11-06T03:00:00Z"
                     *     }
                     */
                    "application/json": components["schemas"]["User"];
                };
            };
            400: components["responses"]["Error400"];
            409: components["responses"]["Error409"];
            413: components["responses"]["Error413"];
            429: components["responses"]["Error429"];
            500: components["responses"]["Error500"];
            503: components["responses"]["Error503"];
        };
    };
    login: {
        parameters: {
            query?: never;
            header: {
                /** @example mobile */
                "X-Client-Type": components["parameters"]["ClientType"];
                /**
                 * @description Required for admin_web login/refresh/logout; strict Origin validation + non-simple JSON/header + credentialed exact-origin CORS. Not a secret. Deployment origins/SameSite must be approved before enabling cookies.
                 * @example 1
                 */
                "X-CSRF-Protection"?: components["parameters"]["CsrfProtection"];
            };
            path?: never;
            cookie?: never;
        };
        /** @description JSON UTF-8, max 32 KiB decoded body. Normalize documented strings then validate; passwords are not normalized. Unknown properties rejected. */
        requestBody: {
            content: {
                /**
                 * @example {
                 *       "email": "viet@example.invalid",
                 *       "password": "Example-only-password-2026",
                 *       "client_name": "Android phone"
                 *     }
                 */
                "application/json": components["schemas"]["LoginRequest"];
            };
        };
        responses: {
            /** @description Success */
            200: {
                headers: {
                    /**
                     * @description Only admin_web: rg_refresh=<opaque>; HttpOnly; Secure; Path=/api/v1/auth; SameSite=Lax for proposed same-site deployment. Cross-site topology needs explicit None + Secure, strict Origin/CORS/CSRF review. Not configured today.
                     * @example rg_refresh=synthetic-cookie-value; HttpOnly; Secure; Path=/api/v1/auth; SameSite=Lax
                     */
                    "Set-Cookie"?: string;
                    /** @example no-store */
                    "Cache-Control"?: "no-store";
                    /**
                     * @description Server-generated request UUID
                     * @example 00000000-0000-4000-8000-000000000001
                     */
                    "X-Request-ID"?: string;
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["TokenResponse"];
                };
            };
            400: components["responses"]["Error400"];
            401: components["responses"]["Error401"];
            403: components["responses"]["Error403"];
            413: components["responses"]["Error413"];
            429: components["responses"]["Error429"];
            500: components["responses"]["Error500"];
            503: components["responses"]["Error503"];
        };
    };
    refresh: {
        parameters: {
            query?: never;
            header: {
                /** @example mobile */
                "X-Client-Type": components["parameters"]["ClientType"];
                /**
                 * @description Required for admin_web login/refresh/logout; strict Origin validation + non-simple JSON/header + credentialed exact-origin CORS. Not a secret. Deployment origins/SameSite must be approved before enabling cookies.
                 * @example 1
                 */
                "X-CSRF-Protection"?: components["parameters"]["CsrfProtection"];
            };
            path?: never;
            cookie?: never;
        };
        /** @description JSON UTF-8, max 32 KiB decoded body. Normalize documented strings then validate; passwords are not normalized. Unknown properties rejected. */
        requestBody: {
            content: {
                "application/json": components["schemas"]["RefreshRequest"];
            };
        };
        responses: {
            /** @description Success */
            200: {
                headers: {
                    /**
                     * @description Only admin_web: rg_refresh=<opaque>; HttpOnly; Secure; Path=/api/v1/auth; SameSite=Lax for proposed same-site deployment. Cross-site topology needs explicit None + Secure, strict Origin/CORS/CSRF review. Not configured today.
                     * @example rg_refresh=synthetic-cookie-value; HttpOnly; Secure; Path=/api/v1/auth; SameSite=Lax
                     */
                    "Set-Cookie"?: string;
                    /** @example no-store */
                    "Cache-Control"?: "no-store";
                    /**
                     * @description Server-generated request UUID
                     * @example 00000000-0000-4000-8000-000000000001
                     */
                    "X-Request-ID"?: string;
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["TokenResponse"];
                };
            };
            400: components["responses"]["Error400"];
            401: components["responses"]["Error401"];
            403: components["responses"]["Error403"];
            413: components["responses"]["Error413"];
            429: components["responses"]["Error429"];
            500: components["responses"]["Error500"];
            503: components["responses"]["Error503"];
        };
    };
    logout: {
        parameters: {
            query?: never;
            header: {
                /** @example mobile */
                "X-Client-Type": components["parameters"]["ClientType"];
                /**
                 * @description Required for admin_web login/refresh/logout; strict Origin validation + non-simple JSON/header + credentialed exact-origin CORS. Not a secret. Deployment origins/SameSite must be approved before enabling cookies.
                 * @example 1
                 */
                "X-CSRF-Protection"?: components["parameters"]["CsrfProtection"];
            };
            path?: never;
            cookie?: never;
        };
        /** @description JSON UTF-8, max 32 KiB decoded body. Normalize documented strings then validate; passwords are not normalized. Unknown properties rejected. */
        requestBody: {
            content: {
                "application/json": components["schemas"]["RefreshRequest"];
            };
        };
        responses: {
            /** @description Success */
            204: {
                headers: {
                    /**
                     * @description Clear web refresh cookie with matching Path/domain policy.
                     * @example rg_refresh=; Max-Age=0; HttpOnly; Secure; Path=/api/v1/auth; SameSite=Lax
                     */
                    "Set-Cookie"?: string;
                    /**
                     * @description Server-generated request UUID
                     * @example 00000000-0000-4000-8000-000000000001
                     */
                    "X-Request-ID"?: string;
                    [name: string]: unknown;
                };
                content?: never;
            };
            400: components["responses"]["Error400"];
            401: components["responses"]["Error401"];
            403: components["responses"]["Error403"];
            413: components["responses"]["Error413"];
            429: components["responses"]["Error429"];
            500: components["responses"]["Error500"];
            503: components["responses"]["Error503"];
        };
    };
    forgotPassword: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /** @description JSON UTF-8, max 32 KiB decoded body. Normalize documented strings then validate; passwords are not normalized. Unknown properties rejected. */
        requestBody: {
            content: {
                /**
                 * @example {
                 *       "email": "viet@example.invalid"
                 *     }
                 */
                "application/json": components["schemas"]["ForgotPasswordRequest"];
            };
        };
        responses: {
            /** @description Success */
            202: {
                headers: {
                    /**
                     * @description Server-generated request UUID
                     * @example 00000000-0000-4000-8000-000000000001
                     */
                    "X-Request-ID"?: string;
                    [name: string]: unknown;
                };
                content: {
                    /**
                     * @example {
                     *       "message": "Nếu tài khoản phù hợp tồn tại, hướng dẫn khôi phục sẽ được gửi."
                     *     }
                     */
                    "application/json": components["schemas"]["Message"];
                };
            };
            400: components["responses"]["Error400"];
            413: components["responses"]["Error413"];
            429: components["responses"]["Error429"];
            500: components["responses"]["Error500"];
            503: components["responses"]["Error503"];
        };
    };
    resetPassword: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /** @description JSON UTF-8, max 32 KiB decoded body. Normalize documented strings then validate; passwords are not normalized. Unknown properties rejected. */
        requestBody: {
            content: {
                /**
                 * @example {
                 *       "reset_token": "synthetic-reset-token-not-a-real-secret",
                 *       "new_password": "Example-only-new-password-2026"
                 *     }
                 */
                "application/json": components["schemas"]["ResetPasswordRequest"];
            };
        };
        responses: {
            /** @description Success */
            204: {
                headers: {
                    /**
                     * @description Server-generated request UUID
                     * @example 00000000-0000-4000-8000-000000000001
                     */
                    "X-Request-ID"?: string;
                    [name: string]: unknown;
                };
                content?: never;
            };
            400: components["responses"]["Error400"];
            413: components["responses"]["Error413"];
            429: components["responses"]["Error429"];
            500: components["responses"]["Error500"];
            503: components["responses"]["Error503"];
        };
    };
    getProfile: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description Success */
            200: {
                headers: {
                    /**
                     * @description Server-generated request UUID
                     * @example 00000000-0000-4000-8000-000000000001
                     */
                    "X-Request-ID"?: string;
                    [name: string]: unknown;
                };
                content: {
                    /**
                     * @example {
                     *       "id": "00000000-0000-4000-8000-000000000001",
                     *       "email": "viet@example.invalid",
                     *       "full_name": "Việt",
                     *       "role": "USER",
                     *       "status": "ACTIVE",
                     *       "created_at": "2026-11-06T03:00:00Z",
                     *       "updated_at": "2026-11-06T03:00:00Z"
                     *     }
                     */
                    "application/json": components["schemas"]["User"];
                };
            };
            400: components["responses"]["Error400"];
            401: components["responses"]["Error401"];
            403: components["responses"]["Error403"];
            429: components["responses"]["Error429"];
            500: components["responses"]["Error500"];
            503: components["responses"]["Error503"];
        };
    };
    updateProfile: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /** @description JSON UTF-8, max 32 KiB decoded body. Normalize documented strings then validate; passwords are not normalized. Unknown properties rejected. */
        requestBody: {
            content: {
                /**
                 * @example {
                 *       "full_name": "Việt"
                 *     }
                 */
                "application/json": components["schemas"]["UpdateProfileRequest"];
            };
        };
        responses: {
            /** @description Success */
            200: {
                headers: {
                    /**
                     * @description Server-generated request UUID
                     * @example 00000000-0000-4000-8000-000000000001
                     */
                    "X-Request-ID"?: string;
                    [name: string]: unknown;
                };
                content: {
                    /**
                     * @example {
                     *       "id": "00000000-0000-4000-8000-000000000001",
                     *       "email": "viet@example.invalid",
                     *       "full_name": "Việt",
                     *       "role": "USER",
                     *       "status": "ACTIVE",
                     *       "created_at": "2026-11-06T03:00:00Z",
                     *       "updated_at": "2026-11-06T03:00:00Z"
                     *     }
                     */
                    "application/json": components["schemas"]["User"];
                };
            };
            400: components["responses"]["Error400"];
            401: components["responses"]["Error401"];
            403: components["responses"]["Error403"];
            413: components["responses"]["Error413"];
            429: components["responses"]["Error429"];
            500: components["responses"]["Error500"];
            503: components["responses"]["Error503"];
        };
    };
    changePassword: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /** @description JSON UTF-8, max 32 KiB decoded body. Normalize documented strings then validate; passwords are not normalized. Unknown properties rejected. */
        requestBody: {
            content: {
                /**
                 * @example {
                 *       "current_password": "Example-only-password-2026",
                 *       "new_password": "Example-only-new-password-2026"
                 *     }
                 */
                "application/json": components["schemas"]["ChangePasswordRequest"];
            };
        };
        responses: {
            /** @description Success */
            204: {
                headers: {
                    /**
                     * @description Server-generated request UUID
                     * @example 00000000-0000-4000-8000-000000000001
                     */
                    "X-Request-ID"?: string;
                    [name: string]: unknown;
                };
                content?: never;
            };
            400: components["responses"]["Error400"];
            401: components["responses"]["Error401"];
            403: components["responses"]["Error403"];
            413: components["responses"]["Error413"];
            429: components["responses"]["Error429"];
            500: components["responses"]["Error500"];
            503: components["responses"]["Error503"];
        };
    };
    listSessions: {
        parameters: {
            query?: {
                /** @example 20 */
                limit?: components["parameters"]["Limit"];
                /**
                 * @description Opaque integrity-protected cursor bound to caller, route, sort and filters. Invalid/tampered/mismatched cursor: 400.
                 * @example opaque-synthetic-cursor
                 */
                cursor?: components["parameters"]["Cursor"];
            };
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description Success */
            200: {
                headers: {
                    /**
                     * @description Server-generated request UUID
                     * @example 00000000-0000-4000-8000-000000000001
                     */
                    "X-Request-ID"?: string;
                    [name: string]: unknown;
                };
                content: {
                    /**
                     * @example {
                     *       "items": [
                     *         {
                     *           "id": "00000000-0000-4000-8000-000000000004",
                     *           "client_type": "mobile",
                     *           "client_name": "Android phone",
                     *           "created_at": "2026-11-06T03:00:00Z",
                     *           "last_seen_at": "2026-11-06T03:00:00Z",
                     *           "expires_at": "2026-12-06T03:00:00Z",
                     *           "is_current": true
                     *         }
                     *       ],
                     *       "next_cursor": null
                     *     }
                     */
                    "application/json": components["schemas"]["SessionList"];
                };
            };
            400: components["responses"]["Error400"];
            401: components["responses"]["Error401"];
            403: components["responses"]["Error403"];
            429: components["responses"]["Error429"];
            500: components["responses"]["Error500"];
            503: components["responses"]["Error503"];
        };
    };
    revokeSession: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                /** @example 00000000-0000-4000-8000-000000000004 */
                session_id: components["parameters"]["session_id"];
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description Success */
            204: {
                headers: {
                    /**
                     * @description Server-generated request UUID
                     * @example 00000000-0000-4000-8000-000000000001
                     */
                    "X-Request-ID"?: string;
                    [name: string]: unknown;
                };
                content?: never;
            };
            400: components["responses"]["Error400"];
            401: components["responses"]["Error401"];
            403: components["responses"]["Error403"];
            404: components["responses"]["Error404"];
            429: components["responses"]["Error429"];
            500: components["responses"]["Error500"];
            503: components["responses"]["Error503"];
        };
    };
    listDevices: {
        parameters: {
            query?: {
                /** @example 20 */
                limit?: components["parameters"]["Limit"];
                /**
                 * @description Opaque integrity-protected cursor bound to caller, route, sort and filters. Invalid/tampered/mismatched cursor: 400.
                 * @example opaque-synthetic-cursor
                 */
                cursor?: components["parameters"]["Cursor"];
                /** @description Optional server status filter, not physical health. */
                status?: "ACTIVE" | "DISABLED" | "RETIRED";
            };
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description Success */
            200: {
                headers: {
                    /**
                     * @description Server-generated request UUID
                     * @example 00000000-0000-4000-8000-000000000001
                     */
                    "X-Request-ID"?: string;
                    [name: string]: unknown;
                };
                content: {
                    /**
                     * @example {
                     *       "items": [
                     *         {
                     *           "id": "00000000-0000-4000-8000-000000000002",
                     *           "binding_id": "00000000-0000-4000-8000-000000000003",
                     *           "serial_number": "RG-000001",
                     *           "nickname": "Gương xe của Việt",
                     *           "model": "RideGuard prototype",
                     *           "hardware_revision": null,
                     *           "status": "ACTIVE",
                     *           "latest_report": null
                     *         }
                     *       ],
                     *       "next_cursor": null
                     *     }
                     */
                    "application/json": components["schemas"]["DeviceList"];
                };
            };
            400: components["responses"]["Error400"];
            401: components["responses"]["Error401"];
            403: components["responses"]["Error403"];
            429: components["responses"]["Error429"];
            500: components["responses"]["Error500"];
            503: components["responses"]["Error503"];
        };
    };
    getDevice: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                /** @example 00000000-0000-4000-8000-000000000002 */
                device_id: components["parameters"]["device_id"];
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description Success */
            200: {
                headers: {
                    /**
                     * @description Server-generated request UUID
                     * @example 00000000-0000-4000-8000-000000000001
                     */
                    "X-Request-ID"?: string;
                    [name: string]: unknown;
                };
                content: {
                    /**
                     * @example {
                     *       "id": "00000000-0000-4000-8000-000000000002",
                     *       "binding_id": "00000000-0000-4000-8000-000000000003",
                     *       "serial_number": "RG-000001",
                     *       "nickname": "Gương xe của Việt",
                     *       "model": "RideGuard prototype",
                     *       "hardware_revision": null,
                     *       "status": "ACTIVE",
                     *       "latest_report": null
                     *     }
                     */
                    "application/json": components["schemas"]["Device"];
                };
            };
            400: components["responses"]["Error400"];
            401: components["responses"]["Error401"];
            403: components["responses"]["Error403"];
            404: components["responses"]["Error404"];
            429: components["responses"]["Error429"];
            500: components["responses"]["Error500"];
            503: components["responses"]["Error503"];
        };
    };
    renameDevice: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                /** @example 00000000-0000-4000-8000-000000000002 */
                device_id: components["parameters"]["device_id"];
            };
            cookie?: never;
        };
        /** @description JSON UTF-8, max 32 KiB decoded body. Normalize documented strings then validate; passwords are not normalized. Unknown properties rejected. */
        requestBody: {
            content: {
                /**
                 * @example {
                 *       "nickname": "Gương xe của Việt"
                 *     }
                 */
                "application/json": components["schemas"]["RenameDeviceRequest"];
            };
        };
        responses: {
            /** @description Success */
            200: {
                headers: {
                    /**
                     * @description Server-generated request UUID
                     * @example 00000000-0000-4000-8000-000000000001
                     */
                    "X-Request-ID"?: string;
                    [name: string]: unknown;
                };
                content: {
                    /**
                     * @example {
                     *       "id": "00000000-0000-4000-8000-000000000002",
                     *       "binding_id": "00000000-0000-4000-8000-000000000003",
                     *       "serial_number": "RG-000001",
                     *       "nickname": "Gương xe của Việt",
                     *       "model": "RideGuard prototype",
                     *       "hardware_revision": null,
                     *       "status": "ACTIVE",
                     *       "latest_report": null
                     *     }
                     */
                    "application/json": components["schemas"]["Device"];
                };
            };
            400: components["responses"]["Error400"];
            401: components["responses"]["Error401"];
            403: components["responses"]["Error403"];
            404: components["responses"]["Error404"];
            413: components["responses"]["Error413"];
            429: components["responses"]["Error429"];
            500: components["responses"]["Error500"];
            503: components["responses"]["Error503"];
        };
    };
    bindDevice: {
        parameters: {
            query?: never;
            header: {
                /**
                 * @description UUID scoped to authenticated user+method+route+key. Retain 7 days after success. Same validated/normalized payload: replay after fresh authorization/state check, no duplicate. Different payload: 409 IDEMPOTENCY_CONFLICT. Closed original binding: 409 BINDING_NO_LONGER_ACTIVE. Full response cache expires after 7 days; retain minimal key/hash/resource tombstone for the resource lifetime. After cache expiry return 409 IDEMPOTENCY_KEY_EXPIRED, with BINDING_NO_LONGER_ACTIVE taking precedence for closed binding. Never silently treat an expired key as a new operation. Same-key success replay is 200; permission/state is checked first. Canonical request hashing includes defaults for omitted optional fields (nickname=null), sorted object keys, UTF-8 and the documented string normalization.
                 * @example 00000000-0000-4000-8000-000000000004
                 */
                "Idempotency-Key": components["parameters"]["IdempotencyKey"];
            };
            path?: never;
            cookie?: never;
        };
        /** @description JSON UTF-8, max 32 KiB decoded body. Normalize documented strings then validate; passwords are not normalized. Unknown properties rejected. */
        requestBody: {
            content: {
                /**
                 * @example {
                 *       "serial_number": "RG-000001",
                 *       "nickname": "Gương xe của Việt"
                 *     }
                 */
                "application/json": components["schemas"]["BindRequest"];
            };
        };
        responses: {
            /** @description Existing own active binding or valid idempotent replay; never changes nickname. */
            200: {
                headers: {
                    /**
                     * @description Server-generated request UUID
                     * @example 00000000-0000-4000-8000-000000000001
                     */
                    "X-Request-ID"?: string;
                    [name: string]: unknown;
                };
                content: {
                    /**
                     * @example {
                     *       "id": "00000000-0000-4000-8000-000000000002",
                     *       "binding_id": "00000000-0000-4000-8000-000000000003",
                     *       "serial_number": "RG-000001",
                     *       "nickname": "Gương xe của Việt",
                     *       "model": "RideGuard prototype",
                     *       "hardware_revision": null,
                     *       "status": "ACTIVE",
                     *       "latest_report": null
                     *     }
                     */
                    "application/json": components["schemas"]["Device"];
                };
            };
            /** @description Success */
            201: {
                headers: {
                    /**
                     * @description Server-generated request UUID
                     * @example 00000000-0000-4000-8000-000000000001
                     */
                    "X-Request-ID"?: string;
                    [name: string]: unknown;
                };
                content: {
                    /**
                     * @example {
                     *       "id": "00000000-0000-4000-8000-000000000002",
                     *       "binding_id": "00000000-0000-4000-8000-000000000003",
                     *       "serial_number": "RG-000001",
                     *       "nickname": "Gương xe của Việt",
                     *       "model": "RideGuard prototype",
                     *       "hardware_revision": null,
                     *       "status": "ACTIVE",
                     *       "latest_report": null
                     *     }
                     */
                    "application/json": components["schemas"]["Device"];
                };
            };
            400: components["responses"]["Error400"];
            401: components["responses"]["Error401"];
            403: components["responses"]["Error403"];
            /** @description Absent or inaccessible private resource; never reveal another owner. */
            404: {
                headers: {
                    /**
                     * @description Server-generated request UUID
                     * @example 00000000-0000-4000-8000-000000000001
                     */
                    "X-Request-ID"?: string;
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["Error"];
                };
            };
            /** @description State, version or idempotency conflict. */
            409: {
                headers: {
                    /**
                     * @description Server-generated request UUID
                     * @example 00000000-0000-4000-8000-000000000001
                     */
                    "X-Request-ID"?: string;
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["Error"];
                };
            };
            413: components["responses"]["Error413"];
            429: components["responses"]["Error429"];
            500: components["responses"]["Error500"];
            503: components["responses"]["Error503"];
        };
    };
    unbindDevice: {
        parameters: {
            query: {
                /**
                 * @description Chỉ đóng đúng binding này, đúng device và caller. Binding đã đóng vẫn trả 204; không ảnh hưởng binding mới.
                 * @example 00000000-0000-4000-8000-000000000003
                 */
                expected_binding_id: string;
            };
            header?: never;
            path: {
                /** @example 00000000-0000-4000-8000-000000000002 */
                device_id: components["parameters"]["device_id"];
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description Success */
            204: {
                headers: {
                    /**
                     * @description Server-generated request UUID
                     * @example 00000000-0000-4000-8000-000000000001
                     */
                    "X-Request-ID"?: string;
                    [name: string]: unknown;
                };
                content?: never;
            };
            400: components["responses"]["Error400"];
            401: components["responses"]["Error401"];
            403: components["responses"]["Error403"];
            404: components["responses"]["Error404"];
            429: components["responses"]["Error429"];
            500: components["responses"]["Error500"];
            503: components["responses"]["Error503"];
        };
    };
    listBindings: {
        parameters: {
            query?: {
                /** @example 20 */
                limit?: components["parameters"]["Limit"];
                /**
                 * @description Opaque integrity-protected cursor bound to caller, route, sort and filters. Invalid/tampered/mismatched cursor: 400.
                 * @example opaque-synthetic-cursor
                 */
                cursor?: components["parameters"]["Cursor"];
                /** @description Derived from unbound_at. */
                status?: "ACTIVE" | "UNBOUND";
                /** @description Optional device filter; still scoped to caller. */
                device_id?: string;
            };
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description Success */
            200: {
                headers: {
                    /**
                     * @description Server-generated request UUID
                     * @example 00000000-0000-4000-8000-000000000001
                     */
                    "X-Request-ID"?: string;
                    [name: string]: unknown;
                };
                content: {
                    /**
                     * @example {
                     *       "items": [
                     *         {
                     *           "id": "00000000-0000-4000-8000-000000000003",
                     *           "device_id": "00000000-0000-4000-8000-000000000002",
                     *           "serial_number": "RG-000001",
                     *           "nickname": "Gương xe của Việt",
                     *           "model": "RideGuard prototype",
                     *           "hardware_revision": null,
                     *           "bound_at": "2026-11-06T03:00:00Z",
                     *           "unbound_at": null,
                     *           "status": "ACTIVE"
                     *         }
                     *       ],
                     *       "next_cursor": null
                     *     }
                     */
                    "application/json": components["schemas"]["BindingList"];
                };
            };
            400: components["responses"]["Error400"];
            401: components["responses"]["Error401"];
            403: components["responses"]["Error403"];
            429: components["responses"]["Error429"];
            500: components["responses"]["Error500"];
            503: components["responses"]["Error503"];
        };
    };
    getDeviceConfig: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                /** @example 00000000-0000-4000-8000-000000000002 */
                device_id: components["parameters"]["device_id"];
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description Success */
            200: {
                headers: {
                    /**
                     * @description Server-generated request UUID
                     * @example 00000000-0000-4000-8000-000000000001
                     */
                    "X-Request-ID"?: string;
                    [name: string]: unknown;
                };
                content: {
                    /**
                     * @example {
                     *       "binding_id": "00000000-0000-4000-8000-000000000003",
                     *       "schema_version": 1,
                     *       "desired_version": 2,
                     *       "reported_applied_version": 1,
                     *       "settings": {
                     *         "display_brightness": 70,
                     *         "vibration_enabled": true
                     *       }
                     *     }
                     */
                    "application/json": components["schemas"]["device-config.schema"];
                };
            };
            400: components["responses"]["Error400"];
            401: components["responses"]["Error401"];
            403: components["responses"]["Error403"];
            404: components["responses"]["Error404"];
            429: components["responses"]["Error429"];
            500: components["responses"]["Error500"];
            503: components["responses"]["Error503"];
        };
    };
    updateDeviceConfig: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                /** @example 00000000-0000-4000-8000-000000000002 */
                device_id: components["parameters"]["device_id"];
            };
            cookie?: never;
        };
        /** @description JSON UTF-8, max 32 KiB decoded body. Normalize documented strings then validate; passwords are not normalized. Unknown properties rejected. */
        requestBody: {
            content: {
                /**
                 * @example {
                 *       "expected_version": 2,
                 *       "settings": {
                 *         "display_brightness": 80,
                 *         "vibration_enabled": true
                 *       }
                 *     }
                 */
                "application/json": components["schemas"]["ConfigPatchRequest"];
            };
        };
        responses: {
            /** @description Success */
            200: {
                headers: {
                    /**
                     * @description Server-generated request UUID
                     * @example 00000000-0000-4000-8000-000000000001
                     */
                    "X-Request-ID"?: string;
                    [name: string]: unknown;
                };
                content: {
                    /**
                     * @example {
                     *       "binding_id": "00000000-0000-4000-8000-000000000003",
                     *       "schema_version": 1,
                     *       "desired_version": 3,
                     *       "reported_applied_version": 1,
                     *       "settings": {
                     *         "display_brightness": 80,
                     *         "vibration_enabled": true
                     *       }
                     *     }
                     */
                    "application/json": components["schemas"]["device-config.schema"];
                };
            };
            400: components["responses"]["Error400"];
            401: components["responses"]["Error401"];
            403: components["responses"]["Error403"];
            404: components["responses"]["Error404"];
            /** @description State, version or idempotency conflict. */
            409: {
                headers: {
                    /**
                     * @description Server-generated request UUID
                     * @example 00000000-0000-4000-8000-000000000001
                     */
                    "X-Request-ID"?: string;
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["Error"];
                };
            };
            413: components["responses"]["Error413"];
            429: components["responses"]["Error429"];
            500: components["responses"]["Error500"];
            503: components["responses"]["Error503"];
        };
    };
    ackDeviceConfig: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                /** @example 00000000-0000-4000-8000-000000000002 */
                device_id: components["parameters"]["device_id"];
            };
            cookie?: never;
        };
        /** @description JSON UTF-8, max 32 KiB decoded body. Normalize documented strings then validate; passwords are not normalized. Unknown properties rejected. */
        requestBody: {
            content: {
                /**
                 * @example {
                 *       "binding_id": "00000000-0000-4000-8000-000000000003",
                 *       "reported_applied_version": 2
                 *     }
                 */
                "application/json": components["schemas"]["ConfigAckRequest"];
            };
        };
        responses: {
            /** @description Success */
            200: {
                headers: {
                    /**
                     * @description Server-generated request UUID
                     * @example 00000000-0000-4000-8000-000000000001
                     */
                    "X-Request-ID"?: string;
                    [name: string]: unknown;
                };
                content: {
                    /**
                     * @example {
                     *       "binding_id": "00000000-0000-4000-8000-000000000003",
                     *       "schema_version": 1,
                     *       "desired_version": 2,
                     *       "reported_applied_version": 2,
                     *       "settings": {
                     *         "display_brightness": 70,
                     *         "vibration_enabled": true
                     *       }
                     *     }
                     */
                    "application/json": components["schemas"]["device-config.schema"];
                };
            };
            400: components["responses"]["Error400"];
            401: components["responses"]["Error401"];
            403: components["responses"]["Error403"];
            404: components["responses"]["Error404"];
            /** @description State, version or idempotency conflict. */
            409: {
                headers: {
                    /**
                     * @description Server-generated request UUID
                     * @example 00000000-0000-4000-8000-000000000001
                     */
                    "X-Request-ID"?: string;
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["Error"];
                };
            };
            413: components["responses"]["Error413"];
            429: components["responses"]["Error429"];
            500: components["responses"]["Error500"];
            503: components["responses"]["Error503"];
        };
    };
    reportDeviceStatus: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                /** @example 00000000-0000-4000-8000-000000000002 */
                device_id: components["parameters"]["device_id"];
            };
            cookie?: never;
        };
        /** @description JSON UTF-8, max 32 KiB decoded body. Normalize documented strings then validate; passwords are not normalized. Unknown properties rejected. */
        requestBody: {
            content: {
                /**
                 * @example {
                 *       "binding_id": "00000000-0000-4000-8000-000000000003",
                 *       "observed_at": "2026-11-06T03:00:00Z",
                 *       "firmware_version": "0.1.0",
                 *       "radar_status": "OK",
                 *       "imu_status": "OK",
                 *       "display_status": "OK",
                 *       "vibration_status": "UNKNOWN",
                 *       "battery_percent": null
                 *     }
                 */
                "application/json": components["schemas"]["device-report.schema"];
            };
        };
        responses: {
            /** @description Success */
            200: {
                headers: {
                    /**
                     * @description Server-generated request UUID
                     * @example 00000000-0000-4000-8000-000000000001
                     */
                    "X-Request-ID"?: string;
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["ReportReceipt"];
                };
            };
            400: components["responses"]["Error400"];
            401: components["responses"]["Error401"];
            403: components["responses"]["Error403"];
            404: components["responses"]["Error404"];
            413: components["responses"]["Error413"];
            429: components["responses"]["Error429"];
            500: components["responses"]["Error500"];
            503: components["responses"]["Error503"];
        };
    };
    listFeedback: {
        parameters: {
            query?: {
                /** @example 20 */
                limit?: components["parameters"]["Limit"];
                /**
                 * @description Opaque integrity-protected cursor bound to caller, route, sort and filters. Invalid/tampered/mismatched cursor: 400.
                 * @example opaque-synthetic-cursor
                 */
                cursor?: components["parameters"]["Cursor"];
                /** @description Optional status filter. */
                status?: "OPEN" | "IN_PROGRESS" | "RESOLVED";
                /** @description Optional category filter. */
                category?: "BUG" | "SUGGESTION" | "OTHER";
                /** @description Optional device association filter; never bypasses ownership. */
                device_id?: string;
            };
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description Success */
            200: {
                headers: {
                    /**
                     * @description Server-generated request UUID
                     * @example 00000000-0000-4000-8000-000000000001
                     */
                    "X-Request-ID"?: string;
                    [name: string]: unknown;
                };
                content: {
                    /**
                     * @example {
                     *       "items": [
                     *         {
                     *           "id": "00000000-0000-4000-8000-000000000004",
                     *           "device_id": "00000000-0000-4000-8000-000000000002",
                     *           "category": "SUGGESTION",
                     *           "content": "Đề nghị làm rõ trạng thái chờ đồng bộ.",
                     *           "status": "OPEN",
                     *           "admin_response": null,
                     *           "created_at": "2026-11-06T03:00:00Z",
                     *           "responded_at": null
                     *         }
                     *       ],
                     *       "next_cursor": null
                     *     }
                     */
                    "application/json": components["schemas"]["FeedbackList"];
                };
            };
            400: components["responses"]["Error400"];
            401: components["responses"]["Error401"];
            403: components["responses"]["Error403"];
            429: components["responses"]["Error429"];
            500: components["responses"]["Error500"];
            503: components["responses"]["Error503"];
        };
    };
    createFeedback: {
        parameters: {
            query?: never;
            header: {
                /**
                 * @description UUID scoped to authenticated user+method+route+key. Retain 7 days after success. Same validated/normalized payload: replay after fresh authorization/state check, no duplicate. Different payload: 409 IDEMPOTENCY_CONFLICT. Closed original binding: 409 BINDING_NO_LONGER_ACTIVE. Full response cache expires after 7 days; retain minimal key/hash/resource tombstone for the resource lifetime. After cache expiry return 409 IDEMPOTENCY_KEY_EXPIRED, with BINDING_NO_LONGER_ACTIVE taking precedence for closed binding. Never silently treat an expired key as a new operation. Same-key success replay is 200; permission/state is checked first. Canonical request hashing includes defaults for omitted optional fields (nickname=null), sorted object keys, UTF-8 and the documented string normalization.
                 * @example 00000000-0000-4000-8000-000000000004
                 */
                "Idempotency-Key": components["parameters"]["IdempotencyKey"];
            };
            path?: never;
            cookie?: never;
        };
        /** @description JSON UTF-8, max 32 KiB decoded body. Normalize documented strings then validate; passwords are not normalized. Unknown properties rejected. */
        requestBody: {
            content: {
                /**
                 * @example {
                 *       "device_id": "00000000-0000-4000-8000-000000000002",
                 *       "category": "SUGGESTION",
                 *       "content": "Đề nghị làm rõ trạng thái chờ đồng bộ."
                 *     }
                 */
                "application/json": components["schemas"]["CreateFeedbackRequest"];
            };
        };
        responses: {
            /** @description Idempotent replay of same own feedback after rechecking current permission to the feedback, not requiring an old binding to remain active. */
            200: {
                headers: {
                    /**
                     * @description Server-generated request UUID
                     * @example 00000000-0000-4000-8000-000000000001
                     */
                    "X-Request-ID"?: string;
                    [name: string]: unknown;
                };
                content: {
                    /**
                     * @example {
                     *       "id": "00000000-0000-4000-8000-000000000004",
                     *       "device_id": "00000000-0000-4000-8000-000000000002",
                     *       "category": "SUGGESTION",
                     *       "content": "Đề nghị làm rõ trạng thái chờ đồng bộ.",
                     *       "status": "OPEN",
                     *       "admin_response": null,
                     *       "created_at": "2026-11-06T03:00:00Z",
                     *       "responded_at": null
                     *     }
                     */
                    "application/json": components["schemas"]["Feedback"];
                };
            };
            /** @description Success */
            201: {
                headers: {
                    /**
                     * @description Server-generated request UUID
                     * @example 00000000-0000-4000-8000-000000000001
                     */
                    "X-Request-ID"?: string;
                    [name: string]: unknown;
                };
                content: {
                    /**
                     * @example {
                     *       "id": "00000000-0000-4000-8000-000000000004",
                     *       "device_id": "00000000-0000-4000-8000-000000000002",
                     *       "category": "SUGGESTION",
                     *       "content": "Đề nghị làm rõ trạng thái chờ đồng bộ.",
                     *       "status": "OPEN",
                     *       "admin_response": null,
                     *       "created_at": "2026-11-06T03:00:00Z",
                     *       "responded_at": null
                     *     }
                     */
                    "application/json": components["schemas"]["Feedback"];
                };
            };
            400: components["responses"]["Error400"];
            401: components["responses"]["Error401"];
            403: components["responses"]["Error403"];
            404: components["responses"]["Error404"];
            /** @description State, version or idempotency conflict. */
            409: {
                headers: {
                    /**
                     * @description Server-generated request UUID
                     * @example 00000000-0000-4000-8000-000000000001
                     */
                    "X-Request-ID"?: string;
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["Error"];
                };
            };
            413: components["responses"]["Error413"];
            429: components["responses"]["Error429"];
            500: components["responses"]["Error500"];
            503: components["responses"]["Error503"];
        };
    };
    getFeedback: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                /** @example 00000000-0000-4000-8000-000000000004 */
                feedback_id: components["parameters"]["feedback_id"];
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description Success */
            200: {
                headers: {
                    /**
                     * @description Server-generated request UUID
                     * @example 00000000-0000-4000-8000-000000000001
                     */
                    "X-Request-ID"?: string;
                    [name: string]: unknown;
                };
                content: {
                    /**
                     * @example {
                     *       "id": "00000000-0000-4000-8000-000000000004",
                     *       "device_id": "00000000-0000-4000-8000-000000000002",
                     *       "category": "SUGGESTION",
                     *       "content": "Đề nghị làm rõ trạng thái chờ đồng bộ.",
                     *       "status": "OPEN",
                     *       "admin_response": null,
                     *       "created_at": "2026-11-06T03:00:00Z",
                     *       "responded_at": null
                     *     }
                     */
                    "application/json": components["schemas"]["Feedback"];
                };
            };
            400: components["responses"]["Error400"];
            401: components["responses"]["Error401"];
            403: components["responses"]["Error403"];
            404: components["responses"]["Error404"];
            429: components["responses"]["Error429"];
            500: components["responses"]["Error500"];
            503: components["responses"]["Error503"];
        };
    };
    adminListUsers: {
        parameters: {
            query?: {
                /** @example 20 */
                limit?: components["parameters"]["Limit"];
                /**
                 * @description Opaque integrity-protected cursor bound to caller, route, sort and filters. Invalid/tampered/mismatched cursor: 400.
                 * @example opaque-synthetic-cursor
                 */
                cursor?: components["parameters"]["Cursor"];
                /** @description Trimmed case-insensitive name/email search. */
                q?: string;
                /** @description Account status. */
                status?: "ACTIVE" | "LOCKED";
                /** @description Role filter, not assignment. */
                role?: "USER" | "ADMIN";
            };
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description Success */
            200: {
                headers: {
                    /**
                     * @description Server-generated request UUID
                     * @example 00000000-0000-4000-8000-000000000001
                     */
                    "X-Request-ID"?: string;
                    [name: string]: unknown;
                };
                content: {
                    /**
                     * @example {
                     *       "items": [
                     *         {
                     *           "id": "00000000-0000-4000-8000-000000000001",
                     *           "email": "viet@example.invalid",
                     *           "full_name": "Việt",
                     *           "role": "USER",
                     *           "status": "ACTIVE",
                     *           "created_at": "2026-11-06T03:00:00Z",
                     *           "updated_at": "2026-11-06T03:00:00Z"
                     *         }
                     *       ],
                     *       "next_cursor": null
                     *     }
                     */
                    "application/json": components["schemas"]["UserList"];
                };
            };
            400: components["responses"]["Error400"];
            401: components["responses"]["Error401"];
            403: components["responses"]["Error403"];
            429: components["responses"]["Error429"];
            500: components["responses"]["Error500"];
            503: components["responses"]["Error503"];
        };
    };
    adminSetUserStatus: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                /** @example 00000000-0000-4000-8000-000000000001 */
                user_id: components["parameters"]["user_id"];
            };
            cookie?: never;
        };
        /** @description JSON UTF-8, max 32 KiB decoded body. Normalize documented strings then validate; passwords are not normalized. Unknown properties rejected. */
        requestBody: {
            content: {
                /**
                 * @example {
                 *       "status": "LOCKED"
                 *     }
                 */
                "application/json": components["schemas"]["SetUserStatusRequest"];
            };
        };
        responses: {
            /** @description Success */
            200: {
                headers: {
                    /**
                     * @description Server-generated request UUID
                     * @example 00000000-0000-4000-8000-000000000001
                     */
                    "X-Request-ID"?: string;
                    [name: string]: unknown;
                };
                content: {
                    /**
                     * @example {
                     *       "id": "00000000-0000-4000-8000-000000000001",
                     *       "email": "viet@example.invalid",
                     *       "full_name": "Việt",
                     *       "role": "USER",
                     *       "status": "LOCKED",
                     *       "created_at": "2026-11-06T03:00:00Z",
                     *       "updated_at": "2026-11-06T03:00:00Z"
                     *     }
                     */
                    "application/json": components["schemas"]["User"];
                };
            };
            400: components["responses"]["Error400"];
            401: components["responses"]["Error401"];
            403: components["responses"]["Error403"];
            404: components["responses"]["Error404"];
            /** @description Admin lock protection */
            409: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    /**
                     * @example {
                     *       "status": 409,
                     *       "code": "ADMIN_SELF_LOCK",
                     *       "message": "Access denied",
                     *       "request_id": "00000000-0000-4000-8000-000000000001"
                     *     }
                     */
                    "application/json": components["schemas"]["Error"];
                };
            };
            413: components["responses"]["Error413"];
            429: components["responses"]["Error429"];
            500: components["responses"]["Error500"];
            503: components["responses"]["Error503"];
        };
    };
    adminListDevices: {
        parameters: {
            query?: {
                /** @example 20 */
                limit?: components["parameters"]["Limit"];
                /**
                 * @description Opaque integrity-protected cursor bound to caller, route, sort and filters. Invalid/tampered/mismatched cursor: 400.
                 * @example opaque-synthetic-cursor
                 */
                cursor?: components["parameters"]["Cursor"];
                /** @description Canonical serial/model search. */
                q?: string;
                /** @description Server device status filter. */
                status?: "ACTIVE" | "DISABLED" | "RETIRED";
            };
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description Success */
            200: {
                headers: {
                    /**
                     * @description Server-generated request UUID
                     * @example 00000000-0000-4000-8000-000000000001
                     */
                    "X-Request-ID"?: string;
                    [name: string]: unknown;
                };
                content: {
                    /**
                     * @example {
                     *       "items": [
                     *         {
                     *           "id": "00000000-0000-4000-8000-000000000002",
                     *           "serial_number": "RG-000001",
                     *           "model": "RideGuard prototype",
                     *           "hardware_revision": null,
                     *           "status": "ACTIVE",
                     *           "created_at": "2026-11-06T03:00:00Z",
                     *           "updated_at": "2026-11-06T03:00:00Z",
                     *           "active_binding_id": "00000000-0000-4000-8000-000000000003"
                     *         }
                     *       ],
                     *       "next_cursor": null
                     *     }
                     */
                    "application/json": components["schemas"]["AdminDeviceList"];
                };
            };
            400: components["responses"]["Error400"];
            401: components["responses"]["Error401"];
            403: components["responses"]["Error403"];
            429: components["responses"]["Error429"];
            500: components["responses"]["Error500"];
            503: components["responses"]["Error503"];
        };
    };
    adminCreateDevice: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /** @description JSON UTF-8, max 32 KiB decoded body. Normalize documented strings then validate; passwords are not normalized. Unknown properties rejected. */
        requestBody: {
            content: {
                /**
                 * @example {
                 *       "serial_number": "RG-000001",
                 *       "model": "RideGuard prototype",
                 *       "hardware_revision": null
                 *     }
                 */
                "application/json": components["schemas"]["CreateDeviceRequest"];
            };
        };
        responses: {
            /** @description Success */
            201: {
                headers: {
                    /**
                     * @description Server-generated request UUID
                     * @example 00000000-0000-4000-8000-000000000001
                     */
                    "X-Request-ID"?: string;
                    [name: string]: unknown;
                };
                content: {
                    /**
                     * @example {
                     *       "id": "00000000-0000-4000-8000-000000000002",
                     *       "serial_number": "RG-000001",
                     *       "model": "RideGuard prototype",
                     *       "hardware_revision": null,
                     *       "status": "ACTIVE",
                     *       "created_at": "2026-11-06T03:00:00Z",
                     *       "updated_at": "2026-11-06T03:00:00Z",
                     *       "active_binding_id": null
                     *     }
                     */
                    "application/json": components["schemas"]["AdminDevice"];
                };
            };
            400: components["responses"]["Error400"];
            401: components["responses"]["Error401"];
            403: components["responses"]["Error403"];
            409: components["responses"]["Error409"];
            413: components["responses"]["Error413"];
            429: components["responses"]["Error429"];
            500: components["responses"]["Error500"];
            503: components["responses"]["Error503"];
        };
    };
    adminSetDeviceStatus: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                /** @example 00000000-0000-4000-8000-000000000002 */
                device_id: components["parameters"]["device_id"];
            };
            cookie?: never;
        };
        /** @description JSON UTF-8, max 32 KiB decoded body. Normalize documented strings then validate; passwords are not normalized. Unknown properties rejected. */
        requestBody: {
            content: {
                /**
                 * @example {
                 *       "status": "DISABLED"
                 *     }
                 */
                "application/json": components["schemas"]["SetDeviceStatusRequest"];
            };
        };
        responses: {
            /** @description Success */
            200: {
                headers: {
                    /**
                     * @description Server-generated request UUID
                     * @example 00000000-0000-4000-8000-000000000001
                     */
                    "X-Request-ID"?: string;
                    [name: string]: unknown;
                };
                content: {
                    /**
                     * @example {
                     *       "id": "00000000-0000-4000-8000-000000000002",
                     *       "serial_number": "RG-000001",
                     *       "model": "RideGuard prototype",
                     *       "hardware_revision": null,
                     *       "status": "DISABLED",
                     *       "created_at": "2026-11-06T03:00:00Z",
                     *       "updated_at": "2026-11-06T03:00:00Z",
                     *       "active_binding_id": "00000000-0000-4000-8000-000000000003"
                     *     }
                     */
                    "application/json": components["schemas"]["AdminDevice"];
                };
            };
            400: components["responses"]["Error400"];
            401: components["responses"]["Error401"];
            403: components["responses"]["Error403"];
            404: components["responses"]["Error404"];
            409: components["responses"]["Error409"];
            413: components["responses"]["Error413"];
            429: components["responses"]["Error429"];
            500: components["responses"]["Error500"];
            503: components["responses"]["Error503"];
        };
    };
    adminListFeedback: {
        parameters: {
            query?: {
                /** @example 20 */
                limit?: components["parameters"]["Limit"];
                /**
                 * @description Opaque integrity-protected cursor bound to caller, route, sort and filters. Invalid/tampered/mismatched cursor: 400.
                 * @example opaque-synthetic-cursor
                 */
                cursor?: components["parameters"]["Cursor"];
                /** @description Optional status filter. */
                status?: "OPEN" | "IN_PROGRESS" | "RESOLVED";
                /** @description Optional category filter. */
                category?: "BUG" | "SUGGESTION" | "OTHER";
                /** @description Optional device association filter; never bypasses ownership. */
                device_id?: string;
                /** @description Admin-only author filter; no self-service impersonation. */
                user_id?: string;
            };
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description Success */
            200: {
                headers: {
                    /**
                     * @description Server-generated request UUID
                     * @example 00000000-0000-4000-8000-000000000001
                     */
                    "X-Request-ID"?: string;
                    [name: string]: unknown;
                };
                content: {
                    /**
                     * @example {
                     *       "items": [
                     *         {
                     *           "id": "00000000-0000-4000-8000-000000000004",
                     *           "device_id": "00000000-0000-4000-8000-000000000002",
                     *           "category": "SUGGESTION",
                     *           "content": "Đề nghị làm rõ trạng thái chờ đồng bộ.",
                     *           "status": "OPEN",
                     *           "admin_response": null,
                     *           "created_at": "2026-11-06T03:00:00Z",
                     *           "responded_at": null,
                     *           "user_id": "00000000-0000-4000-8000-000000000001",
                     *           "binding_id": "00000000-0000-4000-8000-000000000003"
                     *         }
                     *       ],
                     *       "next_cursor": null
                     *     }
                     */
                    "application/json": components["schemas"]["AdminFeedbackList"];
                };
            };
            400: components["responses"]["Error400"];
            401: components["responses"]["Error401"];
            403: components["responses"]["Error403"];
            429: components["responses"]["Error429"];
            500: components["responses"]["Error500"];
            503: components["responses"]["Error503"];
        };
    };
    adminUpdateFeedback: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                /** @example 00000000-0000-4000-8000-000000000004 */
                feedback_id: components["parameters"]["feedback_id"];
            };
            cookie?: never;
        };
        /** @description JSON UTF-8, max 32 KiB decoded body. Normalize documented strings then validate; passwords are not normalized. Unknown properties rejected. */
        requestBody: {
            content: {
                /**
                 * @example {
                 *       "status": "IN_PROGRESS",
                 *       "admin_response": "Đã tiếp nhận phản hồi."
                 *     }
                 */
                "application/json": components["schemas"]["AdminFeedbackPatchRequest"];
            };
        };
        responses: {
            /** @description Success */
            200: {
                headers: {
                    /**
                     * @description Server-generated request UUID
                     * @example 00000000-0000-4000-8000-000000000001
                     */
                    "X-Request-ID"?: string;
                    [name: string]: unknown;
                };
                content: {
                    /**
                     * @example {
                     *       "id": "00000000-0000-4000-8000-000000000004",
                     *       "device_id": "00000000-0000-4000-8000-000000000002",
                     *       "category": "SUGGESTION",
                     *       "content": "Đề nghị làm rõ trạng thái chờ đồng bộ.",
                     *       "status": "IN_PROGRESS",
                     *       "admin_response": "Đã tiếp nhận phản hồi.",
                     *       "created_at": "2026-11-06T03:00:00Z",
                     *       "responded_at": "2026-11-06T03:00:00Z",
                     *       "user_id": "00000000-0000-4000-8000-000000000001",
                     *       "binding_id": "00000000-0000-4000-8000-000000000003"
                     *     }
                     */
                    "application/json": components["schemas"]["AdminFeedback"];
                };
            };
            400: components["responses"]["Error400"];
            401: components["responses"]["Error401"];
            403: components["responses"]["Error403"];
            404: components["responses"]["Error404"];
            413: components["responses"]["Error413"];
            429: components["responses"]["Error429"];
            500: components["responses"]["Error500"];
            503: components["responses"]["Error503"];
        };
    };
    health: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            200: components["responses"]["Healthy"];
            503: components["responses"]["Unhealthy"];
        };
    };
    liveness: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            200: components["responses"]["Healthy"];
            503: components["responses"]["Unhealthy"];
        };
    };
    readiness: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            200: components["responses"]["Healthy"];
            503: components["responses"]["Unhealthy"];
        };
    };
}
