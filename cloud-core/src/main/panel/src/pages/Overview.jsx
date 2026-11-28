import { api } from '../api.js';
import { Badge, ErrorNote, Meter, Stat } from '../components/ui.jsx';
import { Chart } from '../components/Chart.jsx';
import { bytes, duration, pct, usePoll } from '../util.js';

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
      <h1>Overview</h1>
      <ErrorNote error={stats.error} />
      <div class="grid stats">
        <Stat label="Players" value={s?.player_count ?? '–'} />
        <Stat label="Servers" value={s ? list.length || s.server_count : '–'} sub={servers.data ? `${online} online` : null} />
        <Stat label="Templates" value={s?.template_count ?? '–'} />
        <Stat label="Uptime" value={duration(s?.uptime_ms)} sub={s ? `v${s.version}${s.beta ? ' beta' : ''}` : null} />
      </div>

      <div class="grid charts">
        <Chart title="Players" points={series(1)} />
        <Chart title="Servers" points={series(2)} />
        <Chart title={`CPU${session.clustered ? ` (${session.node})` : ''}`} points={series(3)} format={(v) => `${Math.round(v)} %`} max={100} />
        <Chart title={`Memory${session.clustered ? ` (${session.node})` : ''}`} points={series(4)} format={bytes} />
      </div>

      <div class="card">
        <h3>This cloud process{session.clustered ? ` (${session.node})` : ''}</h3>
        <div class="kv">
          <span>CPU</span><span>{pct(s?.cpu_usage)}</span>
          <span>Memory</span>
          <span class="meter-row">
            <Meter value={s?.used_memory} max={s?.max_memory} label="Memory" />
            {bytes(s?.used_memory)} / {bytes(s?.max_memory)}
          </span>
          <span>TPS</span><span>{s ? `${s.tps.toFixed(1)} (avg ${s.avg_tps.toFixed(1)})` : '–'}</span>
          <span>Traffic</span><span>{s ? `${bytes(s.total_traffic.in)} in · ${bytes(s.total_traffic.out)} out` : '–'}</span>
        </div>
      </div>

      {session.clustered && nodes.data && (
        <div class="card">
          <h3>Nodes <span class="muted small">head: {nodes.data.head || 'none'}</span></h3>
          <div class="table-wrap">
            <table>
              <thead><tr><th>Node</th><th>State</th><th>Servers</th><th>CPU</th><th>Memory</th></tr></thead>
              <tbody>
                {nodes.data.nodes.map((n) => (
                  <tr key={n.name}>
                    <td><a href="#/nodes">{n.name}</a>{n.drain && <span class="badge warn">drain</span>}</td>
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
