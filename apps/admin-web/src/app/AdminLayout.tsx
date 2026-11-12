import type { ReactNode } from "react";
import { Shell } from "./Shell";
import { useSession } from "../shared/api/context";

export function AdminLayout({ children }: { children: ReactNode }) {
  const session = useSession();
  if (session.restoration !== "complete" || session.user?.role !== "ADMIN")
    return null;
  return <Shell>{children}</Shell>;
}
