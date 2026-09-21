---
name: code-style
description: Code style, UI component, styling, icon, form, toast, and page implementation rules for this React Vite web app. Use when building or reviewing pages, components, UI states, copy, imports, styling, icons, skeletons, toasts, tables, dialogs, and overlays.
---

# Code Style

Use project primitives before custom UI. Import reusable components from the public barrel `@/components`.

```ts
import { Button, Card, Input } from "@/components";
```

## Pages

- Page components live in `src/features/<feature>`.
- Prefer `page-<screen>.tsx` for new/migrated pages.
- Keep route logic in `src/app/routing`.
- Use named exports for feature components.
- Use semantic HTML where helpful: `section`, `header`, `main`, `form`, `button`.

## UI Components

Use existing UI components from `@/components`:

- **Texto de conteúdo:** `<Typography variant="...">` — nunca raw `<h1>/<p>/<span>`
- **Actions:** `Button`, `Badge`, `CustomLink`, `Kbd`, `Toggle`, `ToggleGroup`
- **Forms:** `FieldWrapper` (label + error + description), `Input`, `Textarea`, `Select`, `Checkbox`, `Switch`, `Combobox`, `MultiCombobox`
- **File upload:** `<FileUI.Input>` — nunca `<input type="file">` direto
- **Form footer:** `<FooterButton isSubmitting isCreateMode>` em páginas create/edit
- **Dates:** `DatePicker`, `DateRangePicker`, `Calendar`, `InputOTP`
- **Containers:** `Card`, `CardHeader`, `CardContent`, `CardFooter`
- **Overlays:**
  - Lista de ações → `DropdownMenu`
  - Right-click → `ContextMenu`
  - Config rápida → `Popover`
  - Decisão bloqueante → `Dialog`
  - Auto centro/lateral → `SmartOverlay`
  - Bottom sheet mobile → `Drawer`
  - Painel lateral estruturado → `AppSheet` (header + scroll body + footer)
- **Navigation:** `Tabs`, `Accordion`, `Collapsible`, `Breadcrumb`, `BreadCrumbComponent`
- **Feedback:** `Skeleton`, `QueryErrorState`, `notify`, `ClipBoard`, `Progress`, `Avatar`
- **Charts:** `ChartContainer` wrapping recharts; cores via `var(--primary)`
- **Data:** `Table` + primitivos (`TableHeader`, `TableBody`, etc.)

Não criar primitivos UI avulsos se já existe componente local.
Ao adicionar componente reutilizável: pasta própria + `index.ts` + `.stories.tsx` + export em `src/components/index.ts`.
Adicionar exemplo em `src/features/public/design-system/page-design-system.tsx`.

## Forms

Use React Hook Form + Zod. Schemas em pasta `schema` ou `form` para formulários não triviais.

```tsx
<FieldWrapper label="Email" htmlFor="email" error={errors.email?.message}>
  <Input id="email" {...register("email")} />
</FieldWrapper>
```

Nunca adicionar `<label>` + `<p>` de erro manualmente — use sempre `FieldWrapper`.

## Toasts

Use the local toast abstraction:

```ts
import { notify } from "@/components";

notify.success("Salvo com sucesso");
notify.error("Nao foi possivel salvar");
```

## Text

Use `<Typography variant="...">` para todo texto de conteúdo. Nunca use raw `<h1>/<p>/<span>` diretamente.

```tsx
import { Typography } from "@/components/typography";
<Typography variant="title-sm">Card title</Typography>
<Typography variant="body-md" className="text-muted-foreground">Descrição</Typography>
```

## Required Patterns

- Use `@/` absolute imports for cross-feature imports.
- Use `type` and `import type` for TypeScript types.
- Prefer arrow functions for new feature code.
- Keep loading, empty, and error states explicit.
- Avoid direct API calls in page components.
- Keep user-facing copy consistent with existing Portuguese copy unless an i18n migration is active.
