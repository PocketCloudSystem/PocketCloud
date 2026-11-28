import { useEffect, useState } from 'preact/hooks';
import { api } from '../api.js';
import { Badge, ConfirmButton, ErrorNote } from '../components/ui.jsx';
import { History } from '../components/History.jsx';
import { navigate } from '../router.js';
import { act, usePoll } from '../util.js';

const FLAGS = [
  ['maintenance', 'Maintenance', 'Only players with the bypass can join.'],
  ['lobby', 'Lobby', 'Players join these servers first.'],
  ['autoStart', 'Auto start', 'Keep at least the minimum servers running.'],
  ['stopOnEmpty', 'Stop when empty', 'Stop a server once the last player left.'],
  ['staticServers', 'Static servers', 'Servers keep their files between starts.'],
  ['alwaysCopyToStaticServers', 'Always copy to static servers', 'Copy the template files on every start.'],
  ['saveOnShutdown', 'Save on shutdown', 'Copy the server files back into the template.'],
  ['deleteOnStop', 'Delete on stop', 'Remove the server folder after it stopped.'],
];

const NUMBERS = [
  ['minServerCount', 'Min servers', 0],
  ['maxServerCount', 'Max servers', 1],
  ['maxPlayerCount', 'Max players', 1],
  ['maxMemory', 'Memory (MB)', 1],
  ['startNewServerThreshold', 'Start new server at (0–1)', 0, 0.05],
  ['startupTimeout', 'Startup timeout (s)', 0],
  ['shutdownTimeout', 'Shutdown timeout (s)', 0],
  ['emptyServerGracePeriod', 'Empty grace period (s)', 0],
  ['priority', 'Priority', 0],
];

function Settings({ name, settings, nodes, onSaved }) {
  const [draft, setDraft] = useState(settings);
  useEffect(() => setDraft(settings), [JSON.stringify(settings)]);
  const changed = Object.fromEntries(Object.entries(draft).filter(([k, v]) => JSON.stringify(v) !== JSON.stringify(settings[k])));
  const set = (key, value) => setDraft({ ...draft, [key]: value });

  const save = async (e) => {
    e.preventDefault();
    if (await act(api('/v1/templates/' + encodeURIComponent(name), { method: 'PATCH', body: changed }))) onSaved();
  };

  return (
    <form class="card stack-sm" onSubmit={save}>
      <h3>Settings</h3>
      <div class="grid flags">
        {FLAGS.map(([key, label, hint]) => (
          <label class="check" key={key}>
            <input type="checkbox" checked={!!draft[key]} onChange={(e) => set(key, e.currentTarget.checked)} />
            <span>{label}<small class="muted">{hint}</small></span>
          </label>
        ))}
      </div>
      <div class="grid fields">
        {NUMBERS.map(([key, label, min, step]) => (
          <label class="field" key={key}>
            <span>{label}</span>
            <input type="number" min={min} max={key === 'startNewServerThreshold' ? 1 : undefined} step={step || 1}
                   value={draft[key]} onInput={(e) => set(key, e.currentTarget.valueAsNumber)} required />
          </label>
        ))}
      </div>
      {nodes && (
        <div class="grid fields">
          <fieldset class="field">
            <legend>Nodes (none = all)</legend>
            <div class="chips">
              {nodes.map((n) => (
                <label class="check inline" key={n}>
                  <input type="checkbox" checked={draft.nodes.includes(n)}
                         onChange={(e) => set('nodes', e.currentTarget.checked ? [...draft.nodes, n] : draft.nodes.filter((x) => x !== n))} />
                  {n}
                </label>
              ))}
            </div>
          </fieldset>
          <label class="field">
            <span>Static node</span>
            <select value={draft.staticNode} onChange={(e) => set('staticNode', e.currentTarget.value)}>
              <option value="">not set</option>
              {nodes.map((n) => <option value={n}>{n}</option>)}
            </select>
          </label>
        </div>
      )}
      <div class="row">
        <button class="primary" disabled={!Object.keys(changed).length}>Save changes</button>
        {Object.keys(changed).length > 0 && <button type="button" class="ghost" onClick={() => setDraft(settings)}>Reset</button>}
      </div>
    </form>
  );
}

export function Template({ name, session }) {
  const path = '/v1/templates/' + encodeURIComponent(name);
  const info = usePoll(() => api(path), 5000, [name]);
  const servers = usePoll(() => api('/v1/servers'), 5000);
  const nodes = usePoll(() => (session.clustered ? api('/v1/nodes') : null), 30000);
  const t = info.data;
  const own = Object.values(servers.data || {}).filter((s) => s.template === name)
    .sort((a, b) => a.name.localeCompare(b.name, undefined, { numeric: true }));

  const remove = async () => {
    if (await act(api(path, { method: 'DELETE' }))) navigate('/templates');
  };
  const start = () => act(api('/v1/servers/start', { method: 'POST', body: { template: name, count: 1 } })).then(servers.reload);

  return (
    <section class="stack">
      <div class="row wrap">
        <a href="#/templates" class="muted">Templates</a><span class="muted">/</span>
        <h1 class="grow">{name}</h1>
        {t && <span class="muted small">{t.templateType.toLowerCase()} · {t.serverSoftware}</span>}
        <button disabled={!t} onClick={start}>Start server</button>
        <ConfirmButton disabled={!t || !own.length} confirmText="Restart all, one by one?"
                       onConfirm={() => act(api(path + '/restart', { method: 'POST', body: {} }))}>Rolling restart</ConfirmButton>
        <ConfirmButton class="danger" disabled={!t} confirmText="Delete template?" onConfirm={remove}>Delete</ConfirmButton>
      </div>
      <ErrorNote error={info.error} />
      {t && (
        <div class="card">
          <h3>Servers <span class="muted small">{own.length} / {t.settings.maxServerCount} · {t.playerCount} players</span></h3>
          <div class="chips">
            {own.map((s) => (
              <a class="chip" href={'#/servers/' + encodeURIComponent(s.name)}>
                {s.name} <Badge status={s.status} />
              </a>
            ))}
            {!own.length && <span class="muted">No servers running.</span>}
          </div>
        </div>
      )}
      {t && <Settings name={name} settings={t.settings} onSaved={info.reload}
                      nodes={nodes.data?.nodes.map((n) => n.name).sort()} />}
      {t && session.clustered && <History name={name} />}
    </section>
  );
}
