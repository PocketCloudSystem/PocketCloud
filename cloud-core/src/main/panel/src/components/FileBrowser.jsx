import { useEffect, useState } from 'preact/hooks';
import { api, base, fileText } from '../api.js';
import { ConfirmButton, ErrorNote } from './ui.jsx';
import { act, bytes } from '../util.js';
import { t } from '../i18n.js';
import { Download, FileText, Folder, FolderPlus, RefreshCw, Save, Trash2, Upload, X } from 'lucide-preact';

const EDITABLE_BYTES = 1024 * 1024;
const TEXT = /\.(ya?ml|json|properties|txt|log|md|toml|cfg|conf|ini|xml|sh|bat|js|lang|csv|html|css|php|lua)$/i;
const join = (dir, name) => (dir ? dir + '/' + name : name);
const parent = (p) => p.split('/').slice(0, -1).join('/');

/**
 * Browses a folder through `<api>?path=`: GET lists a folder or returns a file. With `writable` it also saves
 * (PUT raw body), uploads, creates folders (POST <api>/mkdir) and deletes (DELETE).
 */
export function FileBrowser({ api: endpoint, title = t('Files'), writable = false, empty = t('This folder is empty.') }) {
  const [dir, setDir] = useState('');
  const [listing, setListing] = useState(null);
  const [error, setError] = useState(null);
  const [open, setOpen] = useState(null); // { path, text, original }
  const [route, query] = endpoint.split('?');
  const url = (p, suffix = '') => `${route}${suffix}?${query ? query + '&' : ''}path=${encodeURIComponent(p)}`;

  const load = (p = dir) => api(url(p)).then((d) => { setListing(d); setError(null); }, setError);
  useEffect(() => { setOpen(null); load(dir); }, [dir, endpoint]);

  const view = async (entry) => {
    const path = join(dir, entry.name);
    if (entry.size > EDITABLE_BYTES || (!TEXT.test(entry.name) && entry.size > 0 && !noExtension(entry.name))) {
      window.open(base + url(path), '_blank');
      return;
    }
    try {
      const text = await fileText(url(path));
      setOpen({ path, text, original: text });
    } catch (e) {
      act(Promise.reject(e));
    }
  };
  const save = async () => {
    if (await act(api(url(open.path), { method: 'PUT', body: open.text, raw: true }))) {
      setOpen({ ...open, original: open.text });
      load();
    }
  };
  const upload = async (e) => {
    const input = e.currentTarget; // currentTarget is gone after the first await
    for (const file of [...input.files]) await act(api(url(join(dir, file.name)), { method: 'PUT', body: file, raw: true }));
    input.value = '';
    load();
  };
  const mkdir = async () => {
    const name = prompt(t('Folder name')); // shortcut: native prompt, swap for an inline input if it bothers anyone
    if (name && await act(api(url(join(dir, name), '/mkdir'), { method: 'POST', body: {} }))) load();
  };
  const remove = async (name) => {
    if (await act(api(url(join(dir, name)), { method: 'DELETE' }))) load();
  };

  const crumbs = dir ? dir.split('/') : [];
  return (
    <div class="card stack-sm files">
      <div class="row wrap">
        <h3 class="grow">{title}</h3>
        {writable && <button type="button" onClick={mkdir}><FolderPlus size={15} />{t('New folder')}</button>}
        {writable && <label class="button"><Upload size={15} />{t('Upload')}<input type="file" multiple hidden onChange={upload} /></label>}
        <button type="button" class="ghost" onClick={() => load()}><RefreshCw size={15} />{t('Refresh')}</button>
      </div>
      <nav class="row wrap small crumbs" aria-label={t('Folder')}>
        <a href="javascript:void 0" onClick={() => setDir('')}>/</a>
        {crumbs.map((c, i) => (
          <span key={i}>
            <a href="javascript:void 0" onClick={() => setDir(crumbs.slice(0, i + 1).join('/'))}>{c}</a>
            {i < crumbs.length - 1 && <span class="muted"> / </span>}
          </span>
        ))}
      </nav>
      <ErrorNote error={error} />

      {open ? (
        <div class="stack-sm">
          <div class="row wrap">
            <code class="grow">{open.path}</code>
            {writable && <button class="primary" disabled={open.text === open.original} onClick={save}><Save size={15} />{t('Save')}</button>}
            <a class="button ghost" href={base + url(open.path)} download><Download size={15} />{t('Download')}</a>
            <button class="ghost" onClick={() => setOpen(null)}><X size={15} />{t('Close')}</button>
          </div>
          <textarea class="editor" spellcheck={false} readOnly={!writable} value={open.text}
                    onInput={(e) => setOpen({ ...open, text: e.currentTarget.value })}
                    onKeyDown={(e) => { if (writable && (e.ctrlKey || e.metaKey) && e.key === 's') { e.preventDefault(); save(); } }} />
        </div>
      ) : listing && (
        <div class="table-wrap">
          <table>
            <thead><tr><th>{t('Name')}</th><th>{t('Size')}</th><th>{t('Modified')}</th><th /></tr></thead>
            <tbody>
              {dir && <tr><td colSpan={4}><a href="javascript:void 0" onClick={() => setDir(parent(dir))}>..</a></td></tr>}
              {listing.entries.map((e) => (
                <tr key={e.name}>
                  <td>
                    <a href="javascript:void 0" onClick={() => (e.dir ? setDir(join(dir, e.name)) : view(e))}>
                      {e.dir ? <Folder size={15} class="file-icon dir" /> : <FileText size={15} class="file-icon" />}{e.name}
                    </a>
                  </td>
                  <td class="muted">{e.dir ? '' : bytes(e.size)}</td>
                  <td class="muted">{new Date(e.modified).toLocaleString()}</td>
                  <td class="actions">
                    {!e.dir && <a class="muted small" href={base + url(join(dir, e.name))} download><Download size={15} />{t('Download')}</a>}
                    {writable && <ConfirmButton class="ghost small" confirmText={t('Delete?')} onConfirm={() => remove(e.name)}><Trash2 size={15} />{t('Delete')}</ConfirmButton>}
                  </td>
                </tr>
              ))}
              {!listing.entries.length && <tr><td colSpan={4} class="muted">{empty}</td></tr>}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}

// files with an unknown extension open as a download (they may be binary), extension-less ones in the editor
const noExtension = (name) => !name.includes(".");
