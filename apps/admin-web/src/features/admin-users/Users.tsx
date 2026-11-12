import { useState } from "react";
import { useClient } from "../../shared/api/context";
import type { DTO } from "../../shared/api/client";
import { Listing } from "../../shared/ui/listing";
import { Confirm, useTask } from "../../shared/ui/common";
export function Users() {
  const client = useClient(),
    task = useTask();
  const [selected, setSelected] = useState<DTO<"User">>(),
    [version, setVersion] = useState(0);
  return (
    <>
      <h1>Quản trị người dùng</h1>
      <Listing
        title="Người dùng"
        refresh={version}
        filters={[
          { name: "q", label: "Tìm tên / email", maxLength: 100 },
          {
            name: "status",
            label: "Trạng thái",
            type: "select",
            options: ["", "ACTIVE", "LOCKED"],
          },
          {
            name: "role",
            label: "Vai trò",
            type: "select",
            options: ["", "USER", "ADMIN"],
          },
        ]}
        load={(query, signal) =>
          client.call("adminListUsers", {
            query: query as NonNullable<
              import("../../shared/api/client").Input<"adminListUsers">["query"]
            >,
            signal,
          })
        }
        rowKey={(r) => r.id}
        columns={[
          { label: "Tên", cell: (r) => r.full_name },
          { label: "Email", cell: (r) => r.email },
          { label: "Vai trò", cell: (r) => r.role },
          { label: "Trạng thái", cell: (r) => r.status },
          {
            label: "Thao tác",
            cell: (r) => (
              <button disabled={task.busy} onClick={() => setSelected(r)}>
                {r.status === "ACTIVE" ? "Khóa" : "Mở khóa"}
              </button>
            ),
          },
        ]}
      />
      {task.status}
      {selected && (
        <Confirm
          title="Đổi trạng thái tài khoản"
          text={`${selected.status === "ACTIVE" ? "Khóa và thu hồi các phiên của" : "Mở khóa"} ${selected.email}?`}
          busy={task.busy}
          cancel={() => setSelected(undefined)}
          accept={() => {
            const target = selected;
            setSelected(undefined);
            void task.run(async () => {
              await client.call("adminSetUserStatus", {
                path: { user_id: target.id },
                body: {
                  status: target.status === "ACTIVE" ? "LOCKED" : "ACTIVE",
                },
              });
              setVersion((v) => v + 1);
            });
          }}
        />
      )}
    </>
  );
}
