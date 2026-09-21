---
name: pwa-mobile-stack
description: End-to-end recipe for the Comandas PWA + mobile-first stack — manifest, custom service worker (injectManifest), iOS splash screens, install prompt, offline banner, update prompt, layout vars (dvh + safe-area + portal bars), mobile UI patterns (DialogShell, table-to-cards, charts), and **Web Push notifications** (VAPID, Spring backend service, React hook, browser/iOS quirks). Use when the user asks for PWA setup, mobile/responsive review of admin pages, push notifications (browser or web push), or anything that touches `vite-plugin-pwa`, `sw.ts`, `usePushSubscription`, `PushNotificationService`, or the `--portal-*` CSS variables.
---

# PWA + Mobile-first + Web Push (Comandas)

This skill captures the working stack for the Comandas frontend (React + Vite) and Spring backend. Trust the patterns here — they were debugged end-to-end. When in doubt, follow the file paths exactly: most components are already in place and just need to be reused.

## Read first

- `frontend-comandas/vite.config.ts` (PWA plugin config — must use `injectManifest`)
- `frontend-comandas/src/sw.ts` (custom service worker with push handlers)
- `frontend-comandas/src/index.css` (`--portal-*` CSS vars + iOS body resets)
- `frontend-comandas/src/components/pwa/*` (PwaUpdatePrompt, OfflineBanner, PwaInstallPrompt, PushNotificationsToggle)
- `frontend-comandas/src/hooks/usePushSubscription.ts`
- `frontend-comandas/src/components/dialog/DialogShell.tsx`
- `backend-comandas/src/main/java/com/br/food/service/PushNotificationService.java`
- `backend-comandas/src/main/java/com/br/food/authentication/SecurityFilter.java` (manual whitelist — separate from SecurityConfigurations)

## References (carregar quando necessário)

- `references/icons-and-splash.md` — scripts Node + sharp para regenerar ícones PWA e 20 splash screens iOS a partir do SVG.
- `references/new-push-topic.md` — passo a passo para adicionar um novo topic de push (backend + frontend + trigger).
- `references/admin-page-mobile-checklist.md` — checklist para tornar uma página admin nova mobile-friendly.
- `references/push-debug-cookbook.md` — comandos prontos (curl, SQL, logs esperados, status codes) para diagnosticar push.

---

## 1. PWA bootstrap (already wired)

`vite-plugin-pwa` is configured in `vite.config.ts` with **`strategies: "injectManifest"`** because we need a custom SW for `push`/`notificationclick`. Do not switch back to `generateSW` or push will silently break.

Required pieces:
- `index.html`: `<html lang="pt-BR">`, `viewport-fit=cover`, `theme-color=#ea1d2c`, `apple-mobile-web-app-capable=yes`, manifest link, apple-touch-icon, **20 `<link rel="apple-touch-startup-image">` entries** for iOS splash.
- `public/`: `icon-192.png`, `icon-512.png`, `icon-512-maskable.png`, `apple-touch-icon-180.png`, `favicon-32.png`, `splash/apple-splash-{w}x{h}.png` × 20.
- `vite.config.ts`: `registerType: "prompt"`, `injectRegister: false`, `devOptions: { enabled: true, type: "module" }`, manifest with `shortcuts`.

**Regenerate icons or splashes** with `sharp` from `public/command-icon.svg` if branding changes — use the script template in `references/icons-and-splash.md` (or generate inline via `node -e "..."`). Always include `splash/*.png` in the plugin's `includeAssets`.

**Dev mode caveat**: with `devOptions.enabled: false`, the SW **does not register in `npm run dev`** and push silently breaks. Keep it `true`.

---

## 2. Custom Service Worker (`src/sw.ts`)

The SW must:
1. Precache (`precacheAndRoute(self.__WB_MANIFEST)`).
2. Handle navigation fallback to `index.html` (denylist API + documents).
3. Runtime cache `/menu/*`, `/product-categories`, `/company-profile` with `StaleWhileRevalidate`; `/documents/*` with `CacheFirst`.
4. **Listen to `push`** — parse JSON `{title, body, url}` and call `self.registration.showNotification(title, { body, icon, badge, data: { url } })`.
5. **Listen to `notificationclick`** — close, then `clients.matchAll`/`focus`/`navigate` to `data.url`, fallback `clients.openWindow(url)`.
6. Listen to `message` `SKIP_WAITING` to support the update prompt flow.

Exclude `src/sw.ts` from `tsconfig.app.json` (it has its own webworker lib reference at the top).

---

## 3. App-level PWA components (mount in `App.tsx`)

```tsx
<ErrorBoundary>
  <AppProvider>
    <Toaster />
    <OfflineBanner />
    <PwaUpdatePrompt />
    <PwaInstallPrompt />
    <AppRoutes />
  </AppProvider>
</ErrorBoundary>
```

- `PwaUpdatePrompt`: uses `useRegisterSW` from `virtual:pwa-register/react`. Toast persistente com botão "Atualizar" que chama `updateServiceWorker(true)`.
- `OfflineBanner`: barra fixa top em `--restaurant-primary` quando `navigator.onLine === false` (escuta `online`/`offline`).
- `PwaInstallPrompt`: captura `beforeinstallprompt`, banner customizado bottom-right (acima da nav mobile), cooldown de 14 dias via localStorage `comandas.pwaInstallDismissedAt`.
- `ErrorBoundary`: tela amigável com "Recarregar" para crashes runtime.

---

## 4. Layout / safe-area / dvh (already in `index.css`)

CSS vars no `:root`:
```css
--portal-nav-height: 5rem;
--portal-cart-height: 4.5rem;
--portal-safe-bottom: env(safe-area-inset-bottom, 0px);
--portal-bottom-spacing: calc(var(--portal-nav-height) + var(--portal-safe-bottom) + 1rem);
--portal-bottom-with-cart: calc(var(--portal-nav-height) + var(--portal-cart-height) + var(--portal-safe-bottom) + 1.5rem);
```

Body resets:
```css
html, body {
  -webkit-tap-highlight-color: transparent;
  -webkit-font-smoothing: antialiased;
  text-size-adjust: 100%;
  scroll-behavior: smooth;
}
body { overscroll-behavior-y: contain; }
:focus-visible { outline: 2px solid var(--restaurant-primary); outline-offset: 2px; border-radius: 4px; }
#root { min-height: 100vh; min-height: 100dvh; }
```

`MainLayout` usa `pb-[var(--portal-bottom-spacing)] has-[[data-portal-cart-bar]]:pb-[var(--portal-bottom-with-cart)]` — quando o `StickyCartBar` (com `data-portal-cart-bar`) está montado, o padding cresce automaticamente.

`AdminLayout` header: `padding-top: env(safe-area-inset-top)`. Container principal usa `min-h-[100dvh]`.

**Login/Recovery pages**: usar `min-h-screen min-h-[100dvh]` (não `h-screen`).

---

## 5. Mobile UI patterns

### DialogShell (`components/dialog/DialogShell.tsx`)

Use **sempre** que criar dialog admin. Padroniza header (em `--restaurant-shell` com border bottom), body scrollable, footer com `pb-[calc(var(--portal-safe-bottom)+1.25rem)]`. Em mobile, botões do footer ganham `w-full sm:w-auto`.

```tsx
<Dialog open={...}>
  <form onSubmit={...}>
    <DialogShell
      title="Editar X"
      description="Opcional"
      footer={<>
        <Button variant="ghost" className="w-full sm:w-auto" onClick={cancel}>Cancelar</Button>
        <Button type="submit" className="w-full sm:w-auto" disabled={saving}>Salvar</Button>
      </>}
    >
      {/* conteúdo do form */}
    </DialogShell>
  </form>
</Dialog>
```

### Tabela → cards em mobile

Padrão para todas as listas admin. Envolve a `TableBase` em `hidden md:block` e replica os campos como cards `space-y-3 px-4 py-4 md:hidden`. Use `TableSkeleton` quando `isLoading && data.length === 0`. Veja exemplos em `OrdersManagementPage`, `ProductsManagementPage`, `CustomersManagementPage`.

### TablePagination

Já é responsivo: empilha em mobile, esconde botões "primeira/última página" em telas pequenas, prev/next são 40px touch-friendly. Não modificar.

### Charts (recharts via shadcn ChartContainer)

⚠️ **Bug clássico**: o `ChartContainer` aplica `aspect-video` por default, que combina com `h-[260px]` e força largura `~462px`, estourando mobile. **Sempre passe `aspect-auto`** no className quando definir altura explícita:

```tsx
<ChartContainer className="mt-4 aspect-auto h-[240px] w-full md:h-[300px]" config={...}>
```

Outros ajustes mobile:
- Eixos numéricos com `formatCompactCurrency` (`R$ 1,5k`).
- `tick={{ fontSize: isMobile ? 10 : 12 }}`.
- BarChart vertical: `width={isMobile ? 84 : 120}` no YAxis com labels.
- PieChart: `outerRadius={isMobile ? 70 : 96}` + `innerRadius` opcional para virar donut.
- Altura dinâmica em BarChart vertical: `height = max(220, items.length × 36 + 60)`.

### Touch targets

`MoreHorizontal` triggers e botões `+/-` no carrinho: **mínimo `h-10 w-10`** (40px), preferencialmente `h-11 w-11` (44px) em mobile. Padronizar com `h-10 w-10` é aceitável.

---

## 6. Web Push notifications — **leia tudo antes de mexer**

Esta seção é a mais cara de errar. Trabalhe na ordem.

### Topics existentes

`PushSubscription.TOPIC_*`: `customer`, `kitchen`, `tables`. Cada um tem um `<PushNotificationsToggle topic="..." />` montado em uma página específica. Para adicionar novo topic:
1. Adicionar constante em `PushSubscription.java`.
2. Adicionar string ao `PushTopic` no `frontend-comandas/src/api/services/push.ts`.
3. Adicionar `<PushNotificationsToggle topic="..." />` na página alvo.
4. Adicionar trigger no service backend (vide gatilhos abaixo).

### Gatilhos atuais

- `KitchenService.markReady` → `notifyCustomer(customerId, ...)` ("Seu pedido está pronto").
- `OrderService.create` e `addItems` → `notifyTopic(TOPIC_KITCHEN, ...)` ("Novo pedido para a cozinha").
- `OrderService.requestCheckout` → `notifyTopic(TOPIC_TABLES, ...)` ("Conta solicitada — Mesa N").

### VAPID setup (uma vez por ambiente)

```bash
npx web-push generate-vapid-keys
```

Cola em `backend-comandas/.env` (gitignored):
```
PUSH_VAPID_PUBLIC_KEY=B...
PUSH_VAPID_PRIVATE_KEY=...
PUSH_VAPID_SUBJECT=mailto:contato@dominio
```

`spring-dotenv` (já no `pom.xml`) lê o `.env` no startup e injeta como env vars. `application.properties` resolve `${PUSH_VAPID_*}`. **Sem reiniciar o backend, as chaves não são recarregadas.**

`docker-compose.yml` precisa propagar as 3 variáveis no `environment`. Ver bloco já existente.

### Backend (Spring) — peças críticas

- **`pom.xml`**: dependências `nl.martijndwars:web-push:5.1.1` + `org.bouncycastle:bcprov-jdk15on:1.70`.
- **Entidade `PushSubscription`**: índices em `topic` e `customer_id`. Endpoint **único** (UNIQUE constraint).
- **`PushNotificationService`**:
  - `@PostConstruct` adiciona BouncyCastle ao `Security` provider, instancia `PushService`. Loga `[push] PushService inicializado` ou `[push] VAPID keys ausentes`.
  - `saveSubscription(req)`: upsert por endpoint. Loga `[push] subscription salva id=N topic=Y customerId=Z`.
  - `notifyTopic(topic, title, body, url)` / `notifyCustomer(customerId, ...)`: loga `subscriptions=N`, dispara `pushService.send()` para cada uma. Status 404/410 → remove subscription expirada.
  - **Todos os erros logam com nível `error`**, não engole.
- **`PushController`**: `GET /push/public-key`, `POST /push/subscriptions`, `DELETE /push/subscriptions?endpoint=...`.

### ⚠️ Pegadinha de segurança

O projeto tem **dois** lugares de whitelist:
1. `SecurityConfigurations.java` — `requestMatchers(...).permitAll()`.
2. `SecurityFilter.java` (`shouldBypassAuthentication`) — lista própria de URIs.

**Os dois precisam liberar o endpoint público**, senão o filtro JWT rejeita antes da chain. Sintoma: 401 com mensagem `"Invalid or expired token"` em endpoint que deveria ser público.

Já está liberado para `/push/public-key`, `POST /push/subscriptions`, `DELETE /push/subscriptions`. Para novos endpoints públicos, **adicione nos dois arquivos**.

### Frontend — peças críticas

- **`api/services/push.ts`**: `getPushPublicKey`, `registerPushSubscription`, `removePushSubscription`. Tipo `PushTopic`.
- **`hooks/usePushSubscription.ts`**: lida com permissão, subscribe, send-to-backend, unsubscribe. Sempre **gera p256dh/auth via `subscription.getKey()` + base64url** (`-_` sem padding). NÃO confiar em `subscriptionJson.keys` (alguns browsers retornam undefined). NÃO usar base64 padrão (`+/=`) — quebra a assinatura VAPID no servidor.
- **`components/pwa/PushNotificationsToggle.tsx`**: o botão visível, com toasts de erro específicos por motivo (`denied`, `missing-server-key`, `unsupported`, `error: ...`).

### Service Worker (`src/sw.ts`)

```ts
self.addEventListener("push", (event) => {
  let payload = {};
  try { payload = event.data?.json() ?? {}; } catch { payload = { body: event.data?.text() ?? "" }; }
  const title = payload.title || "Comandas";
  event.waitUntil(self.registration.showNotification(title, {
    body: payload.body || "",
    icon: "/icon-192.png",
    badge: "/icon-192.png",
    data: { url: payload.url || "/" },
  }));
});

self.addEventListener("notificationclick", (event) => {
  event.notification.close();
  const targetUrl = event.notification.data?.url || "/";
  event.waitUntil(
    self.clients.matchAll({ type: "window", includeUncontrolled: true }).then((list) => {
      for (const client of list) {
        if ("focus" in client) {
          client.navigate(targetUrl).catch(() => undefined);
          return client.focus();
        }
      }
      return self.clients.openWindow(targetUrl);
    }),
  );
});
```

---

## 7. Debugging push (em ordem, do mais provável)

1. **Backend não foi reiniciado depois do `.env`** → `[push] VAPID keys ausentes` no startup. Sem isso, `/push/public-key` retorna `{"publicKey": null}`. Reinicia.

2. **Endpoint retorna 401 "Invalid or expired token"** → faltou liberar em `SecurityFilter.shouldBypassAuthentication`. Adiciona o URI na lista do método HTTP correto.

3. **`devOptions.enabled: false` em dev** → SW não registra, `serviceWorker.ready` trava para sempre. Manter `true`.

4. **Subscription ficou com `customer_id=NULL`** → o usuário clicou Ativar antes de logar como cliente (sem `comandas.currentCustomerId` no localStorage). `notifyCustomer(null)` ignora. Solução: desativar, completar perfil, reativar.

5. **`subscriptions=0` no log** → subscription do topic alvo não existe ou foi removida automaticamente após 410/404. Reativa o toggle.

6. **`status=400/401` ao enviar** → VAPID inválido ou subject mal formatado. Verifica que `PUSH_VAPID_SUBJECT` começa com `mailto:` ou `https://`.

7. **Notificação não aparece mesmo com `status=201`** → SW antigo cacheado sem handler de push. F12 → Application → Service Workers → Unregister + Clear site data + reload.

8. **iOS Safari: `Notification.requestPermission()` retorna `denied` direto** → push só funciona em PWA **instalado via "Adicionar à tela inicial"** (iOS 16.4+). No Safari não-instalado, é bloqueado por design.

9. **Em mode dev, push não chega** → mesmo com `devOptions.enabled: true`, alguns browsers tratam SW dev mode com cuidado. Se algo estranho, testar em `npm run preview` (build + serve real).

### Smoke test rápido sem backend

DevTools → Application → Service Workers → campo "Push" → cola JSON e clica:
```json
{"title":"Manual","body":"Sem backend","url":"/conta"}
```
Apareceu notificação? SW e permissão OK. Não apareceu? Problema é local (SW não registrou ou permissão negada).

---

## 8. Listas/Cardápio mobile

### CategoryChips horizontal (mobile-only)

Substitui select de categoria em mobile (visível só `md:hidden`). `overflow-x-auto snap-x snap-mandatory`, scrollbar oculto, chip ativo em `--restaurant-primary`.

### Menu image cards

`aspect-[4/3]`, `object-contain` (não `cover` — produtos com proporções variadas ficam cortados), `loading="lazy"`, `decoding="async"`. Padding interno `p-2 sm:p-3` e fundo `--restaurant-shell` para imagens menores.

### Toolbar sticky

Em `CustomerOrderPage`, busca + chips ficam em wrapper `sticky top-0 z-30 bg-shell/95 backdrop-blur md:static`. Search com **debounce de 300ms** via `useDebounce(search, 300)` antes de passar pra `useMenuProducts`.

### OptionPicker (variações)

Para grupos de variação (sabor, tamanho), use `<OptionPicker>` (em `features/customer-portal/components/OptionPicker.tsx`). Header em `--restaurant-shell` com pill "Escolha 1", lista com `divide-y` e radio dot custom. `level="H"` no QR + `imageSettings` permitem badge centralizado dos números das mesas.

---

## 9. Performance / cache

- React Query `staleTime`: menu products + categories = 5min, company profile = 10min.
- Lazy loading de **todas** as rotas admin via `React.lazy`.
- Runtime cache do SW: `StaleWhileRevalidate` para metadados, `CacheFirst` para imagens (`/documents/*`) com expiração de 30 dias.
- ChartContainer: **sempre `aspect-auto`** quando passar height explícita.

---

## 10. Ordenação / SQL

- `ProductService.findAll` aplica sort default por `description` quando `Pageable.unsorted()`. **Não usar `.ignoreCase()`** porque ProductSpecification.hasCategoryId aplica `query.distinct(true)` e Postgres rejeita `ORDER BY LOWER(...)` fora do SELECT list. Confiar na collation do DB.

Sintoma se errar: `ERROR: for SELECT DISTINCT, ORDER BY expressions must appear in select list`.

---

## Constraints

- Não voltar para `generateSW` no vite-plugin-pwa — perde os handlers de push.
- Não confiar em `subscription.toJSON().keys` para extrair p256dh/auth — sempre derive de `getKey()` + base64url.
- Não esquecer `aspect-auto` ao definir altura no `ChartContainer`.
- Não criar endpoint público sem liberar nos **dois** arquivos de segurança.
- Não usar `h-screen` ou `min-h-screen` puros — sempre par `min-h-screen min-h-[100dvh]`.
- Não chamar `query.distinct(true)` em specs sem necessidade — quebra ordenação por funções.
- Push em iOS só funciona com PWA instalado (iOS 16.4+).
