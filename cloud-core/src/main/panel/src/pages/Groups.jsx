import { useState } from 'preact/hooks';
import { api } from '../api.js';
import { ConfirmButton, ErrorNote } from '../components/ui.jsx';
import { act, usePoll } from '../util.js';
import { t } from '../i18n.js';

function GroupCard({ group, templates, onChange }) {
  const [adding, setAdding] = useState('');
  const path = '/v1/groups/' + encodeURIComponent(group.name);
  const free = templates.filter((t) => !group.templates.includes(t));
  const change = (method, names) => act(api(path + '/templates', { method, body: { templates: names } })).then(onChange);

  return (
    <div class="card stack-sm">
      <div class="row">
        <h3 class="grow">{group.name} <span class="muted small">{t('{n} players', { n: group.playerCount })}</span></h3>
        <ConfirmButton class="danger" confirmText={t('Delete group?')} onConfirm={() => act(api(path, { method: 'DELETE' })).then(onChange)}>{t('Delete')}</ConfirmButton>
      </div>
      <div class="chips">
        {group.templates.map((t) => (
          <span class="chip" key={t}>
            <a href={'#/templates/' + encodeURIComponent(t)}>{t}</a>
            <button class="chip-x" aria-label={`Remove ${t}`} onClick={() => change('DELETE', [t])}>×</button>
          </span>
        ))}
        {!group.templates.length && <span class="muted">{t('No templates.')}</span>}
      </div>
      {free.length > 0 && (
        <form class="row" onSubmit={(e) => { e.preventDefault(); change('POST', [adding || free[0]]); setAdding(''); }}>
          <select class="grow" value={adding || free[0]} onChange={(e) => setAdding(e.currentTarget.value)} aria-label={t('Template')}>
            {free.map((t) => <option value={t}>{t}</option>)}
          </select>
          <button>{t('Add template')}</button>
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
      <h1>{t('Groups')}</h1>
      <p class="muted">{t('Groups bundle templates, for example for player counts or for sending players to any server of a group.')}</p>
      <form class="card row wrap" onSubmit={create}>
        <strong>{t('New group')}</strong>
        <input class="grow" placeholder={t('Name')} value={name} onInput={(e) => setName(e.currentTarget.value)}
               pattern="[A-Za-z0-9_-]+" title={t('Letters, numbers, _ and -')} aria-label={t('Name')} required />
        <button class="primary" disabled={!name}>{t('Create')}</button>
      </form>
      <ErrorNote error={groups.error} />
      {groups.data && !list.length && <p class="muted">{t('No groups yet.')}</p>}
      <div class="grid two">
        {list.map((g) => <GroupCard key={g.name} group={g} templates={templateNames} onChange={groups.reload} />)}
      </div>
    </section>
  );
}
