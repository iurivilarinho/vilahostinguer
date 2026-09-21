# API to local pattern

Use this reference when the task is mainly about fetching, refreshing, hydrating, reconciling, or displaying data.

## Goal

Convert remote data into local source-of-truth state that the UI can consume without depending on transport models.

## Canonical read flow

Remote API -> API DTOs -> mapper -> local/domain entities -> local database/cache -> repository output -> UI state

The UI should never render directly from API DTOs if the feature is intended to be offline-first.

## Responsibilities by layer

### Remote layer

The remote layer is responsible for:

- making HTTP or transport calls
- parsing remote payloads into API DTOs
- handling transport-level concerns such as status codes, headers, pagination cursors, and envelope formats

The remote layer is not responsible for:

- driving UI state directly
- acting as the source of truth
- containing business logic for offline behavior

### Mapper layer

The mapper layer converts:

- remote DTOs into local/domain models
- remote nullability and transport naming into internal shapes
- nested remote structures into normalized local data where appropriate

Mapper rules:

- keep mapping explicit
- do not mix mapper logic into UI state holders
- do not pass DTOs beyond the data boundary
- normalize unstable or optional transport fields before persistence

### Local layer

The local layer is responsible for:

- storing the persistent source of truth
- supporting queries needed by the UI
- preserving state across app restarts
- allowing reads while offline
- holding metadata such as freshness, sync timestamps, or tombstones when needed

### Repository layer

The repository coordinates:

- refresh triggers
- remote fetches
- mapping
- local upserts or merges
- query exposure to the UI
- freshness policy
- cache invalidation strategy
- fallback behavior when network is unavailable

A repository should expose local/domain-facing outputs, not transport-facing outputs.

### UI layer

The UI:

- subscribes to local state
- renders cached or persisted data immediately when available
- reacts to loading and refreshing state separately from content presence
- does not depend on remote schema shape
- remains usable when refresh fails

## Read sequence

1. UI requests data from repository.
2. Repository returns local persisted or cached data immediately when available.
3. Repository decides whether a refresh is needed.
4. Remote call retrieves API DTOs.
5. DTOs are mapped into local/domain entities.
6. Local persistence is updated transactionally when needed.
7. UI observes local changes and re-renders.
8. If refresh fails, keep prior local data visible and report refresh state separately.

## Key design rules

### Separate "has data" from "is refreshing"

Do not clear the screen merely because a refresh started.
The user may already have perfectly usable local data.

### Preserve stable local identifiers

If remote items lack stable IDs, derive a safe local identity strategy before persisting.
Do not let unstable transport identity break local rendering or diffing.

### Normalize before persistence

If the API is nested, denormalized, or inconsistent, fix that at the mapper boundary.
Do not force the UI to understand remote shape quirks.

### Store metadata that helps refresh policy

Useful metadata may include:

- fetchedAt
- lastSuccessfulSyncAt
- version or etag
- pagination cursor
- invalidation marker
- deleted marker

### Support partial availability

A feature may show:

- persisted content
- a background refresh spinner
- a stale-data badge
- a retry affordance

That is still correct offline-first behavior.

## Example reasoning template

When describing a read flow, answer:

- What is fetched remotely?
- What DTOs exist?
- What local entities exist?
- What mapping rules convert DTOs to local entities?
- Where is the source of truth stored?
- How does the UI subscribe to local state?
- What happens if refresh fails?
- What freshness policy triggers new fetches?

## Red flags

These indicate an online-first design:

- UI directly imports API response types
- Repository returns raw DTOs
- Screen waits for network before rendering cached data
- Refresh failure removes already persisted data from view
- Mapping logic is embedded in the screen or view model
- A "get data" use case bypasses local persistence entirely

## Good implementation shape

A good implementation usually has:

- `RemoteDataSource` or equivalent
- API DTO definitions
- mapper modules
- local entity definitions
- DAO/store/cache query layer
- repository with refresh orchestration
- UI state derived from repository outputs

## Acceptance criteria for read flow

A read flow is acceptable when:

- the UI can render the latest persisted local data without network
- remote DTOs do not leak into UI-facing layers
- refresh updates local storage rather than bypassing it
- refresh failure does not destroy usable local content
- repository contracts are domain/local oriented
