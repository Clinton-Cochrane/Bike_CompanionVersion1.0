# Trip Time Tracking and Aggregation

This document describes the implementation of trip duration tracking and its roll-up to bikes and components.

## Data Model

### Ride (Trip)

- `startedAt: Long` — Epoch timestamp when ride started (timezone-safe)
- `endedAt: Long` — Epoch timestamp when ride ended
- `durationMs: Long` — Duration in milliseconds, computed on completion as `endedAt - startedAt`

All timing uses epoch millisecond timestamps to avoid timezone-related bugs.

### Bike

- `totalTimeSeconds: Long` — Denormalized sum of all completed ride durations for this bike. Incremented on each trip completion.

### Component

- `totalTimeSeconds: Long` — Denormalized sum of all completed ride durations on the bike that owns this component. All components on a bike receive the same duration increment when a trip is recorded.

## Aggregation Strategy: Option B (Denormalized)

We store totals on bike and components and increment them when a trip completes.

### Rationale

- **Consistency with existing pattern**: The app already denormalizes `totalDistanceKm` on bikes and `distanceUsedKm` on components. Option B matches this approach.
- **Fast reads**: Bike detail and component cards can display totals without querying the rides table.
- **Simplicity**: No derived queries or complex joins. Single source of truth: RideRepository.saveRideAndUpdateBikeAndComponents.

### Update Flow

On trip completion (ActiveRideActivity.stopRideAndSave or Health Connect import):

1. Persist RideEntity with `durationMs = endedAt - startedAt`.
2. Look up bike by bikeId.
3. Update bike: `totalTimeSeconds += durationMs / 1000`, `totalDistanceKm += distanceKm`.
4. For each component on that bike: `totalTimeSeconds += durationMs / 1000`, `distanceUsedKm += distanceKm`.

### Component Roll-up Rule

When a trip is recorded for a bike, **all** components on that bike receive the trip’s duration added to their `totalTimeSeconds`. There is no per-trip component inclusion/exclusion.

## UI

### Trip Detail (completed ride)

- RideSummaryCard and RideCard show `Duration: HH:MM:SS` using `DurationFormatHelper.formatDurationMs(ride.durationMs)`.

### Active Trip Screen

- Live ticker shows elapsed time using `DurationFormatHelper.formatElapsedFromStart(startTimeMs)`. A `produceState` tick triggers recomposition every second so the display updates in real time.

### Bike Detail

- Bike card shows total time: `DurationFormatHelper.formatDurationSeconds(bike.totalTimeSeconds)`.
- Each component card shows total ride time: `DurationFormatHelper.formatDurationSeconds(component.totalTimeSeconds)`.

## Edge Cases

- **Trip canceled before start** (from Prompt 2): No ride record is created.
- **Trip started but app killed**: RideTrackingService uses START_STICKY; the service continues. User can resume via "View current trip" and complete normally.
- **Timezone changes**: Duration math uses epoch timestamps only; timezone changes do not affect calculations.
- **No double-counting**: Totals are updated only in `saveRideAndUpdateBikeAndComponents`, which runs once per completed trip.

## DB Migration

Migration 2→3 adds:

- `bikes.totalTimeSeconds INTEGER NOT NULL DEFAULT 0`
- `components.totalTimeSeconds INTEGER NOT NULL DEFAULT 0`
