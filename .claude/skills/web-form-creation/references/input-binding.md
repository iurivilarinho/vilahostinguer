# Input Binding

## Native inputs — `register()`

For `Input` and `Textarea`, spread the `register()` result directly:

```tsx
<Input id="name" {...register("name")} />
<Textarea id="description" {...register("description")} />
```

react-hook-form handles `onChange`, `onBlur`, `ref`, and `aria-invalid` automatically via `register()`.

---

## Custom inputs — `watch()` + `setValue()`

Custom inputs are not native HTML elements and do not support `register()`. Use `watch()` to read the current value and `setValue()` to update it.

### StandardCombobox

```tsx
<StandardCombobox
  items={jobPositionsData?.content ?? []}
  value={watch("jobPosition")}
  onValueChange={(value) => setValue("jobPosition", value)}
  itemLabel={(item) => item.name}
/>
```

### StandardMultiCombobox

```tsx
<StandardMultiCombobox
  items={rolesData?.content ?? []}
  value={watch("roles") ?? []}
  onValueChange={(value) => setValue("roles", value)}
  itemLabel={(item) => item.name}
/>
```

### DatePicker

```tsx
<DatePicker value={watch("date")} onChange={(value) => setValue("date", value)} />
```

### Checkbox

```tsx
<Checkbox
  checked={watch("active") ?? false}
  onCheckedChange={(value) => setValue("active", Boolean(value))}
/>
```

### ProfilePictureInput

```tsx
<ProfilePictureInput value={watch("image")} onChange={(file) => setValue("image", file)} />
```

---

## When to use `shouldValidate`

Pass `{ shouldValidate: true }` to `setValue` when the field should trigger validation immediately on change (e.g. a required combobox where the user has already attempted submission):

```tsx
onValueChange={(value) => setValue("jobPosition", value, { shouldValidate: true })}
```

Use this selectively — not on every field by default.

---

## Programmatic errors — `setError()`

Use `setError()` for validation that cannot be expressed in the zod schema (e.g. business rule checks, async server validation):

```ts
setError("password", {
  type: "pattern",
  message:
    "A senha deve ter ao menos uma letra maiúscula, uma letra minúscula, um número e um caractere especial",
});
```

After calling `setError`, return early from `onSubmit` to prevent submission.

---

## Rules

- Native inputs (`Input`, `Textarea`) always use `register()`
- Custom inputs (`StandardCombobox`, `DatePicker`, `Checkbox`, etc.) always use `watch()` + `setValue()`
- Never spread `register()` onto a custom input component
- Use `setError()` only for logic that cannot live in the zod schema
