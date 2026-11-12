import { useRef, useState } from "react";
import { useClient } from "../../shared/api/context";
import {
  Idempotency,
  type DTO,
  type Input,
  type Result,
} from "../../shared/api/client";
import { Listing } from "../../shared/ui/listing";
import {
  Confirm,
  Details,
  FormPanel,
  LoadStatus,
  showBattery,
  text,
  useLoad,
  useTask,
} from "../../shared/ui/common";
import { DeviceConfig } from "../device-config/DeviceConfig";
import { serial } from "../admin-devices/Registry";

export function Devices() {
  const client = useClient(),
    idem = useRef(new Idempotency());
  const [selected, setSelected] = useState<string>(),
    [version, setVersion] = useState(0);
  return (
    <>
      <h1>Gương của tài khoản thử</h1>
      <FormPanel
        title="Liên kết bằng serial"
        submit="Liên kết"
        fields={[
          {
            name: "serial_number",
            label: "Serial đã đăng ký",
            required: true,
            maxLength: 128,
          },
          { name: "nickname", label: "Tên gợi nhớ (tùy chọn)", maxLength: 60 },
        ]}
        onSubmit={async (v) => {
          const body = {
            serial_number: serial(v.serial_number),
            nickname: v.nickname.trim() || null,
          };
          const result = await client.call("bindDevice", {
            body,
            idempotencyKey: idem.current.for(body),
          });
          idem.current.complete();
          setSelected(result.id);
          setVersion((n) => n + 1);
        }}
      >
        <p>
          Không chứng minh quyền sở hữu BLE. Nếu mạng lỗi, đối chiếu danh sách;
          cùng payload thử lại giữ cùng key.
        </p>
      </FormPanel>
      <Listing
        title="Gương đang liên kết"
        refresh={version}
        filters={[
          {
            name: "status",
            label: "Trạng thái registry",
            type: "select",
            options: ["", "ACTIVE", "DISABLED", "RETIRED"],
          },
        ]}
        load={(query, signal) =>
          client.call("listDevices", {
            query: query as Input<"listDevices">["query"],
            signal,
          })
        }
        rowKey={(r) => r.id}
        columns={[
          { label: "Serial", cell: (r) => r.serial_number },
          { label: "Tên", cell: (r) => text(r.nickname) },
          { label: "Trạng thái registry", cell: (r) => r.status },
          {
            label: "Pin báo gần nhất",
            cell: (r) => showBattery(r.latest_report?.battery_percent),
          },
          {
            label: "Thao tác",
            cell: (r) => (
              <button onClick={() => setSelected(r.id)}>Chi tiết gương</button>
            ),
          },
        ]}
      />
      {selected && (
        <DeviceDetail
          key={selected + ":" + version}
          id={selected}
          changed={() => setVersion((n) => n + 1)}
          closed={() => {
            setSelected(undefined);
            setVersion((n) => n + 1);
          }}
        />
      )}
    </>
  );
}
function DeviceDetail({
  id,
  changed,
  closed,
}: {
  id: string;
  changed: () => void;
  closed: () => void;
}) {
  const client = useClient(),
    task = useTask();
  const state = useLoad(
    (signal) => client.call("getDevice", { path: { device_id: id }, signal }),
    id,
  );
  const [target, setTarget] = useState<DTO<"Device">>();
  return (
    <section>
      <h2>Chi tiết gương</h2>
      <LoadStatus {...state} />
      {state.data && (
        <>
          <Details data={{ ...state.data, latest_report: undefined }} />
          <p>Pin: {showBattery(state.data.latest_report?.battery_percent)}</p>
          <p>
            Report là quan sát lịch sử; không dùng để kết luận gương đang
            online.
          </p>
          {state.data.latest_report ? (
            <Details data={state.data.latest_report} />
          ) : (
            <p>Chưa có báo cáo trạng thái.</p>
          )}
          <FormPanel
            title="Đổi tên gợi nhớ"
            submit="Lưu nickname"
            fields={[
              {
                name: "nickname",
                label: "Nickname (để trống để xóa)",
                value: state.data.nickname ?? "",
                maxLength: 60,
              },
            ]}
            onSubmit={async (v) => {
              await client.call("renameDevice", {
                path: { device_id: id },
                body: { nickname: v.nickname.trim() || null },
              });
              changed();
            }}
          />
          <button
            className="danger"
            disabled={task.busy}
            onClick={() => setTarget(state.data)}
          >
            Hủy liên kết gương
          </button>
          <DeviceConfig deviceId={id} />
          <Simulation deviceId={id} bindingId={state.data.binding_id} />
        </>
      )}
      {task.status}
      {target && (
        <Confirm
          title="Hủy đúng liên kết đang hiển thị"
          text={`Hủy serial ${target.serial_number}, binding ${target.binding_id}? ID này sẽ được gửi bằng expected_binding_id, không suy đoán binding mới.`}
          busy={task.busy}
          cancel={() => setTarget(undefined)}
          accept={() => {
            const binding = target.binding_id;
            setTarget(undefined);
            void task.run(async () => {
              await client.call("unbindDevice", {
                path: { device_id: id },
                query: { expected_binding_id: binding },
              });
              closed();
            });
          }}
        />
      )}
    </section>
  );
}
function Simulation({
  deviceId,
  bindingId,
}: {
  deviceId: string;
  bindingId: string;
}) {
  const client = useClient();
  const [ack, setAck] = useState<DTO<"DeviceConfig">>(),
    [report, setReport] = useState<Result<"reportDeviceStatus">>();
  const status = ["UNKNOWN", "OK", "ERROR", "UNAVAILABLE"];
  return (
    <details>
      <summary>
        Thử ACK / report — dữ liệu REST mô phỏng do người thử nhập
      </summary>
      <p>
        Không kết nối thiết bị, không xác minh ESP32 đã áp dụng. Chỉ gửi khi bạn
        bấm nút.
      </p>
      <FormPanel
        title="Gửi ACK thủ công"
        submit="Gửi ACK mô phỏng"
        fields={[
          {
            name: "version",
            label: "Phiên bản đã phát hành muốn ACK",
            type: "number",
            min: 1,
            max: Number.MAX_SAFE_INTEGER,
            required: true,
          },
        ]}
        onSubmit={async (v) =>
          setAck(
            await client.call("ackDeviceConfig", {
              path: { device_id: deviceId },
              body: {
                binding_id: bindingId,
                reported_applied_version: Number(v.version),
              },
            }),
          )
        }
      />
      {ack && <Details data={ack} />}
      <FormPanel
        title="Gửi report thủ công"
        submit="Gửi report mô phỏng"
        fields={[
          {
            name: "observed_at",
            label: "Thời điểm quan sát UTC (RFC 3339, kết thúc Z)",
            required: true,
            hint: "Ví dụ định dạng: YYYY-MM-DDTHH:mm:ssZ; nhập thời điểm thật của lần thử.",
          },
          {
            name: "firmware_version",
            label: "Firmware (bỏ trống = null)",
            maxLength: 64,
          },
          ...[
            "radar_status",
            "imu_status",
            "display_status",
            "vibration_status",
          ].map((name) => ({
            name,
            label: name,
            type: "select" as const,
            options: status,
            value: "UNKNOWN",
            required: true,
          })),
          {
            name: "battery_percent",
            label: "Pin % (bỏ trống = chưa có dữ liệu)",
            type: "number",
            min: 0,
            max: 100,
          },
        ]}
        onSubmit={async (v) =>
          setReport(
            await client.call("reportDeviceStatus", {
              path: { device_id: deviceId },
              body: {
                binding_id: bindingId,
                observed_at: v.observed_at,
                firmware_version: v.firmware_version.trim() || null,
                radar_status:
                  v.radar_status as DTO<"DeviceReportRequest">["radar_status"],
                imu_status:
                  v.imu_status as DTO<"DeviceReportRequest">["imu_status"],
                display_status:
                  v.display_status as DTO<"DeviceReportRequest">["display_status"],
                vibration_status:
                  v.vibration_status as DTO<"DeviceReportRequest">["vibration_status"],
                battery_percent:
                  v.battery_percent === "" ? null : Number(v.battery_percent),
              },
            }),
          )
        }
      />
      {report && <Details data={report} />}
    </details>
  );
}
export function Bindings() {
  const client = useClient();
  return (
    <>
      <h1>Lịch sử liên kết của tài khoản thử</h1>
      <Listing
        title="Các binding"
        filters={[
          {
            name: "status",
            label: "Trạng thái",
            type: "select",
            options: ["", "ACTIVE", "UNBOUND"],
          },
          { name: "device_id", label: "ID thiết bị" },
        ]}
        load={(query, signal) =>
          client.call("listBindings", {
            query: query as Input<"listBindings">["query"],
            signal,
          })
        }
        rowKey={(r) => r.id}
        columns={[
          { label: "Serial", cell: (r) => r.serial_number },
          { label: "Binding ID", cell: (r) => r.id },
          { label: "Trạng thái", cell: (r) => r.status },
          { label: "Liên kết lúc", cell: (r) => r.bound_at },
          { label: "Hủy lúc", cell: (r) => text(r.unbound_at) },
        ]}
      />
    </>
  );
}
