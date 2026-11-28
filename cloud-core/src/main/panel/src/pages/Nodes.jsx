import { api } from '../api.js';
import { Badge, ConfirmButton, ErrorNote, Meter } from '../components/ui.jsx';
import { act, duration, pct, usePoll } from '../util.js';
import { t } from '../i18n.js';

export function Nodes() {
  const nodes = usePoll(() => api('/v1/nodes'), 3000);
  const d = nodes.data;

  const drain = (n, on) => act(api(`/v1/nodes/${encodeURIComponent(n.name)}/${on ? 'drain' : 'undrain'}`, { method: 'POST', body: {} })).then(nodes.reload);

  return (
    <section class="stack">
      <div class="row wrap">
        <h1 class="grow">{t('Nodes')}</h1>
        {d && <span class="muted">{t('head')}: <strong>{d.head || t('none')}</strong> · term {d.term}</span>}
      </div>
      <ErrorNote error={nodes.error} />
      {d && !d.clustered && <p class="muted">{t('This cloud runs without a cluster.')}</p>}
      <div class="grid two">
        {d?.nodes.map((n) => (
          <div class="card stack-sm" key={n.name}>
            <div class="row">
              <h3 class="grow">{n.name}{n.local && <span class="muted small"> ({t('this node')})</span>}</h3>
              <Badge status={n.health === 'ALIVE' ? n.state : n.health}>{(n.health === 'ALIVE' ? n.state : n.health).toLowerCase()}</Badge>
              {n.drain && <Badge status="PENDING">{t('drain')}</Badge>}
            </div>
            <div class="kv">
              <span>{t('Address')}</span><span><code>{n.address}:{n.port}</code></span>
              <span>{t('Servers')}</span><span>{n.servers}</span>
              <span>{t('CPU')}</span>
              <span class="meter-row"><Meter value={n.cpu ?? 0} max={100} label={t('CPU')} />{pct(n.cpu)}{n.cpuLimit ? <span class="muted small">{t('limit')} {n.cpuLimit} %</span> : null}</span>
              <span>{t('Memory')}</span>
              <span class="meter-row">
                {n.maxMemoryMb > 0 && <Meter value={n.usedMemoryMb} max={n.maxMemoryMb} label={t('Memory')} />}
                {n.usedMemoryMb} MB{n.maxMemoryMb > 0 ? ` / ${n.maxMemoryMb} MB` : ` (${t('no limit')})`}
              </span>
              <span>{t('Version')}</span><span>{n.version || '–'}</span>
              <span>{t('Uptime')}</span><span>{n.startedAt ? duration(Date.now() - n.startedAt) : '–'}</span>
              {n.softwares && <><span>{t('Software')}</span><span class="muted small">{n.softwares.join(', ')}</span></>}
            </div>
            <div class="row">
              {n.drain
                ? <button onClick={() => drain(n, false)}>{t('Undrain')}</button>
                : <ConfirmButton disabled={n.health !== 'ALIVE'} confirmText={t('Move servers away?')} onConfirm={() => drain(n, true)}>{t('Drain')}</ConfirmButton>}
            </div>
          </div>
        ))}
      </div>
    </section>
  );
}
