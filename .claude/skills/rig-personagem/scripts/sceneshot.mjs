// renderiza um objeto da CENA (ex.: opala-1980) de vários ângulos. uso:
//   node _t/sceneshot.mjs opala-1980
import http from 'http'; import fs from 'fs'; import path from 'path'; import pup from 'puppeteer';
const ROOT = path.resolve('C:/dev/reforma/src'); const OUT = path.resolve('C:/dev/reforma/_t');
const MIME = { '.html':'text/html','.js':'text/javascript','.json':'application/json','.glb':'model/gltf-binary','.wasm':'application/wasm','.css':'text/css','.png':'image/png' };
const srv = http.createServer((q, r) => { let u = decodeURIComponent(q.url.split('?')[0]); if (u === '/') u = '/index.html'; const f = path.join(ROOT, u); if (!fs.existsSync(f)) { r.writeHead(404); r.end(); return; } r.writeHead(200, { 'Content-Type': MIME[path.extname(f)] || 'application/octet-stream' }); fs.createReadStream(f).pipe(r); });
await new Promise((r) => srv.listen(8813, r));
const target = process.argv[2] || 'opala-1980';
const b = await pup.launch({ headless: 'new', args: ['--no-sandbox', '--use-gl=angle', '--ignore-gpu-blocklist'] });
const pg = await b.newPage(); await pg.setViewport({ width: 900, height: 700 });
pg.on('pageerror', (e) => console.log('ERR', e.message.slice(0, 140)));
await pg.goto('http://localhost:8813/index.html', { waitUntil: 'load' });
await pg.waitForFunction('window.__app&&window.__app.scene', { timeout: 20000 });
await new Promise((r) => setTimeout(r, 3500)); // deixa a cena/modelos carregarem
// esconde overlays de UI (HUD) pra não tampar a cena
await pg.evaluate(() => { document.querySelectorAll('body *').forEach((e) => { if (e.tagName !== 'CANVAS' && !e.contains(document.querySelector('canvas')) && getComputedStyle(e).position && /fixed|absolute/.test(getComputedStyle(e).position)) e.style.display = 'none'; }); });
// ISOLA: esconde TODAS as malhas menos as do alvo (luzes intactas)
await pg.evaluate((name) => {
  const sc = window.__app.scene; let tgt = null;
  sc.traverse((o) => { if (o.name === name) tgt = o; });
  if (!tgt) return;
  sc.traverse((o) => { if (o.isMesh) o.visible = false; });
  tgt.traverse((o) => { if (o.isMesh) o.visible = true; });
}, target);
const info = await pg.evaluate((name) => {
  const sc = window.__app.scene; let obj = null;
  sc.traverse((o) => { if (o.name === name) obj = o; });
  if (!obj) { // procura no projectGroup
    sc.traverse((o) => { if (!obj && o.name === name) obj = o; });
  }
  if (!obj) return { found: false };
  const THREE = window.__app.THREE || null;
  obj.updateWorldMatrix(true, true);
  // bbox manual
  let mn = [1e9,1e9,1e9], mx = [-1e9,-1e9,-1e9];
  obj.traverse((o) => { if (o.isMesh && o.geometry?.attributes?.position) { const p = o.geometry.attributes.position; const idx = o.geometry.index; const cnt = idx ? idx.count : p.count; const m = o.matrixWorld.elements; for (let i=0;i<cnt;i+=7){ const vi = idx ? idx.getX(i) : i; const x=p.getX(vi),y=p.getY(vi),z=p.getZ(vi); const wx=m[0]*x+m[4]*y+m[8]*z+m[12], wy=m[1]*x+m[5]*y+m[9]*z+m[13], wz=m[2]*x+m[6]*y+m[10]*z+m[14]; mn=[Math.min(mn[0],wx),Math.min(mn[1],wy),Math.min(mn[2],wz)]; mx=[Math.max(mx[0],wx),Math.max(mx[1],wy),Math.max(mx[2],wz)]; } } });
  const c = [(mn[0]+mx[0])/2,(mn[1]+mx[1])/2,(mn[2]+mx[2])/2];
  const sz = [mx[0]-mn[0],mx[1]-mn[1],mx[2]-mn[2]];
  // lista submeshes
  const meshes = [];
  obj.traverse((o) => { if (o.isMesh) meshes.push(o.name); });
  return { found: true, c, sz, meshes: meshes.slice(0, 30) };
}, target);
console.log('INFO', JSON.stringify(info));
if (!info.found) { console.log('NOT FOUND'); await b.close(); srv.close(); process.exit(0); }
let [cx, cy, cz] = info.c; let r = Math.max(info.sz[0], info.sz[2]) * 1.4 + 2;
if (process.env.CAM) { const p = process.env.CAM.split(',').map(Number); cx = p[0]; cy = p[1]; cz = p[2]; r = p[3]; info.c = [cx, cy, cz]; }
const views = { side: [cx + r, cy, cz], front: [cx, cy, cz + r], top34: [cx + r * 0.7, cy + r * 0.6, cz + r * 0.7] };
for (const [name, pos] of Object.entries(views)) {
  await pg.evaluate((p, c) => { const cam = window.__app.camera; cam.position.set(p[0], p[1], p[2]); cam.lookAt(c[0], c[1], c[2]); cam.updateMatrixWorld(); window.__app.controls && (window.__app.controls.enabled = false); }, pos, info.c);
  await new Promise((r) => setTimeout(r, 250));
  await pg.screenshot({ path: path.join(OUT, `car_${target}_${name}.png`) });
  console.log('shot', name);
}
await b.close(); srv.close(); console.log('done');
