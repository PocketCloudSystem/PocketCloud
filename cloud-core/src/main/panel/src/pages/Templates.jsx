import { useState } from 'preact/hooks';
import { api } from '../api.js';
import { Badge, ErrorNote, Loading } from '../components/ui.jsx';
import { navigate } from '../router.js';
import { act, usePoll } from '../util.js';
import { t } from '../i18n.js';
import { Plus } from 'lucide-preact';

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
      <strong>{t('New template')}</strong>
      <input class="grow" placeholder={t('Name')} value={name} onInput={(e) => setName(e.currentTarget.value)}
             pattern="[A-Za-z0-9_-]+" title={t('Letters, numbers, _ and -')} aria-label={t('Name')} required />
      <select value={chosen?.name} onChange={(e) => setSoftware(e.currentTarget.value)} aria-label={t('Software')}>
        {list.map((s) => <option value={s.name}>{s.name} ({s.templateType.toLowerCase()})</option>)}
      </select>
      <button class="primary" disabled={!name || !chosen}><Plus size={15} />{t('Create')}</button>
      <button type="button" class="ghost" onClick={onDone}>{t('Cancel')}</button>
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
        <h1 class="grow">{t('Templates')}</h1>
        {!creating && <button class="primary" onClick={() => setCreating(true)}><Plus size={15} />{t('New template')}</button>}
      </div>
      {creating && <CreateForm onDone={() => setCreating(false)} />}
      <ErrorNote error={templates.error} />
      <Loading poll={templates} />
      <div class="card table-wrap">
        <table>
          <thead><tr><th>{t('Name')}</th><th>{t('Servers')}</th><th>{t('Players')}</th><th>{t('Software')}</th><th /></tr></thead>
          <tbody>
            {list.map((tpl) => (
              <tr key={tpl.name}>
                <td><a href={'#/templates/' + encodeURIComponent(tpl.name)}>{tpl.name}</a></td>
                <td>{tpl.serverCount} / {tpl.maxServers}</td>
                <td>{tpl.playerCount} / {tpl.maxPlayers}</td>
                <td class="muted">{tpl.software}</td>
                <td>
                  {tpl.lobby && <Badge status="ONLINE">{t('lobby')}</Badge>}
                  {tpl.maintenance && <Badge status="PENDING">{t('maintenance')}</Badge>}
                </td>
              </tr>
            ))}
            {templates.data && !list.length && <tr><td colSpan="5" class="muted">{t('No templates yet.')}</td></tr>}
          </tbody>
        </table>
      </div>
    </section>
  );
}
