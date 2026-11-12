import { test, expect } from "@playwright/test";
import { apiFixture, login, user } from "./fixture";

test("anonymous deep link redirects to login with no admin layout, then returns after ADMIN login", async ({
  page,
}) => {
  await apiFixture(page);
  await page.goto("/devices");
  await expect(page).toHaveURL(/\/login\?returnTo=%2Fdevices$/);
  await expect(
    page.getByRole("navigation", { name: "Điều hướng" }),
  ).toHaveCount(0);
  await expect(page.locator(".sidebar, .account-line")).toHaveCount(0);
  await page.getByLabel("Email", { exact: true }).fill("admin@test.invalid");
  await page
    .getByLabel("Mật khẩu", { exact: true })
    .fill("fixture-password-only");
  await page.getByRole("button", { name: "Đăng nhập", exact: true }).click();
  await expect(page).toHaveURL(/\/devices$/);
  await expect(
    page.getByRole("navigation", { name: "Điều hướng" }),
  ).toBeVisible();
  await expect(
    page.getByRole("heading", { name: "Registry thiết bị" }),
  ).toBeVisible();
});

test("cookie restoration hides all private layout until response, single flight", async ({
  page,
}) => {
  await apiFixture(page);
  let release!: () => void;
  const gate = new Promise<void>((r) => {
    release = r;
  });
  let requests = 0;
  await page.route("**/api/v1/auth/refresh", async (route) => {
    requests++;
    await gate;
    await route.fulfill({
      json: {
        access_token: "fixture-admin",
        token_type: "Bearer",
        expires_in: 900,
        user: { ...user, role: "ADMIN" },
      },
    });
  });
  await page.goto("/feedback");
  await expect(page.getByRole("status")).toHaveText(
    "Đang xác minh phiên đăng nhập…",
  );
  await expect(page.locator(".sidebar, .account-line, table")).toHaveCount(0);
  expect(requests).toBe(1);
  release();
  await expect(
    page.getByRole("heading", { name: "Quản trị phản hồi" }),
  ).toBeVisible();
  await expect(page.locator(".sidebar")).toBeVisible();
});

test("USER grant is refused and never renders admin menu", async ({ page }) => {
  await apiFixture(page);
  await page.route("**/api/v1/auth/login", (route) =>
    route.fulfill({
      json: {
        access_token: "fixture-user",
        token_type: "Bearer",
        expires_in: 900,
        user,
      },
    }),
  );
  await page.goto("/login");
  await page.getByLabel("Email", { exact: true }).fill(user.email);
  await page
    .getByLabel("Mật khẩu", { exact: true })
    .fill("fixture-password-only");
  await page.getByRole("button", { name: "Đăng nhập", exact: true }).click();
  await expect(page.getByRole("alert")).toContainText("chỉ dành cho ADMIN");
  await expect(page.locator(".sidebar")).toHaveCount(0);
  await expect(page).toHaveURL(/\/login$/);
});

test("logout and rejected refresh unmount account data and protect browser back", async ({
  page,
}) => {
  await apiFixture(page);
  await login(page);
  await expect(page.locator("table")).toContainText("Fixture User");
  await page.getByRole("button", { name: "Đăng xuất", exact: true }).click();
  await expect(page).toHaveURL(/\/login/);
  await expect(page.locator(".sidebar, table, .account-line")).toHaveCount(0);
  await login(page);
  await page.route("**/api/v1/auth/refresh", (route) =>
    route.fulfill({
      status: 401,
      json: { code: "UNAUTHORIZED", message: "Session revoked" },
    }),
  );
  await page.getByRole("button", { name: "Làm mới phiên" }).click();
  await expect(page).toHaveURL(/\/login/);
  await expect(page.locator(".sidebar, table, .account-line")).toHaveCount(0);
  await page.goto("/sessions");
  await expect(page).toHaveURL(/\/login/);
  await expect(page.locator(".sidebar")).toHaveCount(0);
});

test("temporary restoration network failure has distinct message, no redirect/refresh loop", async ({
  page,
}) => {
  let calls = 0;
  await page.route("**/api/v1/auth/refresh", (route) => {
    calls++;
    return route.abort("failed");
  });
  await page.goto("/devices");
  await expect(page).toHaveURL(/\/login/);
  await expect(page.getByRole("status")).toContainText(
    "không phải xác nhận mật khẩu sai",
  );
  await expect(page.getByRole("alert")).toContainText("NETWORK_OR_CORS");
  await expect(page.locator(".sidebar")).toHaveCount(0);
  expect(calls).toBe(1);
});

test("external return URL cannot redirect a signed in admin", async ({
  page,
}) => {
  await apiFixture(page);
  await page.goto("/login?returnTo=https%3A%2F%2Fevil.invalid");
  await page.getByLabel("Email", { exact: true }).fill("admin@test.invalid");
  await page
    .getByLabel("Mật khẩu", { exact: true })
    .fill("fixture-password-only");
  await page.getByRole("button", { name: "Đăng nhập", exact: true }).click();
  await expect(page).toHaveURL("http://127.0.0.1:5176/");
  await expect(page.locator(".sidebar")).toBeVisible();
});

test("forgot/reset deep links use auth layout only", async ({ page }) => {
  await apiFixture(page);
  for (const path of ["/forgot-password", "/reset-password"]) {
    await page.goto(path);
    await expect(page.locator(".auth-page")).toBeVisible();
    await expect(page.locator(".sidebar, .account-line")).toHaveCount(0);
    await expect(page.getByRole("heading", { level: 1 })).toBeVisible();
  }
});
