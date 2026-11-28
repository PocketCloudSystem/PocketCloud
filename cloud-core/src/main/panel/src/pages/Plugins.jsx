import { api } from '../api.js';
import { Badge, ErrorNote } from '../components/ui.jsx';
import { act, usePoll } from '../util.js';

export function Plugins({ session }) {
  const plugins = usePoll(() => api('/v1/plugins'), 10000);
  const list = Object.values(plugins.data || {}).sort((a, b) => a.name.localeCompare(b.name));
  const toggle = (p) => act(api(`/v1/plugins/${encodeURIComponent(p.name)}/${p.enabled ? 'disable' : 'enable'}`, { method: 'POST', body: {} })).then(plugins.reload);

  return (
    <section class="stack">
      <h1>Plugins</h1>
      <p class="muted">Cloud plugins{session.clustered ? ` of ${session.node} (each node loads its own)` : ''}, from <code>plugins/</code>.</p>
      <ErrorNote error={plugins.error} />
      <div class="card table-wrap">
        <table>
          <thead><tr><th>Name</th><th>Version</th><th>Authors</th><th>Status</th><th /></tr></thead>
          <tbody>
            {list.map((p) => (
              <tr key={p.name}>
                <td>{p.name}</td>
                <td>{p.version}</td>
                <td class="muted">{p.authors.join(', ') || '–'}</td>
                <td><Badge status={p.enabled ? 'ONLINE' : 'OFFLINE'}>{p.enabled ? 'enabled' : 'disabled'}</Badge></td>
                <td><button onClick={() => toggle(p)}>{p.enabled ? 'Disable' : 'Enable'}</button></td>
              </tr>
            ))}
            {plugins.data && !list.length && <tr><td colSpan="5" class="muted">No plugins installed.</td></tr>}
          </tbody>
        </table>
      </div>
    </section>
  );
}
