import { api } from '../api.js';
import { Badge, ErrorNote, Loading } from '../components/ui.jsx';
import { act, usePoll } from '../util.js';
import { t } from '../i18n.js';

export function Plugins({ session }) {
  const plugins = usePoll(() => api('/v1/plugins'), 10000);
  const list = Object.values(plugins.data || {}).sort((a, b) => a.name.localeCompare(b.name));
  const toggle = (p) => act(api(`/v1/plugins/${encodeURIComponent(p.name)}/${p.enabled ? 'disable' : 'enable'}`, { method: 'POST', body: {} })).then(plugins.reload);

  return (
    <section class="stack">
      <h1>{t('Plugins')}</h1>
      <p class="muted">{session.clustered ? t('Cloud plugins of {node} (each node loads its own), from', { node: session.node }) : t('Cloud plugins, from')} <code>plugins/</code>.</p>
      <ErrorNote error={plugins.error} />
      <Loading poll={plugins} />
      <div class="card table-wrap">
        <table>
          <thead><tr><th>{t('Name')}</th><th>{t('Version')}</th><th>{t('Authors')}</th><th>{t('Status')}</th><th /></tr></thead>
          <tbody>
            {list.map((p) => (
              <tr key={p.name}>
                <td>{p.name}</td>
                <td>{p.version}</td>
                <td class="muted">{p.authors.join(', ') || '–'}</td>
                <td><Badge status={p.enabled ? 'ONLINE' : 'OFFLINE'}>{p.enabled ? t('enabled') : t('disabled')}</Badge></td>
                <td><button onClick={() => toggle(p)}>{p.enabled ? t('Disable') : t('Enable')}</button></td>
              </tr>
            ))}
            {plugins.data && !list.length && <tr><td colSpan="5" class="muted">{t('No plugins installed.')}</td></tr>}
          </tbody>
        </table>
      </div>
    </section>
  );
}
