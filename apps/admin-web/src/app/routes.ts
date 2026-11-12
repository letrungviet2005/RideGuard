const adminPaths = new Set([
  "/",
  "/devices",
  "/feedback",
  "/profile",
  "/sessions",
  "/health",
]);
/** Only known internal admin destinations; no origin, query credentials or encoded redirects. */
export function safeReturnPath(value: string | null | undefined): string {
  return value && adminPaths.has(value) ? value : "/";
}
