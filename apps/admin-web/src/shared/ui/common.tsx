import {
  useEffect,
  useId,
  useRef,
  useState,
  type ReactNode,
  type FormEvent,
} from "react";
import { ApiError, isAbort } from "../api/client";

export function ErrorBox({ error }: { error: unknown }) {
  if (!error || isAbort(error)) return null;
  return (
    <div role="alert" className="notice error">
      <strong>
        {error instanceof Error ? error.message : "Yêu cầu thất bại."}
      </strong>
      {error instanceof ApiError && (
        <>
          <p>
            Mã: {error.code} · HTTP: {error.status || "chưa nhận phản hồi"}
          </p>
          {error.requestId && <p>request_id: {error.requestId}</p>}
          {error.code === "ADMIN_SELF_LOCK" && (
            <p>Không thể tự khóa tài khoản đang dùng.</p>
          )}
          {error.code === "LAST_ADMIN" && (
            <p>Không thể khóa ADMIN hoạt động cuối cùng.</p>
          )}
          {error.status === 403 && (
            <p>
              Kiểm tra quyền, Origin và header CSRF; không bỏ cơ chế bảo vệ.
            </p>
          )}
          {error.status === 409 && (
            <p>
              Dữ liệu có xung đột. Tải lại và đối chiếu trước khi thao tác tiếp.
            </p>
          )}
        </>
      )}
    </div>
  );
}
export function useTask() {
  const [busy, setBusy] = useState(false),
    [error, setError] = useState<unknown>(),
    [success, setSuccess] = useState("");
  const lock = useRef(false),
    alive = useRef(true);
  useEffect(() => {
    alive.current = true;
    return () => {
      alive.current = false;
    };
  }, []);
  async function run(task: () => Promise<unknown>, message = "Thành công.") {
    if (lock.current) return false;
    lock.current = true;
    setBusy(true);
    setError(undefined);
    setSuccess("");
    try {
      await task();
      if (alive.current) setSuccess(message);
      return true;
    } catch (e) {
      if (alive.current) setError(e);
      return false;
    } finally {
      lock.current = false;
      if (alive.current) setBusy(false);
    }
  }
  return {
    busy,
    run,
    error,
    success,
    status: (
      <>
        <ErrorBox error={error} />
        {success && (
          <p role="status" className="notice success">
            {success}
          </p>
        )}
      </>
    ),
  };
}
export type Field = {
  name: string;
  label: string;
  type?:
    | "text"
    | "email"
    | "password"
    | "number"
    | "checkbox"
    | "textarea"
    | "select"
    | "datetime-local";
  options?: readonly string[];
  required?: boolean;
  value?: string;
  min?: number;
  max?: number;
  maxLength?: number;
  hint?: string;
};
export type Values = Record<string, string>;
export function Fields({
  fields,
  values,
  set,
}: {
  fields: Field[];
  values: Values;
  set: (name: string, value: string) => void;
}) {
  const fieldId = useId();
  return (
    <>
      {fields.map((f) => (
        <label key={f.name}>
          {f.label}
          {f.type === "select" ? (
            <select
              aria-label={f.label}
              name={f.name}
              required={f.required}
              value={values[f.name] ?? ""}
              onChange={(e) => set(f.name, e.target.value)}
            >
              {f.options?.map((o) => (
                <option key={o} value={o}>
                  {o || "Tất cả / không chọn"}
                </option>
              ))}
            </select>
          ) : f.type === "textarea" ? (
            <textarea
              aria-label={f.label}
              name={f.name}
              required={f.required}
              maxLength={f.maxLength}
              value={values[f.name] ?? ""}
              onChange={(e) => set(f.name, e.target.value)}
            />
          ) : (
            <input
              aria-label={f.label}
              aria-describedby={f.hint ? `${fieldId}-${f.name}` : undefined}
              name={f.name}
              type={f.type ?? "text"}
              required={f.required}
              min={f.min}
              max={f.max}
              maxLength={f.maxLength}
              autoComplete={f.type === "password" ? "new-password" : "off"}
              {...(f.type === "checkbox"
                ? { checked: values[f.name] === "true" }
                : { value: values[f.name] ?? "" })}
              onChange={(e) =>
                set(
                  f.name,
                  f.type === "checkbox"
                    ? String(e.target.checked)
                    : e.target.value,
                )
              }
            />
          )}
          {f.hint && <small id={`${fieldId}-${f.name}`}>{f.hint}</small>}
        </label>
      ))}
    </>
  );
}
export function FormPanel({
  title,
  fields,
  submit,
  onSubmit,
  disabled = false,
  confirm,
  children,
}: {
  title: string;
  fields: Field[];
  submit: string;
  onSubmit: (v: Values) => Promise<unknown>;
  disabled?: boolean;
  confirm?: (v: Values) => string;
  children?: ReactNode;
}) {
  const [values, setValues] = useState<Values>(() =>
    Object.fromEntries(fields.map((f) => [f.name, f.value ?? ""])),
  );
  const [pending, setPending] = useState<Values>();
  const task = useTask();
  const execute = async (v: Values) => {
    if (await task.run(() => onSubmit(v)))
      setValues((old) => ({
        ...old,
        ...Object.fromEntries(
          fields.filter((f) => f.type === "password").map((f) => [f.name, ""]),
        ),
      }));
  };
  function handle(e: FormEvent) {
    e.preventDefault();
    if (disabled || task.busy) return;
    if (confirm) setPending({ ...values });
    else void execute({ ...values });
  }
  return (
    <section>
      <h2>{title}</h2>
      <form onSubmit={handle}>
        <fieldset disabled={task.busy || disabled}>
          <Fields
            fields={fields}
            values={values}
            set={(k, v) => setValues((old) => ({ ...old, [k]: v }))}
          />
          {children}
          <button type="submit">{task.busy ? "Đang gửi…" : submit}</button>
        </fieldset>
      </form>
      {task.status}
      {pending && (
        <Confirm
          title="Xác nhận thao tác"
          text={confirm!(pending)}
          busy={task.busy}
          cancel={() => setPending(undefined)}
          accept={() => {
            const v = pending;
            setPending(undefined);
            void execute(v);
          }}
        />
      )}
    </section>
  );
}
export function Confirm({
  title,
  text,
  busy,
  cancel,
  accept,
}: {
  title: string;
  text: string;
  busy: boolean;
  cancel: () => void;
  accept: () => void;
}) {
  const ref = useRef<HTMLDialogElement>(null);
  useEffect(() => {
    const d = ref.current;
    d?.showModal();
    return () => d?.close();
  }, []);
  return (
    <dialog
      ref={ref}
      onCancel={(e) => {
        e.preventDefault();
        if (!busy) cancel();
      }}
      aria-label={title}
    >
      <h2>{title}</h2>
      <p>{text}</p>
      <div className="actions">
        <button disabled={busy} onClick={cancel}>
          Quay lại
        </button>
        <button disabled={busy} className="danger" onClick={accept}>
          Xác nhận
        </button>
      </div>
    </dialog>
  );
}
export function Table<T>({
  rows,
  columns,
  rowKey,
}: {
  rows: T[];
  columns: { label: string; cell: (row: T) => ReactNode }[];
  rowKey: (row: T) => string;
}) {
  if (!rows.length) return <p className="empty">Chưa có dữ liệu phù hợp.</p>;
  return (
    <div className="table-scroll">
      <table>
        <thead>
          <tr>
            {columns.map((c) => (
              <th key={c.label}>{c.label}</th>
            ))}
          </tr>
        </thead>
        <tbody>
          {rows.map((r) => (
            <tr key={rowKey(r)}>
              {columns.map((c) => (
                <td key={c.label}>{c.cell(r)}</td>
              ))}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
export function Details({ data }: { data: Record<string, unknown> }) {
  return (
    <dl>
      {Object.entries(data).map(([k, v]) => (
        <div key={k}>
          <dt>{k}</dt>
          <dd>
            {v === null || v === undefined
              ? "Chưa có dữ liệu"
              : typeof v === "object"
                ? JSON.stringify(v)
                : String(v)}
          </dd>
        </div>
      ))}
    </dl>
  );
}
export const showBattery = (v: number | null | undefined) =>
  v == null ? "Chưa có dữ liệu" : `${v}%`;
export const text = (v: string | null | undefined) => v ?? "Chưa có dữ liệu";

export function useLoad<T>(
  load: (signal: AbortSignal) => Promise<T>,
  key: string,
) {
  const loader = useRef(load);
  loader.current = load;
  const [state, setState] = useState<{
    key: string;
    data?: T;
    error?: unknown;
    busy: boolean;
  }>({ key, busy: true });
  const [version, setVersion] = useState(0);
  useEffect(() => {
    const controller = new AbortController();
    let active = true;
    setState({ key, busy: true });
    void loader.current(controller.signal).then(
      (data) => {
        if (active) setState({ key, data, busy: false });
      },
      (error) => {
        if (active) setState({ key, error, busy: false });
      },
    );
    return () => {
      active = false;
      controller.abort();
    };
  }, [key, version]);
  return {
    ...(state.key === key
      ? state
      : { busy: true, data: undefined, error: undefined }),
    reload: () => setVersion((v) => v + 1),
  };
}
export function LoadStatus({
  busy,
  error,
}: {
  busy: boolean;
  error?: unknown;
}) {
  return (
    <>
      {busy && <p role="status">Đang tải…</p>}
      <ErrorBox error={error} />
    </>
  );
}
