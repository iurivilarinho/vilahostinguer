# Feature Components

Feature components follow the same anatomy as shared components (see `references/anatomy.md`) with a few additions.

---

## Calling data hooks

Feature components may call TanStack Query hooks and mutation hooks directly in the component body. Shared components in `src/components/` never do this.

```tsx
import { useGetUserById } from "../api";
import { useToggleClientUserLink } from "@/features/operational/client";

type UserClientsModalProps = {
  open: boolean;
  userId: number;
  onClose: () => void;
};

export const UserClientsModal = ({ open, userId, onClose }: UserClientsModalProps) => {
  const { data: userData, isLoading } = useGetUserById(open ? userId : undefined);
  const { mutateAsync: toggleLink, isPending } = useToggleClientUserLink();

  // component body...
};
```

Pass the `enabled` condition directly into the query hook — do not `useEffect` to conditionally call queries.

---

## Keeping types inside the feature

Props types and internal types for feature components stay inside the feature. They do not go into `src/api/types` or any shared folder unless reused across features.

```ts
// Stays inside the feature
type UserClientsModalProps = { ... };
type SelectionState = { ... };
```

---

## When to extract a hook

Extract component logic into a hook when:

- The same logic (state + effects + handlers) is reused in more than one component
- The component body becomes hard to read due to the volume of logic
- The logic has clear inputs and outputs that can be encapsulated

Keep logic inline when:

- It is used only in this component
- It is simple enough that extraction adds more indirection than clarity

```tsx
// Extractable — clear inputs/outputs, likely reusable
const { selectedIds, handleToggle, handleSave } = useEntitySelection({ initialIds, onSave });

// Keep inline — trivial, component-specific
const [isOpen, setIsOpen] = useState(false);
```

---

## When a feature component graduates to src/components/

Move a component from a feature to `src/components/` when:

1. It is imported and used in **two or more different features**
2. It has **no feature-specific domain logic** — it is purely presentational or UI-utility
3. Its props type does not reference types from a single feature's DTOs

Do not pre-emptively move components to `src/components/` — wait for actual reuse.

---

## Rules

- Data hooks may be called directly inside feature component bodies
- Props types stay inside the feature unless the component moves to `src/components/`
- Do not call `apiClient` directly — always go through hooks
- Logic is extracted into a hook only when it provides clear value (reuse or readability)
- Move to `src/components/` only after real reuse across features
