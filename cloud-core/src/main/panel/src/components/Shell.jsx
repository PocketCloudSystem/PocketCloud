import { useState } from 'preact/hooks';
import { KeyRound, Languages, LogOut, Menu, Moon, Sun } from 'lucide-preact';
import { api } from '../api.js';
import { pages } from '../pages/index.js';
import { lang, setLang, setTheme, t, theme } from '../i18n.js';
import { act } from '../util.js';

function PasswordForm({ onDone }) {
  const [current, setCurrent] = useState('');
  const [password, setPassword] = useState('');
  const submit = async (e) => {
    e.preventDefault();
    if (await act(api('/panel-api/password', { method: 'POST', body: { current, password } }), t('Password changed.'))) onDone();
  };
  return (
    <form class="card popover stack-sm" onSubmit={submit}>
      <h3>{t('Change password')}</h3>
      <label class="field"><span>{t('Current password')}</span>
        <input type="password" autocomplete="current-password" value={current} onInput={(e) => setCurrent(e.currentTarget.value)} required autofocus />
      </label>
      <label class="field"><span>{t('New password')}</span>
        <input type="password" autocomplete="new-password" minLength={8} value={password} onInput={(e) => setPassword(e.currentTarget.value)} required />
      </label>
      <div class="row">
        <button class="primary">{t('Save')}</button>
        <button type="button" class="ghost" onClick={onDone}>{t('Cancel')}</button>
      </div>
    </form>
  );
}

export function Shell({ session, section, onLogout, children }) {
  const [open, setOpen] = useState(false);
  const [mode, setMode] = useState(theme);
  const [changing, setChanging] = useState(false);
  const toggleTheme = () => { const next = mode === 'dark' ? 'light' : 'dark'; setTheme(next); setMode(next); };
  const logout = async () => {
    try { await api('/panel-api/logout', { method: 'POST' }); } finally { onLogout(); }
  };

  return (
    <div class={'shell' + (open ? ' nav-open' : '')}>
      <header class="topbar">
        <button class="ghost icon menu" aria-label={t('Menu')} onClick={() => setOpen(!open)}><Menu size={18} /></button>
        <img src="./logo.png" alt="" width="28" height="28" class="logo" />
        <strong>{session.cloudName || 'PocketCloud'}</strong>
        <span class="muted small hide-sm">{session.clustered ? `${t('node')} ${session.node}` : ''}</span>
        <span class="spacer" />
        <span class="muted small hide-sm">v{session.version}</span>
        <button class="ghost user-chip" onClick={() => setChanging(!changing)} title={t('Change password')}>
          <span class="avatar">{session.user.slice(0, 1).toUpperCase()}</span>
          <span class="hide-sm">{session.user}</span>
          <span class="badge">{session.role}</span>
          <KeyRound size={14} />
        </button>
        <button class="ghost icon" onClick={toggleTheme} aria-label={mode === 'dark' ? t('Light mode') : t('Dark mode')} title={mode === 'dark' ? t('Light mode') : t('Dark mode')}>
          {mode === 'dark' ? <Sun size={18} /> : <Moon size={18} />}
        </button>
        <button class="ghost icon" onClick={() => setLang(lang === 'de' ? 'en' : 'de')} aria-label="Language / Sprache" title="Language / Sprache">
          <Languages size={18} /><span class="small">{lang === 'de' ? 'EN' : 'DE'}</span>
        </button>
        <button class="ghost icon" onClick={logout} title={t('Sign out')} aria-label={t('Sign out')}><LogOut size={18} /></button>
        {changing && <PasswordForm onDone={() => setChanging(false)} />}
      </header>
      <nav class="sidebar" onClick={() => setOpen(false)}>
        {Object.entries(pages).filter(([id, p]) => p.label && (id !== 'nodes' || session.clustered) && (!p.admin || session.permissions?.includes('admin'))).map(([id, p]) => (
          <a href={'#/' + id} class={id === section ? 'active' : ''}>{p.icon && <p.icon size={17} />}<span>{t(p.label)}</span></a>
        ))}
      </nav>
      <main class="content" key={section}>{children}</main>
    </div>
  );
}
