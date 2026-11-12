import { useState } from "react";
import { useClient } from "../../shared/api/context";
import type { DTO } from "../../shared/api/client";
import { Listing } from "../../shared/ui/listing";
import { Confirm, text, useTask } from "../../shared/ui/common";
export function Sessions() {
  const client = useClient(),
    task = useTask();
  const [selected, setSelected] = useState<DTO<"Session">>(),
    [version, setVersion] = useState(0);
  return (
    <>
      <h1>Phiên đăng nhập</h1>
      <Listing
        title="Phiên còn hiệu lực"
        refresh={version}
        load={(_, signal) => client.call("listSessions", { query: _, signal })}
        rowKey={(r) => r.id}
        columns={[
          {
            label: "Tên / loại",
            cell: (r) => (
              <>
                {text(r.client_name)} · {r.client_type}
              </>
            ),
          },
          { label: "Hiện tại", cell: (r) => (r.is_current ? "Có" : "Không") },
          { label: "Lần dùng gần nhất", cell: (r) => r.last_seen_at },
          { label: "Hết hạn", cell: (r) => r.expires_at },
          {
            label: "Thao tác",
            cell: (r) => (
              <button disabled={task.busy} onClick={() => setSelected(r)}>
                Thu hồi
              </button>
            ),
          },
        ]}
      />
      {task.status}
      {selected && (
        <Confirm
          title="Thu hồi phiên"
          text={`Thu hồi phiên ${text(selected.client_name)}${selected.is_current ? " (phiên hiện tại, sẽ đăng xuất)" : ""}?`}
          busy={task.busy}
          cancel={() => setSelected(undefined)}
          accept={() => {
            const target = selected;
            setSelected(undefined);
            void task.run(async () => {
              await client.call("revokeSession", {
                path: { session_id: target.id },
              });
              if (target.is_current) client.clear();
              else setVersion((v) => v + 1);
            });
          }}
        />
      )}
    </>
  );
}
