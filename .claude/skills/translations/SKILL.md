---
name: translations
description: Copy and internationalization rules for this React Vite web app. Use when adding user-facing text, deciding whether to hardcode Portuguese copy, introducing i18n, or maintaining translation keys.
---

# Translations

This project is currently PT-BR first and does not have a full i18n structure. Do not invent partial i18n files for a single change unless the user asks for an i18n migration.

## Current Rule

- Keep user-facing copy in Portuguese to match the existing product.
- Keep copy consistent and concise.
- Centralize repeated long copy or landing-page content in feature-local content files when useful.
- Do not hardcode raw API error messages; use `getApiErrorMessage`.

## If i18n Is Introduced

Use a complete structure and keep keys synchronized:

```txt
src/translations/
  pt.json
  en.json
src/lib/i18n/
```

Both files must have identical keys. Add i18n in a dedicated migration, not opportunistically in unrelated changes.
