// renderiza um GLB animado CRU (próprio esqueleto), sem retarget. uso: node _t/rawclip.mjs <glb> <frames>
import http from 'http'; import fs from 'fs'; import path from 'path'; import pup from 'puppeteer';
const ROOT = path.resolve('C:/dev/reforma'); const OUT = path.resolve('C:/dev/reforma/_t');
const MIME = { '.html':'text/html','.js':'text/javascript','.glb':'model/gltf-binary','.png':'image/png' };
const srv = http.createServer((q, r) => { let u = decodeURIComponent(q.url.split('?')[0]); const f = path.join(ROOT, u); if (!fs.existsSync(f)) { r.writeHead(404); r.end(); return; } r.writeHead(200, { 'Content-Type': MIME[path.extname(f)] || 'application/octet-stream' }); fs.createReadStream(f).pipe(r); });
await new Promise((r) => srv.listen(8812, r));
const glbs = (process.argv[2] || 'src/anim/mx_walk.glb').split(',');
const nF = parseInt(process.argv[3] || '4', 10);
const html = `<!doctype html><html><head><script type="importmap">{"imports":{"three":"/node_modules/three/build/three.module.js","three/addons/":"/node_modules/three/examples/jsm/"}}</script></head><body><script type="module">
import * as THREE from 'three'; import { GLTFLoader } from 'three/addons/loaders/GLTFLoader.js';
const sc=new THREE.Scene(); sc.background=new THREE.Color(0x8ec5e6);
sc.add(new THREE.HemisphereLight(0xffffff,0x444444,2)); const dl=new THREE.DirectionalLight(0xffffff,1.5); dl.position.set(2,4,3); sc.add(dl);
const cam=new THREE.PerspectiveCamera(40,820/700,0.1,100);
const rnd=new THREE.WebGLRenderer({antialias:true}); rnd.setSize(820,700); document.body.appendChild(rnd.domElement);
window.__ld=async(url)=>{const l=new GLTFLoader();const g=await l.loadAsync(url);sc.add(g.scene);
  const box=new THREE.Box3().setFromObject(g.scene);const c=box.getCenter(new THREE.Vector3());const s=box.getSize(new THREE.Vector3());
  window.__mx=new THREE.AnimationMixer(g.scene); if(g.animations[0]) window.__mx.clipAction(g.animations[0]).play();
  window.__dur=g.animations[0]?g.animations[0].duration:1; window.__c=c; window.__s=s; return {dur:window.__dur};};
window.__view=(v)=>{const c=window.__c,s=window.__s;const h=Math.max(s.y,1);const d=h*1.7;
  if(v==='front')cam.position.set(c.x,c.y,c.z+d); else cam.position.set(c.x+d*0.7,c.y+h*0.1,c.z+d*0.7); cam.lookAt(c.x,c.y,c.z); cam.updateMatrixWorld();};
window.__seek=(t)=>{window.__mx.setTime(t); rnd.render(sc,cam);};
</script></body></html>`;
fs.writeFileSync(path.join(OUT, '_raw.html'), html);
const b = await pup.launch({ headless: 'new', args: ['--no-sandbox', '--use-gl=angle', '--ignore-gpu-blocklist'] });
const pg = await b.newPage(); await pg.setViewport({ width: 820, height: 700 });
pg.on('pageerror', (e) => console.log('ERR', e.message.slice(0, 160)));
await pg.goto('http://localhost:8812/_t/_raw.html', { waitUntil: 'load' });
await new Promise((r) => setTimeout(r, 600));
const VIEW = process.env.VIEW || 'front';
const cells = [];
for (const glb of glbs) {
  const info = await pg.evaluate((u) => window.__ld(u), '/' + glb);
  await pg.evaluate((v) => window.__view(v), VIEW);
  const name = path.basename(glb, '.glb');
  for (let i = 0; i < nF; i++) {
    await pg.evaluate((t) => window.__seek(t), info.dur * (i / nF));
    await new Promise((r) => setTimeout(r, 120));
    await pg.screenshot({ path: path.join(OUT, `raw_${name}_${i}.png`) });
    cells.push(`raw_${name}_${i}.png`);
  }
  console.log('ok', name, 'dur=', info.dur.toFixed(2));
}
await b.close(); srv.close();
console.log('FRAMES', cells.join(','));
