# Bike Companion

Android app for tracking bikes, rides, and component maintenance. Built with Kotlin, Jetpack Compose, and Room.

## Features

- **Trip:** Start a ride (in-app GPS), view past rides, import from Health Connect.
- **Garage:** Manage bikes and components with health bars and replacement alerts.
- **Stats:** Per-bike distance, ride count, averages, elevation.

## Tech stack

- Kotlin, Jetpack Compose, Material 3
- Hilt, Room (BikeCompanionDatabase), Navigation Compose
- Fused Location Provider (foreground service for active rides)
- Health Connect (read cycling sessions)

## Project structure

- `app/src/main/java/com/clintoncochrane/bikecompanion/`
  - **ui/** – Compose screens, ViewModels, theme, navigation
  - **data/** – Room entities, DAOs, repositories (bike, ride, component)
  - **location/** – RideTrackingService (single boundary for location during rides)
  - **healthconnect/** – Health Connect import
  - **notifications/** – Component alert notifications
  - **di/** – Hilt modules

## Privacy

See [PRIVACY.md](PRIVACY.md) for the user-facing policy. Maintainers should also review the
[v1 privacy data-flow inventory](docs/privacy-data-flow-inventory.md) and
[privacy release checklist](docs/privacy-release-checklist.md) whenever a data path changes.

## Dependency policy

See the [v1 production dependency audit](docs/dependency-audit.md) for the reviewed build/runtime
inventory, vulnerability decisions, compatibility pins, and commands to repeat before release.

## Internationalization (i18n)

- All user-visible strings are in **`app/src/main/res/values/strings.xml`** (English). The app uses `stringResource(R.string.*)` in Compose and does not hardcode UI text in code.
- To add a new language:
  1. Add a resource folder `res/values-<locale>/` (e.g. `values-es` for Spanish, `values-fr` for French). Use [BCP 47](https://developer.android.com/guide/topics/resources/localization) codes.
  2. Copy `strings.xml` into that folder and translate the values. Keep the same `name` attributes so keys match.
  3. For plurals, copy and translate the `<plurals>` entries; use `pluralStringResource()` in Compose where needed.
- The default `values/` folder is treated as English. Optionally use `values-en/` for an explicit English locale.

## Building

- **Requirements:** Android SDK 36, JDK 17.
- Open in Android Studio or run:
  - `./gradlew assembleDebug` – debug APK
  - `./gradlew installDebug` – install on connected device

Production App Bundle signing, versioning, and verification are documented in the
[release guide](docs/releasing.md).

## License

See repository or project license file.
