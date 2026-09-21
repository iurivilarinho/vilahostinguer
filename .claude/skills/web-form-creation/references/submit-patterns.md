# Submit Patterns

## Pattern 1 — Simple inline submit (sheet/dialog forms)

Use when the form has a single entity, one or two mutations (create + update), and no complex field remapping.

```tsx
const { mutateAsync: createEntity, isPending: isCreating } = useCreateEntity();
const { mutateAsync: updateEntity, isPending: isUpdating } = useUpdateEntity();

const onSubmit = async (data: EntityFormValues) => {
  if (mode === "create") {
    await createEntity({ name: data.name, description: data.description || undefined });
  }

  if (mode === "edit" && entity) {
    await updateEntity({
      id: entity.id,
      name: data.name,
      description: data.description || undefined,
    });
  }

  onClose();
};
```

### Rules

- Call `mutateAsync` (not `mutate`) so the function is awaitable and `onClose()` runs after success
- Do **not** wrap in `try/catch` — errors are handled by the mutation's `onError` callback
- Call `onClose()` after all mutations complete successfully
- `isSaving` must disable the submit button for the duration

---

## Pattern 2 — Extracted submit hook (complex page forms)

Use when the form involves multiple mutations, complex field remapping (mask stripping, foreign key extraction), or conditional branching (create vs edit paths).

Extract a dedicated `useXxxFormSubmit` hook that owns all submission logic:

### Hook interface

```ts
interface UseXxxFormSubmitParams {
  id: string | undefined; // from URL param
  formType: string | undefined; // "criar" | "editar" from URL param
  entityData: XxxApiDto | undefined; // remote entity for edit diffing
}

export const useXxxFormSubmit = ({ id, formType, entityData }: UseXxxFormSubmitParams) => {
  const { mutateAsync: createXxx, isPending: isCreating } = useCreateXxx();
  const { mutateAsync: updateXxx, isPending: isUpdating } = useUpdateXxx();

  const isLoading = isCreating || isUpdating;

  const handleSubmit = async (data: XxxFormValues) => {
    try {
      // 1. Remap fields (strip masks, extract IDs)
      const payload = {
        name: data.name,
        relatedEntityId: data.relatedEntity?.id,
      };

      // 2. Branch on create vs edit
      if (formType === "criar") {
        await createXxx(payload);
      } else if (formType === "editar" && id) {
        await updateXxx({ id: Number(id), ...payload });
      }
    } catch (error) {
      console.error("Erro ao salvar:", error);
    }
  };

  return { handleSubmit, isLoading };
};
```

### Usage in the page component

```tsx
const { handleSubmit: submitEntity, isLoading } = useXxxFormSubmit({ id, formType, entityData });

const onSubmit = async (data: XxxFormValues) => {
  // business-rule guards that cannot live in zod (e.g. password strength)
  if (formType === "criar" && !data.password) {
    setError("password", { type: "required", message: "Informe a senha" });
    return;
  }

  await submitEntity(data);
};
```

### When to use Promise.all

When multiple mutations fire in parallel (e.g. linking/unlinking many-to-many relations), use `Promise.all`:

```ts
await Promise.all([
  ...toLink.map((id) => linkRelation({ entityId: savedId, relatedId: id, link: true })),
  ...toUnlink.map((id) => linkRelation({ entityId: savedId, relatedId: id, link: false })),
]);
```

### Rules

- The hook returns `{ handleSubmit, isLoading }` — nothing more
- `isLoading` aggregates all `isPending` flags inside the hook
- All field remapping and branching lives **inside the hook** — the page component's `onSubmit` only handles pre-submit guards (e.g. `setError`)
- `try/catch` is used inside the hook (not the page) to prevent unhandled rejections
- Mutations that fire silently (no toast needed) receive `{ showToast: false }`

---

## Choosing between patterns

|                          | Pattern 1 — Inline | Pattern 2 — Extracted hook    |
| ------------------------ | ------------------ | ----------------------------- |
| Number of mutations      | 1–2                | 3+                            |
| Field remapping          | None or trivial    | Mask stripping, ID extraction |
| Create vs edit branching | Simple `if (mode)` | URL-param-based, complex      |
| Many-to-many sync        | No                 | Yes                           |
