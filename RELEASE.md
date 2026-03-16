# Bike Companion – Release & CI/CD Guide

This document covers release options and setting up CI/CD from GitHub to the Google Play Store.

---

## Prerequisites

1. **Google Play Developer Account** – One-time $25 registration at [play.google.com/console](https://play.google.com/console)
2. **App signing key** – Generate a keystore for release signing (see below)
3. **Google Play service account** – For automated uploads via API

---

## 1. Generate a Release Keystore

```bash
keytool -genkey -v -keystore bike-companion-release.keystore \
  -alias bike-companion -keyalg RSA -keysize 2048 -validity 10000
```

Store the keystore and passwords securely. You will need:
- Keystore file
- Keystore password
- Key alias
- Key password

---

## 2. Configure App Signing in Gradle

Add to `app/build.gradle` (or create `keystore.properties` and add to `.gitignore`):

```groovy
android {
    signingConfigs {
        release {
            storeFile file(System.getenv("KEYSTORE_PATH") ?: "keystore/bike-companion-release.keystore")
            storePassword System.getenv("KEYSTORE_PASSWORD")
            keyAlias System.getenv("KEY_ALIAS")
            keyPassword System.getenv("KEY_PASSWORD")
        }
    }
    buildTypes {
        release {
            signingConfig signingConfigs.release
            minifyEnabled true
            proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
        }
    }
}
```

For CI, use environment variables or GitHub Secrets.

---

## 3. Build a Release AAB (Android App Bundle)

```bash
./gradlew bundleRelease
```

Output: `app/build/outputs/bundle/release/app-release.aab`

The AAB is the preferred format for Play Store (smaller downloads via dynamic delivery).

---

## 4. Manual Release to Play Store

1. Go to [Google Play Console](https://play.google.com/console)
2. Create an app (or select existing)
3. **Setup** → App signing (Google can manage your key, or use your own)
4. **Release** → Production → Create new release
5. Upload the AAB
6. Add release notes
7. Review and roll out

---

## 5. CI/CD with GitHub Actions

### 5.1 Create a Google Play Service Account

1. Go to [Google Cloud Console](https://console.cloud.google.com)
2. Create a project (or use existing)
3. Enable **Google Play Android Developer API**
4. **IAM & Admin** → Service Accounts → Create
5. Grant role: **Service Account User**
6. Create JSON key and download
7. In Play Console: **Setup** → API access → Link the service account
8. Grant permissions: **Release to production** (or internal testing)

### 5.2 GitHub Secrets

Add these in **Settings** → **Secrets and variables** → **Actions**:

| Secret | Description |
|--------|-------------|
| `KEYSTORE_BASE64` | Base64-encoded keystore: `base64 -w 0 bike-companion-release.keystore \| pbcopy` |
| `KEYSTORE_PASSWORD` | Keystore password |
| `KEY_ALIAS` | Key alias (e.g. `bike-companion`) |
| `KEY_PASSWORD` | Key password |
| `PLAY_STORE_CREDENTIALS` | Contents of the service account JSON file |

### 5.3 Workflow File

Create `.github/workflows/release.yml`:

```yaml
name: Build and Release

on:
  push:
    tags:
      - 'v*'

jobs:
  build-and-release:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'

      - name: Decode keystore
        run: |
          echo "${{ secrets.KEYSTORE_BASE64 }}" | base64 -d > keystore.jks

      - name: Run tests
        run: ./gradlew testDebugUnitTest

      - name: Build release AAB
        run: ./gradlew bundleRelease
        env:
          KEYSTORE_PATH: keystore.jks
          KEYSTORE_PASSWORD: ${{ secrets.KEYSTORE_PASSWORD }}
          KEY_ALIAS: ${{ secrets.KEY_ALIAS }}
          KEY_PASSWORD: ${{ secrets.KEY_PASSWORD }}

      - name: Upload to Play Store (Internal Testing)
        uses: r0adkll/upload-google-play@v1
        with:
          serviceAccountJsonPlainText: ${{ secrets.PLAY_STORE_CREDENTIALS }}
          packageName: com.you.bikecompanion
          releaseFiles: app/build/outputs/bundle/release/app-release.aab
          track: internal
          status: completed
```

### 5.4 Triggering a Release

Create and push a tag:

```bash
git tag v2.0.0
git push origin v2.0.0
```

The workflow will build, test, and upload to the **internal testing** track.

---

## 6. Release Tracks

| Track | Use case |
|-------|----------|
| `internal` | Quick testing, up to 100 testers |
| `alpha` | Closed testing |
| `beta` | Open testing |
| `production` | Live release |

Change the `track` in the workflow to promote to production when ready.

---

## 7. Version Management

Current version in `app/build.gradle`:
- `versionCode` – Integer, must increase for each upload
- `versionName` – User-visible (e.g. `"2.0.0"`)

Bump before each release:
```groovy
versionCode = 2
versionName = "2.0.1"
```

---

## 8. Checklist Before First Release

- [ ] Keystore generated and backed up
- [ ] ProGuard rules tested (if minifyEnabled)
- [ ] Privacy policy URL (required for Play Store)
- [ ] App content rating questionnaire completed
- [ ] Store listing: title, short description, full description, screenshots
- [ ] Target audience and data safety form filled

---

## 9. Optional: Lint for Unused Resources

```bash
./gradlew lint
```

Review `app/build/reports/lint-results*.html` for unused strings, drawables, etc.
