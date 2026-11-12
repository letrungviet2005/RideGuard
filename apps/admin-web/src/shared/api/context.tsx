import { createContext, useContext, useSyncExternalStore } from "react";
import { ApiClient } from "./client";
export const adminClient = new ApiClient(
  import.meta.env.VITE_API_BASE_URL ?? "",
  "admin_web",
);
export const ClientContext = createContext(adminClient);
export const useClient = () => useContext(ClientContext);
export function useSession() {
  const client = useClient();
  return useSyncExternalStore(client.subscribe, client.getSnapshot);
}
