# Privacy – Location and personal data

Bike Companion treats location and other personal data as sensitive. This document describes where data is collected, stored, and how long it is retained so the next developer or auditor can answer “where does location go?” clearly.

## Location

- **Collected:** Only during an **active ride** started from the app. The single place that receives location updates is [RideTrackingService](app/src/main/java/com/you/bikecompanion/location/RideTrackingService.kt) (Fused Location Provider).
- **Use:** Location is used only to compute **distance** and **elevation** for the current ride. Coordinates are **not** logged, **not** written to the database as track points in the MVP, and **not** sent to analytics or third parties.
- **Storage:** Only derived values (distance, elevation gain/loss, duration, speeds) are saved when you stop the ride. They are stored in the local Room database (BikeCompanionDatabase) on the device.
- **Retention:** No location coordinates are retained after the ride is saved. Aggregated ride stats (distance, duration, etc.) are kept locally until you clear app data or uninstall.
- **Background:** The app may keep tracking in the background while a ride is in progress (foreground service with notification). Background location is not used when no ride is active.

## Health Connect

- **Collected:** If you grant permission, the app **reads** cycling/exercise sessions from Health Connect (e.g. from other apps or devices).
- **Use:** Used only for the “Import from Health Connect” flow to create rides in Bike Companion and update bike/component mileage. The app does not write to Health Connect in the MVP.
- **Storage:** Imported sessions are converted to rides and stored in the local Room database. No raw Health Connect data is stored beyond what is needed for ride records (distance, duration, start/end time).
- **Retention:** Same as ride data—local until you clear data or uninstall. Health Connect data on the device is governed by Android and your Health Connect settings.

## Crash detection and emergency contacts (future)

- If crash detection and emergency contacts are added later, location may be shared with emergency contacts only when the user has opted in and an incident flow is triggered. That will be documented here and in the app.

## Summary

| Data           | Where collected        | Stored                    | Logged / sent off-device |
|----------------|-------------------------|---------------------------|---------------------------|
| GPS coordinates| RideTrackingService     | Not stored (MVP)          | No                        |
| Ride stats     | Derived from GPS / import | Room DB (local)         | No                        |
| Health Connect | Read on import only     | As ride records (local)   | No                        |

No location in logs, no location in crash reports unless we explicitly design for it (e.g. emergency share).
