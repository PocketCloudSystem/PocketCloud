import { render } from 'preact';
import { useEffect, useState } from 'preact/hooks';
import { api } from './api.js';
import { useRoute } from './router.js';
import { Login } from './pages/Login.jsx';
import { Shell } from './components/Shell.jsx';
import { Toasts } from './components/ui.jsx';
import { pages, NotFound } from './pages/index.js';
import './style.css';
import { t } from './i18n.js';

function App() {
  const [session, setSession] = useState(undefined); // undefined = loading, null = logged out
  const route = useRoute();

  useEffect(() => {
    api('/panel-api/session').then(setSession, () => setSession(null));
    const logout = () => setSession(null);
    addEventListener('pc-logout', logout);
    return () => removeEventListener('pc-logout', logout);
  }, []);

  if (session === undefined) return <div class="center muted">{t('Loading…')}</div>;
  if (session === null) return <Login onLogin={setSession} />;

  const [, section = '', ...rest] = route.split('/');
  const Page = pages[section]?.component || NotFound;
  return (
    <Shell session={session} section={section} onLogout={() => setSession(null)}>
      <Page params={rest.map(decodeURIComponent)} session={session} />
      <Toasts />
    </Shell>
  );
}

render(<App />, document.getElementById('app'));
