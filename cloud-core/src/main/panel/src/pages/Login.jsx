import { useState } from 'preact/hooks';
import { api } from '../api.js';

export function Login({ onLogin }) {
  const [key, setKey] = useState('');
  const [user, setUser] = useState('');
  const [error, setError] = useState(null);
  const [busy, setBusy] = useState(false);

  const submit = async (e) => {
    e.preventDefault();
    setBusy(true);
    setError(null);
    try {
      onLogin(await api('/panel-api/login', { method: 'POST', body: user ? { user, password: key } : { key } }));
    } catch (err) {
      setError(err.message);
      setBusy(false);
    }
  };

  return (
    <div class="center">
      <form class="card login" onSubmit={submit}>
        <img src="./logo.png" alt="" width="56" height="56" />
        <h1>PocketCloud</h1>
        <p class="muted">Sign in as a panel user, or leave the name empty and use the auth-key from <code>storage/configs/http.yml</code>.</p>
        <label for="user">Name</label>
        <input id="user" autocomplete="username" value={user} onInput={(e) => setUser(e.currentTarget.value)}
               placeholder="empty for the auth-key" autofocus />
        <label for="key">{user ? 'Password' : 'Auth-key'}</label>
        <input id="key" type="password" autocomplete="current-password" value={key}
               onInput={(e) => setKey(e.currentTarget.value)} required />
        {error && <p class="error" role="alert">{error}</p>}
        <button class="primary" disabled={busy || !key}>{busy ? 'Signing in…' : 'Sign in'}</button>
      </form>
    </div>
  );
}
