# Production release bundles

Bike Companion uses Google Play App Signing. Google holds the production app-signing key;
maintainers keep a separate upload key and use it to sign each Android App Bundle (`.aab`).
The package uploaded to Play is `com.clintoncochrane.bikecompanion`.

## One-time owner setup

1. Create a dedicated upload key in Android Studio (**Build > Generate Signed Bundle/APK**) or
   with `keytool`. Do not use the debug key. Back up the keystore and its passwords in secure,
   access-controlled storage outside this repository.
2. During the first Play Console release, enroll the app in Play App Signing and let Google
   generate and protect the app-signing key. The locally held key becomes the upload key.
3. Store the four inputs below in the local shell/secret manager or CI secret store. Never put
   passwords or private key material in repository `gradle.properties`, workflow YAML, command
   arguments, logs, or artifacts.

Google documents the separation between the upload key and Play-managed app-signing key in
[Sign your app](https://developer.android.com/studio/publish/app-signing).

## Required signing inputs

| Environment variable | Value |
| --- | --- |
| `BIKE_COMPANION_UPLOAD_STORE_FILE` | Absolute path to the upload keystore (`.jks` or `.keystore`) |
| `BIKE_COMPANION_UPLOAD_STORE_PASSWORD` | Upload keystore password |
| `BIKE_COMPANION_UPLOAD_KEY_ALIAS` | Upload key alias |
| `BIKE_COMPANION_UPLOAD_KEY_PASSWORD` | Upload key password |

The same names may instead be placed in the maintainer's user-level
`~/.gradle/gradle.properties`. Environment variables take precedence. Do not place secrets in
the repository's checked-in `gradle.properties`. CI should materialize a base64-encoded keystore
secret into a temporary file, set `BIKE_COMPANION_UPLOAD_STORE_FILE` to that file, mask the
password values, and delete the temporary file after the job.

## Version workflow

The checked-in public defaults are `BIKE_COMPANION_VERSION_CODE=1` and
`BIKE_COMPANION_VERSION_NAME=2.0.0` in `gradle.properties`.

Before every Play upload:

1. Increase `BIKE_COMPANION_VERSION_CODE`; Play requires every uploaded code to be unique and
   higher than the previous release.
2. Set `BIKE_COMPANION_VERSION_NAME` to the intended user-visible release name.
3. Commit both changes with the release work. CI can temporarily override them with environment
   variables of the same names, but the committed values remain the release record.

## Build the candidate

With JDK 17, Android SDK 36, and all signing inputs set, run the required checks followed by a
clean release bundle build:

```bash
./gradlew clean testDebugUnitTest lintDebug --no-daemon --stacktrace
./gradlew bundleRelease --no-daemon --stacktrace
```

`bundleRelease` fails if any signing input is absent or the keystore path is not a file. The
candidate is written to `app/build/outputs/bundle/release/app-release.aab`.

## Verify and test

Download the standalone `bundletool-all` JAR from the official
[bundletool releases](https://github.com/google/bundletool/releases). Verify the upload signature,
validate the bundle structure, and inspect the package/version embedded in its manifest:

```bash
jarsigner -verify -verbose -certs app/build/outputs/bundle/release/app-release.aab
java -jar bundletool-all.jar validate \
  --bundle=app/build/outputs/bundle/release/app-release.aab
java -jar bundletool-all.jar dump manifest \
  --bundle=app/build/outputs/bundle/release/app-release.aab \
  --xpath=/manifest/@package
java -jar bundletool-all.jar dump manifest \
  --bundle=app/build/outputs/bundle/release/app-release.aab \
  --xpath=/manifest/@android:versionCode
java -jar bundletool-all.jar dump manifest \
  --bundle=app/build/outputs/bundle/release/app-release.aab \
  --xpath=/manifest/@android:versionName
```

For a device smoke test, use the official `bundletool` to generate APKs from the exact candidate
and install them on a connected device:

```bash
java -jar bundletool-all.jar build-apks \
  --bundle=app/build/outputs/bundle/release/app-release.aab \
  --output=/tmp/bike-companion-release.apks \
  --overwrite \
  --connected-device
java -jar bundletool-all.jar install-apks \
  --apks=/tmp/bike-companion-release.apks
```

Unless signing flags are supplied, `build-apks` signs its generated test APKs with a debug key;
this does not change or re-sign the candidate AAB. If no device is connected, the validation,
signature, and manifest checks above provide a repeatable artifact inspection path. The Play
Console's internal testing track remains the authoritative pre-production test of
Play-generated, app-signing-key APKs.
