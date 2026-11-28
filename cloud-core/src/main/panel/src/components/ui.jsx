import { useEffect, useState } from 'preact/hooks';
import { useToasts } from '../util.js';
import { t } from '../i18n.js';

export function Stat({ label, value, sub, icon: Icon }) {
  return (
    <div class="card stat">
      {Icon && <span class="stat-icon"><Icon size={20} /></span>}
      <span class="muted small">{label}</span>
      <strong>{value}</strong>
      {sub && <span class="muted small">{sub}</span>}
    </div>
  );
}

export function Meter({ value, max, label }) {
  const ratio = max > 0 ? Math.min(1, value / max) : 0;
  const level = ratio > 0.9 ? 'bad' : ratio > 0.75 ? 'warn' : 'ok';
  return (
    <div class="meter" title={label} role="meter" aria-valuenow={Math.round(ratio * 100)} aria-valuemin="0" aria-valuemax="100" aria-label={label}>
      <div class={'meter-fill ' + level} style={{ width: `${ratio * 100}%` }} />
    </div>
  );
}

const STATUS = { ONLINE: 'ok', HEAD: 'ok', FOLLOWER: 'ok', ALIVE: 'ok', STARTING: 'warn', PENDING: 'warn', JOINING: 'warn', SUSPECT: 'warn', STOPPING: 'warn', OFFLINE: 'bad', DEAD: 'bad', ISOLATED: 'bad' };

export const Badge = ({ status, children }) => (
  <span class={'badge ' + (STATUS[status] || '') + (STATUS[status] === 'ok' ? ' live' : '')}>{children || status?.toLowerCase()}</span>
);

/** Button that asks for a second click instead of a browser dialog. */
export function ConfirmButton({ onConfirm, children, confirmText = 'Sure?', class: cls = '', ...rest }) {
  const [armed, setArmed] = useState(false);
  useEffect(() => {
    if (!armed) return undefined;
    const id = setTimeout(() => setArmed(false), 3000);
    return () => clearTimeout(id);
  }, [armed]);
  return (
    <button {...rest} class={cls + (armed ? ' armed' : '')}
            onClick={() => (armed ? (setArmed(false), onConfirm()) : setArmed(true))}>
      {armed ? confirmText : children}
    </button>
  );
}

export function Toasts() {
  const items = useToasts();
  return (
    <div class="toasts" aria-live="polite">
      {items.map((t) => <div key={t.id} class={'toast ' + t.kind}>{t.message}</div>)}
    </div>
  );
}

/** Shimmer placeholder while a poll has neither data nor an error yet. */
export const Loading = ({ poll, rows = 3 }) => (poll.data === undefined && !poll.error ? (
  <div class="card skeleton" aria-busy="true" aria-label={t('Loading…')}>
    {Array.from({ length: rows }, (_, i) => <div key={i} class="skeleton-line" style={{ width: `${90 - i * 15}%` }} />)}
  </div>
) : null);

export const ErrorNote = ({ error }) => error ? <p class="error">{t('Could not load:')} {error.message}</p> : null;
