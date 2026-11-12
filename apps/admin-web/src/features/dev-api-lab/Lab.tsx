import { Route, Routes, useLocation } from "react-router-dom";
import { ApiClient } from "../../shared/api/client";
import { ClientContext, useSession } from "../../shared/api/context";
import { Shell } from "../../app/Shell";
import { Auth } from "../auth/Auth";
import { Devices, Bindings } from "../my-devices/Devices";
import { Feedback } from "../feedback/Feedback";
import { Profile } from "../profile/Profile";
import { Sessions } from "../sessions/Sessions";
import { Health } from "../health/Health";
const mobileClient = new ApiClient(
  import.meta.env.VITE_API_BASE_URL ?? "",
  "mobile",
);
function LabContent() {
  const session = useSession(),
    location = useLocation();
  if (!session.user) return <Auth lab />;
  return (
    <Shell lab>
      {location.pathname === "/lab/health" ? (
        <Health />
      ) : !session.user ? (
        <Auth lab />
      ) : (
        <Routes>
          <Route index element={<Devices />} />
          <Route path="bindings" element={<Bindings />} />
          <Route path="feedback" element={<Feedback />} />
          <Route path="profile" element={<Profile />} />
          <Route path="sessions" element={<Sessions />} />
          <Route path="*" element={<h1>Không tìm thấy trang thử API</h1>} />
        </Routes>
      )}
    </Shell>
  );
}
export default function Lab() {
  return (
    <ClientContext.Provider value={mobileClient}>
      <LabContent />
    </ClientContext.Provider>
  );
}
