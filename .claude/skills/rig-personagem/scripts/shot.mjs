// captura caminhada (preview) de vários modelos + montagem. uso: node _t/shot.mjs <clip> <ids> <frames>
import http from 'http'; import fs from 'fs'; import path from 'path'; import pup from 'puppeteer';
const ROOT = path.resolve('C:/dev/reforma/src'); const OUT = path.resolve('C:/dev/reforma/_t');
const MIME = { '.html':'text/html','.js':'text/javascript','.json':'application/json','.glb':'model/gltf-binary','.wasm':'application/wasm','.css':'text/css','.png':'image/png' };
const srv = http.createServer((q, r) => { let u = decodeURIComponent(q.url.split('?')[0]); if (u === '/') u = '/index.html'; const f = path.join(ROOT, u); if (!fs.existsSync(f)) { r.writeHead(404); r.end(); return; } r.writeHead(200, { 'Content-Type': MIME[path.extname(f)] || 'application/octet-stream' }); fs.createReadStream(f).pipe(r); });
await new Promise((r) => srv.listen(8810, r));
const clip = process.argv[2] || 'walk';
const ids = (process.argv[3] || 'gojo,hana,nobara,omniman,thragg,mahoraga,toji,choso').split(',');
const nFrames = parseInt(process.argv[4] || '1', 10);
const b = await pup.launch({ headless: 'new', args: ['--no-sandbox', '--use-gl=angle', '--ignore-gpu-blocklist'] });
const pg = await b.newPage(); await pg.setViewport({ width: 820, height: 700 });
pg.on('pageerror', (e) => console.log('ERR', e.message.slice(0, 120)));
await pg.goto('http://localhost:8810/index.html', { waitUntil: 'load' });
await pg.waitForFunction('window.__app&&window.__app.character', { timeout: 20000 });
await new Promise((r) => setTimeout(r, 2800));
await pg.evaluate(() => { window.__app.controls.enabled = false; });
for (const id of ids) {
  await pg.evaluate((file, clip) => { window.__app.character.preview(file, { previewClip: clip }, window.__app.camera); }, './models/' + id + '.glb', clip);
  await new Promise((r) => setTimeout(r, 1700));
  await pg.evaluate(() => { const sc = window.__app.scene; let k = null; sc.traverse((o) => { if (o.isSkinnedMesh && !k) { let t = o; while (t.parent && t.parent !== sc) t = t.parent; k = t; } }); sc.children.forEach((o) => { if (o.isLight) return; if (o === k) return; if (o.type === 'Group' || o.isMesh) o.visible = false; }); });
  const VIEW = process.env.VIEW || '34';
  for (let i = 0; i < nFrames; i++) {
    await pg.evaluate((v) => { const c = window.__app.camera; if (v === 'side') c.position.set(4.6, 1.0, 0.2); else if (v === 'front') c.position.set(0, 1.0, 5.0); else c.position.set(2.1, 1.15, 3.7); c.lookAt(0, 0.9, 0); c.updateMatrixWorld(); }, VIEW);
    await new Promise((r) => setTimeout(r, 240));
    await pg.screenshot({ path: path.join(OUT, `${id}_${i}.png`) });
  }
  console.log('ok', id);
}
await b.close();
// montagem
const cells = [];
for (const id of ids) for (let i = 0; i < nFrames; i++) cells.push(`<div class=c><div>${id}${nFrames > 1 ? ' #' + i : ''}</div><div class=ph><img src="/${id}_${i}.png"></div></div>`);
const srv2 = http.createServer((q, r) => { if (q.url === '/') { r.writeHead(200, { 'Content-Type': 'text/html' }); r.end(`<style>body{margin:0;background:#2a2a2a;font:13px monospace;color:#eee}.c{display:inline-block;margin:1px;vertical-align:top}.ph{width:300px;height:340px;overflow:hidden}.ph img{margin-left:-300px;margin-top:-30px;width:820px}</style><div style=display:flex;flex-wrap:wrap>${cells.join('')}</div>`); return; } const f = path.join(OUT, q.url.slice(1)); if (!fs.existsSync(f)) { r.writeHead(404); r.end(); return; } r.writeHead(200, { 'Content-Type': 'image/png' }); fs.createReadStream(f).pipe(r); });
await new Promise((r) => srv2.listen(8811, r));
const b2 = await pup.launch({ headless: 'new', args: ['--no-sandbox'] }); const pg2 = await b2.newPage();
const cols = Math.min(cells.length, 4); await pg2.setViewport({ width: cols * 304 + 12, height: Math.ceil(cells.length / cols) * 346 + 12 });
await pg2.goto('http://localhost:8811/', { waitUntil: 'load' }); await new Promise((r) => setTimeout(r, 500));
await pg2.screenshot({ path: path.join(OUT, `montage_${clip}.png`), fullPage: true });
await b2.close(); srv2.close(); srv.close(); console.log('montage_' + clip + '.png');
