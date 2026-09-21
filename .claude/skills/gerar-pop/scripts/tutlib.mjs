// Motor de captura de telas WEB para POPs (Playwright).
// Captura + anota com caixas vermelhas e badges numerados ("belezinha").
//
// Config por env var (ou edite os defaults):
//   POP_BASE   -> URL base do sistema        (ex.: https://goldenticket.lat:4000)
//   POP_OUT    -> pasta de saída das imagens  (ex.: C:/dev/checklist/tutorial/imagens/web)
//   POP_USER / POP_PASS -> credenciais de login
import { chromium } from 'playwright';

export const BASE = process.env.POP_BASE || 'https://goldenticket.lat:4000';
export const OUT = process.env.POP_OUT || 'C:/dev/checklist/tutorial/imagens/web';
const USER = process.env.POP_USER || 'admin';
const PASS = process.env.POP_PASS || 'admin@123';

export async function launch({ width = 1440, height = 900 } = {}) {
  const browser = await chromium.launch();
  const ctx = await browser.newContext({
    viewport: { width, height },
    ignoreHTTPSErrors: true,
    deviceScaleFactor: 2, // telas nítidas (retina) — não baixe disso
  });
  const page = await ctx.newPage();
  return { browser, ctx, page };
}

export async function login(page) {
  await page.goto(`${BASE}/login`, { waitUntil: 'networkidle' });
  try {
    const ok = page.locator('[data-slot="dialog-content"] button').first();
    if (await ok.isVisible({ timeout: 1200 })) await ok.click();
  } catch {}
  await page.fill('#username', USER);
  await page.fill('#password', PASS);
  await page.click('button[type="submit"]');
  await page.waitForURL((u) => !u.toString().includes('/login'), { timeout: 15000 });
  await page.waitForLoadState('networkidle');
}

// Injeta anotações: caixas vermelhas + badges numerados + rótulos opcionais.
// targets: [{ selector?, x?,y?,w?,h?, label?, num?, place?, pad? }]
// place: 'tl'|'tr'|'bl'|'br'|'top'|'bottom'|'left'|'right' (posição do badge/rótulo)
export async function annotate(page, targets) {
  await page.evaluate((targets) => {
    const layer = document.createElement('div');
    layer.id = '__tut_layer';
    Object.assign(layer.style, {
      position: 'fixed', inset: '0', zIndex: '2147483647', pointerEvents: 'none',
    });
    document.body.appendChild(layer);
    const RED = '#e11d48';
    for (const t of targets) {
      let r;
      if (t.selector) {
        const el = document.querySelector(t.selector);
        if (!el) continue;
        r = el.getBoundingClientRect();
      } else {
        r = { left: t.x, top: t.y, width: t.w, height: t.h };
      }
      const pad = t.pad ?? 4;
      const box = document.createElement('div');
      Object.assign(box.style, {
        position: 'fixed',
        left: (r.left - pad) + 'px', top: (r.top - pad) + 'px',
        width: (r.width + pad * 2) + 'px', height: (r.height + pad * 2) + 'px',
        border: `3px solid ${RED}`, borderRadius: '8px',
        boxShadow: '0 0 0 3px rgba(225,29,72,0.25)',
        boxSizing: 'border-box',
      });
      layer.appendChild(box);

      if (t.num != null || t.label) {
        const chip = document.createElement('div');
        const txt = [t.num != null ? t.num : null, t.label].filter((x) => x != null && x !== '').join('  ');
        chip.textContent = txt;
        Object.assign(chip.style, {
          position: 'fixed',
          background: RED, color: '#fff',
          font: '600 14px/1.2 system-ui, Segoe UI, Arial, sans-serif',
          padding: '5px 9px', borderRadius: '7px',
          boxShadow: '0 2px 6px rgba(0,0,0,0.35)', whiteSpace: 'nowrap',
        });
        const place = t.place || 'tr';
        chip.style.left = '-9999px'; chip.style.top = '-9999px';
        layer.appendChild(chip);
        const cw = chip.offsetWidth, ch = chip.offsetHeight;
        let cx, cy;
        const gap = 8;
        const L = r.left - pad, T = r.top - pad, W = r.width + pad * 2, H = r.height + pad * 2;
        switch (place) {
          case 'tl': cx = L; cy = T - ch - gap; break;
          case 'tr': cx = L + W - cw; cy = T - ch - gap; break;
          case 'bl': cx = L; cy = T + H + gap; break;
          case 'br': cx = L + W - cw; cy = T + H + gap; break;
          case 'top': cx = L + W / 2 - cw / 2; cy = T - ch - gap; break;
          case 'bottom': cx = L + W / 2 - cw / 2; cy = T + H + gap; break;
          case 'left': cx = L - cw - gap; cy = T + H / 2 - ch / 2; break;
          case 'right': cx = L + W + gap; cy = T + H / 2 - ch / 2; break;
          default: cx = L + W - cw; cy = T - ch - gap;
        }
        cx = Math.max(4, Math.min(cx, window.innerWidth - cw - 4));
        cy = Math.max(4, Math.min(cy, window.innerHeight - ch - 4));
        chip.style.left = cx + 'px'; chip.style.top = cy + 'px';
      }
    }
  }, targets);
}

export async function clearAnnotations(page) {
  await page.evaluate(() => document.getElementById('__tut_layer')?.remove());
}

// Resolve um locator Playwright para {x,y,w,h} + props extras p/ annotate()
export async function coords(loc, extra = {}) {
  const b = await loc.boundingBox();
  if (!b) return null;
  return { x: b.x, y: b.y, w: b.width, h: b.height, ...extra };
}

export async function shot(page, name, { full = false } = {}) {
  await page.screenshot({ path: `${OUT}/${name}.png`, fullPage: full });
  await clearAnnotations(page);
}
