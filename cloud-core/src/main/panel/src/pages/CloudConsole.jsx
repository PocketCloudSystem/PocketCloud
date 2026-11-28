import { api } from '../api.js';
import { Console } from '../components/Console.jsx';
import { t } from '../i18n.js';

export function CloudConsole({ session }) {
  return (
    <section class="stack wide">
      <h1>{t('Cloud console')} {session.clustered && <span class="muted small">{session.node}</span>}</h1>
      <p class="muted">
        {session.clustered ? t('The console of node {node}, like over SSH.', { node: session.node }) : t('The console of the cloud, like over SSH.')}{' '}
        {t('Commands run as if typed there, including confirmations (answer them with y). Every command from the panel is logged.')}
      </p>
      <Console title={t('Console')} stream="/panel-api/console" placeholder={t('Cloud command, e.g. server list')}
               run={(command) => api('/panel-api/console', { method: 'POST', body: { command } })} height="calc(100vh - 290px)" />
    </section>
  );
}
