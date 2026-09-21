// Motor de captura de telas MOBILE (Android) para POPs.
// Tira screenshot via adb, lê os bounds dos elementos via uiautomator dump,
// e renderiza as anotações (caixas vermelhas + badges numerados) com um
// overlay HTML pintado pelo Playwright. Mesmo visual do motor web.
//
// Config por env var (ou edite os defaults):
//   POP_OUT_MOBILE -> pasta de saída (ex.: C:/dev/checklist/tutorial/imagens/mobile)
//   POP_TMP        -> pasta temporária p/ raw + html
//   POP_PLAYWRIGHT -> caminho do pacote playwright já instalado (reaproveita o do web)
import { execFileSync } from 'node:child_process';
import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import { pathToFileURL } from 'node:url';

const PW = process.env.POP_PLAYWRIGHT
  || 'C:/dev/checklist/frontend-checklist/e2e/node_modules/playwright/index.js';
const _pw = await import(pathToFileURL(PW).href);
const chromium = _pw.chromium || _pw.default?.chromium;

const ADB = path.join(os.homedir(), 'AppData/Local/Android/Sdk/platform-tools/adb.exe');
export const OUT = process.env.POP_OUT_MOBILE || 'C:/dev/checklist/tutorial/imagens/mobile';
const TMP = process.env.POP_TMP || 'C:/dev/checklist/tutorial/_cap';
if (!fs.existsSync(TMP)) fs.mkdirSync(TMP, { recursive: true });
if (!fs.existsSync(OUT)) fs.mkdirSync(OUT, { recursive: true });

export function adb(args, opts = {}) {
  return execFileSync(ADB, args, { encoding: 'buffer', maxBuffer: 64 * 1024 * 1024, ...opts });
}
export function adbStr(args) { return adb(args).toString('utf8').trim(); }

export function device() {
  const out = adbStr(['devices']);
  const lines = out.split('\n').slice(1).map((l) => l.trim()).filter(Boolean);
  const dev = lines.find((l) => l.endsWith('\tdevice'));
  return dev ? dev.split('\t')[0] : null;
}

export function capRaw(name) {
  const png = adb(['exec-out', 'screencap', '-p']);
  const file = `${TMP}/${name}.raw.png`;
  fs.writeFileSync(file, png);
  return file;
}

// uiautomator dump -> array de {text, desc, cls, bounds:[x1,y1,x2,y2]}
export function dumpUI() {
  adb(['shell', 'uiautomator', 'dump', '/sdcard/__ui.xml']);
  const xml = adbStr(['shell', 'cat', '/sdcard/__ui.xml']);
  const nodes = [];
  const re = /<node[^>]*\/?>/g;
  let m;
  while ((m = re.exec(xml))) {
    const tag = m[0];
    const attr = (k) => { const mm = tag.match(new RegExp(`${k}="([^"]*)"`)); return mm ? mm[1] : ''; };
    const b = attr('bounds').match(/\[(\d+),(\d+)\]\[(\d+),(\d+)\]/);
    nodes.push({
      text: attr('text'), desc: attr('content-desc'), cls: attr('class'),
      bounds: b ? [+b[1], +b[2], +b[3], +b[4]] : null,
    });
  }
  return nodes.filter((n) => n.bounds);
}

// acha nó por texto/content-desc (exato ou parcial)
export function find(nodes, query, { partial = true } = {}) {
  const q = query.toLowerCase();
  const match = (s) => partial ? (s || '').toLowerCase().includes(q) : (s || '').toLowerCase() === q;
  return nodes.find((n) => match(n.text) || match(n.desc));
}

export function boundsToTarget(bounds, extra = {}) {
  const [x1, y1, x2, y2] = bounds;
  return { x: x1, y: y1, w: x2 - x1, h: y2 - y1, ...extra };
}

// Renderiza a screenshot com caixas vermelhas + badges. targets: {x,y,w,h,num,label,place,pad}
export async function annotateImage(srcPng, outName, targets) {
  const buf = fs.readFileSync(srcPng);
  const W = buf.readUInt32BE(16), H = buf.readUInt32BE(20);
  const b64 = buf.toString('base64');
  const html = `<!doctype html><html><head><style>
    *{margin:0;padding:0;box-sizing:border-box}
    html,body{width:${W}px;height:${H}px}
    #img{position:absolute;inset:0;width:${W}px;height:${H}px}
    .box{position:absolute;border:5px solid #e11d48;border-radius:14px;box-shadow:0 0 0 5px rgba(225,29,72,.25)}
    .chip{position:absolute;background:#e11d48;color:#fff;font:600 30px/1.15 Arial;padding:8px 14px;border-radius:12px;white-space:nowrap;box-shadow:0 3px 8px rgba(0,0,0,.4)}
  </style></head><body>
    <img id="img" src="data:image/png;base64,${b64}">
    <div id="layer"></div>
  </body></html>`;
  const htmlFile = `${TMP}/__annot.html`;
  fs.writeFileSync(htmlFile, html);
  const browser = await chromium.launch();
  const page = await browser.newPage({ viewport: { width: W, height: H }, deviceScaleFactor: 1 });
  await page.goto('file:///' + htmlFile, { waitUntil: 'networkidle' });
  await page.evaluate((targets) => {
    const layer = document.getElementById('layer');
    const GAP = 10;
    for (const t of targets) {
      const pad = t.pad ?? 6;
      const L = t.x - pad, T = t.y - pad, Wd = t.w + pad * 2, Hd = t.h + pad * 2;
      const box = document.createElement('div');
      box.className = 'box';
      box.style.left = L + 'px'; box.style.top = T + 'px';
      box.style.width = Wd + 'px'; box.style.height = Hd + 'px';
      layer.appendChild(box);
      if (t.num != null || t.label) {
        const chip = document.createElement('div');
        chip.className = 'chip';
        chip.textContent = [t.num != null ? t.num : null, t.label].filter((x) => x != null && x !== '').join('  ');
        layer.appendChild(chip);
        const cw = chip.offsetWidth, ch = chip.offsetHeight;
        const place = t.place || 'tr';
        let cx, cy;
        switch (place) {
          case 'tl': cx = L; cy = T - ch - GAP; break;
          case 'tr': cx = L + Wd - cw; cy = T - ch - GAP; break;
          case 'bl': cx = L; cy = T + Hd + GAP; break;
          case 'br': cx = L + Wd - cw; cy = T + Hd + GAP; break;
          case 'top': cx = L + Wd / 2 - cw / 2; cy = T - ch - GAP; break;
          case 'bottom': cx = L + Wd / 2 - cw / 2; cy = T + Hd + GAP; break;
          case 'left': cx = L - cw - GAP; cy = T + Hd / 2 - ch / 2; break;
          case 'right': cx = L + Wd + GAP; cy = T + Hd / 2 - ch / 2; break;
          default: cx = L; cy = T - ch - GAP;
        }
        cx = Math.max(6, Math.min(cx, window.innerWidth - cw - 6));
        cy = Math.max(6, Math.min(cy, window.innerHeight - ch - 6));
        chip.style.left = cx + 'px'; chip.style.top = cy + 'px';
      }
    }
  }, targets);
  await page.screenshot({ path: `${OUT}/${outName}.png` });
  await browser.close();
}

// conveniência: captura + (opcional) anota por queries de texto
//   shot('mobile-02-home', [{ q: 'Iniciar', num: 1, label: 'Iniciar inspeção', place: 'top' }])
export async function shot(name, annots = []) {
  const raw = capRaw(name);
  if (!annots.length) {
    fs.copyFileSync(raw, `${OUT}/${name}.png`);
    return;
  }
  const nodes = dumpUI();
  const targets = [];
  for (const a of annots) {
    if (a.bounds) { targets.push(boundsToTarget(a.bounds, a)); continue; }
    const node = find(nodes, a.q, { partial: a.partial !== false });
    if (node) targets.push(boundsToTarget(node.bounds, a));
    else console.log(`  ! não achei "${a.q}"`);
  }
  await annotateImage(raw, name, targets);
}

// dirigir o app por adb
export function tap(x, y) { adb(['shell', 'input', 'tap', String(x), String(y)]); }
export function swipe(x1, y1, x2, y2, ms = 400) { adb(['shell', 'input', 'swipe', String(x1), String(y1), String(x2), String(y2), String(ms)]); }
export function text(s) { adb(['shell', 'input', 'text', s.replace(/ /g, '%s')]); }
export const sleep = (ms) => new Promise((r) => setTimeout(r, ms));
