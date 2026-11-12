import { expect, type Page, type TestInfo } from "@playwright/test";
import { endpoints } from "../../src/shared/api/endpoints";
const records = new WeakMap<Page, Call[]>();
export async function attachCoverage(page: Page, info: TestInfo) {
  const calls = records.get(page) ?? [];
  const observed = Object.entries(endpoints)
    .filter(([, op]) =>
      calls.some(
        (c) =>
          c.method === op.method &&
          new RegExp("^" + op.path.replace(/\{[^}]+\}/g, "[^/]+") + "$").test(
            c.path,
          ),
      ),
    )
    .map(([id]) => id);
  await info.attach("api-operations", {
    body: JSON.stringify({
      test: info.title,
      status: info.status,
      operations: observed,
    }),
    contentType: "application/json",
  });
}
export const deviceId = "00000000-0000-4000-8000-000000000002";
export const bindingId = "00000000-0000-4000-8000-000000000003";
export const feedbackId = "00000000-0000-4000-8000-000000000004";
export const date = "2026-11-09T00:00:00Z";
export const user = {
  id: "00000000-0000-4000-8000-000000000001",
  email: "user@test.invalid",
  full_name: "Fixture User",
  role: "USER",
  status: "ACTIVE",
  created_at: date,
  updated_at: date,
};
export const device = {
  id: deviceId,
  binding_id: bindingId,
  serial_number: "RG-TEST",
  nickname: null,
  model: "Test mirror",
  hardware_revision: null,
  status: "ACTIVE",
  latest_report: null,
};
export type Call = {
  method: string;
  path: string;
  query: URLSearchParams;
  body: Record<string, unknown>;
  headers: Record<string, string>;
};
export async function apiFixture(page: Page, conflict = false) {
  const calls: Call[] = [];
  records.set(page, calls);
  let webSession = false;
  let version = 1,
    applied: number | null = null,
    bound = true;
  let settings = { display_brightness: 70, vibration_enabled: true };
  const feedback = {
    id: feedbackId,
    device_id: deviceId,
    category: "SUGGESTION",
    content: "Fixture feedback",
    status: "OPEN",
    admin_response: null as string | null,
    created_at: date,
    responded_at: null as string | null,
  };
  await page.route("http://127.0.0.1:8080/**", async (route) => {
    const request = route.request(),
      url = new URL(request.url()),
      method = request.method(),
      path = url.pathname;
    const headers = request.headers();
    const body = request.postData()
      ? (JSON.parse(request.postData()!) as Record<string, unknown>)
      : {};
    if (method === "OPTIONS")
      return route.fulfill({
        status: 200,
        headers: {
          "Access-Control-Allow-Origin": headers.origin || "*",
          "Access-Control-Allow-Credentials": "true",
          "Access-Control-Allow-Methods": "GET,POST,PATCH,DELETE,OPTIONS",
          "Access-Control-Allow-Headers":
            "authorization,content-type,x-client-type,x-csrf-protection,idempotency-key",
        },
      });
    calls.push({ method, path, body, headers, query: url.searchParams });
    const admin = headers.authorization === "Bearer fixture-admin";
    const current = admin
      ? {
          ...user,
          email: "admin@test.invalid",
          full_name: "Fixture Admin",
          role: "ADMIN",
        }
      : user;
    let data: unknown = {},
      status = 200;
    const config = () => ({
      binding_id: bindingId,
      schema_version: 1,
      desired_version: version,
      reported_applied_version: applied,
      settings,
    });
    if (path.startsWith("/actuator/")) {
      expect(headers.authorization).toBeUndefined();
      data = { status: "UP" };
    } else if (path.endsWith("/auth/login") || path.endsWith("/auth/refresh")) {
      const web = headers["x-client-type"] === "admin_web";
      if (web && path.endsWith("/auth/refresh") && !webSession)
        return route.fulfill({
          status: 401,
          json: { status: 401, code: "UNAUTHORIZED", message: "No session" },
        });
      if (web) webSession = true;
      if (web) expect(headers["x-csrf-protection"]).toBe("1");
      data = {
        access_token: web ? "fixture-admin" : "fixture-mobile",
        token_type: "Bearer",
        expires_in: 900,
        user: web
          ? {
              ...user,
              email: "admin@test.invalid",
              full_name: "Fixture Admin",
              role: "ADMIN",
            }
          : user,
        ...(web
          ? {}
          : { refresh_token: "fixture-refresh-token-not-a-real-secret" }),
      };
    } else if (
      path.endsWith("/auth/logout") ||
      path.endsWith("/auth/reset-password") ||
      path.endsWith("/change-password")
    ) {
      if (
        path.endsWith("/auth/logout") &&
        headers["x-client-type"] === "admin_web"
      )
        webSession = false;
      status = 204;
    } else if (path.endsWith("/auth/forgot-password")) {
      status = 202;
      data = {
        message: "If the account exists, reset instructions will be sent.",
      };
    } else if (path.endsWith("/auth/register")) {
      status = 201;
      data = user;
    } else if (path === "/api/v1/users/me")
      data = { ...current, ...(method === "PATCH" ? body : {}) };
    else if (path.endsWith("/sessions"))
      data = {
        items: [
          {
            id: "00000000-0000-4000-8000-000000000010",
            client_type: "mobile",
            client_name: "Other test session",
            created_at: date,
            last_seen_at: date,
            expires_at: date,
            is_current: false,
          },
        ],
        next_cursor: null,
      };
    else if (path.includes("/sessions/") && method === "DELETE") status = 204;
    else if (path === "/api/v1/admin/users")
      data = {
        items: [user],
        next_cursor: url.searchParams.has("cursor")
          ? null
          : "fixture-next-cursor",
      };
    else if (path.startsWith("/api/v1/admin/users/") && method === "PATCH")
      data = { ...user, ...body };
    else if (path === "/api/v1/admin/devices") {
      const registry = {
        ...device,
        active_binding_id: bound ? bindingId : null,
        created_at: date,
        updated_at: date,
      };
      if (method === "POST") {
        status = 201;
        data = { ...registry, ...body };
      } else data = { items: [registry], next_cursor: null };
    } else if (path.startsWith("/api/v1/admin/devices/") && method === "PATCH")
      data = {
        ...device,
        ...body,
        active_binding_id: bindingId,
        created_at: date,
        updated_at: date,
      };
    else if (path === "/api/v1/devices/bind") {
      expect(headers["idempotency-key"]).toMatch(/^[0-9a-f-]{36}$/);
      status = 201;
      bound = true;
      data = device;
    } else if (path === "/api/v1/devices")
      data = { items: bound ? [device] : [], next_cursor: null };
    else if (path.endsWith("/binding") && method === "DELETE") {
      expect(url.searchParams.get("expected_binding_id")).toBe(bindingId);
      bound = false;
      status = 204;
    } else if (path.endsWith("/config/ack")) {
      applied = Number(body.reported_applied_version);
      data = config();
    } else if (path.endsWith("/config")) {
      if (method === "PATCH") {
        if (conflict) {
          conflict = false;
          version = 2;
          settings = { display_brightness: 90, vibration_enabled: false };
          status = 409;
          data = {
            status,
            code: "VERSION_CONFLICT",
            message: "Config changed",
            request_id: "fixture-request-id",
          };
        } else {
          version++;
          settings = body.settings as typeof settings;
          data = config();
        }
      } else data = config();
    } else if (path.endsWith("/reports"))
      data = { accepted: true, received_at: date };
    else if (path === `/api/v1/devices/${deviceId}`)
      data = { ...device, ...(method === "PATCH" ? body : {}) };
    else if (path === "/api/v1/device-bindings")
      data = {
        items: [
          {
            ...device,
            id: bindingId,
            device_id: deviceId,
            bound_at: date,
            unbound_at: bound ? null : date,
            status: bound ? "ACTIVE" : "UNBOUND",
          },
        ],
        next_cursor: null,
      };
    else if (path === "/api/v1/feedbacks") {
      if (method === "POST") {
        expect(headers["idempotency-key"]).toMatch(/^[0-9a-f-]{36}$/);
        status = 201;
        data = { ...feedback, ...body };
      } else data = { items: [feedback], next_cursor: null };
    } else if (path === `/api/v1/feedbacks/${feedbackId}`) data = feedback;
    else if (path === "/api/v1/admin/feedbacks")
      data = {
        items: [{ ...feedback, user_id: user.id, binding_id: bindingId }],
        next_cursor: null,
      };
    else if (path === `/api/v1/admin/feedbacks/${feedbackId}`) {
      Object.assign(feedback, body);
      data = { ...feedback, user_id: user.id, binding_id: bindingId };
    } else
      throw new Error("Unhandled fixture endpoint: " + method + " " + path);
    return route.fulfill({
      status,
      headers: {
        "Access-Control-Allow-Origin": headers.origin || "*",
        "Access-Control-Allow-Credentials": "true",
        "Content-Type": "application/json",
        "X-Request-ID": "fixture-request-id",
      },
      body: status === 204 ? "" : JSON.stringify(data),
    });
  });
  return calls;
}
export async function login(page: Page, lab = false) {
  await page.goto(lab ? "/lab/" : "/");
  await page
    .getByLabel("Email", { exact: true })
    .fill(lab ? "user@test.invalid" : "admin@test.invalid");
  await page
    .getByLabel("Mật khẩu", { exact: true })
    .fill("fixture-password-only");
  await page.getByRole("button", { name: "Đăng nhập", exact: true }).click();
  await expect(
    page.getByRole("heading", {
      name: lab ? "Gương của tài khoản thử" : "Quản trị người dùng",
      exact: true,
    }),
  ).toBeVisible();
}
