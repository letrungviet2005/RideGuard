import { Link, Outlet } from 'react-router-dom';

export function Layout() {
  return <><header><strong>RideGuard / Admin</strong><nav><Link to="/">Nền tảng</Link><Link to="/scope">Phạm vi</Link></nav></header><main><Outlet /></main><footer>Development foundation · 0.1.0</footer></>;
}
