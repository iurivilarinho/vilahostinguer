# Anti-patterns

Use this file to detect and correct designs that look convenient but break offline-first architecture.

## 1. API as source of truth

### Smell

The UI reads directly from HTTP results, and local storage is treated as an optimization.

### Why it fails

Offline support becomes partial and fragile.
The app cannot reliably restore state without network.

### Fix

Make local persistence the authoritative runtime source.
Route remote refresh through mapping and local storage before the UI consumes it.

## 2. Thin repository over remote client

### Smell

The repository just forwards calls to the API client.

### Why it fails

The architectural boundary exists in name only.
No offline coordination, caching policy, persistence strategy, or sync logic lives there.

### Fix

Move orchestration into the repository:

- local reads
- local writes
- remote refresh
- queueing
- reconciliation
- refresh policy

## 3. DTO leakage into UI

### Smell

View models, presenters, or components import remote DTOs or response envelopes.

### Why it fails

Transport concerns spread upward.
Any API shape change ripples into UI logic.
Offline persistence becomes harder to reason about.

### Fix

Keep API DTOs at the remote boundary.
Map into local/domain models and expose only those upward.

## 4. Write-through network dependency

### Smell

A create or edit action is considered successful only after the API confirms it.

### Why it fails

The feature effectively stops working offline.

### Fix

Persist locally first.
Track sync separately.
Report local success and sync state as distinct concepts.

## 5. Ephemeral sync queue

### Smell

Pending sync operations exist only in memory.

### Why it fails

App restarts lose intent.
Background or interrupted sync becomes unreliable.

### Fix

Use durable persistence for pending operations or an equivalent durable job system.

## 6. Refresh wipes good data

### Smell

A refresh failure clears the screen or resets content to empty.

### Why it fails

The user loses access to usable local information because of a transient remote problem.

### Fix

Keep local content visible.
Represent refresh failure separately from no-data state.

## 7. Implicit conflict handling

### Smell

The design says conflicts are "handled later" or assumes they are rare.

### Why it fails

Eventually sync behavior becomes inconsistent and surprising.

### Fix

Define a conflict strategy per write feature:

- server wins
- client wins
- merge
- manual resolution
- reject with remediation path

## 8. Delete without propagation strategy

### Smell

Deleted local items disappear with no tombstone or deferred delete sync plan.

### Why it fails

The server may never receive the delete operation, or the item may reappear after refresh.

### Fix

Use tombstones or equivalent tracking until remote deletion is reconciled.

## 9. One model for everything

### Smell

The same object is used as API payload, local entity, business object, and UI model.

### Why it fails

Each layer becomes constrained by the others.
Boundary-specific concerns become tangled.

### Fix

Split models where responsibilities differ.
At minimum, separate remote DTOs from local/domain entities.

## 10. Offline as a fallback feature

### Smell

The implementation is designed online-first, with plans to add caching or queueing later.

### Why it fails

The fundamental decisions are already wrong:

- UI depends on API shape
- writes assume network
- repositories are remote-centric

### Fix

Start from local truth and asynchronous synchronization from day one.
