---
name: routing
description: React Router rules for this React Vite web app. Use when creating routes, adding pages, handling route params/search params, setting up auth guards, or reviewing route structure.
---

# Routing

This project uses `react-router-dom`, not Expo Router. Routes live in `src/app/routing`.

Feature pages live in `src/features/<feature>` and are imported by the route table or route middleware. Route files should compose navigation and guards, not hold business logic.

## Rules

- Use `BrowserRouter` only in the root provider tree.
- Define route structure in `src/app/routing`.
- Use `Navigate`, `Outlet`, `useNavigate`, `useParams`, `useSearchParams`, and `Link` from `react-router-dom`.
- Auth guards belong in routing middleware components, not inside feature pages.
- Keep feature page logic inside `src/features/<feature>`.
- Store route constants in `src/app/variables/rotas.ts`.
- Prefer absolute imports via `@/`.

## Feature Page Entry

```tsx
import { PageCustomerList } from "@/features/customer/list/page-list";

{
  path: Rotas.protegidas.customer.list,
  element: <PageCustomerList />,
}
```

## Params

```tsx
import { useParams } from "react-router-dom";

export const PageCustomerDetail = () => {
	const { id } = useParams<{ id: string }>();
	// validate id before querying
};
```

## Search Params

```tsx
import { useSearchParams } from "react-router-dom";

const [searchParams, setSearchParams] = useSearchParams();
const tab = searchParams.get("tab") ?? "overview";
```

## Checklist

- Route registered in `src/app/routing`?
- Route path centralized in `Rotas` when reused?
- Auth/public guard handled by middleware?
- Page imports from `@/features/...`?
- No business logic embedded in route composition?
