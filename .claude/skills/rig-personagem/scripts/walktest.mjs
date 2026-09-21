// testa o andar REAL: entra no modo-andar, troca skin, segura W ~1s, e fotografa
// (câmera 3a pessoa atrás). Frente visível = moonwalk; costas = certo.
import http from 'http'; import fs from 'fs'; import path from 'path'; import pup from 'puppeteer';
const ROOT = path.resolve('C:/dev/reforma/src'); const OUT = path.resolve('C:/dev/reforma/_t');
const MIME = { '.html':'text/html','.js':'text/javascript','.json':'application/json','.glb':'model/gltf-binary','.wasm':'application/wasm','.css':'text/css','.png':'image/png' };
const srv = http.createServer((q, r) => { let u = decodeURIComponent(q.url.split('?')[0]); if (u === '/') u = '/index.html'; const f = path.join(ROOT, u); if (!fs.existsSync(f)) { r.writeHead(404); r.end(); return; } r.writeHead(200, { 'Content-Type': MIME[path.extname(f)] || 'application/octet-stream' }); fs.createReadStream(f).pipe(r); });
await new Promise((r) => srv.listen(8816, r));
const ids = (process.argv[2] || 'nobara,yuji').split(',');
const yaws = (process.argv[3] || '0,0').split(',').map(Number);
const flips = (process.argv[4] || '0,0').split(',').map((x) => x === '1');
const labels = (process.argv[5] || ids.join(',')).split(',');
const b = await pup.launch({ headless: 'new', args: ['--no-sandbox', '--use-gl=angle', '--ignore-gpu-blocklist'] });
const pg = await b.newPage(); await pg.setViewport({ width: 700, height: 700 });
pg.on('pageerror', (e) => console.log('ERR', e.message.slice(0, 120)));
await pg.goto('http://localhost:8816/index.html', { waitUntil: 'load' });
await pg.waitForFunction('window.__app&&window.__app.character', { timeout: 20000 });
await new Promise((r) => setTimeout(r, 2800));
for (let i = 0; i < ids.length; i++) {
  const id = ids[i], yaw = yaws[i] || 0, stepFlip = flips[i] || false, label = labels[i] || id;
  await pg.evaluate((file, yaw, stepFlip) => {
    const ch = window.__app.character;
    if (!ch.isActive()) ch.enable({ x: 6, z: 8.5, yaw: 0 });
    ch.setSkin(file, { yaw, stepFlip });
  }, './models/' + id + '.glb', yaw, stepFlip);
  await new Promise((r) => setTimeout(r, 2600)); // espera skin+retarget
  // segura W
  await pg.evaluate(() => { window.dispatchEvent(new KeyboardEvent('keydown', { code: 'KeyW', bubbles: true })); });
  await new Promise((r) => setTimeout(r, 1200)); // app move o avatar no RAF
  await pg.screenshot({ path: path.join(OUT, `walk_${label}.png`) });
  await pg.evaluate(() => { window.dispatchEvent(new KeyboardEvent('keyup', { code: 'KeyW', bubbles: true })); });
  // posição e facing
  const info = await pg.evaluate(() => {
    const sc = window.__app.scene; let sk = null; sc.traverse((o) => { if (!sk && o.isSkinnedMesh) sk = o; });
    return { ok: !!sk };
  });
  console.log('shot', id, 'yaw', yaw, JSON.stringify(info));
  await new Promise((r) => setTimeout(r, 300));
}
await b.close(); srv.close(); console.log('done');
