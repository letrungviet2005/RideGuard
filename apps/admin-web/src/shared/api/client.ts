import type { components, operations } from "./schema";
import { endpoints } from "./endpoints";

export type DTO<K extends keyof components["schemas"]> =
  components["schemas"][K];
export type Operation = keyof operations & keyof typeof endpoints;
type JsonBody<T> = T extends { content: { "application/json": infer B } }
  ? B
  : undefined;
type Responses<K extends Operation> = operations[K]["responses"];
export type Result<K extends Operation> = JsonBody<
  Responses<K>[Extract<keyof Responses<K>, 200 | 201 | 202 | 204>]
>;
export type Input<K extends Operation> = {
  body?: operations[K] extends { requestBody: infer B } ? JsonBody<B> : never;
  path?: operations[K]["parameters"] extends { path: infer P } ? P : never;
  query?: operations[K]["parameters"] extends { query?: infer Q } ? Q : never;
  idempotencyKey?: string;
  signal?: AbortSignal;
};
export class ApiError extends Error {
  constructor(
    public status: number,
    public code: string,
    message: string,
    public requestId?: string,
  ) {
    super(message);
  }
}
export const stale = () =>
  new DOMException("Phiên hoặc yêu cầu đã thay đổi.", "AbortError");
export const isAbort = (e: unknown) =>
  e instanceof Error && e.name === "AbortError";
export function apiBase(raw: string | undefined): string {
  if (!raw)
    throw new ApiError(0, "CONFIGURATION", "Chưa cấu hình VITE_API_BASE_URL.");
  const url = new URL(raw);
  if (
    !["http:", "https:"].includes(url.protocol) ||
    url.username ||
    url.password ||
    url.search ||
    url.hash ||
    url.pathname !== "/"
  ) {
    throw new ApiError(
      0,
      "CONFIGURATION",
      "API base URL phải là origin HTTP(S), không có path, query hoặc credential.",
    );
  }
  return url.origin;
}
export type Transport = "admin_web" | "mobile";
export type Snapshot = {
  user: DTO<"User"> | null;
  epoch: number;
  busy: boolean;
  restoration: "pending" | "complete" | "unavailable";
  reason?: string;
  error?: unknown;
};
type Wire = {
  token?: string;
  auth?: Transport;
  credential?: string;
  signal?: AbortSignal;
};

export class ApiClient {
  private token?: string;
  private credential?: string;
  private epoch = 0;
  private snapshot: Snapshot = {
    user: null,
    epoch: 0,
    busy: false,
    restoration: "pending",
  };
  private restoreAttempted = false;
  private listeners = new Set<() => void>();
  private requests = new Set<AbortController>();
  private authQueue: Promise<unknown> = Promise.resolve();
  private refreshing?: Promise<void>;
  constructor(
    private readonly base: string,
    public readonly mode: Transport,
    private readonly fetcher: typeof fetch = (input, init) =>
      fetch(input, init),
  ) {
    if (mode === "mobile") this.snapshot.restoration = "complete";
  }
  restore(): Promise<void> {
    if (this.restoreAttempted || this.mode !== "admin_web")
      return this.refreshing ?? Promise.resolve();
    this.restoreAttempted = true;
    return this.refresh().catch((error) => {
      if (
        error instanceof ApiError &&
        error.status === 401 &&
        !this.snapshot.user
      )
        this.publish({ reason: undefined, error: undefined });
      throw error;
    });
  }
  getSnapshot = () => this.snapshot;
  subscribe = (listener: () => void) => {
    this.listeners.add(listener);
    return () => {
      this.listeners.delete(listener);
    };
  };
  private publish(next: Partial<Snapshot>) {
    this.snapshot = { ...this.snapshot, ...next, epoch: this.epoch };
    this.listeners.forEach((f) => f());
  }
  private invalidate(reason?: string) {
    this.epoch++;
    this.token = undefined;
    this.requests.forEach((r) => r.abort());
    this.requests.clear();
    this.publish({ user: null, reason, error: undefined });
  }
  clear(reason = "Phiên đã kết thúc. Đăng nhập lại để tiếp tục.") {
    this.invalidate(reason);
    this.credential = undefined;
    this.publish({ restoration: "complete" });
  }
  private serialize<T>(task: () => Promise<T>): Promise<T> {
    const result = this.authQueue.then(task, task);
    this.authQueue = result.catch(() => undefined);
    return result;
  }
  private async wire<K extends Operation>(
    id: K,
    input: Input<K>,
    wire: Wire = {},
  ): Promise<Result<K>> {
    const op = endpoints[id];
    let path: string = op.path;
    for (const [name, value] of Object.entries(input.path ?? {}))
      path = path.replace(`{${name}}`, encodeURIComponent(String(value)));
    if (path.includes("{"))
      throw new ApiError(0, "CLIENT_INPUT", "Thiếu ID trong đường dẫn.");
    const url = new URL(path, apiBase(this.base));
    for (const [name, value] of Object.entries(input.query ?? {}))
      if (value !== undefined && value !== "")
        url.searchParams.set(name, String(value));
    const headers = new Headers({ Accept: "application/json" });
    if (wire.token) headers.set("Authorization", `Bearer ${wire.token}`);
    if (wire.auth) {
      headers.set("X-Client-Type", wire.auth);
      if (wire.auth === "admin_web") headers.set("X-CSRF-Protection", "1");
    }
    if (input.idempotencyKey)
      headers.set("Idempotency-Key", input.idempotencyKey);
    if (input.body !== undefined)
      headers.set("Content-Type", "application/json");
    let response: Response;
    try {
      response = await this.fetcher(url, {
        method: op.method,
        headers,
        body: input.body === undefined ? undefined : JSON.stringify(input.body),
        credentials: wire.auth === "admin_web" ? "include" : "omit",
        signal: AbortSignal.any([
          AbortSignal.timeout(15000),
          ...((wire.signal ?? input.signal)
            ? [wire.signal ?? input.signal!]
            : []),
        ]),
        cache: "no-store",
        redirect: "error",
      });
    } catch (e) {
      if (isAbort(e)) throw e;
      throw new ApiError(
        0,
        "NETWORK_OR_CORS",
        "Không đọc được phản hồi. Kiểm tra backend, mạng, CORS và cookie. Với thao tác ghi, hãy đối chiếu trước khi gửi lại.",
      );
    }
    if (!response.ok) {
      const error: unknown = await response.json().catch(() => null);
      const record =
        error && typeof error === "object"
          ? (error as Record<string, unknown>)
          : {};
      throw new ApiError(
        response.status,
        typeof record.code === "string"
          ? record.code
          : `HTTP_${response.status}`,
        typeof record.message === "string"
          ? record.message
          : "Yêu cầu không thành công.",
        typeof record.request_id === "string"
          ? record.request_id
          : (response.headers.get("X-Request-ID") ?? undefined),
      );
    }
    if (response.status === 204) return undefined as Result<K>;
    try {
      return (await response.json()) as Result<K>;
    } catch {
      throw new ApiError(
        response.status,
        "INVALID_RESPONSE",
        "Phản hồi không phải JSON hợp lệ; không tự gửi lại thao tác.",
      );
    }
  }
  private accept(grant: DTO<"TokenResponse">, epoch: number) {
    // Keep latest rotated credential only for serialized logout; never expose it in UI.
    this.credential =
      "refresh_token" in grant ? grant.refresh_token : undefined;
    if (epoch !== this.epoch) throw stale();
    if (this.mode === "admin_web" && grant.user.role !== "ADMIN") {
      this.credential = undefined;
      throw new ApiError(403, "FORBIDDEN", "Khu quản trị chỉ dành cho ADMIN.");
    }
    this.token = grant.access_token;
    this.publish({
      user: grant.user,
      reason: undefined,
      error: undefined,
      restoration: "complete",
    });
  }
  async login(body: DTO<"LoginRequest">) {
    this.restoreAttempted = true;
    this.invalidate();
    const epoch = this.epoch;
    this.publish({ busy: true, restoration: "complete" });
    try {
      await this.serialize(async () => {
        if (epoch !== this.epoch) throw stale();
        this.credential = undefined;
        this.accept(
          await this.wire("login", { body }, { auth: this.mode }),
          epoch,
        );
      });
    } catch (error) {
      if (epoch === this.epoch) this.publish({ error });
      throw error;
    } finally {
      if (epoch === this.epoch) this.publish({ busy: false });
    }
  }
  refresh(): Promise<void> {
    if (this.refreshing) return this.refreshing;
    const epoch = this.epoch;
    this.publish({ busy: true });
    const task = this.serialize(async () => {
      if (epoch !== this.epoch) throw stale();
      if (this.mode === "mobile" && !this.credential)
        throw new ApiError(
          401,
          "UNAUTHORIZED",
          "Đăng nhập lại phiên thử mobile.",
        );
      const body: DTO<"RefreshRequest"> =
        this.mode === "mobile" ? { refresh_token: this.credential! } : {};
      try {
        this.accept(
          await this.wire("refresh", { body }, { auth: this.mode }),
          epoch,
        );
      } catch (e) {
        if (epoch === this.epoch) {
          const rejected =
            e instanceof ApiError && (e.status === 401 || e.status === 403);
          this.clear(
            rejected
              ? "Phiên không còn hợp lệ hoặc không đủ quyền. Vui lòng đăng nhập lại."
              : "Chưa xác minh được phiên do lỗi kết nối hoặc dịch vụ. Đây không phải xác nhận mật khẩu sai. Không tự gửi lại refresh.",
          );
          this.publish({
            error: e,
            restoration: rejected ? "complete" : "unavailable",
          });
        }
        throw e;
      }
    });
    this.refreshing = task;
    void task
      .finally(() => {
        if (this.refreshing === task) this.refreshing = undefined;
        // clear() may have advanced the epoch on failure; release the auth UI lock.
        if (!this.snapshot.user || epoch === this.epoch)
          this.publish({ busy: false });
      })
      .catch(() => undefined);
    return task;
  }
  async logout() {
    this.restoreAttempted = true;
    this.invalidate(); // Clear private UI immediately, including in-flight data.
    const epoch = this.epoch;
    this.publish({ busy: true, restoration: "complete" });
    try {
      await this.serialize(async () => {
        const body: DTO<"RefreshRequest"> =
          this.mode === "mobile"
            ? { refresh_token: this.credential ?? "" }
            : {};
        try {
          await this.wire("logout", { body }, { auth: this.mode });
        } finally {
          this.credential = undefined;
        }
      });
    } finally {
      if (epoch === this.epoch) this.publish({ busy: false });
    }
  }
  async call<K extends Operation>(
    id: K,
    input: Input<K> = {},
  ): Promise<Result<K>> {
    if (["login", "refresh", "logout"].includes(id))
      throw new Error("Use the transport adapter");
    const epoch = this.epoch;
    const op = endpoints[id];
    if (!op.public && !this.token)
      throw new ApiError(401, "UNAUTHORIZED", "Vui lòng đăng nhập.");
    if (
      op.path.startsWith("/api/v1/admin/") &&
      this.snapshot.user?.role !== "ADMIN"
    )
      throw new ApiError(403, "FORBIDDEN", "Cần quyền ADMIN.");
    const controller = new AbortController();
    const abort = () => controller.abort();
    input.signal?.addEventListener("abort", abort, { once: true });
    if (input.signal?.aborted) controller.abort();
    this.requests.add(controller);
    const valid = () => {
      if (epoch !== this.epoch || controller.signal.aborted) throw stale();
    };
    let usedToken = op.public ? undefined : this.token;
    try {
      valid();
      let value: Result<K>;
      try {
        value = await this.wire(id, input, {
          token: usedToken,
          signal: controller.signal,
        });
      } catch (e) {
        valid();
        if (!(e instanceof ApiError) || e.status !== 401 || op.public) throw e;
        if (usedToken === this.token) await this.refresh();
        valid();
        usedToken = this.token;
        try {
          value = await this.wire(id, input, {
            token: usedToken,
            signal: controller.signal,
          });
        } catch (retryError) {
          if (
            epoch === this.epoch &&
            retryError instanceof ApiError &&
            retryError.status === 401
          )
            this.clear();
          throw retryError;
        }
      }
      valid();
      return value;
    } finally {
      this.requests.delete(controller);
      input.signal?.removeEventListener("abort", abort);
    }
  }
}

/** One key per canonical payload until confirmed success; no automatic mutation retry. */
export class Idempotency {
  private payload?: string;
  private key?: string;
  for(body: unknown) {
    const canonical = JSON.stringify(body);
    if (canonical !== this.payload || !this.key) {
      this.payload = canonical;
      this.key = crypto.randomUUID();
    }
    return this.key;
  }
  complete() {
    this.payload = undefined;
    this.key = undefined;
  }
}
