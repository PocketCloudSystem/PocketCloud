import { useRef, useState } from 'preact/hooks';
import { t } from '../i18n.js';

const W = 600, H = 180, PAD = { l: 56, r: 8, t: 10, b: 24 };

const time = (t) => new Date(t).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });

function niceMax(v) {
  if (v <= 0) return 1;
  const p = 10 ** Math.floor(Math.log10(v));
  return [1, 2, 4, 10].map((m) => m * p).find((m) => m >= v); // halves stay whole numbers
}

/** Single-series line chart with crosshair tooltip. points: [[time, value]], format: value -> string. */
export function Chart({ title, points, format = String, max }) {
  const [hover, setHover] = useState(null);
  const svg = useRef(null);
  if (points.length < 2) {
    return (
      <div class="card chart">
        <h3>{title}</h3>
        <p class="muted small">{t('Collecting data, one point per minute…')}</p>
      </div>
    );
  }
  const t0 = points[0][0], t1 = points[points.length - 1][0];
  const top = max ?? niceMax(Math.max(4, ...points.map((p) => p[1])) * 1.1);
  const x = (t) => PAD.l + ((t - t0) / (t1 - t0 || 1)) * (W - PAD.l - PAD.r);
  const y = (v) => PAD.t + (1 - Math.min(v, top) / top) * (H - PAD.t - PAD.b);
  const line = points.map((p, i) => `${i ? 'L' : 'M'}${x(p[0]).toFixed(1)},${y(p[1]).toFixed(1)}`).join('');
  const area = `${line}L${x(t1)},${y(0)}L${x(t0)},${y(0)}Z`;
  const ticks = [0, top / 2, top];

  const move = (e) => {
    const box = svg.current.getBoundingClientRect();
    const px = ((e.clientX - box.left) / box.width) * W;
    const t = t0 + ((px - PAD.l) / (W - PAD.l - PAD.r)) * (t1 - t0);
    let best = points[0];
    for (const p of points) if (Math.abs(p[0] - t) < Math.abs(best[0] - t)) best = p;
    setHover(best);
  };

  const last = points[points.length - 1][1];
  return (
    <div class="card chart">
      <h3>{title} <span class="muted small">{t('now')} {format(last)}</span></h3>
      <div class="chart-box">
        <svg ref={svg} viewBox={`0 0 ${W} ${H}`} role="img" aria-label={`${title}, last value ${format(last)}`}
             onPointerMove={move} onPointerLeave={() => setHover(null)}>
          {ticks.map((v) => (
            <g key={v}>
              <line x1={PAD.l} x2={W - PAD.r} y1={y(v)} y2={y(v)} class="grid" />
              <text x={PAD.l - 6} y={y(v) + 4} class="axis" text-anchor="end">{format(v)}</text>
            </g>
          ))}
          <text x={PAD.l} y={H - 4} class="axis">{time(t0)}</text>
          <text x={W - PAD.r} y={H - 4} class="axis" text-anchor="end">{time(t1)}</text>
          <path d={area} class="area" />
          <path d={line} class="line" />
          {hover && (
            <g>
              <line x1={x(hover[0])} x2={x(hover[0])} y1={PAD.t} y2={y(0)} class="cross" />
              <circle cx={x(hover[0])} cy={y(hover[1])} r="4" class="dot" />
            </g>
          )}
        </svg>
        {hover && (
          <div class="tip" style={{ left: `${Math.min(88, Math.max(12, (x(hover[0]) / W) * 100))}%` }}>
            <strong>{format(hover[1])}</strong> <span class="muted">{time(hover[0])}</span>
          </div>
        )}
      </div>
    </div>
  );
}
