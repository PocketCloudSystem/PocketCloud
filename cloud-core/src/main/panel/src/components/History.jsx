import { api } from '../api.js';
import { ConfirmButton, ErrorNote } from './ui.jsx';
import { act, usePoll } from '../util.js';

const files = (list, sign) => list.slice(0, 4).map((f) => `${sign}${f}`).join(', ') + (list.length > 4 ? ` +${list.length - 4}` : '');

/** File versions of a template in the cluster, with rollback. */
export function History({ name }) {
  const path = `/v1/templates/${encodeURIComponent(name)}`;
  const history = usePoll(() => api(path + '/history'), 15000, [name]);
  const h = history.data;

  return (
    <div class="card">
      <h3>File history {h?.currentVersion != null && <span class="muted small">current version v{h.currentVersion}</span>}</h3>
      <ErrorNote error={history.error} />
      {h && !h.records.length && <p class="muted">No versions yet.</p>}
      {h?.records.length > 0 && (
        <div class="table-wrap flush">
          <table>
            <thead><tr><th>Version</th><th>Time</th><th>Node</th><th>Change</th><th /></tr></thead>
            <tbody>
              {h.records.map((r) => (
                <tr key={r.version}>
                  <td>v{r.version}</td>
                  <td>{new Date(r.time).toLocaleString()}</td>
                  <td>{r.node}</td>
                  <td class="wrap-cell">
                    {r.note}
                    {r.deleted && <span class="badge bad">deleted</span>}
                    {(r.changed.length > 0 || r.removed.length > 0) && <div class="muted small">{[files(r.changed, '~'), files(r.removed, '-')].filter(Boolean).join(', ')}</div>}
                    {r.conflicts.length > 0 && <div class="error small">conflicts: {r.conflicts.join(', ')}</div>}
                  </td>
                  <td>
                    {r.version !== h.currentVersion && (
                      <ConfirmButton confirmText={`Roll back to v${r.version}?`}
                                     onConfirm={() => act(api(path + '/rollback', { method: 'POST', body: { version: r.version } })).then(() => setTimeout(history.reload, 1500))}>
                        Roll back
                      </ConfirmButton>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
