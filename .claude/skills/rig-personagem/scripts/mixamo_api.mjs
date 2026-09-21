// baixa animações via API do Mixamo (robusto). headful login -> token -> API.
// uso: MX_LIST='walk:walking,run:running,idle:idle,jump:jump' node _t/mixamo_api.mjs
import pup from 'puppeteer';
import fs from 'fs'; import path from 'path'; import https from 'https';
const OUT = 'C:/dev/reforma/_t'; const DL = 'C:/dev/reforma/_anim_fbx';
fs.mkdirSync(DL, { recursive: true });
const EMAIL = process.env.MX_EMAIL, PASS = process.env.MX_PASS;
const sleep = (ms) => new Promise((r) => setTimeout(r, ms));
// item: file:query:inplace(1/0). inplace=1 p/ locomoção (walk/run).
const WANT = (process.env.MX_LIST || 'walk:walking:1').split(',').map((s) => s.split(':'));
const PICK = process.env.MX_PICK || ''; // override exato: "walk=Walking,run=Fast Run"
const picks = {}; PICK.split(',').filter(Boolean).forEach((kv) => { const [k, v] = kv.split('='); picks[k] = v; });
// personagem-fonte: "Adam" (system, masculino, esqueleto mixamorig padrão) — evita
// o balanço/rebolado de personagens femininos. Todos os clips no MESMO esqueleto.
const CHAR_ID = process.env.MX_CHAR || 'd8d42014-7b46-4784-9803-9eb95f197233';

const b = await pup.launch({ headless: false, args: ['--no-sandbox', '--disable-blink-features=AutomationControlled', '--start-maximized'], defaultViewport: null });
const pg = (await b.pages())[0];
await pg.setUserAgent('Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36');
const shot = (n) => pg.screenshot({ path: `${OUT}/api_${n}.png` }).then(() => console.log('shot', n));

function fetchTo(url, dest) {
  return new Promise((res, rej) => {
    const go = (u, n) => { if (n > 5) return rej(new Error('redirects'));
      https.get(u, (r) => { if (r.statusCode >= 300 && r.statusCode < 400 && r.headers.location) { r.resume(); return go(r.headers.location, n + 1); }
        if (r.statusCode !== 200) { r.resume(); return rej(new Error('HTTP ' + r.statusCode)); }
        const f = fs.createWriteStream(dest); r.pipe(f); f.on('finish', () => f.close(() => res(dest))); }).on('error', rej); };
    go(url, 0);
  });
}
async function login() {
  await pg.goto('https://www.mixamo.com/', { waitUntil: 'networkidle2', timeout: 60000 });
  await sleep(2000);
  await pg.evaluate(() => { const e = [...document.querySelectorAll('a,button')].find((x) => /log ?in|sign ?in/i.test(x.textContent || '')); if (e) e.click(); });
  await sleep(4000);
  const emailSel = 'input[type="email"], input[name="username"]';
  if (await pg.$(emailSel)) {
    await pg.type(emailSel, EMAIL, { delay: 40 });
    await pg.evaluate(() => { const b = [...document.querySelectorAll('button')].find((x) => /continue|next/i.test(x.textContent)); if (b) b.click(); });
    await sleep(4000);
    await pg.waitForSelector('input[type="password"]', { timeout: 30000 });
    await pg.type('input[type="password"]', PASS, { delay: 40 });
    await pg.evaluate(() => { const b = [...document.querySelectorAll('button')].find((x) => /continue|sign ?in|log ?in/i.test(x.textContent)); if (b) b.click(); });
    await sleep(9000);
  }
  await pg.goto('https://www.mixamo.com/#/', { waitUntil: 'networkidle2', timeout: 60000 });
  await sleep(4000);
  console.log('after login url:', pg.url());
}

await login();
await shot('logged');
// token: Mixamo guarda em localStorage. Tenta achar a chave do access token.
const token = await pg.evaluate(() => {
  for (const k of Object.keys(localStorage)) {
    if (/access_token|adobeid|token/i.test(k)) {
      try { const v = JSON.parse(localStorage.getItem(k)); if (v && (v.access_token || v.tokenValue)) return v.access_token || v.tokenValue; } catch {}
      const raw = localStorage.getItem(k); if (raw && raw.length > 40 && /^[\w-]+\.[\w-]+\./.test(raw)) return raw;
    }
  }
  return null;
});
console.log('token?', token ? token.slice(0, 18) + '...' : 'NONE');

// helper: chama a API DENTRO da página (cookies + bearer). Mixamo exige header X-Api-Key.
async function api(pathUrl, opts) {
  return pg.evaluate(async (pathUrl, opts, token) => {
    const r = await fetch('https://www.mixamo.com' + pathUrl, {
      method: (opts && opts.method) || 'GET',
      headers: Object.assign({ 'Accept': 'application/json', 'X-Api-Key': 'mixamo2', 'Content-Type': 'application/json', ...(token ? { 'Authorization': 'Bearer ' + token } : {}) }, (opts && opts.headers) || {}),
      credentials: 'include',
      body: opts && opts.body ? JSON.stringify(opts.body) : undefined,
    });
    const t = await r.text(); let j = null; try { j = JSON.parse(t); } catch {}
    return { status: r.status, json: j, text: t.slice(0, 300) };
  }, pathUrl, opts || {}, token);
}

const charId = CHAR_ID;
console.log('character_id:', charId);

for (const [file, query, inplaceFlag] of WANT) {
  try {
    const prods = await api('/api/v1/products?page=1&limit=48&type=Motion&query=' + encodeURIComponent(query));
    if (!prods.json || !prods.json.results) { console.log('search fail', file, prods.status, prods.text); continue; }
    const list = prods.json.results;
    console.log(file, 'results:', list.map((p) => p.name).slice(0, 12).join(' | '));
    const want = (picks[file] || query).toLowerCase();
    let prod = list.find((p) => p.name.toLowerCase() === want)
      || list.find((p) => p.name.toLowerCase() === query.toLowerCase())
      || list.filter((p) => !/zombie|injured|drunk|sad|scared|silly|sexy|crazy|hurt|limp|crouch|sneak|stagger|hobble|left|right|turn|back|in place|reaction|stop|start/i.test(p.name))
              .sort((a, b) => a.name.length - b.name.length)[0]
      || list[0];
    console.log('  -> picked:', prod.name, prod.id);
    // detalhes p/ pegar gms_hash (com o character certo p/ casar model-id)
    const det = await api('/api/v1/products/' + prod.id + '?similar=0&character_id=' + charId);
    const d = det.json;
    if (!d || !d.details || !d.details.gms_hash) { console.log('  no gms_hash', det.status, det.text); continue; }
    const g = d.details.gms_hash;
    const inplace = inplaceFlag === '1' && d.details.supports_inplace !== false;
    // params vira string de VALORES separados por vírgula (formato do export)
    const gms = {
      'model-id': g['model-id'], mirror: false, trim: g.trim || [0, 100],
      inplace, 'arm-space': g['arm-space'] || 0,
      params: (g.params || []).map((p) => Array.isArray(p) ? p[1] : p).join(','),
    };
    const exportBody = {
      character_id: charId,
      gms_hash: [gms], // export de 1 motion = array de 1 elemento
      preferences: { format: 'fbx7_2019', skin: 'true', fps: '30', reducekf: '0' },
      product_name: prod.name,
      type: 'Motion',
    };
    const exp = await api('/api/v1/animations/export', { method: 'POST', body: exportBody });
    console.log('  export status', exp.status, exp.text.slice(0, 120));
    // poll monitor
    let url = null;
    for (let i = 0; i < 40; i++) {
      await sleep(1500);
      const mon = await api('/api/v1/characters/' + charId + '/monitor');
      const m = mon.json;
      if (m && m.status === 'completed' && m.job_result) { url = m.job_result; break; }
      if (m && m.status === 'failed') { console.log('  export FAILED', mon.text); break; }
    }
    if (url) { await fetchTo(url, path.join(DL, file + '.fbx')); console.log('  SAVED', file + '.fbx', fs.statSync(path.join(DL, file + '.fbx')).size); }
    else console.log('  NO URL for', file);
  } catch (e) { console.log('ERR', file, e.message); }
}
await sleep(1500);
await b.close();
console.log('done; files:', fs.readdirSync(DL));
