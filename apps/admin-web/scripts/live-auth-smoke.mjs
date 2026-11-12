import { chromium, expect } from "@playwright/test";

// No fixtures, response logging, traces, storageState or credential files.
const base = process.env.LIVE_FE_URL || "http://127.0.0.1:5173";
if (base !== "http://127.0.0.1:5173")
  throw new Error("Live smoke is limited to local FE port 5173.");
const email = process.env.DEMO_ADMIN_EMAIL;
const password = process.env.DEMO_ADMIN_PASSWORD;
const anonymousOnly = process.argv.includes("--anonymous");
if (!anonymousOnly && (!email || !password)) {
  console.error(
    "NOT RUN: supply DEMO_ADMIN_EMAIL and DEMO_ADMIN_PASSWORD through private environment.",
  );
  process.exit(2);
}
const browser = await chromium.launch({
  channel: process.env.PLAYWRIGHT_CHANNEL || "chrome",
});
let stage = "anonymous deep link";
try {
  const context = await browser.newContext();
  const page = await context.newPage();
  const initialRefresh = page.waitForResponse(
    (r) =>
      r.url() === "http://127.0.0.1:8080/api/v1/auth/refresh" &&
      r.request().method() === "POST",
  );
  await page.goto(base + "/devices");
  if ((await initialRefresh).status() !== 401)
    throw new Error("Expected anonymous refresh rejection");
  await expect(page).toHaveURL(/\/login\?returnTo=%2Fdevices$/);
  await expect(page.locator(".sidebar")).toHaveCount(0);
  if (anonymousOnly) {
    await expect(
      page.getByRole("heading", { name: "Đăng nhập quản trị" }),
    ).toBeVisible();
    console.log(
      "PASS real Chrome + local BE: anonymous protected route; no ADMIN login/cookie rotation tested.",
    );
    await context.close();
  } else {
    stage = "ADMIN login";
    await page.getByLabel("Email", { exact: true }).fill(email);
    await page.getByLabel("Mật khẩu", { exact: true }).fill(password);
    await page.getByRole("button", { name: "Đăng nhập", exact: true }).click();
    await expect(page.locator(".sidebar")).toBeVisible();
    await expect(page).toHaveURL(base + "/devices");
    stage = "HttpOnly cookie";
    const cookie = (
      await context.cookies("http://127.0.0.1:8080/api/v1/auth/refresh")
    ).find((c) => c.name === "rg_refresh");
    if (!cookie?.httpOnly) throw new Error("Missing HttpOnly cookie");
    stage = "reload and refresh rotation";
    const refreshed = page.waitForResponse(
      (r) =>
        r.url().endsWith("/api/v1/auth/refresh") &&
        r.request().method() === "POST",
    );
    await page.reload();
    if ((await refreshed).status() !== 200) throw new Error("Refresh rejected");
    await expect(page.locator(".sidebar")).toBeVisible();
    const rotated = (
      await context.cookies("http://127.0.0.1:8080/api/v1/auth/refresh")
    ).find((c) => c.name === "rg_refresh");
    if (!rotated?.httpOnly || rotated.value === cookie.value)
      throw new Error("Cookie did not rotate");
    stage = "logout";
    const loggedOut = page.waitForResponse(
      (r) =>
        r.url().endsWith("/api/v1/auth/logout") &&
        r.request().method() === "POST",
    );
    await page.getByRole("button", { name: "Đăng xuất", exact: true }).click();
    if ((await loggedOut).status() !== 204) throw new Error("Logout rejected");
    await expect(page.locator(".sidebar, table, .account-line")).toHaveCount(0);
    if (
      (await context.cookies("http://127.0.0.1:8080/api/v1/auth/refresh")).some(
        (c) => c.name === "rg_refresh",
      )
    )
      throw new Error("Cookie remains");
    await page.goto(base + "/devices");
    await expect(page).toHaveURL(/\/login/);
    const sizes = await page.evaluate(() => [
      localStorage.length,
      sessionStorage.length,
    ]);
    if (sizes.some((n) => n !== 0)) throw new Error("Unexpected stored data");
    await context.close();
    console.log(
      "PASS real Chrome + local BE: protected route, ADMIN login, HttpOnly cookie, reload/rotation, logout, empty storage.",
    );
  }
} catch {
  console.error(
    `FAIL real browser at: ${stage}. No tokens, passwords or response bodies logged.`,
  );
  process.exitCode = 1;
} finally {
  await browser.close();
}
