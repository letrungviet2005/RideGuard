import { useState } from "react";
import { useClient } from "../../shared/api/context";
import type { DTO, Input } from "../../shared/api/client";
import { Listing } from "../../shared/ui/listing";
import { FormPanel, text } from "../../shared/ui/common";
export const serial = (s: string) =>
  s.trim().replace(/[a-z]/g, (c) => c.toUpperCase());
export function Registry() {
  const client = useClient();
  const [selected, setSelected] = useState<DTO<"AdminDevice">>(),
    [version, setVersion] = useState(0);
  return (
    <>
      <h1>Registry thiết bị</h1>
      <FormPanel
        title="Đăng ký serial mới"
        submit="Đăng ký thiết bị"
        fields={[
          {
            name: "serial_number",
            label: "Serial",
            required: true,
            maxLength: 128,
          },
          { name: "model", label: "Model", required: true, maxLength: 100 },
          {
            name: "hardware_revision",
            label: "Hardware revision (tùy chọn)",
            maxLength: 64,
          },
        ]}
        onSubmit={async (v) => {
          await client.call("adminCreateDevice", {
            body: {
              serial_number: serial(v.serial_number),
              model: v.model.trim(),
              hardware_revision: v.hardware_revision.trim() || null,
            },
          });
          setVersion((n) => n + 1);
        }}
      />
      <Listing
        title="Thiết bị đã đăng ký"
        refresh={version}
        filters={[
          { name: "q", label: "Serial / model", maxLength: 100 },
          {
            name: "status",
            label: "Trạng thái",
            type: "select",
            options: ["", "ACTIVE", "DISABLED", "RETIRED"],
          },
        ]}
        load={(query, signal) =>
          client.call("adminListDevices", {
            query: query as Input<"adminListDevices">["query"],
            signal,
          })
        }
        rowKey={(r) => r.id}
        columns={[
          { label: "Serial", cell: (r) => r.serial_number },
          { label: "Model", cell: (r) => r.model },
          { label: "Trạng thái", cell: (r) => r.status },
          { label: "Binding hiện tại", cell: (r) => text(r.active_binding_id) },
          {
            label: "Thao tác",
            cell: (r) => (
              <button onClick={() => setSelected(r)}>Chọn trạng thái</button>
            ),
          },
        ]}
      />
      {selected && (
        <FormPanel
          key={selected.id + selected.status}
          title={`Trạng thái ${selected.serial_number}`}
          submit="Lưu trạng thái"
          fields={[
            {
              name: "status",
              label: "Trạng thái mới",
              type: "select",
              options:
                selected.status === "RETIRED"
                  ? ["RETIRED"]
                  : ["ACTIVE", "DISABLED", "RETIRED"],
              value: selected.status,
              required: true,
            },
          ]}
          confirm={(v) =>
            v.status === "RETIRED"
              ? `Ngừng sử dụng vĩnh viễn ${selected.serial_number}? RETIRED không thể đảo ngược.`
              : `Đổi trạng thái ${selected.serial_number} sang ${v.status}?`
          }
          onSubmit={async (v) => {
            const updated = await client.call("adminSetDeviceStatus", {
              path: { device_id: selected.id },
              body: { status: v.status as DTO<"AdminDevice">["status"] },
            });
            setSelected(updated);
            setVersion((n) => n + 1);
          }}
        />
      )}
    </>
  );
}
