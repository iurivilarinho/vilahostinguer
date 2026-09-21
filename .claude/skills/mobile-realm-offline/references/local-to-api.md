# Local to API pattern

Use this reference when the task is mainly about create, edit, delete, submit, acknowledge, reorder, or any user action that changes state.

## Goal

Apply user intent locally first, then synchronize outward to the API without making immediate network success a prerequisite for a successful user action.

## Canonical write flow

UI intent -> repository -> local write -> pending sync record / operation queue -> sync worker -> API request DTO -> remote API -> sync result -> local reconciliation

## Responsibilities by layer

### UI layer

The UI should:

- express intent, not transport details
- show local success immediately after persistence
- display pending-sync or failed-sync state when relevant
- avoid direct knowledge of API request shape

### Repository layer

The repository should:

- validate and translate UI/domain intent
- write the authoritative local mutation
- create sync metadata or queue records
- expose updated local state to the UI
- trigger immediate or deferred synchronization based on policy

### Local persistence

Local persistence should store:

- the user-visible data
- enough metadata to identify pending synchronization
- operation status such as pending, syncing, failed, completed
- retry count, timestamps, and optional conflict markers when useful

### Sync layer

The sync layer should:

- read queued or pending operations
- map pending operations into API request DTOs
- execute requests safely and repeatedly
- handle retries and backoff
- mark operations complete, failed, or conflicted
- reconcile remote-confirmed state back into local storage

## Write sequence

1. User performs an action.
2. UI sends an intent or command to the repository.
3. Repository persists the local state change first.
4. Repository records a pending sync operation.
5. UI reflects the local change immediately.
6. Sync worker attempts remote delivery now or later.
7. On success, pending state is cleared or marked completed.
8. On failure, operation remains retryable and visible to diagnostics.
9. On conflict, local reconciliation rules are applied explicitly.

## Important rules

### Local success and remote success are different events

Do not collapse them into a single boolean result.
Local persistence may succeed while remote sync remains pending or fails later.

### Pending state must be observable

A feature should be able to answer:

- which items are pending sync
- which operation failed
- when the last retry occurred
- what can be retried safely

### Queue durable intent, not just transient memory

If the app restarts, pending operations should not vanish.
Persist operation records durably unless the platform provides an equivalent durable mechanism.

### Prefer idempotent remote writes

When possible, use request identifiers, mutation IDs, or endpoints that tolerate retries.
Offline-first systems must expect duplicate attempts.

### Reconcile after remote success

The server may:

- assign canonical IDs
- return normalized values
- update timestamps
- reject stale versions
- enrich or transform the submitted data

Always reconcile those results back into local state through explicit rules.

## Sync record guidance

A pending operation record may include:

- operationId
- entityType
- entityLocalId
- operationType
- payload snapshot or reference
- createdAt
- updatedAt
- retryCount
- nextRetryAt
- status
- lastError
- correlationId or idempotency key

Do not treat this list as mandatory schema. Adapt it to the platform.

## Conflict handling

Every write feature should define conflict strategy. Examples:

- server wins
- client wins
- merge by field
- require manual resolution
- reject and surface a user-visible remediation path

Do not leave conflict behavior implicit.

## Deletes

Deletes require special care:

- support tombstones when needed so deletion can sync later
- do not remove all evidence of an item before sync if the delete still needs to be propagated
- define how deleted items behave in the UI while pending

## Ordering and dependent operations

If operations depend on each other, define ordering rules.
Examples:

- create must sync before update on the same remote entity
- child creation may depend on parent remote ID
- delete may cancel pending updates for the same item

## Example reasoning template

When describing a write flow, answer:

- What user intent starts the write?
- What local entities change immediately?
- What pending operation is created?
- What data is stored for retries?
- When does sync run?
- How are failures surfaced?
- How is success reconciled?
- What is the conflict strategy?

## Red flags

These indicate a non-offline-first write design:

- button press sends API request before local persistence
- app reports failure solely because network is unavailable
- pending operations live only in memory
- sync failures are invisible and not retryable
- server-assigned IDs are required before the UI can proceed
- delete removes the item with no tombstone or deferred propagation strategy

## Acceptance criteria for write flow

A write flow is acceptable when:

- user intent persists locally without requiring network
- the change is visible in the UI immediately after local write
- sync can happen later without losing intent
- failures remain retryable and diagnosable
- reconciliation rules are explicit
- DTOs for remote writes are created in the sync boundary, not the UI
