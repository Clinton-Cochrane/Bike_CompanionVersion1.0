# Room and Persisted-Data Integrity Audit

Issue: #63  
Audit date: 2026-09-14  
Database version after audit: 17

## Invariant checklist

- PASS — Production has one Room builder, registers every migration, and has no destructive fallback.
- PASS — The reconstructed version-1 database and the existing version-3 fixture migrate to version 17 without losing representative bike, ride, or component data.
- PASS — Every exported schema fixture (versions 12 through 17) opens and migrates to the current schema.
- PASS — Bike deletion preserves rides and swap history with null bike references; component deletion cascades contexts, swaps, and service intervals.
- PASS — Ride save, edit, delete, and reassignment use the shared Room transaction boundary. Commit, rollback, no-op, validation, and exactly-once cases are covered.
- PASS — Bike totals preserve odometer baselines and rebuild from authoritative ride history without becoming negative.
- PASS — Component and service totals are guarded against negative values and duplicate ride application by the covered repository paths.
- PASS — New installed components and seeded components atomically create service intervals and one active installation record.
- PASS — Migration 16→17 repairs missing or inconsistent active installation records. Database triggers reject overlapping active installations on migrated and fresh production databases.
- PASS — Known, approximate, and unknown component prior-use states and baselines survive persistence.
- PASS — Health Connect record identity remains unique across database close/reopen and prevents repeated mileage application.
- PASS — Representative bikes, rides, components, context, service intervals, swaps, baselines, and import identity survive database close/reopen with no foreign-key violations.

## Fixes made during the audit

- Added an atomic component-creation boundary covering the component, initial service intervals, and initial swap row.
- Added version 17 to replace a Room-incompatible partial index with equivalent SQLite triggers and repair existing active installation history.
- Removed the obsolete direct bike-deletion repository path so production deletion cannot bypass component disposition and lifecycle reconciliation.
- Repaired invalid JUnit `@Before` signatures that had prevented seven Room instrumentation classes from running.
- Corrected the completed-ride edit fixture so its starting aggregates match its seeded ride history.
- Added complete migration-chain, all-exported-fixture, active-installation, rollback, foreign-key, dedupe, and close/reopen regression coverage.

## Follow-up issues

These are substantive model changes and were intentionally not folded into this audit.

1. #111 — Persist service-completion boundaries for historical ride reconciliation (P0 before the Pixel alpha). Current service intervals store only progress since the latest reset. Deleting, editing, or reassigning a ride that predates a completed service can subtract that ride from post-service progress because the reset timestamp/history is not persisted.
2. #112 — Reconcile component membership when an edited ride crosses installation boundaries (P1 before timestamp editing ships). The edit operation adjusts components installed at the original ride end time; changing the end time across a swap should remove the original ride from the old component set and apply the replacement ride to the new set.

## Verification

- `./gradlew assembleDebug --no-daemon`
- `./gradlew testDebugUnitTest lintDebug --no-daemon`
- `./gradlew connectedDebugAndroidTest --no-daemon`

The final verification runs completed successfully. Instrumentation executed on the API 35 emulator (`emulator-5554`).
