# Edit Mode

## When edit mode is needed

Forms that support both create and edit modes receive an optional entity prop (sheet/dialog) or fetch the entity by URL param (page). In both cases, the form must be populated with the remote data when editing.

---

## Pattern 1 — Props-based (sheet/dialog forms)

The entity is passed as a prop. Use `useEffect` with `[mode, entity, reset]` as dependencies:

```tsx
useEffect(() => {
  if (mode === "edit" && entity) {
    reset({
      name: entity.name,
      description: entity.description ?? "",
    });
    return;
  }

  reset(DEFAULT_ENTITY_FORM_VALUES);
}, [mode, entity, reset]);
```

- Always reset to `DEFAULT_*_FORM_VALUES` when `mode === "create"` to avoid stale data from a previous edit session
- Map DTO fields to form values explicitly — never spread the DTO directly into `reset()`

---

## Pattern 2 — URL param-based (page forms)

The entity is fetched by ID from the URL. Use `useEffect` with `[userData, reset]` as dependencies:

```tsx
const { data: userData } = useGetEntityById(id ? Number(id) : undefined);

useEffect(() => {
  if (userData) {
    reset({
      name: userData.name,
      email: userData.email ?? "",
      // map all fields explicitly
    });
  }
}, [userData, reset]);
```

---

## Async population

When edit mode requires loading additional data (e.g. converting a document ID to a File), wrap the `reset` and async steps in an inner async function:

```tsx
useEffect(() => {
  if (userData) {
    const populate = async () => {
      reset({
        name: userData.name,
        // other fields...
      });

      if (userData.imageId) {
        try {
          const doc = await getDocumentById(userData.imageId);
          setValue("image", (await documentToFile(doc)) || undefined);
        } catch (error) {
          console.error("Erro ao carregar imagem:", error);
        }
      }
    };

    populate();
  }
}, [userData, reset]);
```

Use `setValue` (not `reset`) for fields that are populated asynchronously after the initial `reset()` call.

---

## Rules

- `reset` must always be listed in the `useEffect` dependency array (it is stable, but lint requires it)
- Never spread the DTO directly into `reset()` — always map fields explicitly
- Async data loading inside `useEffect` must use an inner named async function — never `async` on the `useEffect` callback itself
- When switching back to create mode, always reset to `DEFAULT_*_FORM_VALUES`
