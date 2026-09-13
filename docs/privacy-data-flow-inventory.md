# V1 privacy data-flow inventory

Audit baseline: `ddc6739` plus the focused changes for issue #64. Last reviewed September 13, 2026.

This is the engineering inventory behind `PRIVACY.md` and the in-app privacy policy. Keep all three aligned when a source, field, sink, retention rule, SDK, permission, or backup behavior changes.

## Data flows

| Source | Values accessed | Processing | Persistent sink | Off-device path | Retention |
|---|---|---|---|---|---|
| Fused Location Provider during a user-started ride | Latitude, longitude, altitude, accuracy, speed, timestamp | Rejects stale or inaccurate fixes; calculates distance, speed, elevation, and movement state in memory | Derived statistics and timestamps only; Room after save and Preferences DataStore while the ride is active | None initiated by Bike Companion | Raw fixes are replaced in memory and discarded when tracking ends |
| Health Connect import initiated by the user | Cycling exercise start/end time, metadata record ID, and aggregate distance for the same origin and time range | Filters biking sessions from the previous 30 days, presents them for review, and requires a bike assignment | Room ride row containing timestamps, duration, distance, source, bike ID, and record ID | Health Connect IPC on the device; no app-operated server | Local copy remains until the ride or application data is deleted |
| Bike and component forms | Names, make/model, year, descriptions, notes, serial number, purchase details, odometer/prior-use values, service settings | Validates and calculates maintenance and accumulated statistics | Room entities | None | Until the record or application data is deleted |
| App interactions | Maintenance threshold, Health Connect disclaimer state, dismissed ride/reminder IDs, reminder snooze time | Controls local presentation and reminders | `app_preferences` Preferences DataStore | None | Until application data is deleted; some flags can be replaced by later interaction |
| Active ride service | Bike assignment, start/checkpoint time, pause state, distance, speeds, elevation, update count | Captures the minimum state needed for interrupted-ride recovery | `active_ride_checkpoint` Preferences DataStore | None | Cleared when the ride is stopped or discarded |
| Ride and maintenance state | Ride status and distance; component names needing attention | Builds Android notifications | Android notification service only | May appear on system UI according to user settings; notification visibility is private | Until the notification is dismissed or the active service stops |

## Persistence boundaries

### Room database

`BikeCompanionDatabase` stores six entity groups:

- `bikes`: identity, description, notes, odometer baseline, derived totals, dates, and bike configuration.
- `components`: identity, assignment and lifecycle status, make/model, notes, service thresholds, prior-use state, and derived totals.
- `component_context`: notes, serial number, purchase details, and service notes.
- `component_swaps`: component/bike assignments and install/remove timestamps.
- `service_intervals`: service names, interval policy, and tracked use.
- `rides`: bike assignment, timestamps, source, derived statistics, and Health Connect record identifier.

The database contains no latitude or longitude columns.

### Preferences DataStore

- `app_preferences` contains user settings and UI reminder/disclaimer state.
- `active_ride_checkpoint` contains derived ride recovery state. It deliberately has no coordinate keys.

### Memory-only location state

`RideTrackingService` is the application boundary for raw location fixes. The current and previous coordinate pair and altitude are held only in service memory to calculate deltas. They are reset when a ride starts and lost when the service ends.

## Logs and diagnostics

- Location permission warnings contain fixed text only.
- Active-checkpoint failures contain a fixed operation description and exception class name only. Exception messages and stack traces are not logged.
- Health Connect failures contain a fixed description and exception class name only. Session values and record identifiers are not logged.
- No analytics or crash-reporting SDK is configured.

## Network and SDK review

- The application manifest does not request `android.permission.INTERNET`.
- No HTTP client or application server integration is present.
- Google Play Services Location supplies location fixes through Android APIs.
- AndroidX Health Connect supplies user-authorized health records through on-device IPC.
- External Play Store intents used to install or update Health Connect contain only the provider package identifier.

Recheck both the merged release manifest and release runtime dependency graph before submission because libraries can contribute manifest entries or new behavior.

## Backup decision

V1 intentionally provides no Android cloud backup or device-to-device transfer. `android:allowBackup` is false, and both supported backup-rule formats exclude root, file, database, shared-preference, external, and device-protected storage domains.

This decision favors the documented device-only privacy model. The tradeoff is that clearing app storage, uninstalling, losing the device, or moving to another device permanently loses Bike Companion data.

## Deletion behavior

- Individual ride, bike, and component deletion follows the relevant application flow and Room relationships.
- Clearing application storage or uninstalling removes Room and DataStore content.
- Revoking Health Connect permissions stops future reads but does not remove already-imported rides.
- Deleting Bike Companion data does not change records held by Health Connect or another source application.

## Change-review rule

Any change that adds a permission, SDK, network endpoint, database field, DataStore key, file type, diagnostic system, backup path, or sensitive notification content must update this inventory and the two user-facing policy copies in the same pull request.
