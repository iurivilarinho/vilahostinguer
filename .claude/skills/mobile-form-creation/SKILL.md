---
name: mobile-form-creation
description: create or modify forms in React Native using react-hook-form and zod with Realm-backed data. use when creating a new form screen or modal, adding fields to an existing form, implementing edit mode for an existing entity, or extracting a submit hook for complex multi-mutation flows.
---

This project builds forms using **react-hook-form** with **zod** schema validation and **@hookform/resolvers**.

---

## Stack

| Library                   | Purpose                                       |
| ------------------------- | --------------------------------------------- |
| `react-hook-form`         | Form state, registration, validation triggers |
| `zod`                     | Schema definition and type inference          |
| `@hookform/resolvers/zod` | Connects zod schema to react-hook-form        |

---

## Architecture Overview

### 1. Schema

- Defined at module scope with `z.object()`
- Type inferred via `z.infer<typeof schema>`
- Default values constant defined alongside the schema

### 2. Anatomy

- Forms use the shared form components in `src/components/form/`:
  - `fieldWrapper/` — wraps a field with label and error display
  - `input/` — text input bound to react-hook-form
  - `textarea/` — multiline text input
  - `imageAttach/` — image attachment input
- Error display and submit button state follow a consistent pattern across screens

### 3. Input binding

- Native inputs: use `Controller` from react-hook-form with the relevant form component
- Custom inputs (pickers, checkboxes, image): use `Controller` with `render` prop
- Never use uncontrolled inputs outside of react-hook-form

### 4. Edit mode

- Populate the form from Realm data using a `useEffect` with `reset()`
- Load data via the relevant Realm service hook (`useObject` or `useQuery`)
- Only reset after data is confirmed available

### 5. Submit

Two escalating patterns:

**Simple** (modal/sheet/inline form):

- Inline `onSubmit` handler in the component
- Calls the Realm service write function directly (which also creates a `PushQueueItem`)
- Show success/error feedback via `react-native-toast-message`

**Complex** (full screen, multi-step, multi-entity):

- Extract a `useXxxFormSubmit` hook
- Handles orchestration of multiple Realm writes and `PushQueueItem` creation
- Returns `{ onSubmit, isSubmitting }` to the form component

---

## When to use

- Creating a new **form screen or modal**
- Adding fields to an existing form
- Implementing **edit mode** for an existing entity
- Extracting a submit hook for complex multi-mutation flows
- Ensuring consistent form patterns across features

Always inspect existing forms in `src/features/` before introducing custom variations.
