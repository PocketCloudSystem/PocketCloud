import { useState } from 'preact/hooks';
import { api } from '../api.js';
import { ConfirmButton, ErrorNote } from '../components/ui.jsx';
import { act, usePoll } from '../util.js';
import { t } from '../i18n.js';

const EXAMPLES = [
  ['0 4 * * *', 'every day at 04:00'],
  ['*/30 * * * *', 'every 30 minutes'],
  ['0 6 * * 1', 'Mondays at 06:00'],
];

export function Schedules() {
  const schedules = usePoll(() => api('/v1/schedules'), 10000);
  const [cron, setCron] = useState('');
  const [command, setCommand] = useState('');

  const add = async (e) => {
    e.preventDefault();
    if (await act(api('/v1/schedules', { method: 'POST', body: { cron, command } }))) {
      setCron('');
      setCommand('');
      schedules.reload();
    }
  };

  return (
    <section class="stack">
      <h1>{t('Schedules')}</h1>
      <p class="muted">{t('Console commands that run at cron times (minute hour day month weekday). In a cluster the head runs them.')}</p>
      <form class="card stack-sm" onSubmit={add}>
        <div class="grid fields">
          <label class="field">
            <span>{t('Cron')}</span>
            <input value={cron} onInput={(e) => setCron(e.currentTarget.value)} placeholder="0 4 * * *" required class="mono" />
          </label>
          <label class="field wide">
            <span>{t('Command')}</span>
            <input value={command} onInput={(e) => setCommand(e.currentTarget.value)} placeholder="template restart Lobby" required class="mono" />
          </label>
        </div>
        <div class="row wrap">
          <button class="primary">{t('Add schedule')}</button>
          {EXAMPLES.map(([c, label]) => <button type="button" class="ghost small" onClick={() => setCron(c)}><code>{c}</code> {label}</button>)}
        </div>
      </form>
      <ErrorNote error={schedules.error} />
      <div class="card table-wrap">
        <table>
          <thead><tr><th>{t('Id')}</th><th>{t('Cron')}</th><th>{t('Command')}</th><th /></tr></thead>
          <tbody>
            {(schedules.data || []).map((s) => (
              <tr key={s.id}>
                <td class="muted">{s.id}</td>
                <td><code>{s.cron}</code></td>
                <td><code>{s.command}</code></td>
                <td>
                  <ConfirmButton class="danger" confirmText={t('Remove?')}
                                 onConfirm={() => act(api('/v1/schedules/' + encodeURIComponent(s.id), { method: 'DELETE' })).then(schedules.reload)}>
                    {t('Remove')}
                  </ConfirmButton>
                </td>
              </tr>
            ))}
            {schedules.data?.length === 0 && <tr><td colSpan="4" class="muted">{t('No scheduled commands.')}</td></tr>}
          </tbody>
        </table>
      </div>
    </section>
  );
}
