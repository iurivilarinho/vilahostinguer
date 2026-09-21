---
name: web-form-creation
description: Creates forms in React using react-hook-form and zod with a standardized architecture based on schema definition, input binding, edit-mode population, and submit patterns. Use when creating or modifying forms in this project.
---

# Form Creation Skill

This project builds forms using **react-hook-form** with **zod** schema validation and **@hookform/resolvers**.

When implementing or modifying forms, follow the architecture and patterns defined in the reference documents.

## Stack

| Library                   | Purpose                                       |
| ------------------------- | --------------------------------------------- |
| `react-hook-form`         | Form state, registration, validation triggers |
| `zod`                     | Schema definition and type inference          |
| `@hookform/resolvers/zod` | Connects zod schema to react-hook-form        |

## Architecture Overview

1. **Schema** — defined at module scope with `z.object()`. Type inferred via `z.infer<>`. Default values constant alongside. See `references/schema.md`.

2. **Anatomy** — forms use the `Field`, `FieldLabel`, `FieldError`, `FieldGroup` base components for layout. Error display and submit button state follow a consistent pattern. See `references/anatomy.md`.

3. **Input binding** — native inputs use `register()`. Custom inputs (combobox, date picker, checkbox) use `watch()` + `setValue()`. See `references/input-binding.md`.

4. **Edit mode** — populate the form from remote data using a `useEffect` with `reset()`. See `references/edit-mode.md`.

5. **Submit** — two escalating patterns:
   - **Simple** (sheet/dialog form): inline `onSubmit` calling `mutateAsync` directly
   - **Complex** (full page, multi-mutation): extracted `useXxxFormSubmit` hook
     See `references/submit-patterns.md`.

---

# Usage Guidance

Use this skill when:

- Creating a new **form component** (sheet, dialog, or page)
- Adding fields to an existing form
- Implementing **edit mode** for an existing entity
- Extracting a submit hook for complex multi-mutation flows
- Ensuring consistent form patterns across features

Always follow the reference patterns before introducing custom variations.

## References

- [data-mapping](./references/data-mapping.md) — when and how to extract API-to-form mapping into a pure utility
