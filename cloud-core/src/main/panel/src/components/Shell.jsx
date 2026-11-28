import { useState } from 'preact/hooks';
import { api } from '../api.js';
import { pages } from '../pages/index.js';

export function Shell({ session, section, onLogout, children }) {
  const [open, setOpen] = useState(false);
  const logout = async () => {
    try { await api('/panel-api/logout', { method: 'POST' }); } finally { onLogout(); }
  };

  return (
    <div class={'shell' + (open ? ' nav-open' : '')}>
      <header class="topbar">
        <button class="ghost menu" aria-label="Menu" onClick={() => setOpen(!open)}>☰</button>
        <img src="./logo.png" alt="" width="28" height="28" />
        <strong>{session.cloudName || 'PocketCloud'}</strong>
        <span class="muted small">{session.clustered ? `node ${session.node}` : ''}</span>
        <span class="spacer" />
        <span class="muted small hide-sm">v{session.version}</span>
        <span class="muted small">{session.user}</span>
        <button class="ghost" onClick={logout}>Sign out</button>
      </header>
      <nav class="sidebar" onClick={() => setOpen(false)}>
        {Object.entries(pages).filter(([id, p]) => p.label && (id !== 'nodes' || session.clustered) && (!p.admin || session.permissions?.includes('admin'))).map(([id, p]) => (
          <a href={'#/' + id} class={id === section ? 'active' : ''}>{p.label}</a>
        ))}
      </nav>
      <main class="content">{children}</main>
    </div>
  );
}
