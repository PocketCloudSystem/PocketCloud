import { useEffect, useRef, useState } from 'preact/hooks';
import { events } from '../api.js';
import { toast } from '../util.js';

const MAX_LINES = 2000;
const COLORS = {
  0: '#000', 1: '#00a', 2: '#0a0', 3: '#0aa', 4: '#a00', 5: '#a0a', 6: '#fa0', 7: '#aaa',
  8: '#777', 9: '#55f', a: '#5f5', b: '#5ff', c: '#f55', d: '#f5f', e: '#ff5', f: '#fff',
};

const ANSI = { 30: '0', 31: '4', 32: '2', 33: '6', 34: '1', 35: '5', 36: '3', 37: '7', 90: '8', 91: 'c', 92: 'a', 93: 'e', 94: '9', 95: 'd', 96: 'b', 97: 'f' };

// ANSI colour escapes (cloud console) to § codes, so one parser handles both
const ansiToSection = (line) => line.replace(/\x1b\[([0-9;]*)m/g, (_, codes) => codes.split(';').map((c) => {
  const n = Number(c || 0);
  if (n === 0 || n === 39) return '§r';
  if (n === 1) return '§l';
  if (n === 3) return '§o';
  return ANSI[n] ? '§' + ANSI[n] : '';
}).join(''));

// Minecraft § colour codes to spans
function colorize(line) {
  const parts = [];
  let style = {};
  for (const [i, chunk] of ansiToSection(line).split('§').entries()) {
    let text = chunk;
    if (i > 0 && chunk.length) {
      const code = chunk[0].toLowerCase();
      text = chunk.slice(1);
      if (COLORS[code]) style = { ...style, color: COLORS[code] };
      else if (code === 'l') style = { ...style, fontWeight: 600 };
      else if (code === 'o') style = { ...style, fontStyle: 'italic' };
      else if (code === 'r') style = {};
    }
    if (text) parts.push(<span style={style}>{text}</span>);
  }
  return parts;
}

let lineId = 0;

/** Live console: `stream` is the SSE path, `run(command)` sends a command. */
export function Console({ stream, run, title = 'Console', placeholder = 'Command, e.g. list', height }) {
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
    return events(stream, {
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
  }, [stream]);

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
      await run(cmd);
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
        <strong>{title}</strong>
        <span class={'badge ' + (state === 'live' ? 'ok' : state === 'ended' || state.startsWith('failed') ? 'bad' : 'warn')}>{state}</span>
      </div>
      <pre ref={box} class="console-lines" style={height ? { height } : null} onScroll={(e) => {
        const el = e.currentTarget;
        stick.current = el.scrollHeight - el.scrollTop - el.clientHeight < 24;
      }}>
        {lines.map((l) => <div key={l.id}>{colorize(l.text)}</div>)}
      </pre>
      <form class="console-input" onSubmit={send}>
        <span class="muted">&gt;</span>
        <input value={command} onInput={(e) => setCommand(e.currentTarget.value)} onKeyDown={onKey}
               placeholder={placeholder} aria-label="Command" disabled={state === 'ended'} />
        <button disabled={!command.trim() || state === 'ended'}>Send</button>
      </form>
    </div>
  );
}
