import { useState } from 'preact/hooks';
import { api } from '../api.js';
import { Badge, ConfirmButton, ErrorNote } from '../components/ui.jsx';
import { act, usePoll } from '../util.js';

function StartForm({ onStarted }) {
  const templates = usePoll(() => api('/v1/templates'), 15000);
  const [template, setTemplate] = useState('');
  const [count, setCount] = useState(1);
  const [busy, setBusy] = useState(false);
  const names = Object.keys(templates.data || {}).sort();
  const chosen = template || names[0] || '';

  const start = async (e) => {
    e.preventDefault();
    setBusy(true);
    await act(api('/v1/servers/start', { method: 'POST', body: { template: chosen, count: Number(count) } }));
    setBusy(false);
    onStarted();
  };

  return (
    <form class="card row wrap" onSubmit={start}>
      <strong>Start servers</strong>
      <select value={chosen} onChange={(e) => setTemplate(e.currentTarget.value)} aria-label="Template" class="grow">
        {names.map((n) => <option value={n}>{n}</option>)}
      </select>
      <input type="number" min="1" max="50" value={count} onInput={(e) => setCount(e.currentTarget.value)} aria-label="Count" class="narrow" />
      <button class="primary" disabled={busy || !chosen}>Start</button>
    </form>
  );
}

export function Servers() {
  const servers = usePoll(() => api('/v1/servers'), 3000);
  const [filter, setFilter] = useState('');
  const list = Object.values(servers.data || {})
    .filter((s) => !filter || s.name.toLowerCase().includes(filter.toLowerCase()) || s.node?.includes(filter))
    .sort((a, b) => a.name.localeCompare(b.name, undefined, { numeric: true }));

  return (
    <section class="stack">
      <div class="row wrap">
        <h1 class="grow">Servers</h1>
        <input type="search" placeholder="Filter by name or node" value={filter} onInput={(e) => setFilter(e.currentTarget.value)} class="search" aria-label="Filter" />
        <ConfirmButton class="danger" confirmText="Stop all servers?" onConfirm={() => act(api('/v1/servers/stopAll', { method: 'POST', body: {} })).then(servers.reload)}>Stop all</ConfirmButton>
      </div>
      <StartForm onStarted={servers.reload} />
      <ErrorNote error={servers.error} />
      <div class="card table-wrap">
        <table>
          <thead><tr><th>Name</th><th>Status</th><th>Players</th><th>Node</th></tr></thead>
          <tbody>
            {list.map((s) => (
              <tr key={s.uuid}>
                <td><a href={'#/servers/' + encodeURIComponent(s.name)}>{s.name}</a></td>
                <td><Badge status={s.status} /></td>
                <td>{s.playerCount} / {s.maxPlayers}</td>
                <td>{s.node || '–'}</td>
              </tr>
            ))}
            {servers.data && !list.length && <tr><td colSpan="4" class="muted">No servers{filter ? ' match the filter' : ' are running'}.</td></tr>}
          </tbody>
        </table>
      </div>
    </section>
  );
}
