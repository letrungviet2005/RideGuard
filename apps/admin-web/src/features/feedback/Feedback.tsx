import { useRef, useState } from "react";
import { useClient } from "../../shared/api/context";
import { Idempotency, type DTO, type Input } from "../../shared/api/client";
import { Listing } from "../../shared/ui/listing";
import {
  Details,
  FormPanel,
  LoadStatus,
  text,
  useLoad,
} from "../../shared/ui/common";
export function Feedback({ admin = false }: { admin?: boolean }) {
  const client = useClient(),
    idem = useRef(new Idempotency());
  const [version, setVersion] = useState(0),
    [selected, setSelected] = useState<
      DTO<"Feedback"> | DTO<"AdminFeedback">
    >();
  return (
    <>
      <h1>{admin ? "Quản trị phản hồi" : "Phản hồi của tài khoản thử"}</h1>
      {!admin && (
        <FormPanel
          title="Tạo phản hồi"
          submit="Gửi phản hồi"
          fields={[
            {
              name: "device_id",
              label: "ID thiết bị (bỏ trống nếu phản hồi chung)",
              hint: "Chỉ chọn thiết bị đang thuộc tài khoản.",
            },
            {
              name: "category",
              label: "Loại",
              type: "select",
              options: ["BUG", "SUGGESTION", "OTHER"],
              value: "SUGGESTION",
              required: true,
            },
            {
              name: "content",
              label: "Nội dung",
              type: "textarea",
              required: true,
              maxLength: 5000,
            },
          ]}
          onSubmit={async (v) => {
            const body: DTO<"CreateFeedbackRequest"> = {
              device_id: v.device_id.trim() || null,
              category: v.category as DTO<"CreateFeedbackRequest">["category"],
              content: v.content.trim(),
            };
            await client.call("createFeedback", {
              body,
              idempotencyKey: idem.current.for(body),
            });
            idem.current.complete();
            setVersion((n) => n + 1);
          }}
        >
          <p>
            Nếu mất phản hồi mạng, đối chiếu danh sách trước khi gửi lại. Cùng
            nội dung đang thử lại giữ cùng idempotency key.
          </p>
        </FormPanel>
      )}
      <Listing<DTO<"Feedback"> | DTO<"AdminFeedback">>
        title="Danh sách phản hồi"
        refresh={version}
        filters={[
          {
            name: "status",
            label: "Trạng thái",
            type: "select",
            options: ["", "OPEN", "IN_PROGRESS", "RESOLVED"],
          },
          {
            name: "category",
            label: "Loại",
            type: "select",
            options: ["", "BUG", "SUGGESTION", "OTHER"],
          },
          { name: "device_id", label: "ID thiết bị" },
          ...(admin ? [{ name: "user_id", label: "ID tác giả" }] : []),
        ]}
        load={(query, signal) =>
          admin
            ? client.call("adminListFeedback", {
                query: query as Input<"adminListFeedback">["query"],
                signal,
              })
            : client.call("listFeedback", {
                query: query as Input<"listFeedback">["query"],
                signal,
              })
        }
        rowKey={(r) => r.id}
        columns={[
          {
            label: "Loại / trạng thái",
            cell: (r) => `${r.category} · ${r.status}`,
          },
          { label: "Nội dung", cell: (r) => r.content },
          { label: "Trả lời ADMIN", cell: (r) => text(r.admin_response) },
          {
            label: "Thao tác",
            cell: (r) => (
              <button onClick={() => setSelected(r)}>Xem phản hồi</button>
            ),
          },
        ]}
      />
      {selected &&
        (admin ? (
          <section key={selected.id}>
            <h2>Phản hồi được chọn</h2>
            <Details data={selected} />
            <FormPanel
              title="Trạng thái và trả lời"
              submit="Lưu trả lời"
              fields={[
                {
                  name: "status",
                  label: "Trạng thái phản hồi",
                  type: "select",
                  options: ["OPEN", "IN_PROGRESS", "RESOLVED"],
                  value: selected.status,
                },
                {
                  name: "admin_response",
                  label: "Trả lời (bỏ trống để giữ nguyên)",
                  type: "textarea",
                  maxLength: 5000,
                  value: selected.admin_response ?? "",
                },
              ]}
              onSubmit={async (v) => {
                const updated = await client.call("adminUpdateFeedback", {
                  path: { feedback_id: selected.id },
                  body: {
                    status: v.status as DTO<"Feedback">["status"],
                    ...(v.admin_response.trim()
                      ? { admin_response: v.admin_response.trim() }
                      : {}),
                  },
                });
                setSelected(updated);
                setVersion((n) => n + 1);
              }}
            />
          </section>
        ) : (
          <FeedbackDetail key={selected.id} id={selected.id} />
        ))}
    </>
  );
}
function FeedbackDetail({ id }: { id: string }) {
  const client = useClient(),
    state = useLoad(
      (signal) =>
        client.call("getFeedback", { path: { feedback_id: id }, signal }),
      id,
    );
  return (
    <section>
      <h2>Chi tiết và trả lời ADMIN</h2>
      <LoadStatus {...state} />
      {state.data && <Details data={state.data} />}
      <button disabled={state.busy} onClick={state.reload}>
        Tải lại phản hồi
      </button>
      <p>
        Hợp đồng chưa có thao tác người dùng trả lời thêm vào cùng phản hồi. Bạn
        có thể tạo phản hồi mới.
      </p>
    </section>
  );
}
