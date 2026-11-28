import { useState } from 'preact/hooks';
import { api } from '../api.js';
import { ConfirmButton, ErrorNote } from '../components/ui.jsx';
import { act, bytes, usePoll } from '../util.js';

export function Backups({ session }) {
  const backups = usePoll(() => api('/v1/backups'), 15000);
  const [busy, setBusy] = useState(null);
  const run = async (what, promise) => {
    setBusy(what);
    await act(promise);
    setBusy(null);
    backups.reload();
  };

  return (
    <section class="stack">
      <div class="row wrap">
        <h1 class="grow">Backups</h1>
        <button class="primary" disabled={!!busy} onClick={() => run('create', api('/v1/backups', { method: 'POST', body: {} }))}>
          {busy === 'create' ? 'Creating…' : 'Create backup'}
        </button>
      </div>
      <p class="muted">
        Templates, groups and configs{session.clustered ? `, stored on ${session.node}` : ''}. A restore replaces templates
        and groups{session.clustered ? ' on all nodes' : ''} and saves the current state as a backup first.
      </p>
      <ErrorNote error={backups.error} />
      <div class="card table-wrap">
        <table>
          <thead><tr><th>Backup</th><th>Created</th><th>Size</th><th /></tr></thead>
          <tbody>
            {(backups.data || []).map((b) => (
              <tr key={b.name}>
                <td><code>{b.name}</code></td>
                <td>{new Date(b.time).toLocaleString()}</td>
                <td>{bytes(b.size)}</td>
                <td>
                  <ConfirmButton disabled={!!busy} confirmText="Replace current state?"
                                 onConfirm={() => run(b.name, api(`/v1/backups/${encodeURIComponent(b.name)}/restore`, { method: 'POST', body: {} }))}>
                    {busy === b.name ? 'Restoring…' : 'Restore'}
                  </ConfirmButton>
                </td>
              </tr>
            ))}
            {backups.data?.length === 0 && <tr><td colSpan="4" class="muted">No backups yet.</td></tr>}
          </tbody>
        </table>
      </div>
    </section>
  );
}
