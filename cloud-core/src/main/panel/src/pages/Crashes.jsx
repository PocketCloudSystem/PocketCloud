import { useState } from 'preact/hooks';
import { api } from '../api.js';
import { FileBrowser } from '../components/FileBrowser.jsx';
import { usePoll } from '../util.js';
import { t } from '../i18n.js';

export function Crashes({ session }) {
  const nodes = usePoll(() => (session.clustered ? api('/v1/nodes') : null), 30000);
  const [node, setNode] = useState('');
  const names = nodes.data?.nodes.map((n) => n.name).sort() || [];
  return (
    <section class="stack">
      <div class="row wrap">
        <h1 class="grow">{t('Crash reports')}</h1>
        {names.length > 0 && (
          <select value={node} onChange={(e) => setNode(e.currentTarget.value)} aria-label={t('Node')}>
            <option value="">{t('this node')}</option>
            {names.map((n) => <option value={n}>{n}</option>)}
          </select>
        )}
      </div>
      <FileBrowser key={node} api={'/v1/crashes' + (node ? '?node=' + encodeURIComponent(node) : '')}
                   title={t('Crash reports')} empty={t('No crash reports.')} />
    </section>
  );
}
