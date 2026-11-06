const baseUrl = import.meta.env.VITE_API_BASE_URL as string | undefined;

export async function apiRequest<T>(path: `/api/v1/${string}`, init?: RequestInit): Promise<T> {
  if (!baseUrl) throw new Error('VITE_API_BASE_URL is not configured');
  const headers = new Headers(init?.headers);
  if (!headers.has('Accept')) headers.set('Accept', 'application/json');
  const response = await fetch(`${baseUrl.replace(/\/$/, '')}${path}`, {
    ...init, headers,
  });
  if (!response.ok) throw new Error(`API request failed (${response.status})`);
  if (response.status === 204) return undefined as T;
  return response.json() as Promise<T>;
}
