# Tornar uma página admin mobile-friendly

Checklist quando uma página de admin precisa ficar 100% usável em mobile/PWA. Trabalhe na ordem.

## 1. Filtros responsivos

Padrão obrigatório pra toda lista admin: **placeholder dentro do input, sem `Field`/`FieldLabel` por fora**. A referência é `OrdersManagementPage` e `ProductsManagementPage`. Não introduzir labels externos em filtros — quebra a sensação mobile e foi rejeitado pelo usuário.

```tsx
<div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-5">
  <Input
    placeholder="Filtrar por numero da nota"
    value={filters.invoiceNumber}
    onChange={(event) => setFilters((c) => ({ ...c, invoiceNumber: event.target.value }))}
  />
  <SearchableSelect
    value={filters.status}
    placeholder="Todos os status"
    options={[...]}
    onValueChange={(v) => setFilters((c) => ({ ...c, status: v }))}
  />
  <DateFilterInput
    placeholder="Emissao (inicio)"
    value={filters.issueDateStart}
    onChange={(v) => setFilters((c) => ({ ...c, issueDateStart: v }))}
  />
</div>
```

### Filtros de data: use `DateFilterInput`

`<input type="date">` **não mostra placeholder em mobile real** (Safari/Chrome iOS/Android, só no DevTools desktop). Pra manter o padrão "placeholder dentro" sem cair em label externo, usar **sempre** `src/components/input/DateFilterInput.tsx`. Ele alterna `type="text"` (mostra placeholder) e `type="date"` (foco/valor preenchido), e dispara `showPicker()` no foco.

```tsx
import { DateFilterInput } from "@/components/input/DateFilterInput";

<DateFilterInput
  placeholder="Data inicial"
  value={filters.startDate}
  onChange={(value) => setFilters((c) => ({ ...c, startDate: value }))}
/>
```

Não usar `<Input type="date">` direto em filtros — perde o placeholder em mobile.

### Layout do grid

- Default sem `grid-cols-*` = 1 coluna em mobile (correto).
- Para 2–3 filtros: `sm:grid-cols-2 lg:grid-cols-3`.
- Para 4–5 filtros: `sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4|5`.
- **Nunca abrir 4+ colunas no breakpoint `md:` (768–1023px)** — cada coluna fica < 200px e date/select com texto longo estouram a viewport.

### Overflow / largura no mobile

- O `Input` padrão já vem com `w-full min-w-0`.
- `SearchableSelect` já tem `min-w-0` + `truncate` no trigger.
- Para `Select` (não searchable), passar `className="h-10 w-full min-w-0"` no `SelectTrigger`.
- Para grids com `grid-cols-[Xfr_Yfr]` que contêm date/time (em forms, não em filtros), usar `minmax(0, Xfr)` no template — `1fr` puro é `minmax(auto, 1fr)` e o picker nativo iOS empurra a coluna além da viewport.
- Time inputs (`<input type="time">`) ficam apertados em qualquer grid de 2 colunas em iPhone pequeno (375px ou menos): preferir `flex-col` em mobile + `md:grid` desktop. Ver `CompanyProfilePage` (horários de funcionamento) como referência.
- `FileUploadInput` já vem com `w-full min-w-0` — quando colocar 2 lado a lado, usar `grid grid-cols-1 gap-4 lg:grid-cols-2` (não confiar no auto do `grid` sem template).

### Forms (não filtros)

Em diálogos de criação/edição, manter o padrão `Field` + `FieldLabel` (vide `web-form-creation` skill). A regra de placeholder-only vale **só pra filtros de listagem**.

## 2. Tabela → cards

Envolve `<TableBase>` em `hidden md:block`. Adiciona bloco mobile com cards depois:

```tsx
<AdminPanel className="overflow-hidden p-0">
  {isLoading && data.length === 0 ? (
    <div className="px-4 py-4">
      <TableSkeleton rows={5} columns={6} />
    </div>
  ) : (
    <>
      <div className="hidden overflow-x-auto md:block">
        <TableBase>...</TableBase>
      </div>

      <div className="space-y-3 px-4 py-4 md:hidden">
        {data.length === 0 ? (
          <div className="rounded-xl border border-dashed border-[color:var(--restaurant-border)] bg-[color:var(--restaurant-surface)] p-6 text-center text-sm text-[color:var(--restaurant-muted)]">
            Nenhum registro encontrado.
          </div>
        ) : (
          data.map((item) => (
            <div key={item.id} className="space-y-2 rounded-xl border border-[color:var(--restaurant-border)] bg-[color:var(--restaurant-surface)] p-4">
              {/* layout vertical, sem colunas espremidas */}
            </div>
          ))
        )}
      </div>
    </>
  )}
  <TablePagination ... />
</AdminPanel>
```

Pega `isLoading` no destructure: `const { data, isLoading } = useXxxPage(...)`.

## 3. Touch targets

Substitui qualquer `h-8 w-8` (32px) por `h-10 w-10` (40px). Mínimo aceitável é 40, ideal é 44 (`h-11 w-11`). Aplica a:
- `<DropdownMenuTrigger>` em listas
- Botões `+/-` de quantidade
- Botões icon-only

## 4. Dialog → DialogShell

Use sempre. Padroniza header em `--restaurant-shell`, body scrollable, footer com safe-area.

```tsx
<Dialog open={...} onOpenChange={...}>
  <form onSubmit={handleSubmit(onSubmit)}>
    <DialogShell
      title="Editar X"
      description="Texto opcional"
      footer={
        <>
          <Button variant="ghost" className="w-full sm:w-auto" onClick={cancel}>
            Cancelar
          </Button>
          <Button type="submit" className="w-full sm:w-auto" disabled={saving}>
            Salvar
          </Button>
        </>
      }
    >
      {/* form fields */}
    </DialogShell>
  </form>
</Dialog>
```

Para dialogs grandes (detail view, com muito scroll), pode usar `DialogContent` direto mas seguindo o **mesmo template**: `flex max-h-[92vh] max-h-[92dvh] w-[calc(100%-1.5rem)] max-w-Xxl flex-col overflow-hidden p-0` + header `--restaurant-shell` + body `min-h-0 flex-1 overflow-y-auto px-4 py-4` + footer `pb-[calc(var(--portal-safe-bottom)+1.25rem)]`.

## 5. Charts (se houver)

```tsx
<ChartContainer
  className="mt-4 aspect-auto h-[240px] w-full md:h-[300px]"
  // ↑ aspect-auto é OBRIGATÓRIO se passar height. Default `aspect-video` força largura > viewport mobile.
  config={...}
>
```

Para detalhes finos (formatter compacto, YAxis menor em mobile, isMobile, etc.), siga o padrão de `FinancialManagementPage.tsx`.

## 6. KPIs

```tsx
// Antes — 4 colunas em mobile fica ilegível
<div className="grid gap-3 sm:grid-cols-2 xl:grid-cols-4">

// Depois — 1 coluna em mobile, 2 em tablet, 4 em desktop
<div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
```

`KpiCard` já tem `truncate` removido — títulos longos quebram em 2 linhas em vez de cortar.

## 7. Pagination

`<TablePagination>` já é responsiva. Não modificar. Só passar `pagination`, `onPageChange`, `onPageSizeChange`.

## 8. Validação final

```bash
cd frontend-comandas
npx tsc --noEmit -p tsconfig.app.json
```

## Skip-list (já estão prontos, não tocar)

- `MainLayout` (cliente) — `pb-[var(--portal-bottom-spacing)] has-[[data-portal-cart-bar]]:pb-[var(--portal-bottom-with-cart)]`.
- `AdminLayout` — header com `safe-area-inset-top`.
- `Login`, `PasswordRecovery` — `min-h-screen min-h-[100dvh]`.
- `KitchenPage`, `OrdersManagementPage`, `ProductsManagementPage`, `TablesManagementPage`, `CustomersManagementPage`, `ProductCategoriesManagementPage`, `PromotionsManagementPage`, `StockManagementPage`, `SupplyInvoicesPage`, `FinancialManagementPage` — já passaram pelo refactor mobile.
