import { useEffect, useRef, useState } from 'preact/hooks';

/** Calls fn now and every ms; returns { data, error, reload }. */
export function usePoll(fn, ms, deps = []) {
  const [state, setState] = useState({ data: undefined, error: null });
  const fnRef = useRef(fn);
  fnRef.current = fn;
  const reload = async () => {
    try { setState({ data: await fnRef.current(), error: null }); }
    catch (error) { setState((s) => ({ data: s.data, error })); }
  };
  useEffect(() => {
    reload();
    const id = setInterval(() => document.hidden || reload(), ms);
    return () => clearInterval(id);
  }, deps);
  return { ...state, reload };
}

export function bytes(n) {
  if (n == null || n < 0) return '–';
  const units = ['B', 'KB', 'MB', 'GB', 'TB'];
  let i = 0;
  while (n >= 1024 && i < units.length - 1) { n /= 1024; i++; }
  return `${n.toFixed(i && n < 10 ? 1 : 0)} ${units[i]}`;
}

export function duration(ms) {
  if (ms == null || ms < 0) return '–';
  const s = Math.floor(ms / 1000), d = Math.floor(s / 86400), h = Math.floor(s / 3600) % 24, m = Math.floor(s / 60) % 60;
  if (d) return `${d}d ${h}h`;
  if (h) return `${h}h ${m}m`;
  if (m) return `${m}m ${s % 60}s`;
  return `${s}s`;
}

export const pct = (v) => (v == null || v < 0 ? '–' : `${v.toFixed(v < 10 ? 1 : 0)} %`);

// toasts: one global list, shown by <Toasts/>
const listeners = new Set();
export function toast(message, kind = 'ok') {
  for (const l of listeners) l({ id: Math.random(), message, kind });
}
export function useToasts() {
  const [items, setItems] = useState([]);
  useEffect(() => {
    const add = (t) => {
      setItems((list) => [...list, t]);
      setTimeout(() => setItems((list) => list.filter((x) => x !== t)), 4000);
    };
    listeners.add(add);
    return () => listeners.delete(add);
  }, []);
  return items;
}

/** Runs an API action and shows its message or error as a toast. */
export async function act(promise, fallback = 'Done.') {
  try {
    const res = await promise;
    toast(res?.message || fallback);
    return res;
  } catch (e) {
    toast(e.message, 'bad');
    return undefined;
  }
}
