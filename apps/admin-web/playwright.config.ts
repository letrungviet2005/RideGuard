import { defineConfig } from "@playwright/test";
export default defineConfig({
  testDir: "./tests/browser",
  timeout: 30000,
  workers: 1,
  reporter: [["list"], ["json", { outputFile: "test-results/report.json" }]],
  use: {
    channel: process.env.PLAYWRIGHT_CHANNEL || "chrome",
    headless: true,
    screenshot: "only-on-failure",
    trace: "off",
  },
  projects: [
    {
      name: "development",
      testMatch: ["flows.spec.ts", "auth-guard.spec.ts"],
      use: { baseURL: "http://127.0.0.1:5176" },
    },
    {
      name: "production",
      testMatch: "production.spec.ts",
      use: { baseURL: "http://127.0.0.1:5177" },
    },
  ],
  webServer: [
    {
      command: `${process.platform === "win32" ? "npm.cmd" : "npm"} run dev -- --port 5176 --strictPort`,
      url: "http://127.0.0.1:5176",
      reuseExistingServer: false,
      env: {
        VITE_API_BASE_URL: "http://127.0.0.1:8080",
        VITE_ENABLE_MOBILE_LAB: "true",
      },
    },
    {
      command: `${process.platform === "win32" ? "npm.cmd" : "npm"} run preview -- --port 5177 --strictPort`,
      url: "http://127.0.0.1:5177",
      reuseExistingServer: false,
    },
  ],
});
