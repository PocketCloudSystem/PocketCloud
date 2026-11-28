import { useState } from 'preact/hooks';
import { api } from '../api.js';
import { pages } from '../pages/index.js';
import { lang, setLang, setTheme, t, theme } from '../i18n.js';

export function Shell({ session, section, onLogout, children }) {
  const [open, setOpen] = useState(false);
  const [mode, setMode] = useState(theme);
  const toggleTheme = () => { const next = mode === 'dark' ? 'light' : 'dark'; setTheme(next); setMode(next); };
  const logout = async () => {
    try { await api('/panel-api/logout', { method: 'POST' }); } finally { onLogout(); }
  };

  return (
    <div class={'shell' + (open ? ' nav-open' : '')}>
      <header class="topbar">
        <button class="ghost menu" aria-label={t('Menu')} onClick={() => setOpen(!open)}>☰</button>
        <img src="./logo.png" alt="" width="28" height="28" />
        <strong>{session.cloudName || 'PocketCloud'}</strong>
        <span class="muted small">{session.clustered ? `${t('node')} ${session.node}` : ''}</span>
        <span class="spacer" />
        <span class="muted small hide-sm">v{session.version}</span>
        <span class="muted small">{session.user}</span>
        <button class="ghost" onClick={toggleTheme} aria-label={mode === 'dark' ? 'Light mode' : 'Dark mode'} title={mode === 'dark' ? 'Light mode' : 'Dark mode'}>
          {mode === 'dark' ? '☀' : '☾'}
        </button>
        <button class="ghost small" onClick={() => setLang(lang === 'de' ? 'en' : 'de')} aria-label="Language / Sprache">{lang === 'de' ? 'EN' : 'DE'}</button>
        <button class="ghost" onClick={logout}>{t('Sign out')}</button>
      </header>
      <nav class="sidebar" onClick={() => setOpen(false)}>
        {Object.entries(pages).filter(([id, p]) => p.label && (id !== 'nodes' || session.clustered) && (!p.admin || session.permissions?.includes('admin'))).map(([id, p]) => (
          <a href={'#/' + id} class={id === section ? 'active' : ''}>{t(p.label)}</a>
        ))}
      </nav>
      <main class="content">{children}</main>
    </div>
  );
}
