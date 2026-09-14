# Privacy release verification

Run this checklist against the release candidate on the Pixel 5 and Pixel 8 launch targets. Use synthetic location and Health Connect data rather than personal data.

Record the corresponding Play Console answers and submission evidence in
`docs/play-console-declarations.md`.

## Static release checks

- Build the release APK/AAB and inspect the merged release manifest. Confirm there is no `android.permission.INTERNET`, no background-location permission, and no unexpected exported component.
- Confirm `android:allowBackup="false"`, `android:fullBackupContent`, and `android:dataExtractionRules` are present in the merged manifest.
- Inspect the release runtime dependency graph for analytics, advertising, crash reporting, HTTP clients, or other new network-capable SDKs.
- Search production sources for `Log`, `println`, `printStackTrace`, coordinate field names, Health Connect record values, and serialization of location objects. Review every match rather than relying only on the search count.
- Compare every Room entity and DataStore key with `docs/privacy-data-flow-inventory.md` and both privacy-policy copies.

## Health Connect policy entry points

- On Android 13 or lower with Health Connect installed, open Bike Companion from Health Connect's privacy-policy link. Confirm the in-app policy opens through `androidx.health.ACTION_SHOW_PERMISSIONS_RATIONALE`.
- On Android 14 or higher, open the app's permission-usage/privacy link from Health Connect. Confirm the same policy opens through `android.intent.action.VIEW_PERMISSION_USAGE` and the `HEALTH_PERMISSIONS` category.
- Open Settings inside Bike Companion and select **Privacy and data**. Confirm the same policy is readable with large fonts and screen-reader navigation.
- Confirm the Play Console privacy-policy URL displays the same facts and effective date.

## Runtime sensitive-data logging

- Install a release build, clear Logcat, and create synthetic location fixes with distinctive coordinate, altitude, speed, and timestamp values.
- Start, pause, resume, interrupt, recover, and stop a GPS ride. Search Logcat for every synthetic value and confirm none appears.
- Import a synthetic Health Connect cycling session with distinctive distance, timestamps, and record ID. Search Logcat for every value and confirm none appears.
- Trigger checkpoint and Health Connect failure paths where practical. Confirm diagnostics contain only the fixed operation and exception class, never exception messages, stack traces, sessions, or identifiers.

## Backup and transfer

- Use Android backup tooling to request a backup of the release package. Confirm the package is ineligible or produces no application payload.
- Exercise supported device-transfer setup where practical and confirm Bike Companion data is not transferred.
- Uninstall and reinstall after a backup attempt. Confirm bikes, rides, Health Connect identifiers, preferences, and active checkpoints are not restored.

## Notifications

- Start a ride and trigger a maintenance notification.
- On a secure lock screen and during screen sharing, confirm Android conceals private notification details according to the device's notification settings.
- Confirm notification actions still pause, resume, and stop the active ride.

Record the device, Android version, release commit, commands, and results in the release pull request. Any unexpected data path is a release blocker and should receive its own focused issue.
