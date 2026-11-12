import type { ReactNode } from "react";
import { Link } from "react-router-dom";
import { labEnabled } from "./Shell";

export function AuthLayout({
  children,
  lab = false,
}: {
  children: ReactNode;
  lab?: boolean;
}) {
  return (
    <div className="auth-page">
      <div className="auth-brandbar">
        <Link to="/login" className="brand">
          <span className="brand-mark" aria-hidden="true">
            R↗
          </span>
          RideGuard
        </Link>
        {lab ? (
          <Link to="/login">Về quản trị</Link>
        ) : (
          labEnabled && <Link to="/lab/">Thử API mobile</Link>
        )}
      </div>
      <main id="main-content" className="auth-main">
        <div className={lab ? "auth-layout lab-auth" : "auth-layout"}>
          <aside className="auth-intro">
            <span className="eyebrow">
              RIDEGUARD / {lab ? "API LAB" : "ADMIN CONSOLE"}
            </span>
            <h2>
              Mọi kết nối.
              <br />
              Một nơi quản lý.
            </h2>
            <p>
              Theo dõi thiết bị, quản lý tài khoản và tiếp nhận phản hồi trong
              một không gian làm việc.
            </p>
            <div className="road-art" aria-hidden="true">
              <svg viewBox="0 0 360 190" fill="none">
                <path
                  d="M-20 175C90 175 20 50 140 90s80-100 240-60"
                  stroke="currentColor"
                  strokeWidth="56"
                  opacity=".08"
                />
                <path
                  d="M-20 175C90 175 20 50 140 90s80-100 240-60"
                  stroke="currentColor"
                  strokeWidth="2"
                  strokeDasharray="7 9"
                  opacity=".5"
                />
                <circle cx="140" cy="90" r="24" fill="#dbf88a" />
                <path
                  d="m130 91 7 7 14-16"
                  stroke="#193c32"
                  strokeWidth="3"
                  strokeLinecap="round"
                  strokeLinejoin="round"
                />
              </svg>
            </div>
            <div className="intro-caption">
              <span aria-hidden="true">01 — 03</span>
              <span>Tài khoản · Thiết bị · Phản hồi</span>
            </div>
          </aside>
          <div className="auth-content">{children}</div>
        </div>
      </main>
      <footer>RideGuard · Không gian quản trị</footer>
    </div>
  );
}
