import { useClient } from "../../shared/api/context";
import {
  Details,
  FormPanel,
  LoadStatus,
  useLoad,
} from "../../shared/ui/common";
import { passwordField, PasswordRecovery } from "../auth/Auth";
export function Profile() {
  const client = useClient();
  const state = useLoad(
    (signal) => client.call("getProfile", { signal }),
    "profile",
  );
  return (
    <>
      <h1>Tài khoản hiện tại</h1>
      <LoadStatus {...state} />
      <button onClick={state.reload} disabled={state.busy}>
        Tải lại hồ sơ
      </button>
      {state.data && (
        <>
          <Details data={state.data} />
          <FormPanel
            key={state.data.updated_at}
            title="Sửa họ tên"
            submit="Lưu hồ sơ"
            fields={[
              {
                name: "full_name",
                label: "Họ tên",
                required: true,
                maxLength: 100,
                value: state.data.full_name,
              },
            ]}
            onSubmit={async (v) => {
              await client.call("updateProfile", {
                body: { full_name: v.full_name.trim() },
              });
              state.reload();
            }}
          />
        </>
      )}
      <FormPanel
        title="Đổi mật khẩu"
        submit="Đổi mật khẩu"
        fields={[
          passwordField("current_password", "Mật khẩu hiện tại"),
          passwordField("new_password", "Mật khẩu mới"),
        ]}
        onSubmit={(v) =>
          client.call("changePassword", {
            body: {
              current_password: v.current_password,
              new_password: v.new_password,
            },
          })
        }
      >
        <p>
          Các phiên khác sẽ bị thu hồi; phiên hiện tại được giữ theo chính sách
          backend.
        </p>
      </FormPanel>
      <PasswordRecovery />
    </>
  );
}
