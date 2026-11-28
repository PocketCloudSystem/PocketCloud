import { useState } from 'preact/hooks';
import { api } from '../api.js';
import { ErrorNote, Loading } from '../components/ui.jsx';
import { act, usePoll } from '../util.js';
import { t } from '../i18n.js';
import { ArrowLeftRight, Ban, MessageSquare } from 'lucide-preact';

const TEXT_TYPES = ['message', 'popup', 'tip', 'title', 'action_bar', 'toast'];

function PlayerActions({ player, servers, onDone }) {
  const [mode, setMode] = useState(null);
  const [text, setText] = useState('');
  const [type, setType] = useState('message');
  const [target, setTarget] = useState('');
  const path = '/v1/players/' + encodeURIComponent(player.name);

  const submit = async (e) => {
    e.preventDefault();
    const call = mode === 'text'
      ? api(path + '/text', { method: 'POST', body: { type, title: text, body: text } })
      : mode === 'kick'
        ? api(path + '/kick', { method: 'POST', body: { reason: text } })
        : api(path + '/transfer', { method: 'POST', body: { server: target || servers[0] } });
    if (await act(call)) { setMode(null); setText(''); onDone(); }
  };

  if (!mode) {
    return (
      <div class="row">
        <button onClick={() => setMode('text')}><MessageSquare size={15} />{t('Message')}</button>
        <button onClick={() => setMode('transfer')}><ArrowLeftRight size={15} />{t('Transfer')}</button>
        <button class="danger" onClick={() => setMode('kick')}><Ban size={15} />{t('Kick')}</button>
      </div>
    );
  }
  return (
    <form class="row wrap" onSubmit={submit}>
      {mode === 'text' && (
        <select value={type} onChange={(e) => setType(e.currentTarget.value)} aria-label={t('Type')}>
          {TEXT_TYPES.map((t) => <option value={t}>{t.replace('_', ' ')}</option>)}
        </select>
      )}
      {mode === 'transfer' ? (
        <select value={target} onChange={(e) => setTarget(e.currentTarget.value)} aria-label={t('Server')} class="grow">
          {servers.map((s) => <option value={s}>{s}</option>)}
        </select>
      ) : (
        <input class="grow" value={text} onInput={(e) => setText(e.currentTarget.value)} autofocus
               placeholder={mode === 'kick' ? t('Reason (optional)') : t('Text')} required={mode === 'text'} aria-label={t('Text')} />
      )}
      <button class={mode === 'kick' ? 'danger' : 'primary'}>{mode === 'kick' ? t('Kick') : mode === 'text' ? t('Send') : t('Transfer')}</button>
      <button type="button" class="ghost" onClick={() => setMode(null)}>{t('Cancel')}</button>
    </form>
  );
}

export function Players() {
  const players = usePoll(() => api('/v1/players'), 5000);
  const servers = usePoll(() => api('/v1/servers'), 15000);
  const [filter, setFilter] = useState('');
  const list = Object.values(players.data || {})
    .filter((p) => !filter || p.name.toLowerCase().includes(filter.toLowerCase()) || p.server?.toLowerCase().includes(filter.toLowerCase()))
    .sort((a, b) => a.name.localeCompare(b.name));
  const serverNames = Object.keys(servers.data || {}).sort();

  return (
    <section class="stack">
      <div class="row wrap">
        <h1 class="grow">{t('Players')} <span class="muted small">{t('{n} online', { n: Object.keys(players.data || {}).length })}</span></h1>
        <input type="search" placeholder={t('Filter by name or server')} value={filter} onInput={(e) => setFilter(e.currentTarget.value)} class="search" aria-label={t('Filter')} />
      </div>
      <ErrorNote error={players.error} />
      <Loading poll={players} />
      <div class="card table-wrap">
        <table>
          <thead><tr><th>{t('Name')}</th><th>{t('Server')}</th><th>{t('Proxy')}</th><th /></tr></thead>
          <tbody>
            {list.map((p) => (
              <tr key={p.name}>
                <td>{p.name}</td>
                <td>{p.server ? <a href={'#/servers/' + encodeURIComponent(p.server)}>{p.server}</a> : '–'}</td>
                <td class="muted">{p.proxy || '–'}</td>
                <td><PlayerActions player={p} servers={serverNames} onDone={players.reload} /></td>
              </tr>
            ))}
            {players.data && !list.length && <tr><td colSpan="4" class="muted">{filter ? t('No player matches the filter.') : t('Nobody is online.')}</td></tr>}
          </tbody>
        </table>
      </div>
    </section>
  );
}
