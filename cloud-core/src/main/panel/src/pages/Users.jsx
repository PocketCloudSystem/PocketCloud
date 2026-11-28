import { useState } from 'preact/hooks';
import { api } from '../api.js';
import { ConfirmButton, ErrorNote } from '../components/ui.jsx';
import { act, usePoll } from '../util.js';

const HINTS = {
  servers: 'start, stop, save, server commands',
  templates: 'templates, their files, groups, maintenance',
  players: 'message, transfer, kick',
  console: 'cloud console commands (can do anything)',
  admin: 'everything, incl. users, nodes, backups, schedules, plugins',
};

const empty = { name: '', password: '', permissions: [], editing: false };

export function Users() {
  const users = usePoll(() => api('/panel-api/users'), 30000);
  const [form, setForm] = useState(empty);
  const toggle = (p, on) => setForm({ ...form, permissions: on ? [...form.permissions, p] : form.permissions.filter((x) => x !== p) });

  const save = async (e) => {
    e.preventDefault();
    const { name, password, permissions } = form;
    if (await act(api('/panel-api/users', { method: 'POST', body: { name, password, permissions } }), 'Saved.')) {
      setForm(empty);
      users.reload();
    }
  };

  return (
    <section class="stack">
      <h1>Users</h1>
      <p class="muted">Everyone who can sign in may look at everything. Changes need the permissions below. Users are stored on this node only.</p>
      <form class="card stack-sm" onSubmit={save}>
        <h3>{form.editing ? `Edit ${form.name}` : 'New user'}</h3>
        <div class="grid fields">
          <label class="field">
            <span>Name</span>
            <input value={form.name} disabled={form.editing} required pattern="[A-Za-z0-9_.\-]{2,32}" autocomplete="off"
                   onInput={(e) => setForm({ ...form, name: e.currentTarget.value })} />
          </label>
          <label class="field">
            <span>{form.editing ? 'New password (empty keeps it)' : 'Password'}</span>
            <input type="password" value={form.password} minLength={8} required={!form.editing} autocomplete="new-password"
                   onInput={(e) => setForm({ ...form, password: e.currentTarget.value })} />
          </label>
        </div>
        <div class="grid flags">
          {(users.data?.permissions || []).map((p) => (
            <label class="check" key={p}>
              <input type="checkbox" checked={form.permissions.includes(p)} onChange={(e) => toggle(p, e.currentTarget.checked)} />
              <span>{p}<small class="muted">{HINTS[p]}</small></span>
            </label>
          ))}
        </div>
        <div class="row">
          <button class="primary">{form.editing ? 'Save user' : 'Add user'}</button>
          {form.editing && <button type="button" class="ghost" onClick={() => setForm(empty)}>Cancel</button>}
        </div>
      </form>
      <ErrorNote error={users.error} />
      <div class="card table-wrap">
        <table>
          <thead><tr><th>Name</th><th>Permissions</th><th /></tr></thead>
          <tbody>
            {(users.data?.users || []).map((u) => (
              <tr key={u.name}>
                <td>{u.name}</td>
                <td>{u.permissions.length ? u.permissions.join(', ') : <span class="muted">read only</span>}</td>
                <td class="actions">
                  <button class="small" onClick={() => setForm({ name: u.name, password: '', permissions: u.permissions, editing: true })}>Edit</button>
                  <ConfirmButton class="danger small" confirmText="Delete?"
                                 onConfirm={() => act(api('/panel-api/users/' + encodeURIComponent(u.name), { method: 'DELETE' }), 'Deleted.').then(users.reload)}>
                    Delete
                  </ConfirmButton>
                </td>
              </tr>
            ))}
            {users.data?.users.length === 0 && <tr><td colSpan="3" class="muted">No users yet. Until then, sign in with the auth-key.</td></tr>}
          </tbody>
        </table>
      </div>
    </section>
  );
}
