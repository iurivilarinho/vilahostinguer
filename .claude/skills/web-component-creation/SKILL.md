---
name: web-component-creation
description: Creates reusable React components with a standardized architecture based on separation of concerns, styling conventions, and accessibility guidelines. Use when creating new UI components or refactoring existing ones in React applications.
---

# Component Creation Skill

This project builds components as **named arrow function exports** with **typed props** and **`cn`-based className merging**.

The base UI primitives (buttons, inputs, dialogs, etc.) come from **shadcn/ui** and are treated as third-party code — do not modify, refactor, or enforce project conventions on them. This skill covers **custom components** created for the application.

## Two scopes

| Scope         | Location                      | Purpose                                                |
| ------------- | ----------------------------- | ------------------------------------------------------ |
| Shared        | `src/components/`             | Reused across 2+ features, purely presentational       |
| Feature-level | `features/<name>/components/` | Used only within a single feature, may call data hooks |

See `references/feature-components.md` for what's different at the feature level.

---

## Architecture Overview

1. **Anatomy** — props type, arrow function, inline named export, `cn` merging. See `references/anatomy.md`.

2. **Variants** — style variants with `cva` and `VariantProps`. See `references/variants.md`.

3. **Compound patterns** — static siblings for structural composition; context-based for shared state. See `references/compound-patterns.md`.

4. **Feature components** — can call data hooks; props stay inside the feature. See `references/feature-components.md`.

---

## Usage Guidance

Use this skill when:

- Creating a new **shared UI component** in `src/components/`
- Creating a new **feature component** in `features/<name>/components/`
- Adding **style variants** to an existing component
- Composing a **compound component** (e.g. card with header/body/footer)
- Deciding whether a component should be shared or stay inside a feature

Always follow the reference patterns before introducing custom variations.
