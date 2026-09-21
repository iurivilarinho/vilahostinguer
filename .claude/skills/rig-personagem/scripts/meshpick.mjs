// Lista bbox/centroide/verts por malha de um GLB, p/ achar ARMA/SHIKIGAMI a remover
// quando as malhas tem nome generico (object_N). Marca candidatos a arma (finos/laterais).
// uso: node _t/meshpick.mjs caminho/para/modelo.glb
globalThis.self = globalThis;
import { GLTFLoader } from 'three/addons/loaders/GLTFLoader.js';
import * as THREE from 'three';
import fs from 'fs';
const file = process.argv[2];
const loader = new GLTFLoader();
const buf = fs.readFileSync(file);
await new Promise((res) => {
  loader.parse(buf.buffer.slice(buf.byteOffset, buf.byteOffset + buf.byteLength), '', (g) => {
    g.scene.updateMatrixWorld(true);
    const whole = new THREE.Box3().setFromObject(g.scene);
    const ws = whole.getSize(new THREE.Vector3());
    const wc = whole.getCenter(new THREE.Vector3());
    const H = Math.max(ws.x, ws.y, ws.z) || 1;
    const rows = [];
    g.scene.traverse((o) => {
      if (!o.isMesh) return;
      o.geometry.computeBoundingBox();
      const b = o.geometry.boundingBox.clone().applyMatrix4(o.matrixWorld);
      const s = b.getSize(new THREE.Vector3());
      const c = b.getCenter(new THREE.Vector3());
      const dims = [s.x, s.y, s.z].sort((a, b) => b - a);
      const elong = dims[0] / (dims[1] || 1e-6);          // fino/comprido?
      const lateral = Math.abs(c.x - wc.x) / (ws.x / 2 || 1); // longe do centro lateral?
      const vol = s.x * s.y * s.z;
      const verts = o.geometry.attributes.position ? o.geometry.attributes.position.count : 0;
      const matn = Array.isArray(o.material) ? o.material.map((m) => m && m.name).join(',') : (o.material && o.material.name);
      // heuristica: arma costuma ser fina (elong alto) E/OU lateral E/OU volume pequeno
      const armaScore = (elong > 3 ? 1 : 0) + (lateral > 0.55 ? 1 : 0) + (vol < (H * H * H) * 0.002 ? 1 : 0);
      rows.push({ name: o.name, verts, size: `${s.x.toFixed(2)}x${s.y.toFixed(2)}x${s.z.toFixed(2)}`, elong: elong.toFixed(1), lateral: lateral.toFixed(2), flag: armaScore >= 2 ? ' <== ARMA?' : '', mat: matn });
    });
    rows.sort((a, b) => b.elong - a.elong);
    console.log(`modelo ${file}  meshes=${rows.length}  bboxH=${H.toFixed(2)}`);
    for (const r of rows) console.log(`${(r.name || '?').padEnd(26)} v=${String(r.verts).padEnd(7)} sz=${r.size.padEnd(18)} elong=${String(r.elong).padEnd(5)} lat=${r.lateral} mat=${r.mat}${r.flag}`);
    res();
  }, (e) => { console.log('FAIL', e.message || e); res(); });
});
