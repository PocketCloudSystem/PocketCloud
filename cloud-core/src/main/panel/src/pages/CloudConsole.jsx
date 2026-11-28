import { api } from '../api.js';
import { Console } from '../components/Console.jsx';

export function CloudConsole({ session }) {
  return (
    <section class="stack wide">
      <h1>Cloud console {session.clustered && <span class="muted small">{session.node}</span>}</h1>
      <p class="muted">
        The console of {session.clustered ? `node ${session.node}` : 'the cloud'}, like over SSH. Commands run as if typed there,
        including confirmations (answer them with <code>y</code>). Every command from the panel is logged.
      </p>
      <Console title="Console" stream="/panel-api/console" placeholder="Cloud command, e.g. server list"
               run={(command) => api('/panel-api/console', { method: 'POST', body: { command } })} height="calc(100vh - 290px)" />
    </section>
  );
}
