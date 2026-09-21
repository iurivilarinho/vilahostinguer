// AUTO-RIG completo no Mixamo: upload FBX -> orient -> marcadores -> rig ->
// finaliza -> baixa o FBX riggado (mixamorig). uso:
//   MX_FILE=_t/_rig/geto_young.fbx MX_OUT=geto_young node _t/mixamo_rig.mjs
import pup from 'puppeteer';
import fs from 'fs'; import path from 'path'; import https from 'https';
const OUT = 'C:/dev/reforma/_t'; const DL = 'C:/dev/reforma/_t/_rigged';
fs.mkdirSync(DL, { recursive: true });
const EMAIL = process.env.MX_EMAIL, PASS = process.env.MX_PASS;
const FILE = path.resolve(process.env.MX_FILE || '_t/_rig/geto_young.fbx');
const NAME = process.env.MX_OUT || path.basename(FILE, '.fbx');
const sleep = (ms) => new Promise((r) => setTimeout(r, ms));
const b = await pup.launch({ headless: false, args: ['--no-sandbox', '--disable-blink-features=AutomationControlled', '--start-maximized'], defaultViewport: null });
const pg = (await b.pages())[0];
await pg.setUserAgent('Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36');
const shot = (n) => pg.screenshot({ path: `${OUT}/rig_${n}.png` }).then(() => console.log('shot', n)).catch(() => {});

// captura URL S3 do download
const dlState = { url: null, file: null };
try {
  const c = await b.target().createCDPSession();
  await c.send('Browser.setDownloadBehavior', { behavior: 'deny', eventsEnabled: true });
  c.on('Browser.downloadWillBegin', (e) => { dlState.url = e.url; dlState.file = e.suggestedFilename; console.log('dlBegin', e.suggestedFilename); });
} catch (e) { console.log('cdp fail', e.message); }
function fetchTo(url, dest) {
  return new Promise((res, rej) => { const go = (u, n) => { if (n > 5) return rej(new Error('redir')); https.get(u, (r) => { if (r.statusCode >= 300 && r.statusCode < 400 && r.headers.location) { r.resume(); return go(r.headers.location, n + 1); } if (r.statusCode !== 200) { r.resume(); return rej(new Error('HTTP ' + r.statusCode)); } const f = fs.createWriteStream(dest); r.pipe(f); f.on('finish', () => f.close(() => res(dest))); }).on('error', rej); }; go(url, 0); });
}
const clickByText = async (re) => { for (let k = 0; k < 3; k++) { try { return await pg.evaluate((rs) => { const re = new RegExp(rs, 'i'); const b = [...document.querySelectorAll('button,a')].find((x) => re.test((x.textContent || '').trim())); if (b) { b.click(); return (b.textContent || '').trim(); } return null; }, re.source); } catch (e) { await sleep(1200); } } return null; };
const bodyText = async () => { for (let k = 0; k < 4; k++) { try { return await pg.evaluate(() => document.body.innerText); } catch (e) { console.log('bodyText retry', (e.message || '').slice(0, 40)); await sleep(1500); } } return ''; };

async function login() {
  for (let attempt = 0; attempt < 2; attempt++) {
    try {
      await pg.goto('https://www.mixamo.com/', { waitUntil: 'networkidle2', timeout: 60000 }); await sleep(2000);
      await pg.evaluate(() => { const e = [...document.querySelectorAll('a,button')].find((x) => /log ?in|sign ?in/i.test(x.textContent || '')); if (e) e.click(); });
      await sleep(4000);
      if (await pg.$('input[type="email"], input[name="username"]')) {
        await pg.type('input[type="email"], input[name="username"]', EMAIL, { delay: 40 });
        await pg.evaluate(() => { const b = [...document.querySelectorAll('button')].find((x) => /continue|next/i.test(x.textContent)); if (b) b.click(); });
        await sleep(4000); await pg.waitForSelector('input[type="password"]', { timeout: 30000 });
        await pg.type('input[type="password"]', PASS, { delay: 40 });
        await pg.evaluate(() => { const b = [...document.querySelectorAll('button')].find((x) => /continue|sign ?in|log ?in/i.test(x.textContent)); if (b) b.click(); });
        await sleep(9000);
      }
      await pg.goto('https://www.mixamo.com/#/', { waitUntil: 'networkidle2', timeout: 60000 }); await sleep(4000);
      if (!/login|signin/i.test(pg.url())) return;
    } catch (e) { console.log('login retry', e.message); await sleep(3000); }
  }
}

await login();
console.log('logged', pg.url());
// UPLOAD CHARACTER
await pg.evaluate(() => { const all = [...document.querySelectorAll('button,a,span,div')]; const ex = all.filter((x) => (x.textContent || '').trim().toLowerCase() === 'upload character'); (ex[ex.length - 1] || all.find((x) => /upload character/i.test(x.textContent || '') && x.textContent.length < 25))?.click(); });
await sleep(3000);
let fi = await pg.$('input[type="file"]'); if (!fi) { await sleep(2000); fi = await pg.$('input[type="file"]'); }
if (!fi) { console.log('NO FILE INPUT'); await shot('err_nofile'); await b.close(); process.exit(1); }
await fi.uploadFile(FILE); console.log('uploaded', FILE);
// espera o processamento do upload terminar (arquivos grandes demoram): aguarda
// a tela "Orient" aparecer (e sumir "processing"), até ~90s.
let oriented = false;
for (let i = 0; i < 30; i++) {
  await sleep(3000);
  const t = await bodyText();
  if (/orient|t-pose|place markers/i.test(t) && !/processing character/i.test(t)) { oriented = true; break; }
}
console.log('oriented?', oriented);
await sleep(1500); await shot('a_orient');
// ORIENT -> NEXT
await clickByText(/^next$/); await sleep(6000); await shot('b_markers');
// MARCADORES
async function drag(from, to) { await pg.mouse.move(from[0], from[1]); await sleep(110); await pg.mouse.down(); await sleep(110); await pg.mouse.move((from[0] + to[0]) / 2, (from[1] + to[1]) / 2, { steps: 8 }); await sleep(70); await pg.mouse.move(to[0], to[1], { steps: 8 }); await sleep(110); await pg.mouse.up(); await sleep(180); }
const M = { chin: [565, 234], wristL: [446, 276], wristR: [684, 276], elbowL: [507, 277], elbowR: [623, 277], kneeL: [543, 535], kneeR: [589, 535], groin: [565, 432] };
let mk = [];
for (let k = 0; k < 3; k++) { try { mk = await pg.evaluate(() => [...document.querySelectorAll('.autorig-marker')].map((m) => { const r = m.getBoundingClientRect(); return [Math.round(r.x + r.width / 2), Math.round(r.y + r.height / 2)]; })); break; } catch (e) { console.log('marker read retry', (e.message || '').slice(0, 40)); await sleep(1500); } }
console.log('markers', mk.length);
const order = ['chin', 'wristL', 'wristR', 'elbowL', 'elbowR', 'kneeL', 'kneeR', 'groin'];
// MX_NOPLACE=1: NAO força marcador fixo (A-pose). Usa a estimativa automática do Mixamo
// (melhor p/ pose não-A: combate, mãos na cintura, braço-baixo).
if (process.env.MX_NOPLACE) { console.log('NOPLACE: usando marcadores estimados do Mixamo'); }
else { for (let i = 0; i < Math.min(mk.length, order.length); i++) await drag(mk[i], M[order[i]]); }
await shot('c_placed');
// NEXT -> inicia rig
await clickByText(/^next$/); await sleep(4000); await shot('d_rigstart');
// espera rig terminar: some "auto-rigging", aparece preview/Next clicável
let done = false;
for (let i = 0; i < 100; i++) { // ~300s (modelos pesados rigam devagar)
  await sleep(3000);
  const t = await bodyText();
  const hasWait = /please wait|auto-rigging/i.test(t);
  if (!hasWait) { done = true; break; }
}
console.log('rig done?', done); await shot('e_rigdone');
// FINALIZA: clica NEXT/Finish até o modal sumir E confirmar "Proceed with this new
// character?" (senão o download pega o personagem ANTERIOR ainda ativo = bug fern).
for (let i = 0; i < 8; i++) {
  const c = await clickByText(/^(next|finish|done|proceed)$/); console.log('finalize click', c); await sleep(3500);
  const open = /auto-rigger|proceed with this new character|has been uploaded/i.test(await bodyText());
  if (!open) break;
}
await sleep(5000); await shot('f_afterfinalize');
// DOWNLOAD do personagem riggado
dlState.url = null;
await clickByText(/^download$/); await sleep(3000); await shot('g_dlmodal');
// confirma no modal (DOWNLOAD ao lado do CANCEL)
await pg.evaluate(() => { const btns = [...document.querySelectorAll('button')]; const cancel = btns.find((x) => /^\s*cancel\s*$/i.test(x.textContent || '')); const cy = cancel ? cancel.getBoundingClientRect().y : 0; const dl = btns.find((x) => x !== cancel && /^\s*download\s*$/i.test(x.textContent || '') && (!cancel || Math.abs(x.getBoundingClientRect().y - cy) < 60)); (dl || btns.find((x) => /^\s*download\s*$/i.test(x.textContent || '')))?.click(); });
const t0 = Date.now();
while (!dlState.url && Date.now() - t0 < 40000) await sleep(500);
if (dlState.url) { const dlf = (dlState.file || '').toLowerCase(); const base = NAME.toLowerCase().split('_')[0]; if (dlf && !dlf.includes(base)) console.log('WARN: download name', dlState.file, 'NAO bate com', NAME, '-> possivel personagem ERRADO'); try { await fetchTo(dlState.url, path.join(DL, NAME + '.fbx')); console.log('SAVED', NAME + '.fbx', fs.statSync(path.join(DL, NAME + '.fbx')).size, 'dlname=' + dlState.file); } catch (e) { console.log('FETCH FAIL', e.message); } }
else { console.log('NO DL URL'); await shot('h_nodl'); }
await sleep(2000); await b.close(); console.log('done');
