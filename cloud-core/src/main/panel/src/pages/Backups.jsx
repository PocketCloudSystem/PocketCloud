import { useState } from 'preact/hooks';
import { api } from '../api.js';
import { ConfirmButton, ErrorNote, Loading } from '../components/ui.jsx';
import { act, bytes, usePoll } from '../util.js';
import { t } from '../i18n.js';

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
        <h1 class="grow">{t('Backups')}</h1>
        <button class="primary" disabled={!!busy} onClick={() => run('create', api('/v1/backups', { method: 'POST', body: {} }))}>
          {busy === 'create' ? t('Creating…') : t('Create backup')}
        </button>
      </div>
      <p class="muted">
        {session.clustered ? t('Templates, groups and configs, stored on {node}. A restore replaces templates and groups on all nodes and saves the current state as a backup first.', { node: session.node })
          : t('Templates, groups and configs. A restore replaces templates and groups and saves the current state as a backup first.')}
      </p>
      <ErrorNote error={backups.error} />
      <Loading poll={backups} />
      <div class="card table-wrap">
        <table>
          <thead><tr><th>{t('Backup')}</th><th>{t('Created')}</th><th>{t('Size')}</th><th /></tr></thead>
          <tbody>
            {(backups.data || []).map((b) => (
              <tr key={b.name}>
                <td><code>{b.name}</code></td>
                <td>{new Date(b.time).toLocaleString()}</td>
                <td>{bytes(b.size)}</td>
                <td>
                  <ConfirmButton disabled={!!busy} confirmText={t('Replace current state?')}
                                 onConfirm={() => run(b.name, api(`/v1/backups/${encodeURIComponent(b.name)}/restore`, { method: 'POST', body: {} }))}>
                    {busy === b.name ? t('Restoring…') : t('Restore')}
                  </ConfirmButton>
                </td>
              </tr>
            ))}
            {backups.data?.length === 0 && <tr><td colSpan="4" class="muted">{t('No backups yet.')}</td></tr>}
          </tbody>
        </table>
      </div>
    </section>
  );
}
