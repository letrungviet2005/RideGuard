import test from "node:test";
import assert from "node:assert/strict";
import {
  ApiClient,
  ApiError,
  Idempotency,
  apiBase,
  type DTO,
} from "../src/shared/api/client.ts";
import { showBattery } from "../src/shared/ui/common.tsx";

const user: DTO<"User"> = {
  id: "user-a",
  email: "fixture@example.invalid",
  full_name: "Fixture",
  role: "USER",
  status: "ACTIVE",
  created_at: "2026-11-09T00:00:00Z",
  updated_at: "2026-11-09T00:00:00Z",
};
const grant = (
  access = "test-access",
  refresh = "test-refresh",
  admin = false,
) => ({
  access_token: access,
  refresh_token: refresh,
  token_type: "Bearer",
  expires_in: 900,
  user: { ...user, role: admin ? "ADMIN" : "USER" },
});
const json = (body: unknown, status = 200) =>
  new Response(JSON.stringify(body), {
    status,
    headers: { "Content-Type": "application/json" },
  });
const unauthorized = () =>
  json(
    {
      status: 401,
      code: "UNAUTHORIZED",
      message: "Denied",
      request_id: "request-test",
    },
    401,
  );
function deferred<T>() {
  let resolve!: (value: T) => void;
  const promise = new Promise<T>((r) => {
    resolve = r;
  });
  return { promise, resolve };
}
const credentials = { email: user.email, password: "test-only-passphrase" };
const tick = () => new Promise<void>((r) => setImmediate(r));
const headers = (init?: RequestInit) => new Headers(init?.headers);

test("204 has no JSON parsing; mobile auth omits cookies", async () => {
  const c = new ApiClient("http://api.test", "mobile", async (_, init) => {
    assert.equal(init?.credentials, "omit");
    return init?.method === "DELETE"
      ? new Response(null, { status: 204 })
      : json(grant());
  });
  await c.login(credentials);
  assert.equal(
    await c.call("revokeSession", { path: { session_id: "session-a" } }),
    undefined,
  );
});
test("concurrent 401 requests refresh single-flight and retry each once", async () => {
  let refreshes = 0,
    profiles = 0;
  const barrier = deferred<Response>();
  const c = new ApiClient("http://api.test", "mobile", async (url, init) => {
    if (String(url).endsWith("/login")) return json(grant("old"));
    if (String(url).endsWith("/refresh")) {
      refreshes++;
      return barrier.promise;
    }
    profiles++;
    return headers(init).get("Authorization") === "Bearer old"
      ? unauthorized()
      : json(user);
  });
  await c.login(credentials);
  const p = Promise.all([c.call("getProfile"), c.call("getProfile")]);
  await tick();
  assert.equal(refreshes, 1);
  barrier.resolve(json(grant("new", "rotated")));
  assert.equal((await p).length, 2);
  assert.equal(profiles, 4);
});
test("late old-token 401 uses already refreshed token without rotating again", async () => {
  let count = 0,
    refreshes = 0;
  const slow = deferred<Response>();
  const c = new ApiClient("http://api.test", "mobile", async (url, init) => {
    if (String(url).endsWith("/login")) return json(grant("old"));
    if (String(url).endsWith("/refresh")) {
      refreshes++;
      return json(grant("new"));
    }
    if (headers(init).get("Authorization") === "Bearer old")
      return ++count === 1 ? slow.promise : unauthorized();
    return json(user);
  });
  await c.login(credentials);
  const first = c.call("getProfile");
  await c.call("getProfile");
  slow.resolve(unauthorized());
  await first;
  assert.equal(refreshes, 1);
});
test("refresh failure never retries unknown rotated credential, clears session", async () => {
  let refreshes = 0;
  const c = new ApiClient("http://api.test", "mobile", async (url) => {
    if (String(url).endsWith("/login")) return json(grant());
    if (String(url).endsWith("/refresh")) {
      refreshes++;
      throw new TypeError("network");
    }
    return unauthorized();
  });
  await c.login(credentials);
  await assert.rejects(
    c.call("getProfile"),
    (e: unknown) => e instanceof ApiError && e.code === "NETWORK_OR_CORS",
  );
  assert.equal(c.getSnapshot().user, null);
  await assert.rejects(c.call("getProfile"));
  assert.equal(refreshes, 1);
});
test("second 401 terminates the session rather than looping", async () => {
  let refreshes = 0,
    attempts = 0;
  const c = new ApiClient("http://api.test", "mobile", async (url) => {
    if (String(url).endsWith("/login")) return json(grant());
    if (String(url).endsWith("/refresh")) {
      refreshes++;
      return json(grant("new"));
    }
    attempts++;
    return unauthorized();
  });
  await c.login(credentials);
  await assert.rejects(c.call("getProfile"));
  assert.equal(refreshes, 1);
  assert.equal(attempts, 2);
  assert.equal(c.getSnapshot().user, null);
});
test("logout clears immediately and waits for refresh before revoking rotated credential", async () => {
  const slow = deferred<Response>();
  let logoutBody: unknown;
  const c = new ApiClient("http://api.test", "mobile", async (url, init) => {
    if (String(url).endsWith("/login")) return json(grant());
    if (String(url).endsWith("/refresh")) return slow.promise;
    logoutBody = JSON.parse(String(init?.body));
    return new Response(null, { status: 204 });
  });
  await c.login(credentials);
  const refresh = c.refresh();
  const rejected = assert.rejects(refresh, { name: "AbortError" });
  await tick();
  const logout = c.logout();
  assert.equal(c.getSnapshot().user, null);
  assert.equal(logoutBody, undefined);
  slow.resolve(json(grant("new", "rotated")));
  await rejected;
  await logout;
  assert.deepEqual(logoutBody, { refresh_token: "rotated" });
  assert.equal(c.getSnapshot().user, null);
});
test("old private response cannot populate a newly logged in account", async () => {
  const slow = deferred<Response>();
  let logins = 0;
  const c = new ApiClient("http://api.test", "mobile", async (url) => {
    if (String(url).endsWith("/login")) {
      logins++;
      return json({ ...grant(), user: { ...user, id: `user-${logins}` } });
    }
    return slow.promise;
  });
  await c.login(credentials);
  const old = c.call("getProfile");
  const rejected = assert.rejects(old, { name: "AbortError" });
  await c.login(credentials);
  slow.resolve(json(user));
  await rejected;
  assert.equal(c.getSnapshot().user?.id, "user-2");
});
test("aborted filter response is rejected even if transport ignores abort", async () => {
  const slow = deferred<Response>(),
    controller = new AbortController();
  const c = new ApiClient("http://api.test", "mobile", async (url) =>
    String(url).endsWith("/login") ? json(grant()) : slow.promise,
  );
  await c.login(credentials);
  const p = c.call("listDevices", { signal: controller.signal });
  const rejected = assert.rejects(p, { name: "AbortError" });
  controller.abort();
  slow.resolve(json({ items: [], next_cursor: null }));
  await rejected;
  assert.notEqual(c.getSnapshot().user, null);
});
test("web transport requires ADMIN and uses CSRF/cookie only for auth", async () => {
  const calls: RequestInit[] = [];
  const c = new ApiClient("http://api.test", "admin_web", async (url, init) => {
    calls.push(init!);
    if (String(url).endsWith("/login") || String(url).endsWith("/refresh")) {
      const g = grant("admin", "", true);
      const { refresh_token: unused, ...web } = g;
      void unused;
      return json(web);
    }
    return json(user);
  });
  await c.login(credentials);
  await c.refresh();
  await c.call("getProfile");
  assert.equal(calls[0].credentials, "include");
  assert.equal(headers(calls[0]).get("X-CSRF-Protection"), "1");
  assert.equal(headers(calls[0]).has("Origin"), false);
  assert.equal(calls[1].body, "{}");
  assert.equal(calls[2].credentials, "omit");
  assert.equal(headers(calls[2]).get("Authorization"), "Bearer admin");
  const denied = new ApiClient("http://api.test", "admin_web", async () =>
    json(grant()),
  );
  await assert.rejects(
    denied.login(credentials),
    (e: unknown) => e instanceof ApiError && e.status === 403,
  );
  assert.equal(denied.getSnapshot().user, null);
});
test("USER cannot call admin routes; public 401 does not refresh", async () => {
  let count = 0;
  const c = new ApiClient("http://api.test", "mobile", async (url) => {
    count++;
    return String(url).endsWith("/login") ? json(grant()) : unauthorized();
  });
  await c.login(credentials);
  await assert.rejects(
    c.call("adminListUsers"),
    (e: unknown) => e instanceof ApiError && e.status === 403,
  );
  assert.equal(count, 1);
  await assert.rejects(
    c.call("forgotPassword", { body: { email: user.email } }),
  );
  assert.equal(count, 2);
});
test("idempotency key survives manual retry and changes for payload or next completed action", () => {
  const i = new Idempotency(),
    body = { content: "A" };
  const a = i.for(body);
  assert.equal(i.for({ content: "A" }), a);
  assert.notEqual(i.for({ content: "B" }), a);
  const b = i.for(body);
  i.complete();
  assert.notEqual(i.for(body), b);
});
test("network mutation is sent only once and same key is transmitted on manual retry", async () => {
  const keys: (string | null)[] = [];
  const c = new ApiClient("http://api.test", "mobile", async (url, init) => {
    if (String(url).endsWith("/login")) return json(grant());
    keys.push(headers(init).get("Idempotency-Key"));
    throw new TypeError("offline");
  });
  await c.login(credentials);
  const i = new Idempotency(),
    body = { serial_number: "RG-1" };
  await assert.rejects(
    c.call("bindDevice", { body, idempotencyKey: i.for(body) }),
  );
  assert.equal(keys.length, 1);
  await assert.rejects(
    c.call("bindDevice", { body, idempotencyKey: i.for(body) }),
  );
  assert.equal(keys[0], keys[1]);
});
test("unbind sends exact expected_binding_id and handles backend error envelope", async () => {
  let sent = "";
  const c = new ApiClient("http://api.test", "mobile", async (url) => {
    if (String(url).endsWith("/login")) return json(grant());
    sent = String(url);
    return json(
      {
        status: 409,
        code: "VERSION_CONFLICT",
        message: "Conflict",
        request_id: "rid",
      },
      409,
    );
  });
  await c.login(credentials);
  await assert.rejects(
    c.call("unbindDevice", {
      path: { device_id: "device" },
      query: { expected_binding_id: "old-binding" },
    }),
    (e: unknown) =>
      e instanceof ApiError &&
      e.requestId === "rid" &&
      e.code === "VERSION_CONFLICT",
  );
  assert.equal(
    new URL(sent).searchParams.get("expected_binding_id"),
    "old-binding",
  );
});
test("health sends neither token nor cookies; redirect cannot forward credentials", async () => {
  const c = new ApiClient("http://api.test", "mobile", async (url, init) => {
    if (String(url).endsWith("/login")) return json(grant());
    assert.equal(headers(init).has("Authorization"), false);
    assert.equal(init?.credentials, "omit");
    assert.equal(init?.redirect, "error");
    assert.equal(new URL(String(url)).origin, "http://api.test");
    return json({ status: "UP" });
  });
  await c.login(credentials);
  await c.call("health");
});
test("base URL rejects credentials, arbitrary paths and non-HTTP protocols", () => {
  for (const url of [
    "https://user:pass@api.test",
    "https://api.test/path",
    "javascript:alert(1)",
    "https://api.test?q=1",
  ])
    assert.throws(() => apiBase(url));
  assert.equal(apiBase("https://api.test/"), "https://api.test");
});
test("null battery is unavailable, zero remains 0 percent", () => {
  assert.equal(showBattery(null), "Chưa có dữ liệu");
  assert.equal(showBattery(undefined), "Chưa có dữ liệu");
  assert.equal(showBattery(0), "0%");
});

// Return destinations are deliberately an allowlist, not arbitrary URL parsing.
test("return URLs reject external, encoded and auth-loop destinations", async () => {
  const { safeReturnPath } = await import("../src/app/routes.ts");
  assert.equal(safeReturnPath("/devices"), "/devices");
  for (const value of [
    "//evil.invalid",
    "https://evil.invalid",
    "/\\evil",
    "/%2f%2fevil",
    "/login",
    "/lab/",
    "/devices?returnTo=https://evil.invalid",
    "\n/devices",
  ])
    assert.equal(safeReturnPath(value), "/");
});

test("restoration is attempted once and distinguishes outage from rejected credential", async () => {
  for (const rejected of [true, false]) {
    let count = 0;
    const c = new ApiClient("http://api.test", "admin_web", async () => {
      count++;
      if (rejected) return unauthorized();
      throw new TypeError("offline");
    });
    await assert.rejects(c.restore());
    await c.restore();
    assert.equal(count, 1);
    assert.equal(c.getSnapshot().user, null);
    assert.equal(
      c.getSnapshot().restoration,
      rejected ? "complete" : "unavailable",
    );
  }
});
