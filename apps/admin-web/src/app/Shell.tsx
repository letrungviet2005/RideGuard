import { NavLink, Link, useLocation } from "react-router-dom";
import type { ReactNode } from "react";
import { useClient, useSession } from "../shared/api/context";
import { useTask } from "../shared/ui/common";
export const labEnabled =
  import.meta.env.DEV && import.meta.env.VITE_ENABLE_MOBILE_LAB === "true";
export function Shell({
  lab = false,
  children,
}: {
  lab?: boolean;
  children: ReactNode;
}) {
  const client = useClient(),
    session = useSession(),
    task = useTask(),
    location = useLocation();
  const prefix = lab ? "/lab" : "";
  const menu = lab
    ? [
        ["", "Gương của tôi"],
        ["/bindings", "Lịch sử liên kết"],
        ["/feedback", "Phản hồi"],
      ]
    : [
        ["", "Người dùng"],
        ["/devices", "Registry thiết bị"],
        ["/feedback", "Phản hồi quản trị"],
      ];
  return (
    <div className={`app-shell${lab ? " lab-shell" : ""}`}>
      <a className="skip-link" href="#main-content">
        Đến nội dung chính
      </a>
      <aside className="sidebar">
        <Link to="/" className="brand">
          <span className="brand-mark" aria-hidden="true">
            R<span>↗</span>
          </span>
          RideGuard
        </Link>
        <p className="sidebar-subtitle">
          {lab ? "Development API lab" : "Không gian quản trị"}
        </p>
        <p className="nav-caption">{lab ? "THỬ NGHIỆM" : "QUẢN LÝ HỆ THỐNG"}</p>
        <nav aria-label="Điều hướng">
          {menu.map(([path, label], index) => (
            <NavLink end key={path} to={prefix + (path || "/")}>
              <NavIcon kind={index} />
              {label}
            </NavLink>
          ))}
          <NavLink to={prefix + "/profile"}>
            <NavIcon kind={3} />
            Hồ sơ / mật khẩu
          </NavLink>
          <NavLink to={prefix + "/sessions"}>
            <NavIcon kind={4} />
            Phiên đăng nhập
          </NavLink>
          <NavLink to={prefix + "/health"}>
            <NavIcon kind={5} />
            Health
          </NavLink>
        </nav>
        <hr />
        {lab ? (
          <Link to="/">Về quản trị</Link>
        ) : (
          labEnabled && <Link to="/lab/">Thử API mobile</Link>
        )}
        <div className="sidebar-note">
          <span className="note-symbol" aria-hidden="true">
            ◇
          </span>
          <p>
            Quản lý tập trung.
            <br />
            <span className="small">
              Không có kết nối BLE trong giao diện này.
            </span>
          </p>
        </div>
      </aside>
      <div className="workspace">
        <header>
          <div className="workspace-heading">
            <span className="workspace-kicker">
              {lab ? "DEVELOPMENT" : "WORKSPACE"}
            </span>
            <strong>
              {lab
                ? "Thử API mobile — không kết nối thiết bị"
                : "Quản trị RideGuard"}
            </strong>
            <div className="account-line">
              <span
                className={`account-dot${session.user ? " signed-in" : ""}`}
                aria-hidden="true"
              />
              {session.user
                ? `${session.user.email} · ${session.user.role}`
                : "Chưa đăng nhập"}
            </div>
          </div>
          {session.user && (
            <div className="actions">
              <button
                disabled={session.busy || task.busy}
                onClick={() =>
                  void task.run(() => client.refresh(), "Đã làm mới phiên.")
                }
              >
                Làm mới phiên
              </button>
              <button
                disabled={session.busy || task.busy}
                onClick={() =>
                  void task.run(() => client.logout(), "Đã đăng xuất.")
                }
              >
                Đăng xuất
              </button>
            </div>
          )}
        </header>
        <div className="global-status">{task.status}</div>
        <main
          id="main-content"
          tabIndex={-1}
          key={`${session.epoch}:${location.pathname}`}
        >
          {children}
        </main>
        <footer>
          <span>
            RideGuard <span aria-hidden="true">/</span>{" "}
            {lab ? "API Lab" : "Admin Console"}
          </span>
          <span>
            API: {import.meta.env.VITE_API_BASE_URL || "Chưa cấu hình"}
          </span>
        </footer>
      </div>
    </div>
  );
}

function NavIcon({ kind }: { kind: number }) {
  const paths = [
    "M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2M9 11a4 4 0 1 0 0-8 4 4 0 0 0 0 8m8-7a4 4 0 0 1 0 8m5 9v-2a4 4 0 0 0-3-3.87",
    "M5 3h14v14H5zM9 21h6m-3-4v4M8 7h8m-8 4h5",
    "M21 3H3v14h5l4 4 4-4h5zM7 7h10M7 11h6",
    "M20 21v-2a7 7 0 0 0-14 0v2M13 11a4 4 0 1 0 0-8 4 4 0 0 0 0 8",
    "M12 3 3 7v5c0 5 9 9 9 9s9-4 9-9V7zM8 12l3 3 5-6",
    "M2 12h5l3-8 4 16 3-8h5",
  ];
  return (
    <svg
      className="nav-icon"
      width="19"
      height="19"
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="1.6"
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
    >
      <path d={paths[kind]} />
    </svg>
  );
}
