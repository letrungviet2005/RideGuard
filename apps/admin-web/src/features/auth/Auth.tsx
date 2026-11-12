import { AuthLayout } from "../../app/AuthLayout";
import { ErrorBox, FormPanel, type Field } from "../../shared/ui/common";
import { useClient, useSession } from "../../shared/api/context";

export const passwordField = (name: string, label: string): Field => ({
  name,
  label,
  type: "password",
  required: true,
  hint: "12–128 ký tự Unicode; không tự cắt hoặc bỏ khoảng trắng.",
});
export function Auth({ lab = false }: { lab?: boolean }) {
  const client = useClient(),
    session = useSession();
  return (
    <AuthLayout lab={lab}>
      <span className="eyebrow">
        {lab ? "PHIÊN THỬ RIÊNG BIỆT" : "CHÀO MỪNG TRỞ LẠI"}
      </span>
      <h1>
        {lab ? "Thử API mobile — không kết nối thiết bị" : "Đăng nhập quản trị"}
      </h1>
      <p>
        {lab
          ? "Phiên USER riêng trong memory. Đây là công cụ development thử REST, không phải ứng dụng Android."
          : "Chỉ tài khoản ADMIN đã được tạo bằng CLI backend. Không có tài khoản mặc định."}
      </p>
      {session.reason && (
        <p role="status" className="notice">
          {session.reason}
        </p>
      )}
      <ErrorBox error={session.error} />
      <FormPanel
        title={lab ? "Đăng nhập phiên thử" : "Đăng nhập ADMIN"}
        disabled={session.busy}
        submit="Đăng nhập"
        fields={[
          { name: "email", label: "Email", type: "email", required: true },
          passwordField("password", "Mật khẩu"),
        ]}
        onSubmit={(v) =>
          client.login({
            email: v.email.trim().toLowerCase(),
            password: v.password,
            client_name: lab ? "Development REST lab" : "RideGuard admin web",
          })
        }
      />
      {lab && (
        <FormPanel
          title="Đăng ký USER thử API"
          submit="Đăng ký"
          fields={[
            {
              name: "email",
              label: "Email đăng ký",
              type: "email",
              required: true,
            },
            {
              name: "full_name",
              label: "Họ tên",
              required: true,
              maxLength: 100,
            },
            passwordField("password", "Mật khẩu đăng ký"),
          ]}
          onSubmit={(v) =>
            client.call("register", {
              body: {
                email: v.email.trim().toLowerCase(),
                full_name: v.full_name.trim(),
                password: v.password,
              },
            })
          }
        />
      )}
      <PasswordRecovery />
    </AuthLayout>
  );
}
export function PasswordRecovery({ expanded = false }: { expanded?: boolean }) {
  const client = useClient();
  return (
    <details open={expanded || undefined}>
      <summary>Quên mật khẩu / đặt lại mật khẩu</summary>
      <FormPanel
        title="Yêu cầu email đặt lại"
        submit="Gửi yêu cầu"
        fields={[
          {
            name: "email",
            label: "Email khôi phục",
            type: "email",
            required: true,
          },
        ]}
        onSubmit={async (v) => {
          await client.call("forgotPassword", {
            body: { email: v.email.trim().toLowerCase() },
          });
        }}
      >
        <p>
          Nếu tài khoản tồn tại, hướng dẫn sẽ được gửi. Gửi yêu cầu thành công
          không đảm bảo thư đã vào hộp thư.
        </p>
      </FormPanel>
      <FormPanel
        title="Đặt lại mật khẩu"
        submit="Đặt lại"
        fields={[
          {
            name: "reset_token",
            label: "Mã reset đầy đủ từ email",
            type: "password",
            required: true,
            hint: "Token opaque, không phải OTP sáu số. Không chia sẻ hoặc đưa lên URL.",
          },
          passwordField("new_password", "Mật khẩu mới"),
        ]}
        onSubmit={async (v) => {
          await client.call("resetPassword", {
            body: { reset_token: v.reset_token, new_password: v.new_password },
          });
          client.clear("Đã đặt lại mật khẩu. Đăng nhập lại.");
        }}
      />
    </details>
  );
}
