import { test, expect } from "@playwright/test";
import fs from "node:fs/promises";
test("production omits development lab route/chunk even when the build flag is true", async ({
  page,
}) => {
  await page.route("**/api/v1/auth/refresh", (route) =>
    route.fulfill({ status: 401, json: { code: "UNAUTHORIZED" } }),
  );
  await page.goto("/");
  await expect(
    page.getByRole("heading", { name: "Đăng nhập quản trị" }),
  ).toBeVisible();
  await expect(
    page.getByRole("link", { name: "Thử API mobile", exact: true }),
  ).toHaveCount(0);
  await page.goto("/lab/");
  await expect(
    page.getByRole("heading", { name: "Đăng ký USER thử API" }),
  ).toHaveCount(0);
  await expect(
    page.getByRole("button", { name: "Gửi ACK mô phỏng" }),
  ).toHaveCount(0);
  const files = await fs.readdir("dist/assets");
  expect(files.some((f) => /^Lab-/.test(f))).toBe(false);
  const js = (
    await Promise.all(
      files
        .filter((f) => f.endsWith(".js"))
        .map((f) => fs.readFile("dist/assets/" + f, "utf8")),
    )
  ).join("");
  expect(js).not.toContain("Gửi ACK mô phỏng");
  expect(js).not.toContain("Gửi report mô phỏng");
  await page.goto("/");
  await page.screenshot({
    path: "test-results/admin-production.png",
    fullPage: true,
  });
});
