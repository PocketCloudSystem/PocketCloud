import { api } from '../api.js';
import { Badge, ErrorNote, Meter, Stat } from '../components/ui.jsx';
import { Chart } from '../components/Chart.jsx';
import { bytes, duration, pct, usePoll } from '../util.js';
import { t } from '../i18n.js';
import { Clock, Cpu, Layers, Network, Server, Users } from 'lucide-preact';

export function Overview({ session }) {
  const stats = usePoll(() => api('/v1/stats'), 5000);
  const nodes = usePoll(() => (session.clustered ? api('/v1/nodes') : null), 5000);
  const servers = usePoll(() => api('/v1/servers'), 5000);
  const history = usePoll(() => api('/v1/stats/history'), 60000);
  const series = (i) => (history.data?.points || []).map((p) => [p[0], p[i]]);
  const s = stats.data;
  const list = Object.values(servers.data || {});
  const online = list.filter((x) => x.status === 'ONLINE').length;

  return (
    <section class="stack">
      <h1>{t('Overview')}</h1>
      <ErrorNote error={stats.error} />
      <div class="grid stats">
        <Stat icon={Users} label={t('Players')} value={s?.player_count ?? '–'} />
        <Stat icon={Server} label={t('Servers')} value={s ? list.length || s.server_count : '–'} sub={servers.data ? t('{n} online', { n: online }) : null} />
        <Stat icon={Layers} label={t('Templates')} value={s?.template_count ?? '–'} />
        <Stat icon={Clock} label={t('Uptime')} value={duration(s?.uptime_ms)} sub={s ? `v${s.version}${s.beta ? ' beta' : ''}` : null} />
      </div>

      <div class="grid charts">
        <Chart title={t('Players')} points={series(1)} />
        <Chart title={t('Servers')} points={series(2)} />
        <Chart title={`${t('CPU')}${session.clustered ? ` (${session.node})` : ''}`} points={series(3)} format={(v) => `${Math.round(v)} %`} max={100} />
        <Chart title={`${t('Memory')}${session.clustered ? ` (${session.node})` : ''}`} points={series(4)} format={bytes} />
      </div>

      <div class="card">
        <h3><Cpu size={16} class="h-icon" />{t('This cloud process')}{session.clustered ? ` (${session.node})` : ''}</h3>
        <div class="kv">
          <span>{t('CPU')}</span><span>{pct(s?.cpu_usage)}</span>
          <span>{t('Memory')}</span>
          <span class="meter-row">
            <Meter value={s?.used_memory} max={s?.max_memory} label={t('Memory')} />
            {bytes(s?.used_memory)} / {bytes(s?.max_memory)}
          </span>
          <span>{t('TPS')}</span><span>{s ? `${s.tps.toFixed(1)} (avg ${s.avg_tps.toFixed(1)})` : '–'}</span>
          <span>{t('Traffic')}</span><span>{s ? `${bytes(s.total_traffic.in)} in · ${bytes(s.total_traffic.out)} out` : '–'}</span>
        </div>
      </div>

      {session.clustered && nodes.data && (
        <div class="card">
          <h3><Network size={16} class="h-icon" />{t('Nodes')} <span class="muted small">{t('head')}: {nodes.data.head || t('none')}</span></h3>
          <div class="table-wrap">
            <table>
              <thead><tr><th>{t('Node')}</th><th>{t('State')}</th><th>{t('Servers')}</th><th>{t('CPU')}</th><th>{t('Memory')}</th></tr></thead>
              <tbody>
                {nodes.data.nodes.map((n) => (
                  <tr key={n.name}>
                    <td><a href="#/nodes">{n.name}</a>{n.drain && <span class="badge warn">{t('drain')}</span>}</td>
                    <td><Badge status={n.health === 'ALIVE' ? n.state : n.health}>{n.health === 'ALIVE' ? n.state.toLowerCase() : n.health.toLowerCase()}</Badge></td>
                    <td>{n.servers}</td>
                    <td>{pct(n.cpu)}</td>
                    <td>{n.usedMemoryMb} MB{n.maxMemoryMb > 0 ? ` / ${n.maxMemoryMb} MB` : ''}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}
    </section>
  );
}
