# GitHub release distribution

Bike Companion is distributed directly through GitHub Releases as a signed Android APK.

The package is `com.clintoncochrane.bikecompanion`. Users download the APK from the repository's
latest GitHub Release and install it through Android's normal sideload flow.

## Signing key

Direct APK updates must always be signed with the same key.

The existing Bike Companion release keystore is therefore the app's long-term signing identity.
Back it up securely outside GitHub. If this key is lost, Android will not accept future APKs as
updates to an installed copy. Because Bike Companion intentionally disables Android backup,
requiring users to uninstall/reinstall could also cause loss of their local Bike Companion data.

Never commit the keystore, passwords, aliases, or other private key material.

The Gradle build currently expects these signing inputs:

| Environment variable | Value |
| --- | --- |
| `BIKE_COMPANION_UPLOAD_STORE_FILE` | Absolute path to the release keystore |
| `BIKE_COMPANION_UPLOAD_STORE_PASSWORD` | Keystore password |
| `BIKE_COMPANION_UPLOAD_KEY_ALIAS` | Key alias |
| `BIKE_COMPANION_UPLOAD_KEY_PASSWORD` | Key password |

The variable names retain the historical `UPLOAD` wording, but for GitHub Releases this key signs
the actual APK delivered to users.

## One-time GitHub setup

In **Repository > Settings > Secrets and variables > Actions**, add these repository secrets:

- `BIKE_COMPANION_RELEASE_KEYSTORE_BASE64`
- `BIKE_COMPANION_UPLOAD_STORE_PASSWORD`
- `BIKE_COMPANION_UPLOAD_KEY_ALIAS`
- `BIKE_COMPANION_UPLOAD_KEY_PASSWORD`

Create the base64 value locally without modifying the keystore:

```bash
base64 -w 0 /absolute/path/to/bike-companion-release.jks
```

Copy the resulting single line into the
`BIKE_COMPANION_RELEASE_KEYSTORE_BASE64` GitHub Actions secret. Keep the original keystore backed
up separately; GitHub Secrets are not a backup strategy.

## Version workflow

The checked-in defaults live in `gradle.properties`:

```properties
BIKE_COMPANION_VERSION_CODE=1
BIKE_COMPANION_VERSION_NAME=1.0.0
```

Before each public release:

1. Increase `BIKE_COMPANION_VERSION_CODE`.
2. Set `BIKE_COMPANION_VERSION_NAME` to the release version.
3. Commit and merge those changes to `main`.
4. Tag that exact `main` commit with `v<versionName>`, for example `v1.0.0`.
5. Push the tag.

The release workflow rejects a tag whose version does not match
`BIKE_COMPANION_VERSION_NAME`.

## Test the pipeline without publishing

The release workflow supports manual runs.

Open **Actions > GitHub Release APK > Run workflow**. A manual run performs the tests, builds the
signed release APK, verifies its signature, creates a SHA-256 checksum, and stores both as a
short-lived GitHub Actions artifact. It does not create a public GitHub Release.

Use this once after configuring the secrets to verify the signing pipeline.

## Publish a release

After the manual build succeeds:

```bash
git switch main
git pull --ff-only
git tag v1.0.0
git push origin v1.0.0
```

A pushed `v*` tag triggers `.github/workflows/release.yml`. The workflow:

1. checks that the tag matches the committed app version;
2. restores the signing keystore from GitHub Secrets;
3. runs unit tests and Android lint;
4. builds `assembleRelease`;
5. verifies the resulting APK signature;
6. generates a SHA-256 checksum; and
7. creates a GitHub Release containing the APK and checksum.

The public APK name is formatted like:

```text
Bike-Companion-v1.0.0.apk
```

GitHub generates the release notes from commits and pull requests associated with the tag.

## Local release build

A signed local release can still be built with the four Gradle signing inputs configured:

```bash
./gradlew clean testDebugUnitTest lintDebug assembleRelease --no-daemon --stacktrace
```

The output is:

```text
app/build/outputs/apk/release/app-release.apk
```

That local APK and the GitHub-generated APK must be signed by the same release key if they are
intended to update one another on a user's device.

## User installation and updates

Users download the APK from GitHub Releases. On first install, Android will require permission to
install unknown apps for the browser or file manager used to open the APK.

For an update, the user downloads the newer APK and installs it over the existing app. Android
preserves the app's local data as long as:

- the package name stays `com.clintoncochrane.bikecompanion`; and
- the APK is signed with the same release key.

There is no automatic updater in Bike Companion v1. Users check the GitHub Releases page for a
new version and install it manually.
