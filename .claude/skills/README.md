# Claude Skills — WMS

Mirror of `.codex/skills/` adapted for use with Claude. Each skill lives in its own folder with a `SKILL.md` (frontmatter + Markdown body) and may include `agents/` and `references/` subfolders.

To invoke a skill, ask Claude to "use the `<skill-name>` skill" or load `SKILL.md` directly when starting a task in that domain.

---

## Planning & review

| Skill | Use for |
| --- | --- |
| [architect](architect/SKILL.md) | Research and design technical solutions before implementation. Produces an implementation plan; never writes production code. |
| [debug](debug/SKILL.md) | Investigate bugs, identify root causes, propose the safest fix, and implement it. |
| [qa](qa/SKILL.md) | Decide what to test, identify critical scenarios, review test quality, and write valuable automated tests. |
| [ux-ui](ux-ui/SKILL.md) | Review and improve usability, interaction flow, and visual clarity for screens and flows. |

## Frontend (React 19 + TypeScript)

Top-level entry: **[web-dev](web-dev/SKILL.md)** — day-to-day frontend implementation following project conventions.

| Skill | Use for |
| --- | --- |
| [web-feature-structure](web-feature-structure/SKILL.md) | Decide folder/file layout under `src/features/`. Where does a page, hook, or api file belong? |
| [web-component-creation](web-component-creation/SKILL.md) | Create or refactor reusable React components (separation of concerns, styling, a11y). |
| [web-form-creation](web-form-creation/SKILL.md) | Build forms with `react-hook-form` + `zod`: schema, binding, edit-mode, submit. |
| [web-api-integration](web-api-integration/SKILL.md) | Add or modify TanStack Query hooks, query keys, and mutations against the backend. |

## Backend (Java Spring)

Top-level entry: **[spring-backend-standards](spring-backend-standards/SKILL.md)** — naming, mapping, DI, audit, Swagger, persistence; routes to the narrower skills below.

| Skill | Use for |
| --- | --- |
| [spring-crud-module](spring-crud-module/SKILL.md) | Create or change CRUD modules (entity, repository, service, controller, request/response). |
| [spring-filters-specification](spring-filters-specification/SKILL.md) | Dynamic filtered listing endpoints with `Specification` (null-safe, pageable). |
| [spring-status-enums](spring-status-enums/SKILL.md) | Status enums with transition validation and Swagger-documented values. |
| [spring-security-jwt](spring-security-jwt/SKILL.md) | Stateless Spring Security + JWT (filter chain, token validation, authenticated user). |
| [spring-error-logging](spring-error-logging/SKILL.md) | Global `@RestControllerAdvice` + request/response interceptor with timing and request ids. |
| [spring-audit-log](spring-audit-log/SKILL.md) | Who moved an entity, when, and what changed: `createdBy`/`updatedBy` stamping + append-only audit log. |
| [spring-report-excel](spring-report-excel/SKILL.md) | Excel exports with Apache POI and the reusable report engine. |
| [spring-document-storage](spring-document-storage/SKILL.md) | Binary file storage (images, PDFs, video) with Postgres vs SQL Server JPA mappings. |
| [spring-md-content-storage](spring-md-content-storage/SKILL.md) | Persist long Markdown / textual content via JPA `@Lob String`. |
| [spring-tests-junit-mockito](spring-tests-junit-mockito/SKILL.md) | Unit/controller tests with JUnit 5 + Mockito (mocks, capture, exception flows). |

## Obsidian & content tooling

| Skill | Use for |
| --- | --- |
| [obsidian-cli](obsidian-cli/SKILL.md) | Interact with the Obsidian vault from the CLI (read, search, create notes, manage plugins). |
| [obsidian-markdown](obsidian-markdown/SKILL.md) | Author Obsidian-flavored Markdown: wikilinks, callouts, embeds, properties. |
| [obsidian-bases](obsidian-bases/SKILL.md) | Build `.base` files: views, filters, formulas, summaries. |
| [json-canvas](json-canvas/SKILL.md) | Author `.canvas` files (nodes, edges, groups) for diagrams and mind maps. |
| [defuddle](defuddle/SKILL.md) | Extract clean Markdown from a web page via Defuddle CLI (preferred over `WebFetch` for HTML). |

## Shared

- [`shared/api`](shared/api/) — shared API references used by multiple skills.
- [`shared/references`](shared/references/) — cross-skill reference material.

---

## Codex parity

This tree is kept in sync with `.codex/skills/`. When updating a skill, change both copies (or update one and run a diff:

```bash
diff -rq .codex/skills .claude/skills
```

Some references to `#tool:vscode/...` inside skill bodies are Codex-specific and have no Claude equivalent — when running under Claude, treat them as: `#tool:vscode/askQuestions` → ask the user directly (or use `AskUserQuestion`); `#tool:vscode/memory` → write under `memoria-do-projeto/`.

See `AGENTS.md` at the repo root for the persistent context policy that applies to both runtimes.
