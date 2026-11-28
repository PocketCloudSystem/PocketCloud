import { useState } from 'preact/hooks';
import { Languages, LoaderCircle, LogIn } from 'lucide-preact';
import { api } from '../api.js';
import { lang, setLang, t } from '../i18n.js';

export function Login({ onLogin }) {
  const [user, setUser] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState(null);
  const [busy, setBusy] = useState(false);

  const submit = async (e) => {
    e.preventDefault();
    setBusy(true);
    setError(null);
    try {
      onLogin(await api('/panel-api/login', { method: 'POST', body: { user, password } }));
    } catch (err) {
      setError(err.message);
      setBusy(false);
    }
  };

  return (
    <div class="center login-bg">
      <form class={'card login' + (error ? ' shake' : '')} onSubmit={submit} key={error}>
        <img src="./logo.png" alt="" width="64" height="64" class="logo" />
        <h1>PocketCloud</h1>
        <p class="muted">{t('Sign in with your panel user.')}</p>
        <label class="field"><span>{t('Name')}</span>
          <input id="user" autocomplete="username" value={user} onInput={(e) => setUser(e.currentTarget.value)} autofocus required />
        </label>
        <label class="field"><span>{t('Password')}</span>
          <input id="password" type="password" autocomplete="current-password" value={password} onInput={(e) => setPassword(e.currentTarget.value)} required />
        </label>
        {error && <p class="error" role="alert">{error}</p>}
        <button class="primary" disabled={busy || !user || !password}>
          {busy ? <LoaderCircle size={16} class="spin" /> : <LogIn size={16} />}{busy ? t('Signing in…') : t('Sign in')}
        </button>
        <p class="muted small">{t('No user yet? Create one in the cloud console:')} <code>user create &lt;name&gt; admin</code></p>
        <button type="button" class="ghost small" onClick={() => setLang(lang === 'de' ? 'en' : 'de')}><Languages size={14} />{lang === 'de' ? 'English' : 'Deutsch'}</button>
      </form>
    </div>
  );
}
