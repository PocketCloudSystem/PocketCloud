import { useState } from 'preact/hooks';
import { Pencil, Plus, Save, Trash2 } from 'lucide-preact';
import { api } from '../api.js';
import { ConfirmButton, ErrorNote, Loading } from '../components/ui.jsx';
import { t } from '../i18n.js';
import { act, usePoll } from '../util.js';

const ROLE_HINTS = {
  admin: 'everything, incl. users, nodes, backups, schedules, plugins and the cloud console',
  developer: 'servers and templates (incl. files, groups, maintenance)',
  moderator: 'servers and players',
  supporter: 'players: message, transfer, kick',
  viewer: 'read only',
};

const empty = { name: '', password: '', role: 'viewer', editing: false };

export function Users() {
  const users = usePoll(() => api('/panel-api/users'), 30000);
  const [form, setForm] = useState(empty);
  const roles = Object.keys(users.data?.roles || {});

  const save = async (e) => {
    e.preventDefault();
    const { name, password, role } = form;
    if (await act(api('/panel-api/users', { method: 'POST', body: { name, password, role } }), t('Saved.'))) {
      setForm(empty);
      users.reload();
    }
  };

  return (
    <section class="stack">
      <h1>{t('Users')}</h1>
      <p class="muted">{t('Everyone who can sign in may look at everything; the role decides what they may change. In the cloud console:')} <code>user create &lt;name&gt; &lt;role&gt;</code>. {t('Users are stored on this node only.')}</p>
      <form class="card stack-sm" onSubmit={save}>
        <h3>{form.editing ? t('Edit {name}', { name: form.name }) : t('New user')}</h3>
        <div class="grid fields">
          <label class="field">
            <span>{t('Name')}</span>
            <input value={form.name} disabled={form.editing} required pattern="[A-Za-z0-9_.\-]{2,32}" autocomplete="off"
                   onInput={(e) => setForm({ ...form, name: e.currentTarget.value })} />
          </label>
          <label class="field">
            <span>{form.editing ? t('New password (empty keeps it)') : t('Password')}</span>
            <input type="password" value={form.password} minLength={8} required={!form.editing} autocomplete="new-password"
                   onInput={(e) => setForm({ ...form, password: e.currentTarget.value })} />
          </label>
        </div>
        <div class="grid flags" role="radiogroup" aria-label={t('Role')}>
          {roles.map((r) => (
            <label class={'check role' + (form.role === r ? ' selected' : '')} key={r}>
              <input type="radio" name="role" checked={form.role === r} onChange={() => setForm({ ...form, role: r })} />
              <span>{r}<small class="muted">{t(ROLE_HINTS[r] || '')}</small></span>
            </label>
          ))}
        </div>
        <div class="row">
          <button class="primary">{form.editing ? <Save size={16} /> : <Plus size={16} />}{form.editing ? t('Save user') : t('Add user')}</button>
          {form.editing && <button type="button" class="ghost" onClick={() => setForm(empty)}>{t('Cancel')}</button>}
        </div>
      </form>
      <ErrorNote error={users.error} />
      <Loading poll={users} />
      {users.data && (
        <div class="card table-wrap">
          <table>
            <thead><tr><th>{t('Name')}</th><th>{t('Role')}</th><th /></tr></thead>
            <tbody>
              {users.data.users.map((u) => (
                <tr key={u.name}>
                  <td><span class="avatar">{u.name.slice(0, 1).toUpperCase()}</span> {u.name}</td>
                  <td><span class="badge">{u.role}</span></td>
                  <td class="actions">
                    <button class="small" onClick={() => setForm({ name: u.name, password: '', role: u.role, editing: true })}><Pencil size={13} />{t('Edit')}</button>
                    <ConfirmButton class="danger small" confirmText={t('Delete?')}
                                   onConfirm={() => act(api('/panel-api/users/' + encodeURIComponent(u.name), { method: 'DELETE' }), t('Deleted.')).then(users.reload)}>
                      <Trash2 size={13} />{t('Delete')}
                    </ConfirmButton>
                  </td>
                </tr>
              ))}
              {users.data.users.length === 0 && <tr><td colSpan="3" class="muted">{t('No users yet.')}</td></tr>}
            </tbody>
          </table>
        </div>
      )}
    </section>
  );
}
