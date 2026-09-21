---
name: gerar-pop
description: Gera um POP (Procedimento Operacional Padrão / tutorial ilustrado) de um sistema, do mesmo jeito que o tutorial em D:/dev/aurora/checklist/tutorial — markdown passo a passo + .docx de marca, com prints capturados AUTOMATICAMENTE do sistema (web via Playwright, mobile Android via adb) e anotados com caixas vermelhas e badges numerados ("belezinha") que batem com os passos numerados do texto. Use quando o usuário pedir um "pop", tutorial, manual, guia do usuário, passo a passo ou material de treinamento com imagens do sistema.
---

# Gerar POP (tutorial ilustrado de sistema)

POP aqui = **manual passo a passo com imagens reais do sistema**, cada imagem com
**caixas vermelhas + números** ("belezinha") indicando onde clicar/tocar, e os números
batendo com a **lista numerada** do texto. Entrega em `.md` **autocontido** (imagens
embutidas em base64 — um arquivo único, sem pasta junto) e `.docx` (para entregar ao
cliente, com marca). Referência viva do resultado esperado: `D:/dev/aurora/checklist/tutorial/`.

> **Padrão: o `.md` final é AUTOCONTIDO.** Você escreve o markdown referenciando as imagens
> em `imagens/` (fácil de editar), e no fim roda `embed_images.py` para gerar o `.md` de
> entrega com as imagens embutidas como `data:image/...;base64,...`. Assim o `.md` vira **um
> arquivo único** que mostra as figuras em qualquer lugar (GitHub, VS Code, ao mandar avulso),
> sem depender da pasta `imagens/`. A pasta `imagens/` é mantida como **fonte** para reeditar/regerar.

**O diferencial:** as imagens NÃO são pedidas ao usuário nem mockadas — eu **capturo do
sistema rodando** e **anoto programaticamente**. Web por Playwright; mobile Android por adb.

## O que entregar (layout fixo)

```
<destino>/                         # ex.: D:/dev/aurora/checklist/tutorial
  00-LEIA-ME.md                    # índice: o que é cada arquivo + resumo do fluxo (AUTOCONTIDO)
  01-Tutorial-<Algo>.md            # ex.: 01-Tutorial-Web.md — ENTREGA, autocontido (base64)
  01-Tutorial-<Algo>.docx          # gerado pelo md2docx.py
  02-Tutorial-<Outro>.md           # ex.: 02-Tutorial-Mobile.md (se houver)
  02-Tutorial-<Outro>.docx
  _src/                            # opcional: rascunho do .md com links p/ imagens/ (antes de embutir)
  imagens/                         # PNGs originais — FONTE das imagens (mantida p/ regerar)
    web/    web-01-login.png  web-02-dashboard.png ...
    mobile/ mobile-01-login.png  mobile-02-home.png ...
  _cap/                            # temporários do mobile (pode apagar no fim)
```

> O `.md` de **entrega** é autocontido. Se quiser manter um fonte editável com links relativos
> (`![](imagens/web/web-01-login.png)`), guarde-o em `_src/` e gere o autocontido com `embed_images.py`.

Nomes de imagem: **`<canal>-NN-nome-curto.png`**, NN com 2 dígitos na ordem do tutorial.
Variações da mesma tela ganham sufixo de letra: `web-04b-clientes-acoes.png`.

## Convenção da "belezinha" (não desvie disto)

- Caixa: borda vermelha `#e11d48`, cantos arredondados, leve glow. Web `pad:4`, mobile `pad:6`.
- Badge: pílula vermelha, texto branco, com **número** e (opcional) **rótulo** curto.
- `place`: `tl|tr|bl|br|top|bottom|left|right` — posiciona o badge fora da caixa sem cobrir conteúdo.
- **Regra de ouro:** o número do badge na imagem = o número do passo na lista do markdown.
  Imagem mostra "1, 2, 3"; o texto logo abaixo lista "1. … 2. … 3. …" na mesma ordem.
- Telas só ilustrativas (dashboard, formulário inteiro) entram **sem** badges, com `full: true`.

Os três motores que produzem isso já estão em `./scripts/` — **use-os, não reinvente**.

---

## Processo (passo a passo)

### 0. Combinar o escopo
Descubra com o usuário (ou com o repo): quais **canais** (web? mobile? ambos?), quais **telas/fluxos**
entram e em que **ordem**, a **URL/credenciais** do ambiente e a **pasta destino**. Espelhe a ordem
real de uso do sistema (no LIS: Clientes → Ambientes → Objetos → Checklists → Cronograma → …).

### 1. Capturar as telas WEB (Playwright)
Motor: `./scripts/tutlib.mjs` (`launch`, `login`, `annotate`, `coords`, `shot`).

1. Garanta o Playwright instalado (reusa o do projeto): `frontend-checklist/e2e/node_modules/playwright`.
   Se não houver, `cd frontend-checklist/e2e && npm i playwright && npx playwright install chromium`.
2. Configure por env var: `POP_BASE` (URL), `POP_OUT` (pasta imagens/web), `POP_USER`, `POP_PASS`.
3. Escreva um `cap_<sistema>.mjs` (na pasta e2e do projeto) que importa o motor e, **por tela**:
   navega → (anota alvos) → `shot`. Padrão de cada bloco:

```js
import { launch, login, annotate, shot, coords, BASE } from './scripts/tutlib.mjs';
const { browser, page } = await launch();         // viewport 1440x900, retina 2x

// tela COM destaques (campos do login)
await page.goto(`${BASE}/login`, { waitUntil: 'networkidle' });
await page.fill('#username', 'admin'); await page.fill('#password', 'admin@123');
await annotate(page, [
  { ...await coords(page.locator('#username')),               num: 1, label: 'Usuário', place: 'right' },
  { ...await coords(page.locator('#password')),               num: 2, label: 'Senha',   place: 'right' },
  { ...await coords(page.locator('button[type="submit"]')),   num: 3, label: 'Entrar',  place: 'right' },
]);
await shot(page, 'web-01-login');

await login(page);                                  // loga de verdade p/ o resto

// tela só ilustrativa (página inteira, sem badges)
await page.goto(`${BASE}/`, { waitUntil: 'networkidle' });
await shot(page, 'web-02-dashboard', { full: true });

await browser.close();
```

Dicas: prefira `data-slot`, `a[href="…"]`, `getByRole('button', { name: … })`. Use `coords(locator, {num,label,place,pad})`
para resolver o retângulo do elemento. `shot` limpa as anotações sozinho depois. Rode com `node cap_<sistema>.mjs`.
Veja exemplos completos em `frontend-checklist/e2e/cap_web.mjs`.

### 2. Capturar as telas MOBILE (Android via adb)
Motor: `./scripts/mshot.mjs`. Precisa de **emulador ou aparelho conectado** (`adb devices`) com o app aberto.

- `shot(name, annots)` — tira o print e, se passar anotações, lê os bounds via `uiautomator dump`
  e pinta as caixas/badges (mesmo visual do web). Anote por **texto** ou por **bounds**:

```js
import { shot, tap, swipe, text, sleep, dumpUI } from './scripts/mshot.mjs';

// dirige o app
tap(540, 1600); await sleep(800);                  // toca em coordenada
// text('admin');                                   // digita no campo focado

// print + destaques (acha o elemento pelo texto visível)
await shot('mobile-02-home', [
  { q: 'Iniciar inspeção', num: 1, label: 'Começar', place: 'top' },
  { q: 'Sincronizar',      num: 2, place: 'left' },
]);

// quando o texto não basta, passe bounds manuais [x1,y1,x2,y2]:
const n = dumpUI();
const bar = n.find(x => /Galpão/.test(x.text));
await shot('mobile-04-checklist', [{ bounds: [0, bar.bounds[1]-6, 1080, bar.bounds[3]+6], num: 1, label: 'Ambiente e tempo', place: 'bottom', pad: 0 }]);
```

- Config: `POP_OUT_MOBILE`, `POP_TMP`, `POP_PLAYWRIGHT` (caminho do playwright já instalado).
- Telas sem destaque: `await shot('mobile-12-sair')` (sem o 2º argumento → só copia o print).
- Exemplos reais (driving + dump + annotate) em `mobile-lis-checklist/_tut/`.

### 3. Escrever o markdown do tutorial
Estilo (copie de `tutorial/01-Tutorial-Web.md`):

- `# Título` (vira capa no docx) → parágrafo de boas-vindas → `## Índice` com lista de links âncora.
- Cada seção: `## N. Nome da seção`, um parágrafo curto, a **imagem**, depois a **lista numerada**
  cujos números batem com os badges. Separe seções com `---`.
- **Avisos/dicas** em blockquote com emoji: `> 💡 …`, `> 🔑 …`, `> 📅 …` (viram caixas azuis no docx).
- **Legendas de itens** (ex.: menu) em tabela `| Nº | Item | Para que serve |`.
- Caminho das imagens **relativo**: `![alt](imagens/web/web-01-login.png)`.
- Português claro, tom de quem ensina o cliente. Sem jargão técnico desnecessário.

Crie/atualize também o `00-LEIA-ME.md` (tabela de arquivos + "Resumo do fluxo do sistema").

> 💻 **POPs técnicos (CLI/infra):** quando o sistema for de linha de comando (sem tela web/mobile
> pra fotografar), as "imagens" são **diagramas**. Escreva um HTML por diagrama (cards/setas/código)
> e capture com Playwright (`page.screenshot({ fullPage:true })`, viewport 1120, `deviceScaleFactor:2`).
> Comandos longos vão em **blocos de código** ```` ``` ```` — o `md2docx.py` os renderiza em fonte mono
> com fundo cinza. **Valide os comandos rodando-os** antes de documentar.

### 3b. Tornar o markdown AUTOCONTIDO (padrão de entrega)
Depois de escrever o `.md` com links relativos (`![](imagens/...)`), embuta as imagens:

```bash
python ./scripts/embed_images.py <destino>/_src/01-Tutorial-Web.md  <destino>/01-Tutorial-Web.md
```

Isso troca cada `![](imagens/x.png)` por `![](data:image/png;base64,…)`, gerando **um `.md` único**
que mostra as figuras sem a pasta `imagens/` ao lado. Faça o mesmo no `00-LEIA-ME.md`. Mantenha o
fonte com links em `_src/` (ou use o mesmo arquivo de entrada e saída se não quiser guardar o fonte).

### 4. Gerar os .docx
`pip install python-docx` (uma vez). Depois, por arquivo (funciona tanto no `.md` autocontido
quanto no fonte com links relativos):

```bash
python ./scripts/md2docx.py <destino>/01-Tutorial-Web.md  <destino>/01-Tutorial-Web.docx
```

O conversor aplica a marca (capa azul + subtítulo `POP_BRAND`, default "LIS Checklist"),
caixas de aviso, tabelas, **blocos de código** (fonte mono, fundo cinza), imagens centralizadas
(paisagem 6.4" / retrato 2.95") — embutidas de caminho **ou** de data URI base64 — e listas
numeradas preservando o número do markdown. Mude o subtítulo com `POP_BRAND="Nome do Cliente"`.

### 5. Conferir e fechar
- Abra 1–2 PNGs e confirme que os badges não cobrem conteúdo e batem com o texto.
- Confirme que o `.md` de entrega é **autocontido** (cada `![…]` é `data:image/...;base64,…`).
- Apague `_cap/` (temporários mobile) se quiser. Mantenha `imagens/` como fonte.

---

## Notas
- Os motores em `./scripts/` são genéricos; os scripts `cap_*.mjs`/`nav_*.mjs` são específicos
  do sistema e você os (re)escreve a cada POP — guarde-os na pasta `e2e`/`_tut` do projeto-alvo.
- Defaults atuais (LIS Checklist): web `https://goldenticket.lat:4000`, login `admin`/`admin@123`,
  saída em `D:/dev/aurora/checklist/tutorial/imagens/{web,mobile}`. Troque por env var para outro sistema.
- Se o sistema-alvo não for o LIS, ajuste seletores/fluxo de login em `tutlib.mjs login()`.
