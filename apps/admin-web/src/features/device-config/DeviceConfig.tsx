import { useState } from "react";
import { useClient } from "../../shared/api/context";
import { ApiError, type DTO } from "../../shared/api/client";
import {
  Details,
  FormPanel,
  LoadStatus,
  useLoad,
} from "../../shared/ui/common";

export function DeviceConfig({ deviceId }: { deviceId: string }) {
  const client = useClient();
  const state = useLoad(
    (signal) =>
      client.call("getDeviceConfig", { path: { device_id: deviceId }, signal }),
    deviceId,
  );
  const [conflict, setConflict] = useState<DTO<"DeviceConfig">>();
  return (
    <section>
      <h2>Cấu hình gương</h2>
      <LoadStatus {...state} />
      <button
        disabled={state.busy}
        onClick={() => {
          setConflict(undefined);
          state.reload();
        }}
      >
        Tải lại cấu hình / bỏ bản nháp
      </button>
      {state.data && (
        <>
          <Details data={state.data} />
          <p className="notice">
            {state.data.reported_applied_version === state.data.desired_version
              ? "Backend đã nhận ACK cho phiên bản mong muốn; không chứng minh phần cứng đang kết nối."
              : "Chờ gương xác nhận"}
          </p>
          <FormPanel
            key={`${state.data.binding_id}:${state.data.desired_version}`}
            title="Chỉnh cấu hình"
            submit="Gửi cấu hình"
            disabled={Boolean(conflict)}
            fields={[
              {
                name: "display_brightness",
                label: "Độ sáng (0–100)",
                type: "number",
                min: 0,
                max: 100,
                required: true,
                value: String(state.data.settings.display_brightness),
              },
              {
                name: "vibration_enabled",
                label: "Bật cơ cấu rung",
                type: "checkbox",
                value: String(state.data.settings.vibration_enabled),
              },
            ]}
            onSubmit={async (v) => {
              try {
                await client.call("updateDeviceConfig", {
                  path: { device_id: deviceId },
                  body: {
                    expected_version: state.data!.desired_version,
                    settings: {
                      display_brightness: Number(v.display_brightness),
                      vibration_enabled: v.vibration_enabled === "true",
                    },
                  },
                });
                state.reload();
              } catch (e) {
                if (e instanceof ApiError && e.code === "VERSION_CONFLICT")
                  setConflict(
                    await client.call("getDeviceConfig", {
                      path: { device_id: deviceId },
                    }),
                  );
                throw e;
              }
            }}
          >
            <p>Tắt rung không tắt HUD/risk. Không tự gửi ACK sau khi lưu.</p>
          </FormPanel>
        </>
      )}
      {conflict && (
        <aside className="notice">
          <h3>VERSION_CONFLICT — dữ liệu mới trên server</h3>
          <Details data={conflict} />
          <p>
            Bản nháp phía trên được giữ để đối chiếu. Chọn “Tải lại cấu hình /
            bỏ bản nháp”, rồi chỉnh và gửi lại theo phiên bản mới. Không tự ghi
            đè.
          </p>
        </aside>
      )}
    </section>
  );
}
