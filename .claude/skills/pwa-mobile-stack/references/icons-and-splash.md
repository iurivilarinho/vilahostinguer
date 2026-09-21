# Gerar ícones + iOS splash screens

Use quando trocar branding, gerar para novo projeto ou recriar artefatos perdidos. Tudo a partir de um único SVG (`public/command-icon.svg`).

## Pré-requisitos

```bash
npm install -D sharp @vite-pwa/assets-generator
```

`sharp` está sob `devDependencies`. `@vite-pwa/assets-generator` é opcional — o script abaixo já cobre tudo.

## Script — ícones PWA (192, 512, maskable, apple, favicon)

Roda na raiz do `frontend-comandas`:

```bash
node -e "
const sharp = require('sharp');
const fs = require('fs');
const path = require('path');

const svg = fs.readFileSync('public/command-icon.svg');
const out = (size, name) => sharp(svg).resize(size, size).png().toFile(path.join('public', name));

(async () => {
  await out(192, 'icon-192.png');
  await out(512, 'icon-512.png');
  await out(180, 'apple-touch-icon-180.png');
  await out(32,  'favicon-32.png');

  // Maskable: precisa safe-area extra (~10% padding) com bg da brand.
  await sharp(svg)
    .resize(410, 410)
    .extend({ top: 51, bottom: 51, left: 51, right: 51, background: '#ea1d2c' })
    .png()
    .toFile(path.join('public', 'icon-512-maskable.png'));

  console.log('ok');
})();
"
```

## Script — Apple splash screens (20 tamanhos)

```bash
mkdir -p public/splash

node -e "
const sharp = require('sharp');
const fs = require('fs');
const path = require('path');

const svg = fs.readFileSync('public/command-icon.svg');
const bg = '#f5f5f5';

const splashes = [
  { w: 750,  h: 1334 }, { w: 1334, h: 750  },  // iPhone SE/8
  { w: 1125, h: 2436 }, { w: 2436, h: 1125 },  // iPhone X/11 Pro
  { w: 828,  h: 1792 }, { w: 1792, h: 828  },  // iPhone 11/XR
  { w: 1242, h: 2688 }, { w: 2688, h: 1242 },  // iPhone 11 Pro Max
  { w: 1170, h: 2532 }, { w: 2532, h: 1170 },  // iPhone 12/13/14
  { w: 1179, h: 2556 }, { w: 2556, h: 1179 },  // iPhone 14 Pro
  { w: 1290, h: 2796 }, { w: 2796, h: 1290 },  // iPhone 14/15 Pro Max
  { w: 1536, h: 2048 }, { w: 2048, h: 1536 },  // iPad
  { w: 1668, h: 2388 }, { w: 2388, h: 1668 },  // iPad Pro 11
  { w: 2048, h: 2732 }, { w: 2732, h: 2048 },  // iPad Pro 12.9
];

(async () => {
  for (const { w, h } of splashes) {
    const logoSize = Math.round(Math.min(w, h) * 0.32);
    const logoBuf = await sharp(svg).resize(logoSize, logoSize).png().toBuffer();
    await sharp({ create: { width: w, height: h, channels: 4, background: bg } })
      .composite([{ input: logoBuf, gravity: 'center' }])
      .png()
      .toFile(path.join('public', 'splash', \`apple-splash-\${w}x\${h}.png\`));
  }
  console.log('ok');
})();
"
```

## Os 20 `<link rel="apple-touch-startup-image">` no `index.html`

Cada par portrait/landscape exige media query distinto com device-width, device-height e device-pixel-ratio. Padrão:

```html
<link rel="apple-touch-startup-image" href="/splash/apple-splash-WxH.png"
      media="(device-width: AAApx) and (device-height: BBBpx) and (-webkit-device-pixel-ratio: D) and (orientation: portrait)" />
```

Ver `frontend-comandas/index.html` para a tabela completa (20 entradas) que cobre iPhone SE até iPad Pro 12.9". Não inventar valores — copiar exatamente desses entries.

## Adicionar ao `vite-plugin-pwa`

```ts
includeAssets: [
  "command-icon.svg",
  "favicon-32.png",
  "apple-touch-icon-180.png",
  "runtime-config.js",
  "splash/*.png",  // <- splash entra no precache
],
manifest: {
  icons: [
    { src: "/icon-192.png", sizes: "192x192", type: "image/png" },
    { src: "/icon-512.png", sizes: "512x512", type: "image/png" },
    { src: "/icon-512-maskable.png", sizes: "512x512", type: "image/png", purpose: "maskable" },
  ],
}
```
