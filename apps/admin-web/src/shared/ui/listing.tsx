import { useState, type ReactNode } from "react";
import {
  Fields,
  LoadStatus,
  Table,
  useLoad,
  type Field,
  type Values,
} from "./common";

export function Listing<T>({
  title,
  filters = [],
  load,
  columns,
  rowKey,
  refresh = 0,
}: {
  title: string;
  filters?: Field[];
  load: (
    query: Record<string, string>,
    signal: AbortSignal,
  ) => Promise<{ items: T[]; next_cursor: string | null }>;
  columns: { label: string; cell: (row: T) => ReactNode }[];
  rowKey: (row: T) => string;
  refresh?: number;
}) {
  const [draft, setDraft] = useState<Values>({}),
    [query, setQuery] = useState<Values>({});
  const [history, setHistory] = useState<string[]>([]);
  const cursor = history.at(-1);
  const key = JSON.stringify([query, cursor, refresh]);
  const state = useLoad(
    (signal) => load({ ...query, ...(cursor ? { cursor } : {}) }, signal),
    key,
  );
  return (
    <section>
      <h2>{title}</h2>
      {filters.length > 0 && (
        <form
          className="filters"
          onSubmit={(e) => {
            e.preventDefault();
            setHistory([]);
            setQuery(
              Object.fromEntries(
                Object.entries(draft)
                  .filter(([, v]) => v.trim())
                  .map(([k, v]) => [k, v.trim()]),
              ),
            );
          }}
        >
          <Fields
            fields={filters}
            values={draft}
            set={(k, v) => setDraft((old) => ({ ...old, [k]: v }))}
          />
          <button>Áp dụng bộ lọc</button>
        </form>
      )}
      <LoadStatus {...state} />
      {Boolean(state.error) && (
        <button disabled={state.busy} onClick={state.reload}>
          Thử tải lại danh sách
        </button>
      )}
      {state.data && (
        <>
          <Table rows={state.data.items} columns={columns} rowKey={rowKey} />
          <div className="actions">
            <button
              disabled={state.busy || history.length === 0}
              onClick={() => setHistory((h) => h.slice(0, -1))}
            >
              Trước
            </button>
            <button
              disabled={state.busy || !state.data.next_cursor}
              onClick={() =>
                setHistory((h) => [...h, state.data!.next_cursor!])
              }
            >
              Tiếp
            </button>
            <button disabled={state.busy} onClick={state.reload}>
              Tải lại danh sách
            </button>
          </div>
        </>
      )}
    </section>
  );
}
