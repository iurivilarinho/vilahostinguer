---
name: mobile-api-integration
description: create or modify API integrations in the mobile app. use when adding a new API endpoint, defining DTOs, or ensuring API functions are correctly typed and use checklistApi. trigger on api call, endpoint, dto, axios, checklistApi, or any question about how the mobile app talks to the backend.
---

This project is **offline-first**. There is no TanStack Query. The UI reads exclusively from **Realm**. API calls are used only for sync (pull and push), and they live inside the feature that owns the data.

---

## Where API code lives

API functions and DTOs belong inside the feature that owns the corresponding domain entity:

```
src/features/<name>/api/
  dtos/
    <entity>.ts          ← TypeScript interfaces for API contracts
  services/
    <entity>Api.ts       ← plain async functions using checklistApi
  index.ts               ← re-exports services only
```

The only shared API file is the axios instance:

- `src/api/client/checklistApi.ts` — configured with base URL, auth headers, and token refresh interceptor. Import `checklistApi` from here. Never create a second axios instance.

---

## API function pattern

API functions are plain async functions. They take typed params, call `checklistApi`, and return typed data. No Realm access. No React hooks.

```ts
// File: api/services/myEntityApi.ts
import { checklistApi } from '../../../../api/client/checklistApi';
import { MyEntityDTO, UpdateMyEntityRequest } from '../dtos/myEntity';

export const getMyEntities = async (): Promise<MyEntityDTO[]> => {
  const { data } = await checklistApi.get<MyEntityDTO[]>('/my-entities');
  return data;
};

export const updateMyEntity = async (
  id: number,
  body: UpdateMyEntityRequest,
): Promise<void> => {
  await checklistApi.patch(`/my-entities/${id}`, body);
};
```

---

## DTO pattern

DTOs are TypeScript types that mirror the **API contract** exactly. Always derive them from `docs/api-docs.json` — never invent field names or types by hand.

```ts
// File: api/dtos/myEntity.ts
export type MyEntityDTO = {
  id: number;
  name: string;
  status: string;
  createdAt: string; // ISO string from the API — convert to Date in the adapter
};

export type UpdateMyEntityRequest = {
  id: number;
  status: string;
};
```

**Rules for DTOs:**

- Field names must match the API exactly (check `docs/api-docs.json`)
- Use `string` for date/datetime fields — conversion to `Date` happens in the adapter
- Use `?` for optional fields (`nullable: true` or not in `required[]` in the API spec)
- Do not extend or mix with Zod-inferred form types — DTOs and form schemas serve different purposes and must stay separate

---

## api/index.ts

Exports only API services. Never re-exports DTOs.

```ts
// api/index.ts
export * from './services/myEntityApi';
```

---

## How API functions connect to the rest of the system

```
Feature API function          ← pure axios call, returns DTO
      ↓
Pull sync hook                ← calls API, writes DTO to Realm via service
      ↓
Realm service (upsertFromApi) ← adapts DTO and writes to Realm
      ↓
Realm                         ← source of truth for the UI

Feature UI (mutation)
      ↓
Realm service (write + createPushQueueItem)
      ↓
Push sync handler             ← reads PushQueueItem, calls API function
      ↓
Feature API function          ← sends mutation to server
```

---

## When to use this skill

- Adding a **new API endpoint** for a feature
- Defining **DTOs** for a new or existing entity
- Ensuring API functions are correctly typed and use `checklistApi`
- Refactoring API calls that ended up in the wrong place

For the **Realm service**, **pull sync**, and **push sync** side of the integration, use the `realm-offline` skill.

---

## Rules

- API functions go in `api/services/` inside the feature
- DTOs go in `api/dtos/` inside the feature — never in `src/api/dtos/` (existing ones there are legacy)
- `api/index.ts` exports services only — DTOs are internal to the feature
- DTOs must be derived from `docs/api-docs.json` — never invented by hand
- Always use `checklistApi` — never create a new axios instance
- API functions must not import from Realm or React hooks
- Feature components must never call `checklistApi` directly — always go through the Realm service layer
- DTOs must never be mixed with or extended from Zod form schemas
