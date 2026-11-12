import { useState } from "react";
import { useClient } from "../../shared/api/context";
import { Details, useTask } from "../../shared/ui/common";
import type { DTO } from "../../shared/api/client";
export function Health() {
  return (
    <>
      <h1>Kiểm tra health</h1>
      <p>
        Ba endpoint hạ tầng, không phải chức năng nghiệp vụ. Không gửi token hay
        cookie.
      </p>
      <p>
        Nếu khác origin, backend hiện chỉ cấu hình CORS cho /api/**. Health có
        thể bị trình duyệt chặn dù backend hoạt động; xem runbook, không dùng
        no-cors.
      </p>
      {(["health", "liveness", "readiness"] as const).map((id) => (
        <Probe key={id} id={id} />
      ))}
    </>
  );
}
function Probe({ id }: { id: "health" | "liveness" | "readiness" }) {
  const client = useClient(),
    task = useTask(),
    [data, setData] = useState<DTO<"Health">>();
  return (
    <section>
      <h2>{id}</h2>
      <button
        disabled={task.busy}
        onClick={() => {
          setData(undefined);
          void task.run(async () => setData(await client.call(id)));
        }}
      >
        {task.busy ? "Đang kiểm tra…" : `Kiểm tra ${id}`}
      </button>
      {task.status}
      {data && <Details data={data} />}
    </section>
  );
}
