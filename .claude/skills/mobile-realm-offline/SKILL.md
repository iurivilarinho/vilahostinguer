---
name: realm-offline
description: design, implement, review, and refactor features in an offline-first architecture using Realm as the local source of truth. use when building new features, planning data flow, reviewing pull requests for offline correctness, debugging sync issues, or fixing code that treats the API as the primary source of truth. trigger on realm sync, write queues, local-first data, offline behavior, pending sync, API-to-local mapping, "what happens when offline", sync retry, conflict handling, or any question about where data lives and flows at runtime.
---

## Core principle

The local database is the primary runtime source of truth. The API is a synchronization boundary, not a data source.

This matters because: if the UI depends on the API directly, offline support cannot be added later without a full rewrite. Every decision that puts the API in the hot path is load-bearing technical debt. Network unavailability is a normal operating condition — never an exception path.

## Route based on task type

Read the right reference before proceeding. Tasks can be multiple types — load all that apply.

| Task type | Trigger phrases | Load this reference |
|-----------|----------------|---------------------|
| Read flow | fetch, display, refresh, hydrate, reconcile, show data | [api-to-local.md](references/api-to-local.md) |
| Write flow | create, edit, delete, submit, save, any user action that changes state | [local-to-api.md](references/local-to-api.md) |
| Full feature design | new feature, implement from scratch, end-to-end | [implementation-playbook.md](references/implementation-playbook.md) |
| Review / PR | review this, check if this is correct, audit | [review-checklist.md](references/review-checklist.md) |
| Smell / debugging | something's wrong offline, why is sync failing, what's wrong here | [anti-patterns.md](references/anti-patterns.md) |

## Architecture boundaries

Every feature must have explicit, separate layers:

```
Remote API  →  API DTOs  →  [mapper]  →  Realm entities  →  [repository]  →  UI state
                                                                  ↑
                                                           UI write intent
                                                                  ↓
                                                     local write + sync queue → remote
```

The repository is the main architectural boundary. It owns: local reads, local writes, remote fetch, sync queueing, reconciliation, and conflict policy. A repository that just calls the API and returns the result is not an offline-first repository — it's a pass-through, and it needs to be fixed.

## Required design questions

For any feature proposal or implementation, answer these before writing code. If you can't answer #1, the design isn't done yet.

1. What is the local source of truth? (Name the Realm entity or cache shape)
2. What triggers a remote fetch?
3. How does remote data merge into local state?
4. What does the user see when there is no network?
5. What happens when a local write succeeds but sync fails?
6. How is pending sync represented and made durable (survives app restart)?
7. What prevents API DTOs from reaching the UI layer?
8. How is stuck sync detected and diagnosed?

## Output format

Structure your response as:

**Feature summary** — what the user sees and does

**Source of truth** — which Realm entities back the feature and what queries the UI needs

**Read flow** — remote fetch → mapper → Realm upsert → UI observes local state

**Write flow** — UI intent → local write → sync queue record → async sync → reconciliation

**Sync behavior** — retry policy, idempotency assumptions, conflict strategy (be explicit — "handle later" is not a strategy)

**Failure modes** — offline cold start, stale data, partial sync failure, app restart mid-sync, conflict

**Acceptance checks** — conditions that prove the feature is truly offline-first

## Review mode

When reviewing existing code, these require a fix, not just a comment:

- UI importing API DTOs directly
- Repository that just calls the API and returns the raw result
- Screen that blocks on network before showing available local data
- Write that fails because the API is unreachable
- Pending sync state stored only in memory (lost on restart)
- Refresh that clears the screen while new data loads

If any of these are present, recommend the smallest refactor toward local-first reads and local-first writes.

## What a good answer looks like

Concrete. Names specific layers, models, and mappers. Says *when* persistence happens and *when* sync happens. Explains what the UI shows while offline. Describes how broken sync is detected and repaired.

Vague answers ("use caching", "sync later", "handle offline as an enhancement") are not acceptable for this app.
