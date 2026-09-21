# Schema and Form Initialization

## Schema definition

Define the zod schema at module scope — never inside a component:

```ts
import { z } from "zod";

const entityFormSchema = z.object({
  name: z.string().min(1, "Informe o nome"),
  description: z.string().optional(),
});
```

## Type inference

Infer the form type from the schema — never write a separate interface:

```ts
type EntityFormValues = z.infer<typeof entityFormSchema>;
```

## Default values

Define a typed `DEFAULT_*_FORM_VALUES` constant at module scope alongside the schema:

```ts
const DEFAULT_ENTITY_FORM_VALUES: EntityFormValues = {
  name: "",
  description: "",
};
```

All fields must be present in the default values, even optional ones, to avoid uncontrolled → controlled input warnings.

## useForm initialization

```ts
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";

const {
  register,
  handleSubmit,
  reset,
  formState: { errors },
} = useForm<EntityFormValues>({
  resolver: zodResolver(entityFormSchema),
  defaultValues: DEFAULT_ENTITY_FORM_VALUES,
});
```

For complex forms requiring `watch`, `setValue`, or `setError`, destructure those from the same call:

```ts
const {
  register,
  handleSubmit,
  reset,
  watch,
  setValue,
  setError,
  formState: { errors },
} = useForm<EntityFormValues>({
  resolver: zodResolver(entityFormSchema),
  defaultValues: DEFAULT_ENTITY_FORM_VALUES,
});
```

## Custom types in schemas

For fields holding complex objects (DTOs), use `z.custom<T>()`:

```ts
import type { RoleApiDto } from "../../role/api/dtos/role";

const schema = z.object({
  roles: z.custom<RoleApiDto[]>().optional(),
  jobPosition: z.custom<JobPositionApiDto>().optional(),
});
```

## Rules

- Schema, type, and default values must be defined at **module scope**
- Type always comes from `z.infer<typeof schema>` — never handwritten
- Default values must cover all schema fields
- `zodResolver` is always the resolver — no custom validation functions
