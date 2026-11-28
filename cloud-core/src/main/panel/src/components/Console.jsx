import { useEffect, useRef, useState } from 'preact/hooks';
import { api, events } from '../api.js';
import { toast } from '../util.js';

const MAX_LINES = 2000;
const COLORS = {
  0: '#000', 1: '#00a', 2: '#0a0', 3: '#0aa', 4: '#a00', 5: '#a0a', 6: '#fa0', 7: '#aaa',
  8: '#777', 9: '#55f', a: '#5f5', b: '#5ff', c: '#f55', d: '#f5f', e: '#ff5', f: '#fff',
};

// Minecraft § colour codes to spans; ANSI escapes are dropped
function colorize(line) {
  const parts = [];
  let style = {};
  for (const [i, chunk] of line.replace(/\x1b\[[0-9;]*m/g, '').split('§').entries()) {
    let text = chunk;
    if (i > 0 && chunk.length) {
      const code = chunk[0].toLowerCase();
      text = chunk.slice(1);
      if (COLORS[code]) style = { color: COLORS[code] };
      else if (code === 'l') style = { ...style, fontWeight: 600 };
      else if (code === 'o') style = { ...style, fontStyle: 'italic' };
      else if (code === 'r') style = {};
    }
    if (text) parts.push(<span style={style}>{text}</span>);
  }
  return parts;
}

let lineId = 0;

export function Console({ server }) {
  const [lines, setLines] = useState([]);
  const [state, setState] = useState('connecting');
  const [command, setCommand] = useState('');
  const [history, setHistory] = useState([]);
  const [historyAt, setHistoryAt] = useState(-1);
  const box = useRef(null);
  const stick = useRef(true);

  useEffect(() => {
    setLines([]);
    setState('connecting');
    return events(`/panel-api/servers/${encodeURIComponent(server)}/console`, {
      open: () => setState('live'),
      lines: (data) => {
        setState('live');
        const fresh = data.split('\n').map((text) => ({ id: lineId++, text }));
        setLines((old) => [...old, ...fresh].slice(-MAX_LINES));
      },
      end: (_, source) => { source.close(); setState('ended'); },
      failure: (data, source) => { source.close(); setState('failed: ' + data); },
      error: (_, source) => source.readyState === EventSource.CLOSED || setState('reconnecting'),
    });
  }, [server]);

  useEffect(() => {
    if (stick.current && box.current) box.current.scrollTop = box.current.scrollHeight;
  }, [lines]);

  const send = async (e) => {
    e.preventDefault();
    const cmd = command.trim().replace(/^\//, '');
    if (!cmd) return;
    setHistory((h) => [cmd, ...h.filter((x) => x !== cmd)].slice(0, 50));
    setHistoryAt(-1);
    setCommand('');
    try {
      await api(`/v1/servers/${encodeURIComponent(server)}/dispatch`, { method: 'POST', body: { command: cmd } });
    } catch (err) {
      toast(err.message, 'bad');
    }
  };

  const onKey = (e) => {
    if (e.key !== 'ArrowUp' && e.key !== 'ArrowDown') return;
    e.preventDefault();
    const next = Math.max(-1, Math.min(history.length - 1, historyAt + (e.key === 'ArrowUp' ? 1 : -1)));
    setHistoryAt(next);
    setCommand(next < 0 ? '' : history[next]);
  };

  return (
    <div class="console card">
      <div class="console-head">
        <strong>Console</strong>
        <span class={'badge ' + (state === 'live' ? 'ok' : state === 'ended' || state.startsWith('failed') ? 'bad' : 'warn')}>{state}</span>
      </div>
      <pre ref={box} class="console-lines" onScroll={(e) => {
        const el = e.currentTarget;
        stick.current = el.scrollHeight - el.scrollTop - el.clientHeight < 24;
      }}>
        {lines.map((l) => <div key={l.id}>{colorize(l.text)}</div>)}
      </pre>
      <form class="console-input" onSubmit={send}>
        <span class="muted">&gt;</span>
        <input value={command} onInput={(e) => setCommand(e.currentTarget.value)} onKeyDown={onKey}
               placeholder="Command, e.g. list" aria-label="Command" disabled={state === 'ended'} />
        <button disabled={!command.trim() || state === 'ended'}>Send</button>
      </form>
    </div>
  );
}
