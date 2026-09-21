// inspeciona esqueleto de GLBs. uso: node _t/inspect.mjs gojo_young,geto_young,toji,...
globalThis.self = globalThis;
import { GLTFLoader } from 'three/addons/loaders/GLTFLoader.js';
import { buildBoneMap } from '../src/anim/bonemap.js';
import fs from 'fs';
const ids = (process.argv[2] || '').split(',').filter(Boolean);
import * as THREE from 'three';
const loader = new GLTFLoader();
const norm = (n) => n.replace(/[\s.:[\]/]/g, '').toLowerCase();
for (const id of ids) {
  const buf = fs.readFileSync('src/models/' + id + '.glb');
  await new Promise((res) => {
    loader.parse(buf.buffer.slice(buf.byteOffset, buf.byteOffset + buf.byteLength), '', (g) => {
      let skins = 0, bones = [];
      g.scene.traverse((o) => { if (o.isSkinnedMesh) { skins++; if (o.skeleton) bones = o.skeleton.bones.map((b) => b.name); } });
      const has = (re) => bones.filter((b) => re.test(norm(b)));
      console.log('=== ' + id, 'skins=' + skins, 'bones=' + bones.length, 'anims=' + (g.animations || []).map((a) => a.name).join('|'));
      console.log('   leg:', has(/leg|thigh|upleg|calf|shin|knee/).slice(0, 8).join(','));
      console.log('   arm:', has(/arm|shoulder|hand/).slice(0, 8).join(','));
      console.log('   spine/hips:', has(/hip|pelvis|spine|chest|neck/).slice(0, 8).join(','));
      if (skins > 0) {
        const bm = buildBoneMap(g.scene);
        console.log('   MAP:');
        for (const tn in bm.map) console.log('      ', bm.map[tn].replace('mixamorig:', '').padEnd(13), '<-', tn);
        console.log('   MISSING:', bm.missing.join(','));
      }
      res();
    }, (e) => { console.log('FAIL', id, e.message || e); res(); });
  });
}
