# Form Anatomy

## Field layout

Each form field is composed of base components from `@/components/input/base/Field`:

```tsx
import { Field, FieldGroup, FieldLabel, FieldError } from "@/components/input/base/Field";
import { Input } from "@/components/input/base/Input";

<FieldGroup>
  <Field>
    <FieldLabel htmlFor="field-id">Label</FieldLabel>
    <Input id="field-id" {...register("fieldName")} />
    {errors.fieldName && <FieldError>{errors.fieldName.message}</FieldError>}
  </Field>
</FieldGroup>;
```

## Available layout components

| Component          | Purpose                                                                |
| ------------------ | ---------------------------------------------------------------------- |
| `FieldGroup`       | Wraps a set of fields, applies vertical spacing                        |
| `FieldSet`         | Semantic grouping (`<fieldset>`), for sections with a legend           |
| `FieldLegend`      | Section title inside a `FieldSet`                                      |
| `Field`            | Wraps a single label + input + error unit                              |
| `FieldLabel`       | Accessible label, always paired with `htmlFor` matching the input `id` |
| `FieldDescription` | Optional helper text below the label or legend                         |
| `FieldError`       | Renders a validation error message                                     |
| `FieldSeparator`   | Visual divider between field groups                                    |

## FieldError

`FieldError` renders nothing when empty — always conditionally render it:

```tsx
{
  errors.fieldName && <FieldError>{errors.fieldName.message}</FieldError>;
}
```

For optional fields that have no required constraint, omit `FieldError` entirely.

## isSaving — submit button state

Aggregate all mutation `isPending` flags into a single `isSaving` variable and use it to disable the button and update its label:

```tsx
const { mutateAsync: createEntity, isPending: isCreating } = useCreateEntity();
const { mutateAsync: updateEntity, isPending: isUpdating } = useUpdateEntity();

const isSaving = isCreating || isUpdating;

// In JSX:
<Button type="submit" disabled={isSaving}>
  {isSaving ? "Salvando..." : "Salvar"}
</Button>;
```

For forms where only one mutation is possible (e.g. create-only), `isSaving` can alias directly:

```tsx
const { mutateAsync: createEntity, isPending: isSaving } = useCreateEntity();
```

## Form element

Always attach `handleSubmit` to the `<form>` element directly:

```tsx
<form onSubmit={handleSubmit(onSubmit)}>{/* fields */}</form>
```

Do not call `handleSubmit` inside a button `onClick`.
