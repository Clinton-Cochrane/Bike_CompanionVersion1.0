# Room and Persisted-Data Integrity Audit

Issue: #63

Audit date: 2026-09-16

Database version after audit: 21

## Invariant checklist

- PASS — Production has one Room builder, registers the complete 1→21 migration chain, and has no destructive migration fallback.
- PASS — The reconstructed version-1 database and every committed schema fixture (versions 12 through 21) open and migrate without losing representative bike, ride, component, baseline, or lifecycle data.
- PASS — Bike deletion preserves rides and component swap history with null bike references; component deletion cascades contexts, swaps, and service intervals.
- PASS — Ride save, edit, delete, and reassignment use the shared Room transaction boundary. Commit, rollback, no-op, validation, exactly-once, installation-history, and service-reset-boundary cases are covered.
- PASS — Bike totals preserve odometer baselines and rebuild from authoritative ride history without becoming negative.
- PASS — Component and service totals are guarded against negative values and duplicate ride application by the covered repository paths.
- PASS — New installed components and seeded component batches atomically create components, service intervals, and one active installation record; a mid-seed failure rolls back the entire batch.
- PASS — Migration 20→21 repairs missing or inconsistent active installation records. Database triggers reject overlapping active installations on migrated and fresh production databases.
- PASS — Known, approximate, and unknown component prior-use states and baselines survive persistence.
- PASS — Service-completion history and reset boundaries survive persistence and prevent older ride edits, deletes, or reassignments from corrupting post-service progress.
- PASS — Health Connect record identity remains unique across database close/reopen and prevents repeated mileage application.
- PASS — Representative bikes, rides, components, context, service intervals, swaps, baselines, and import identity survive database close/reopen with no foreign-key violations.

## Fixes made during the audit

- Added an atomic component-creation boundary covering the component, initial service intervals, initial swap row, and bulk default seeding.
- Added version 21 to replace a Room-incompatible partial index with equivalent SQLite triggers and repair existing active installation history while preserving the current 16→20 migration chain.
- Removed the obsolete direct bike-deletion repository path so production deletion cannot bypass component disposition and lifecycle reconciliation.
- Repaired invalid JUnit `@Before` signatures that had prevented seven Room instrumentation classes from running.
- Corrected the completed-ride edit fixture so its starting aggregates match its seeded ride history.
- Added complete migration-chain, all-exported-fixture, active-installation, rollback, foreign-key, dedupe, service-boundary, atomic-seeding, and close/reopen regression coverage.

## Follow-ups

- No open P0 persisted-data finding remains.
- #111 and #112, originally identified by this audit, are closed. Service reset boundaries are persisted and covered; completed-ride timestamp editing remains outside the exposed v1 behavior.
- #156 tracks a reproducible, non-Room Stats Compose navigation test failure found by the broad instrumentation run. It does not affect the Room acceptance result.

## Verification

- `./gradlew testDebugUnitTest --no-daemon --stacktrace` — PASS.
- `./gradlew assembleDebugAndroidTest --no-daemon --stacktrace` — PASS.
- Focused Room/migration/accounting instrumentation suite on API 35 emulator — PASS, 53/53.
- Full instrumentation suite on API 35 emulator — Room coverage passed; 62/63 passed overall with only #156 failing outside this audit's scope.
- The connected OnePlus 7 Pro was not modified because an installed build has a different signing key; uninstalling it would erase app data.
