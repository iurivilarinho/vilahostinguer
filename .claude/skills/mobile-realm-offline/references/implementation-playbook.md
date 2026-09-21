# Implementation playbook

Use this file when the task is to design a new feature end to end, or to refactor an existing online-first feature into an offline-first feature.

## Step 1: Define the feature in user terms

Write down:

- what the user can see
- what the user can do
- which actions must still work offline
- which degraded behaviors are acceptable while offline

Do not start from endpoints. Start from user capability.

## Step 2: Define source of truth

For each screen or use case, identify:

- the local entity or document that backs it
- the query or projection the UI needs
- which fields are authoritative locally
- which metadata is needed for freshness and sync

If you cannot name the local source of truth, the feature is not designed yet.

## Step 3: Separate models by boundary

Define:

- API DTOs for remote transport
- local/domain entities for persistence and app logic
- UI models only if the UI needs a specialized projection

Do not reuse one model for all layers unless the boundaries are truly identical and stable. In most offline-first work, they are not.

## Step 4: Design read flow

For each read use case, specify:

- how local data is queried
- when refresh happens
- how remote data maps into local state
- what the UI shows during refresh failure
- what stale but usable data looks like

Use [references/api-to-local.md](references/api-to-local.md).

## Step 5: Design write flow

For each write use case, specify:

- the user intent
- the local mutation
- the pending sync record
- when sync triggers
- retry strategy
- reconciliation behavior
- conflict rules

Use [references/local-to-api.md](references/local-to-api.md).

## Step 6: Design sync observability

At minimum, define:

- where pending operations can be inspected
- how retry counts and last errors are stored
- what logs or metrics would help diagnose stuck sync
- what the UI exposes to users, if anything

Offline-first systems fail silently unless observability is designed intentionally.

## Step 7: Implement in this order

Preferred order:

1. local entities and persistence
2. query interfaces for UI reads
3. repository contracts
4. API DTOs and remote datasource
5. mappers
6. local-first write path
7. sync queue / worker
8. reconciliation logic
9. UI integration
10. diagnostics and tests

This ordering reduces the chance of accidentally building an API-first design.

## Step 8: Review for boundary leaks

Check that:

- UI does not import transport models
- repositories are not remote pass-throughs
- local write exists before sync logic
- refresh updates local storage rather than bypassing it
- pending state is durable

## Step 9: Test the unhappy paths

Every feature should be reasoned through under:

- cold start with no network
- existing cached data plus failed refresh
- local write with no network
- app restart before queued sync completes
- duplicate sync attempt
- remote conflict or validation error
- remote success that changes canonical values

## Step 10: Define acceptance criteria

A feature is only done when:

- the user can read persisted state offline where expected
- the user can perform supported writes offline where expected
- sync can recover from interruptions
- architecture boundaries are explicit
- failures are diagnosable

## Suggested output format for agents

When asked to design or review, produce:

1. feature summary
2. local source of truth
3. read flow
4. write flow
5. sync and reconciliation
6. failure and conflict handling
7. boundary definitions
8. acceptance criteria
