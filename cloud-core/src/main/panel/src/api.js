// The panel lives at <prefix>/panel/, the API at <prefix>/...
export const base = location.pathname.replace(/\/panel(\/.*)?$/, '');

export class ApiError extends Error {
  constructor(status, message) {
    super(message);
    this.status = status;
  }
}

/** `raw: true` sends body (string, Blob or File) as it is instead of JSON. */
export async function api(path, { method = 'GET', body, raw = false } = {}) {
  const headers = { 'X-PocketCloud-Panel': '1' };
  if (body !== undefined) headers['Content-Type'] = raw ? 'application/octet-stream' : 'application/json';
  const res = await fetch(base + path, {
    method, headers, credentials: 'same-origin',
    body: body === undefined || raw ? body : JSON.stringify(body),
  });
  const text = await res.text();
  let data = text;
  try { data = text ? JSON.parse(text) : null; } catch { /* plain text */ }
  if (res.status === 401 && path !== '/panel-api/login') window.dispatchEvent(new Event('pc-logout'));
  if (!res.ok) throw new ApiError(res.status, data?.message || data?.error || res.statusText);
  return data;
}

/** Server-Sent Events from the panel API; returns a function that closes the stream. */
export function events(path, handlers) {
  const source = new EventSource(base + path, { withCredentials: true });
  for (const [name, fn] of Object.entries(handlers)) source.addEventListener(name, (e) => fn(e.data, source));
  return () => source.close();
}

/** File contents as text, never parsed as JSON. */
export async function fileText(path) {
  const res = await fetch(base + path, { headers: { 'X-PocketCloud-Panel': '1' }, credentials: 'same-origin' });
  const text = await res.text();
  if (!res.ok) {
    let message = res.statusText;
    try { message = JSON.parse(text).message || message; } catch { /* plain text */ }
    throw new ApiError(res.status, message);
  }
  return text;
}
