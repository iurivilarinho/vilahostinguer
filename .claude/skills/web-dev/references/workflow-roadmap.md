# AI Workflow Improvement Roadmap

## Progress

| Phase | Target                                                 | Status  |
| ----- | ------------------------------------------------------ | ------- |
| 1     | `api-integration` skill                                | ✅ Done |
| 2     | `form-creation` skill                                  | ✅ Done |
| 3     | `component-creation` skill                             | ✅ Done |
| 4     | `feature-structure` skill                              | ✅ Done |
| 5     | All 5 agents                                           | ✅ Done |
| A     | `copilot-instructions.md` — naming, alias, UI patterns | ✅ Done |

| B | New `routing` skill | ⬜ Pending |
| C | New `error-handling` skill | ⬜ Pending |
| D | Agent handoff chains | ⬜ Pending |
| E | `debug.agent.md` — TypeScript patterns | ⬜ Pending |
| F | `qa.agent.md` — reframe as risk analysis | ⬜ Pending |
| G | New `state-management` skill | ⬜ Pending |
| H | New `code-review` agent (optional) | ⬜ Pending |

---

## Gap Analysis

### 1. `copilot-instructions.md` (Foundation — affects every agent)

- No naming conventions (PascalCase components, `useXxx` hooks, `XxxApiDto` types)
- No `@/` import alias documentation (project uses it everywhere)
- No UI pattern decisions (Dialog vs Sheet vs Page) — AI guesses on every feature
- No reminder to check `src/components/` before creating new UI components
- Feature Structure section was outdated (showed unused `types/` folder and `page.tsx`)

### 2. Missing Skills

- **`routing`** — react-router-dom v7: `routes.ts` constants, `ProtectedRoute`, route params, search params. Used every time a new page or feature is created.
- **`error-handling`** — Error states in queries, error boundaries, recovery patterns, consistent error display. Affects all components.
- **`state-management`** — When to use local state, lifted state, Context, or React Query. Lower priority since React Query handles most UI state.

### 3. Agent Limitations

| Agent              | Gap                                                                                            |
| ------------------ | ---------------------------------------------------------------------------------------------- |
| `debug.agent.md`   | No TypeScript-specific debugging guidance (type errors, narrowing issues, declaration merging) |
| `qa.agent.md`      | No test framework installed — needs to be repositioned as risk analysis + testing readiness    |
| `ux-ui.agent.md`   | No handoff to web-dev defined after finding issues                                             |
| `web-dev.agent.md` | No explicit reminder to use existing `src/components/` before creating new UI                  |

### 4. Workflow Coordination

- Only `architect → web-dev` is a defined handoff chain; the others have no explicit inbound/outbound
- No definition of "done" for a feature
- No code review step before a feature is considered complete

---

## Phase Details

### Phase B — `routing` skill

New skill: `.github/skills/routing/`

Reference files to create:

- `SKILL.md` — overview, when to use this skill
- `references/routes.md` — `ROUTES` constant in `routes.ts`, how to add new routes
- `references/protected-route.md` — `ProtectedRoute` usage, auth gating
- `references/params.md` — `useParams` for route params (`:formType`, `:id?`), `useSearchParams` for filters/search

### Phase C — `error-handling` skill

New skill: `.github/skills/error-handling/`

Reference files to create:

- `SKILL.md` — overview
- `references/query-errors.md` — how to handle `isError` from `useQuery`, error display conventions
- `references/mutation-errors.md` — already covered by `api-integration`, link/summarize
- `references/display.md` — when to use toast vs inline error vs empty state

### Phase D — Agent handoff chains

Update all 5 agent files to define:

- Which agents can invoke this one (inbound)
- Which agents to hand off to (outbound)
- What "done" looks like before handing off

Proposed full workflow chain:

```
architect → web-dev → ux-ui (review) → qa (risk check) → done
                ↑
             debug (when issues found)
```

### Phase E — `debug.agent.md`

Add TypeScript-specific debugging section:

- Common type errors and how to trace them
- Type narrowing failures
- Generic constraint violations
- Declaration file issues

### Phase F — `qa.agent.md`

Reframe the agent since no test framework is installed:

- Primary role: risk analysis — what _should_ be tested and why
- Secondary role: testing readiness — document test scenarios so they're ready when Vitest is added
- Keep the Vitest/RTL stack recommendation for when tests are introduced

### Phase G — `state-management` skill

New skill: `.github/skills/state-management/`

Cover:

- Local `useState` — component-only UI state (modals open, selected tabs)
- Lifted state — shared between sibling components
- React Query — all server state (don't duplicate with `useState`)
- URL state via `useSearchParams` — filters, pagination, sort that survive navigation
- Context — rare, only for deeply shared non-server state (e.g., auth user)

### Phase H — `code-review` agent (optional)

New agent for reviewing completed features before committing:

- Checks adherence to skill patterns (api-integration, form-creation, etc.)
- Flags TypeScript issues
- Identifies missing error/loading/empty states
- Notes any pattern inconsistencies
