import { useState } from 'preact/hooks';
import { api } from '../api.js';
import { Badge, ErrorNote } from '../components/ui.jsx';
import { navigate } from '../router.js';
import { act, usePoll } from '../util.js';

function CreateForm({ onDone }) {
  const softwares = usePoll(() => api('/panel-api/softwares'), 60000);
  const [name, setName] = useState('');
  const [software, setSoftware] = useState('');
  const list = softwares.data || [];
  const chosen = list.find((s) => s.name === software) || list[0];

  const create = async (e) => {
    e.preventDefault();
    const res = await act(api('/v1/templates', { method: 'POST', body: { name, templateType: chosen.templateType, serverSoftware: chosen.name } }));
    if (res) navigate('/templates/' + encodeURIComponent(name));
  };

  return (
    <form class="card row wrap" onSubmit={create}>
      <strong>New template</strong>
      <input class="grow" placeholder="Name" value={name} onInput={(e) => setName(e.currentTarget.value)}
             pattern="[A-Za-z0-9_-]+" title="Letters, numbers, _ and -" aria-label="Name" required />
      <select value={chosen?.name} onChange={(e) => setSoftware(e.currentTarget.value)} aria-label="Software">
        {list.map((s) => <option value={s.name}>{s.name} ({s.templateType.toLowerCase()})</option>)}
      </select>
      <button class="primary" disabled={!name || !chosen}>Create</button>
      <button type="button" class="ghost" onClick={onDone}>Cancel</button>
    </form>
  );
}

export function Templates() {
  const templates = usePoll(() => api('/v1/templates'), 5000);
  const [creating, setCreating] = useState(false);
  const list = Object.values(templates.data || {}).sort((a, b) => a.name.localeCompare(b.name));

  return (
    <section class="stack">
      <div class="row">
        <h1 class="grow">Templates</h1>
        {!creating && <button class="primary" onClick={() => setCreating(true)}>New template</button>}
      </div>
      {creating && <CreateForm onDone={() => setCreating(false)} />}
      <ErrorNote error={templates.error} />
      <div class="card table-wrap">
        <table>
          <thead><tr><th>Name</th><th>Servers</th><th>Players</th><th>Software</th><th /></tr></thead>
          <tbody>
            {list.map((t) => (
              <tr key={t.name}>
                <td><a href={'#/templates/' + encodeURIComponent(t.name)}>{t.name}</a></td>
                <td>{t.serverCount} / {t.maxServers}</td>
                <td>{t.playerCount} / {t.maxPlayers}</td>
                <td class="muted">{t.software}</td>
                <td>
                  {t.lobby && <Badge status="ONLINE">lobby</Badge>}
                  {t.maintenance && <Badge status="PENDING">maintenance</Badge>}
                </td>
              </tr>
            ))}
            {templates.data && !list.length && <tr><td colSpan="5" class="muted">No templates yet.</td></tr>}
          </tbody>
        </table>
      </div>
    </section>
  );
}
