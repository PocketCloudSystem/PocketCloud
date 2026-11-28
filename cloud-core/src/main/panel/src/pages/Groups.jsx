import { useState } from 'preact/hooks';
import { api } from '../api.js';
import { ConfirmButton, ErrorNote } from '../components/ui.jsx';
import { act, usePoll } from '../util.js';

function GroupCard({ group, templates, onChange }) {
  const [adding, setAdding] = useState('');
  const path = '/v1/groups/' + encodeURIComponent(group.name);
  const free = templates.filter((t) => !group.templates.includes(t));
  const change = (method, names) => act(api(path + '/templates', { method, body: { templates: names } })).then(onChange);

  return (
    <div class="card stack-sm">
      <div class="row">
        <h3 class="grow">{group.name} <span class="muted small">{group.playerCount} players</span></h3>
        <ConfirmButton class="danger" confirmText="Delete group?" onConfirm={() => act(api(path, { method: 'DELETE' })).then(onChange)}>Delete</ConfirmButton>
      </div>
      <div class="chips">
        {group.templates.map((t) => (
          <span class="chip" key={t}>
            <a href={'#/templates/' + encodeURIComponent(t)}>{t}</a>
            <button class="chip-x" aria-label={`Remove ${t}`} onClick={() => change('DELETE', [t])}>×</button>
          </span>
        ))}
        {!group.templates.length && <span class="muted">No templates.</span>}
      </div>
      {free.length > 0 && (
        <form class="row" onSubmit={(e) => { e.preventDefault(); change('POST', [adding || free[0]]); setAdding(''); }}>
          <select class="grow" value={adding || free[0]} onChange={(e) => setAdding(e.currentTarget.value)} aria-label="Template">
            {free.map((t) => <option value={t}>{t}</option>)}
          </select>
          <button>Add template</button>
        </form>
      )}
    </div>
  );
}

export function Groups() {
  const groups = usePoll(() => api('/v1/groups'), 10000);
  const templates = usePoll(() => api('/v1/templates'), 30000);
  const [name, setName] = useState('');
  const list = Object.values(groups.data || {}).sort((a, b) => a.name.localeCompare(b.name));
  const templateNames = Object.keys(templates.data || {}).sort();

  const create = async (e) => {
    e.preventDefault();
    if (await act(api('/v1/groups', { method: 'POST', body: { name, templates: [] } }))) { setName(''); groups.reload(); }
  };

  return (
    <section class="stack">
      <h1>Groups</h1>
      <p class="muted">Groups bundle templates, for example for player counts or for sending players to any server of a group.</p>
      <form class="card row wrap" onSubmit={create}>
        <strong>New group</strong>
        <input class="grow" placeholder="Name" value={name} onInput={(e) => setName(e.currentTarget.value)}
               pattern="[A-Za-z0-9_-]+" title="Letters, numbers, _ and -" aria-label="Name" required />
        <button class="primary" disabled={!name}>Create</button>
      </form>
      <ErrorNote error={groups.error} />
      {groups.data && !list.length && <p class="muted">No groups yet.</p>}
      <div class="grid two">
        {list.map((g) => <GroupCard key={g.name} group={g} templates={templateNames} onChange={groups.reload} />)}
      </div>
    </section>
  );
}
