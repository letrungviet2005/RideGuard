import { test, expect } from "@playwright/test";
import { apiFixture, attachCoverage, login, bindingId, date } from "./fixture";
test.afterEach(async ({ page }, info) => {
  await attachCoverage(page, info);
});

test("admin users, cursor filters, registry and irreversible status confirmation", async ({
  page,
}) => {
  const calls = await apiFixture(page);
  await login(page);
  await page.getByRole("button", { name: "Tiếp", exact: true }).click();
  await expect
    .poll(() =>
      calls
        .filter((c) => c.path.endsWith("/admin/users"))
        .at(-1)
        ?.query.get("cursor"),
    )
    .toBe("fixture-next-cursor");
  await page.getByLabel("Tìm tên / email").fill("User");
  await page.getByRole("button", { name: "Áp dụng bộ lọc" }).click();
  await expect
    .poll(() =>
      calls
        .filter((c) => c.path.endsWith("/admin/users"))
        .at(-1)
        ?.query.get("q"),
    )
    .toBe("User");
  expect(
    calls
      .filter((c) => c.path.endsWith("/admin/users"))
      .at(-1)
      ?.query.has("cursor"),
  ).toBe(false);
  await page.getByRole("button", { name: "Khóa", exact: true }).click();
  await page.getByRole("button", { name: "Xác nhận", exact: true }).click();
  await expect
    .poll(() =>
      calls.some(
        (c) => c.method === "PATCH" && c.path.includes("/admin/users/"),
      ),
    )
    .toBe(true);
  await page.getByRole("link", { name: "Registry thiết bị" }).click();
  await page.getByLabel("Serial", { exact: true }).fill("rg-new");
  await page.getByLabel("Model", { exact: true }).fill("Prototype");
  await page
    .getByRole("button", { name: "Đăng ký thiết bị", exact: true })
    .click();
  await expect
    .poll(() =>
      calls.some(
        (c) => c.method === "POST" && c.body.serial_number === "RG-NEW",
      ),
    )
    .toBe(true);
  await page.getByRole("button", { name: "Chọn trạng thái" }).click();
  await page.getByLabel("Trạng thái mới").selectOption("RETIRED");
  await page.getByRole("button", { name: "Lưu trạng thái" }).click();
  await expect(page.getByRole("dialog")).toContainText("không thể đảo ngược");
  await page.getByRole("button", { name: "Quay lại", exact: true }).click();
  expect(
    calls.filter(
      (c) => c.path.endsWith("/status") && c.path.includes("/admin/devices/"),
    ),
  ).toHaveLength(0);
  await page.getByRole("button", { name: "Lưu trạng thái" }).click();
  await page.getByRole("button", { name: "Xác nhận", exact: true }).click();
  await expect
    .poll(() => calls.some((c) => c.body.status === "RETIRED"))
    .toBe(true);
});

test("admin profile, password, sessions, feedback reply, refresh, health and logout", async ({
  page,
}) => {
  const calls = await apiFixture(page);
  await login(page);
  await page.getByRole("link", { name: "Hồ sơ / mật khẩu" }).click();
  await page.getByLabel("Họ tên", { exact: true }).fill("Edited");
  await page.getByRole("button", { name: "Lưu hồ sơ" }).click();
  await page.getByLabel("Mật khẩu hiện tại").fill("fixture-password-only");
  await page
    .getByLabel("Mật khẩu mới", { exact: true })
    .first()
    .fill("fixture-new-password");
  await page.getByRole("button", { name: "Đổi mật khẩu", exact: true }).click();
  await expect
    .poll(() => calls.some((c) => c.path.endsWith("/change-password")))
    .toBe(true);
  await page
    .getByRole("link", { name: "Phiên đăng nhập", exact: true })
    .click();
  await page.getByRole("button", { name: "Thu hồi", exact: true }).click();
  await page.getByRole("button", { name: "Xác nhận", exact: true }).click();
  await expect
    .poll(() =>
      calls.some((c) => c.method === "DELETE" && c.path.includes("/sessions/")),
    )
    .toBe(true);
  await page.getByRole("link", { name: "Phản hồi quản trị" }).click();
  await page.getByRole("button", { name: "Xem phản hồi" }).click();
  await page.getByLabel("Trả lời (bỏ trống để giữ nguyên)").fill("Reviewed");
  await page.getByRole("button", { name: "Lưu trả lời" }).click();
  await expect
    .poll(() => calls.some((c) => c.body.admin_response === "Reviewed"))
    .toBe(true);
  await page.getByRole("button", { name: "Làm mới phiên" }).click();
  await expect
    .poll(() => calls.some((c) => c.path.endsWith("/auth/refresh")))
    .toBe(true);
  await page.getByRole("link", { name: "Health", exact: true }).click();
  for (const probe of ["health", "liveness", "readiness"]) {
    await page
      .getByRole("button", { name: "Kiểm tra " + probe, exact: true })
      .click();
  }
  await expect
    .poll(() => calls.filter((c) => c.path.startsWith("/actuator/")).length)
    .toBe(3);
  await page.getByRole("button", { name: "Đăng xuất" }).click();
  await expect(
    page.getByRole("link", { name: "Người dùng", exact: true }),
  ).toHaveCount(0);
  await expect(page).toHaveURL(/\/login/);
  await expect(
    page.getByRole("heading", { name: "Đăng nhập quản trị" }),
  ).toBeVisible();
  expect(await page.evaluate(() => Object.keys(localStorage))).toEqual([]);
});

test("lab bind, rename, config CAS, explicit ACK/report, feedback and exact stale-unbind target", async ({
  page,
}) => {
  const calls = await apiFixture(page);
  await login(page, true);
  await expect(
    page.getByRole("cell", { name: "Chưa có dữ liệu", exact: true }).first(),
  ).toBeVisible();
  await page.getByLabel("Serial đã đăng ký").fill("RG-TEST");
  await page.getByRole("button", { name: "Liên kết", exact: true }).click();
  await expect(
    page.getByRole("heading", { name: "Chi tiết gương" }),
  ).toBeVisible();
  await page.getByLabel("Nickname (để trống để xóa)").fill("Mirror");
  await page.getByRole("button", { name: "Lưu nickname" }).click();
  await page.getByLabel("Độ sáng (0–100)").fill("80");
  await page.getByLabel("Bật cơ cấu rung").uncheck();
  await page.getByRole("button", { name: "Gửi cấu hình", exact: true }).click();
  await expect(
    page.getByText("Chờ gương xác nhận", { exact: true }),
  ).toBeVisible();
  expect(
    calls.filter(
      (c) => c.path.endsWith("/config/ack") || c.path.endsWith("/reports"),
    ),
  ).toHaveLength(0);
  await page
    .getByText("Thử ACK / report — dữ liệu REST mô phỏng do người thử nhập", {
      exact: true,
    })
    .click();
  await page.getByLabel("Phiên bản đã phát hành muốn ACK").fill("2");
  await page.getByRole("button", { name: "Gửi ACK mô phỏng" }).click();
  await page
    .getByLabel("Thời điểm quan sát UTC (RFC 3339, kết thúc Z)")
    .fill(date);
  await page.getByRole("button", { name: "Gửi report mô phỏng" }).click();
  await expect
    .poll(() => calls.some((c) => c.path.endsWith("/reports")))
    .toBe(true);
  expect(
    calls.find((c) => c.path.endsWith("/reports"))?.body.battery_percent,
  ).toBeNull();
  await page.getByRole("button", { name: "Hủy liên kết gương" }).click();
  await expect(page.getByRole("dialog")).toContainText(bindingId);
  await page.getByRole("button", { name: "Xác nhận", exact: true }).click();
  await expect
    .poll(() =>
      calls.some(
        (c) =>
          c.method === "DELETE" &&
          c.query.get("expected_binding_id") === bindingId,
      ),
    )
    .toBe(true);
  await page.getByRole("link", { name: "Lịch sử liên kết" }).click();
  await expect(
    page.getByRole("cell", { name: "UNBOUND", exact: true }),
  ).toBeVisible();
  await page.getByRole("link", { name: "Phản hồi", exact: true }).click();
  await page.getByLabel("Nội dung", { exact: true }).fill("New feedback");
  await page.getByRole("button", { name: "Gửi phản hồi", exact: true }).click();
  await page.getByRole("button", { name: "Xem phản hồi" }).click();
  await expect(
    page.getByRole("heading", { name: "Chi tiết và trả lời ADMIN" }),
  ).toBeVisible();
  expect(
    calls.find((c) => c.path.endsWith("/config") && c.method === "PATCH")?.body
      .expected_version,
  ).toBe(1);
});

test("config conflict preserves draft and loads latest for explicit comparison, never auto PATCH/ACK", async ({
  page,
}) => {
  const calls = await apiFixture(page, true);
  await login(page, true);
  await page
    .getByRole("button", { name: "Chi tiết gương", exact: true })
    .click();
  await page.getByLabel("Độ sáng (0–100)").fill("40");
  await page.getByRole("button", { name: "Gửi cấu hình", exact: true }).click();
  await expect(
    page.getByRole("heading", {
      name: "VERSION_CONFLICT — dữ liệu mới trên server",
    }),
  ).toBeVisible();
  await expect(page.getByLabel("Độ sáng (0–100)")).toHaveValue("40");
  await expect(
    page.getByRole("button", { name: "Gửi cấu hình", exact: true }),
  ).toBeDisabled();
  expect(
    calls.filter((c) => c.method === "PATCH" && c.path.endsWith("/config")),
  ).toHaveLength(1);
  expect(calls.filter((c) => c.path.endsWith("/config/ack"))).toHaveLength(0);
  await page
    .getByRole("button", { name: "Tải lại cấu hình / bỏ bản nháp" })
    .click();
  await expect(page.getByLabel("Độ sáng (0–100)")).toHaveValue("90");
});

test("development registration/recovery use opaque reset token, preserve admin/mobile isolation", async ({
  page,
}) => {
  const calls = await apiFixture(page);
  await login(page);
  await page.getByRole("link", { name: "Thử API mobile", exact: true }).click();
  await page
    .getByLabel("Email đăng ký", { exact: true })
    .fill("new@test.invalid");
  await page.getByLabel("Họ tên", { exact: true }).fill("New User");
  await page
    .getByLabel("Mật khẩu đăng ký", { exact: true })
    .fill("fixture-password-only");
  await page.getByRole("button", { name: "Đăng ký", exact: true }).click();
  await page
    .getByText("Quên mật khẩu / đặt lại mật khẩu", { exact: true })
    .click();
  await page.getByLabel("Email khôi phục").fill("new@test.invalid");
  await page.getByRole("button", { name: "Gửi yêu cầu", exact: true }).click();
  await page
    .getByLabel("Mã reset đầy đủ từ email")
    .fill("fixture-opaque-token-not-an-otp");
  await page
    .getByLabel("Mật khẩu mới", { exact: true })
    .fill("fixture-new-password");
  await page.getByRole("button", { name: "Đặt lại", exact: true }).click();
  await expect
    .poll(() =>
      calls.some(
        (c) =>
          c.path.endsWith("/reset-password") &&
          c.body.reset_token === "fixture-opaque-token-not-an-otp",
      ),
    )
    .toBe(true);
  await page.getByRole("link", { name: "Về quản trị", exact: true }).click();
  await expect(
    page.getByRole("heading", { name: "Quản trị người dùng", exact: true }),
  ).toBeVisible();
  expect(
    calls
      .filter((c) => c.path.endsWith("/admin/users"))
      .every((c) => c.headers.authorization === "Bearer fixture-admin"),
  ).toBe(true);
  expect(calls.filter((c) => c.path.endsWith("/auth/register"))).toHaveLength(
    1,
  );
});

test("mobile layout is usable, private data vanishes on logout, no storage tokens", async ({
  page,
}) => {
  await page.setViewportSize({ width: 390, height: 844 });
  await apiFixture(page);
  await login(page, true);
  await expect(
    page.getByRole("heading", { name: "Gương của tài khoản thử", exact: true }),
  ).toBeVisible();
  await page.screenshot({
    path: "test-results/lab-mobile.png",
    fullPage: true,
  });
  await page.getByRole("button", { name: "Đăng xuất", exact: true }).click();
  await expect(
    page.getByRole("heading", { name: "Đăng nhập phiên thử", exact: true }),
  ).toBeVisible();
  await expect(page.getByText("RG-TEST", { exact: true })).toHaveCount(0);
  expect(
    await page.evaluate(() => [localStorage.length, sessionStorage.length]),
  ).toEqual([0, 0]);
});

test("network failure has clear error and no fake data", async ({ page }) => {
  await page.route("http://127.0.0.1:8080/**", (route) =>
    route.abort("failed"),
  );
  await page.goto("/");
  await page.getByLabel("Email", { exact: true }).fill("admin@test.invalid");
  await page
    .getByLabel("Mật khẩu", { exact: true })
    .fill("fixture-password-only");
  await page.getByRole("button", { name: "Đăng nhập", exact: true }).click();
  await expect(page.getByRole("alert")).toContainText("NETWORK_OR_CORS");
  await expect(
    page.getByRole("heading", { name: "Quản trị người dùng", exact: true }),
  ).toHaveCount(0);
  await page.screenshot({
    path: "test-results/admin-network.png",
    fullPage: true,
  });
});
