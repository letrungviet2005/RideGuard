import { lazy, Suspense, useEffect } from "react";
import {
  Navigate,
  Link,
  Route,
  Routes,
  useLocation,
  useSearchParams,
} from "react-router-dom";
import { labEnabled } from "./Shell";
import { AdminLayout } from "./AdminLayout";
import { AuthLayout } from "./AuthLayout";
import { safeReturnPath } from "./routes";
import { useClient, useSession } from "../shared/api/context";
import { Auth, PasswordRecovery } from "../features/auth/Auth";
import { Users } from "../features/admin-users/Users";
import { Registry } from "../features/admin-devices/Registry";
import { Feedback } from "../features/feedback/Feedback";
import { Profile } from "../features/profile/Profile";
import { Sessions } from "../features/sessions/Sessions";
import { Health } from "../features/health/Health";
const Lab =
  import.meta.env.DEV && import.meta.env.VITE_ENABLE_MOBILE_LAB === "true"
    ? lazy(() => import("../features/dev-api-lab/Lab"))
    : null;
function Restoring() {
  return (
    <main className="session-loading">
      <p role="status">Đang xác minh phiên đăng nhập…</p>
    </main>
  );
}
function Admin() {
  const client = useClient(),
    session = useSession(),
    location = useLocation();
  const [params] = useSearchParams();
  useEffect(() => {
    void client.restore().catch(() => undefined);
  }, [client]);
  if (session.restoration === "pending") return <Restoring />;
  const authRoute = ["/login", "/forgot-password", "/reset-password"].includes(
    location.pathname,
  );
  if (session.user && session.user.role !== "ADMIN")
    return (
      <AuthLayout>
        <p role="alert">Khu quản trị chỉ dành cho ADMIN.</p>
        <button onClick={() => void client.logout().catch(() => undefined)}>
          Đăng xuất
        </button>
      </AuthLayout>
    );
  if (!session.user) {
    if (!authRoute)
      return (
        <Navigate
          replace
          to={`/login?returnTo=${encodeURIComponent(safeReturnPath(location.pathname))}`}
        />
      );
    if (location.pathname !== "/login")
      return (
        <AuthLayout>
          <h1>
            {location.pathname === "/forgot-password"
              ? "Quên mật khẩu"
              : "Đặt lại mật khẩu"}
          </h1>
          <PasswordRecovery expanded />
          <Link to="/login">Về đăng nhập</Link>
        </AuthLayout>
      );
    return <Auth key={session.epoch} />;
  }
  if (authRoute)
    return <Navigate replace to={safeReturnPath(params.get("returnTo"))} />;
  return (
    <AdminLayout>
      <Routes>
        <Route index element={<Users />} />
        <Route path="devices" element={<Registry />} />
        <Route path="feedback" element={<Feedback admin />} />
        <Route path="profile" element={<Profile />} />
        <Route path="sessions" element={<Sessions />} />
        <Route path="health" element={<Health />} />
        <Route path="*" element={<h1>Không tìm thấy trang</h1>} />
      </Routes>
    </AdminLayout>
  );
}
export function App() {
  return (
    <Suspense fallback={<Restoring />}>
      <Routes>
        {labEnabled && Lab && <Route path="/lab/*" element={<Lab />} />}
        <Route path="/*" element={<Admin />} />
      </Routes>
    </Suspense>
  );
}
