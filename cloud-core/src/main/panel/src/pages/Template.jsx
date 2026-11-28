import { useEffect, useState } from 'preact/hooks';
import { api } from '../api.js';
import { Badge, ConfirmButton, ErrorNote, Loading } from '../components/ui.jsx';
import { History } from '../components/History.jsx';
import { FileBrowser } from '../components/FileBrowser.jsx';
import { navigate } from '../router.js';
import { act, usePoll } from '../util.js';
import { t } from '../i18n.js';
import { Play, RotateCw, Save, Trash2 } from 'lucide-preact';

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
      <h3>{t('Settings')}</h3>
      <div class="grid flags">
        {FLAGS.map(([key, label, hint]) => (
          <label class="check" key={key}>
            <input type="checkbox" checked={!!draft[key]} onChange={(e) => set(key, e.currentTarget.checked)} />
            <span>{t(label)}<small class="muted">{t(hint)}</small></span>
          </label>
        ))}
      </div>
      <div class="grid fields">
        {NUMBERS.map(([key, label, min, step]) => (
          <label class="field" key={key}>
            <span>{t(label)}</span>
            <input type="number" min={min} max={key === 'startNewServerThreshold' ? 1 : undefined} step={step || 1}
                   value={draft[key]} onInput={(e) => set(key, e.currentTarget.valueAsNumber)} required />
          </label>
        ))}
      </div>
      {nodes && (
        <div class="grid fields">
          <fieldset class="field">
            <legend>{t('Nodes (none = all)')}</legend>
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
            <span>{t('Static node')}</span>
            <select value={draft.staticNode} onChange={(e) => set('staticNode', e.currentTarget.value)}>
              <option value="">{t('not set')}</option>
              {nodes.map((n) => <option value={n}>{n}</option>)}
            </select>
          </label>
        </div>
      )}
      <div class="row">
        <button class="primary" disabled={!Object.keys(changed).length}><Save size={15} />{t('Save changes')}</button>
        {Object.keys(changed).length > 0 && <button type="button" class="ghost" onClick={() => setDraft(settings)}>{t('Reset')}</button>}
      </div>
    </form>
  );
}

export function Template({ name, session }) {
  const path = '/v1/templates/' + encodeURIComponent(name);
  const info = usePoll(() => api(path), 5000, [name]);
  const servers = usePoll(() => api('/v1/servers'), 5000);
  const nodes = usePoll(() => (session.clustered ? api('/v1/nodes') : null), 30000);
  const tpl = info.data;
  const own = Object.values(servers.data || {}).filter((s) => s.template === name)
    .sort((a, b) => a.name.localeCompare(b.name, undefined, { numeric: true }));

  const remove = async () => {
    if (await act(api(path, { method: 'DELETE' }))) navigate('/templates');
  };
  const start = () => act(api('/v1/servers/start', { method: 'POST', body: { template: name, count: 1 } })).then(servers.reload);

  return (
    <section class="stack">
      <div class="row wrap">
        <a href="#/templates" class="muted">{t('Templates')}</a><span class="muted">/</span>
        <h1 class="grow">{name}</h1>
        {tpl && <span class="muted small">{tpl.templateType.toLowerCase()} · {tpl.serverSoftware}</span>}
        <button disabled={!tpl} onClick={start}><Play size={15} />{t('Start server')}</button>
        <ConfirmButton disabled={!tpl || !own.length} confirmText={t('Restart all, one by one?')}
                       onConfirm={() => act(api(path + '/restart', { method: 'POST', body: {} }))}><RotateCw size={15} />{t('Rolling restart')}</ConfirmButton>
        <ConfirmButton class="danger" disabled={!tpl} confirmText={t('Delete template?')} onConfirm={remove}><Trash2 size={15} />{t('Delete')}</ConfirmButton>
      </div>
      <ErrorNote error={info.error} />
      <Loading poll={info} />
      {tpl && (
        <div class="card">
          <h3>{t('Servers')} <span class="muted small">{own.length} / {tpl.settings.maxServerCount} · {t('{n} players', { n: tpl.playerCount })}</span></h3>
          <div class="chips">
            {own.map((s) => (
              <a class="chip" href={'#/servers/' + encodeURIComponent(s.name)}>
                {s.name} <Badge status={s.status} />
              </a>
            ))}
            {!own.length && <span class="muted">{t('No servers running.')}</span>}
          </div>
        </div>
      )}
      {tpl && <Settings name={name} settings={tpl.settings} onSaved={info.reload}
                      nodes={nodes.data?.nodes.map((n) => n.name).sort()} />}
      {tpl && <FileBrowser api={path + '/files'} title={t('Template files')} writable />}
      {tpl && session.clustered && <History name={name} />}
    </section>
  );
}
