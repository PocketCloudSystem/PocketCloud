import { useState } from 'preact/hooks';
import { api } from '../api.js';
import { ErrorNote } from '../components/ui.jsx';
import { act, usePoll } from '../util.js';

export function Maintenance() {
  const list = usePoll(() => api('/v1/maintenance'), 10000);
  const templates = usePoll(() => api('/v1/templates'), 10000);
  const [player, setPlayer] = useState('');
  const inMaintenance = Object.values(templates.data || {}).filter((t) => t.maintenance).map((t) => t.name).sort();

  const add = async (e) => {
    e.preventDefault();
    if (await act(api('/v1/maintenance', { method: 'POST', body: { player } }))) { setPlayer(''); list.reload(); }
  };
  const remove = (p) => act(api('/v1/maintenance', { method: 'DELETE', body: { player: p } })).then(list.reload);

  return (
    <section class="stack">
      <h1>Maintenance</h1>
      <p class="muted">
        Players on this list can join templates in maintenance.{' '}
        {inMaintenance.length ? <>In maintenance now: {inMaintenance.map((t, i) => <>{i ? ', ' : ''}<a href={'#/templates/' + encodeURIComponent(t)}>{t}</a></>)}.</> : 'No template is in maintenance right now.'}
      </p>
      <form class="card row wrap" onSubmit={add}>
        <input class="grow" placeholder="Player name" value={player} onInput={(e) => setPlayer(e.currentTarget.value)} aria-label="Player name" required />
        <button class="primary" disabled={!player.trim()}>Add</button>
      </form>
      <ErrorNote error={list.error} />
      <div class="card">
        <div class="chips">
          {(list.data || []).slice().sort().map((p) => (
            <span class="chip" key={p}>{p}<button class="chip-x" aria-label={`Remove ${p}`} onClick={() => remove(p)}>×</button></span>
          ))}
          {list.data?.length === 0 && <span class="muted">Nobody is on the list.</span>}
        </div>
      </div>
    </section>
  );
}
