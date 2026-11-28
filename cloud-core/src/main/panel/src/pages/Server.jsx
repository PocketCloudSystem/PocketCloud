import { api } from '../api.js';
import { Console } from '../components/Console.jsx';
import { FileBrowser } from '../components/FileBrowser.jsx';
import { Badge, ConfirmButton, ErrorNote, Meter } from '../components/ui.jsx';
import { navigate } from '../router.js';
import { act, bytes, duration, pct, usePoll } from '../util.js';
import { t } from '../i18n.js';

export function Server({ name }) {
  const path = `/v1/servers/${encodeURIComponent(name)}`;
  const info = usePoll(() => api(path), 3000, [name]);
  const s = info.data;
  const d = s?.data || {};
  const gone = info.error?.status === 404;

  const stop = (force) => act(api(path + '/stop', { method: 'POST', body: { force } })).then(info.reload);

  return (
    <section class="stack">
      <div class="row wrap">
        <a href="#/servers" class="muted">{t('Servers')}</a><span class="muted">/</span>
        <h1 class="grow">{name}</h1>
        {s && <Badge status={s.status} />}
        <button disabled={!s} onClick={() => act(api(path + '/save', { method: 'POST', body: {} }))}>{t('Save')}</button>
        <ConfirmButton disabled={!s} confirmText={t('Stop server?')} onConfirm={() => stop(false)}>{t('Stop')}</ConfirmButton>
        <ConfirmButton class="danger" disabled={!s} confirmText={t('Kill process?')} onConfirm={() => stop(true)}>{t('Force stop')}</ConfirmButton>
      </div>
      {gone ? (
        <div class="card"><p>{t('This server is not running anymore.')}</p><button onClick={() => navigate('/servers')}>{t('Back to servers')}</button></div>
      ) : <ErrorNote error={info.error} />}

      {s && (
        <div class="grid two">
          <div class="card kv">
            <span>{t('Template')}</span><span>{s.templateName}</span>
            <span>{t('Node')}</span><span>{s.node || '–'}</span>
            <span>{t('Address')}</span><span><code>{d.address}:{d.port}</code></span>
            <span>{t('Players')}</span><span>{s.playerCount} / {d.maxPlayers}</span>
            <span>{t('Uptime')}</span><span>{duration(s.startTime ? Date.now() - s.startTime : null)}</span>
          </div>
          <div class="card kv">
            <span>{t('TPS')}</span><span>{d.tps != null ? `${d.tps.toFixed(1)} (avg ${(d.avgTps ?? 0).toFixed(1)})` : '–'}</span>
            <span>{t('CPU')}</span><span>{pct(d.cpuUsage)}</span>
            <span>{t('Memory')}</span>
            <span class="meter-row">
              <Meter value={d.memoryUsage} max={d.memoryLimit} label={t('Memory')} />
              {bytes(d.memoryUsage)}{d.memoryLimit > 0 ? ` / ${bytes(d.memoryLimit)}` : ''}
            </span>
            <span>{t('Peak')}</span><span>{bytes(d.memoryPeak)}</span>
            <span>{t('PID')}</span><span>{d.processId ?? '–'}</span>
          </div>
        </div>
      )}

      {s?.players?.length > 0 && (
        <div class="card">
          <h3>{t('Players')}</h3>
          <div class="chips">{s.players.map((p) => <a class="chip" href={'#/players/' + encodeURIComponent(p.name || p)}>{p.name || p}</a>)}</div>
        </div>
      )}

      {!gone && <Console stream={`/panel-api/servers/${encodeURIComponent(name)}/console`}
                          run={(command) => api(path + '/dispatch', { method: 'POST', body: { command } })} />}
      {s && <FileBrowser api={path + '/files'} title={t('Server files (read only)')} />}
    </section>
  );
}
