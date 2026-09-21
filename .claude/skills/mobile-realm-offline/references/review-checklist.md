# Review checklist

Use this checklist for pull requests, architecture reviews, refactors, or implementation plans.

## Source of truth

- Is the local database or local store clearly identified as the runtime source of truth?
- Can the UI render from local data without network?
- Is remote data persisted locally before becoming user-visible state?

## Model boundaries

- Are API DTOs isolated to the remote boundary?
- Are local/domain entities distinct from transport models where needed?
- Are mapping rules explicit and centralized enough to maintain?

## Repository behavior

- Does the repository coordinate local and remote concerns rather than proxying remote calls?
- Does repository output align with local/domain needs instead of transport needs?
- Does refresh write into local state?

## Read path

- Does the UI get immediate local content when available?
- Does refresh happen without wiping already persisted data?
- Are stale-data and refresh-error states separated from empty-data state?

## Write path

- Does user intent persist locally first?
- Is there a durable pending-sync representation?
- Can the feature function offline without immediate server success?
- Is sync retriable?

## Sync and reconciliation

- Is there an explicit sync trigger?
- Are retries and backoff defined?
- Is idempotency considered?
- Is reconciliation after remote success defined?
- Is conflict behavior defined?

## Deletes and special cases

- Are deletions propagated safely with tombstones or equivalent behavior if needed?
- Are dependent operations ordered correctly?
- Are server-assigned IDs handled without blocking local UX?

## Observability

- Can pending operations be inspected?
- Are failed operations diagnosable?
- Are last error and retry counts captured where useful?
- Can stuck sync be debugged from logs, metrics, or stored metadata?

## Smells that require changes

Any "yes" answer below should trigger concern:

- Does the UI import API models?
- Does a screen wait on network before showing available local data?
- Does a write fail entirely because the API is currently unreachable?
- Does refresh bypass local storage?
- Are pending operations stored only in memory?
- Is conflict handling unspecified?
