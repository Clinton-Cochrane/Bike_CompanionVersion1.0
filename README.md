# Bike Companion

Bike Companion is a native Android app for tracking bicycle mileage, rides, and component
maintenance. It is a single-module Kotlin project built with Jetpack Compose, Hilt, and Room.
Bike and ride data stay on the device; the app has no account or cloud sync.

Signed APKs are distributed through [GitHub Releases](https://github.com/Clinton-Cochrane/Bike_CompanionVersion1.0/releases).
For local development, start with the setup below.

## Features

- **Trip:** Record GPS rides, pause and resume tracking, review ride history, and import cycling
  sessions from Health Connect.
- **Garage:** Manage bikes and components, track component usage and service intervals, and receive
  maintenance reminders.
- **Stats:** View distance, ride duration, and ride counts across all bikes or for an individual bike.

## Tech stack

- **UI:** Jetpack Compose, Material 3, Navigation Compose, and ViewModels with `StateFlow`.
- **Data:** Room for bikes, rides, and components; DataStore for app preferences.
- **Dependency injection:** Hilt.
- **Device integrations:** Fused Location Provider with a foreground ride-tracking service, Health
  Connect for cycling-session import, and Android notifications for ride controls and reminders.
- **Tests:** JUnit 4, MockK, coroutine test utilities, AndroidX Test, Espresso, and Compose UI tests.

Build plugins and dependencies are declared in [build.gradle](build.gradle) and
[app/build.gradle](app/build.gradle). Compatibility pins and dependency review decisions are
documented in the [dependency audit](docs/dependency-audit.md).

## Local development

### Requirements

- JDK 17. Set `JAVA_HOME` for command-line builds and select JDK 17 as the Gradle JDK in Android Studio.
- Android SDK Platform 36 and Android SDK Platform-Tools, installed through the SDK Manager.
- Android Studio with support for the project's Android Gradle Plugin if using the IDE.
- An emulator or device running Android 8.0 (API 26) or newer to run the app.

The project compiles against and targets SDK 36. Use the checked-in Gradle wrapper; a separate
Gradle installation is unnecessary.

### Clone and build

```bash
git clone https://github.com/Clinton-Cochrane/Bike_CompanionVersion1.0.git
cd Bike_CompanionVersion1.0
```

Open the repository root in Android Studio and let it configure the SDK location and sync Gradle.
For a command-line-only checkout, create an untracked `local.properties` file in the root with
your SDK's absolute path:

```properties
sdk.dir=/absolute/path/to/Android/Sdk
```

Build the debug APK:

```bash
./gradlew assembleDebug
```

The APK is written to `app/build/outputs/apk/debug/app-debug.apk`. Debug builds use Android's
debug signing key and need no release keystore or API keys.

To install on a running emulator or a device connected with USB debugging enabled:

```bash
./gradlew installDebug
```

On Windows, use `gradlew.bat` in place of `./gradlew`.

### Try the integrations

Add a bike in Garage before recording a ride. GPS tracking needs location permission and a
device or emulator that supplies location updates. Health Connect import needs an available
Health Connect provider, cycling records, and read permission for exercise sessions and distance.
Maintenance notifications need notification permission on Android versions that require it.

Use the [ride-tracking device checklist](docs/ride-tracking-device-checklist.md) to verify tracking,
permissions, and ride recovery on a device.

## Testing and static analysis

Run commands from the repository root:

| Command | Purpose |
| --- | --- |
| `./gradlew testDebugUnitTest` | Run JVM unit tests. |
| `./gradlew lintDebug` | Run Android static analysis. |
| `./gradlew connectedDebugAndroidTest` | Run instrumented, Compose UI, and database migration tests on a connected emulator or device. |

[Android CI](.github/workflows/android-ci.yml) builds the debug APK and runs JVM unit tests for
pull requests targeting `main`. Run lint and relevant device tests locally as well.

## Project structure

Production Kotlin code lives in `app/src/main/java/com/clintoncochrane/bikecompanion/`:

| Directory | Responsibility |
| --- | --- |
| `ui/` | Compose screens, ViewModels, navigation, and theme. |
| `data/` | Room database, entities, DAOs, repositories, and preferences. |
| `di/` | Hilt dependency bindings. |
| `location/` | GPS ride tracking, foreground service, and checkpoint mapping. |
| `healthconnect/` | Health Connect import and permission handling. |
| `notifications/` | Maintenance notifications and notification permission handling. |
| `util/` | Shared formatting and domain helpers. |

Android resources are in `app/src/main/res/`. Local unit tests mirror production packages under
`app/src/test/`; device tests live under `app/src/androidTest/`. Room schema fixtures live in
`app/schemas/`; see the [schema fixture guide](app/schemas/README.md) before changing the database.

## Contributing

Read the [repository guidelines](AGENTS.md) before making changes. Keep pull requests focused,
follow the existing Kotlin and Compose patterns, and put database access behind repositories.
Add regression tests for behavior changes and migration coverage for Room schema changes.

Create a branch such as `feature/<issue>-<summary>` or `bug/<issue>-<summary>` and open a pull request
against `main`. Include the problem being solved, a linked issue when available, verification
commands and results, and screenshots for UI changes. Call out changes to database schemas,
permissions, or privacy behavior.

### Localization

Keep user-visible text in [strings.xml](app/src/main/res/values/strings.xml) and use
`stringResource()` or `pluralStringResource()` in Compose. To add a translation, create a locale
resource directory such as `app/src/main/res/values-es/`, copy the strings and plural resources,
and translate their values while preserving resource names and format placeholders.

## Privacy and configuration

GPS coordinates are used in memory to calculate ride statistics; raw routes are not persisted.
Health Connect access is read-only and initiated by the user. Android backup and device-to-device
transfer are disabled, and the app does not request internet permission.

Review the [privacy policy](PRIVACY.md), [data-flow inventory](docs/privacy-data-flow-inventory.md),
and [privacy release checklist](docs/privacy-release-checklist.md) when changing how data is handled.
Keep `local.properties`, `.env` files, `secrets.properties`, signing keys, and credentials out of Git.

## Releases

The [release guide](docs/releasing.md) covers signed APK builds, versioning, GitHub Actions secrets,
and publishing to GitHub Releases. The [release workflow](.github/workflows/release.yml) publishes
an APK and SHA-256 checksum for `v*` tags; manual runs produce a build artifact without publishing
a release.

## License

This repository currently has no license file.
