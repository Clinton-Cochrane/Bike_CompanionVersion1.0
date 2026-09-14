# Production dependency audit

Audit date: September 13, 2026  
Baseline: `main` at `9d087c3` plus issue #66 changes

This is the v1 release-risk inventory. It is intentionally not a request to keep every
library at its newest version. Re-run it before a release or whenever the build toolchain,
target SDK, Health Connect, Room, Hilt, or location stack changes.

## Build toolchain

| Component | Version | Stability | Decision |
| --- | --- | --- | --- |
| JDK | 17 | LTS | Required by the repository and supported by AGP. |
| Gradle wrapper | 8.13 | Stable | Retained with AGP 8.13.2. |
| Android Gradle Plugin | 8.13.2 | Stable | Retained; supports compile/target SDK 36 and Kotlin 2.0. |
| Kotlin Android plugin | 2.0.20 | Stable | Retained with Hilt 2.55 and Room 2.7.0 processors. A trial of Kotlin 2.3.10 failed because those processors reject Kotlin 2.3 metadata. |
| Kotlin Compose plugin | 2.0.20 | Stable | Kept identical to the Kotlin Android plugin. |
| Hilt Gradle plugin | 2.55 | Stable | Retained with the matching runtime/compiler. |

The project deliberately resolves Kotlin artifacts to `2.0.20` and Okio to `3.9.0`.
Removing those rules requires a coordinated Kotlin, Hilt, Room, Compose, and dependent-library
upgrade. That is broader than this release-risk audit and is not required to address a known
runtime vulnerability.

## Production dependencies

Versions under **Resolved** are from `releaseRuntimeClasspath` after this audit.

| Direct dependency | Declared | Resolved | Stability | Decision |
| --- | --- | --- | --- | --- |
| Compose BOM | 2024.02.00 | UI 1.9.4; Material 3 1.2.0; icons 1.6.1 | Stable | Retained. The resolved UI version is raised transitively; a coordinated Compose update is deferred because there is no release blocker and it can change UI behavior. |
| AndroidX Core KTX | 1.12.0 | 1.16.0 | Stable | Retained; conflict resolution already supplies the newer stable runtime. |
| Lifecycle runtime/process/ViewModel Compose | 2.7.0 | 2.9.4 | Stable | Retained declarations; the production graph resolves one stable 2.9.4 family. |
| Activity Compose | 1.8.2 | 1.8.2 | Stable | Retained; no target SDK 36 blocker found. |
| Navigation Compose | 2.7.7 | 2.7.7 | Stable | Retained; no security or release blocker found. |
| Hilt runtime/compiler | 2.55 | 2.55 | Stable | Retained; changing it would be a toolchain migration. |
| AndroidX Hilt Navigation Compose | 1.1.0 | 1.1.0 | Stable | Retained; no release blocker found. |
| Room runtime/KTX/compiler | 2.7.0 | 2.7.0 | Stable | Retained to avoid an unnecessary persistence-toolchain change before release. |
| DataStore Preferences | 1.0.0 | 1.0.0 | Stable | Retained; no advisory or compatibility blocker found. |
| Play Services Location | 21.1.0 | 21.1.0 | Stable | Retained; current foreground location behavior is covered by the ride-service tests and release checklist. |
| Health Connect client | 1.1.0 | 1.1.0 | Stable | Changed from 1.2.0-alpha02. The app only uses stable exercise-session, distance, permission, read, and aggregate APIs. |
| Coil Compose | 3.4.0 | 3.4.0 | Stable | Retained; no OSV advisory was returned. |
| Guava constraint | 32.0.1-android | 32.0.1-android | Stable | Added because Health Connect 1.1.0 otherwise requests vulnerable 31.1-android. |

The following unused dependencies were removed instead of upgraded:

- AppCompat, CardView, ConstraintLayout, RecyclerView, and the View-system Material library.
- WorkManager. No production source referenced WorkManager APIs. Its removal also removes its
  merged-manifest network-state, wake-lock, boot receiver, service, and diagnostics surface.
- The unused AppCompat XML theme definitions that required the removed View libraries.

## Debug and test dependencies

| Dependency | Declared version | Stability / decision |
| --- | --- | --- |
| Compose UI tooling and test manifest | BOM-managed | Debug-only; retained. |
| JUnit | 4.13.2 | Stable; retained. |
| Kotlin coroutines core/test | 1.8.0 | Stable declarations; retained for the existing coroutine tests. |
| MockK | 1.13.10 | Stable; retained. |
| AndroidX Test JUnit | 1.1.5 | Stable; retained. |
| Espresso Core | 3.5.1 | Stable; retained. |
| Room Testing | 2.7.0 | Matches production Room. |
| Compose UI test JUnit4 | BOM-managed | Matches the resolved Compose test stack. |

Test-only version age is not a production APK risk. Upgrade these only for a concrete test or
toolchain need.

## Security findings and mitigations

### Guava runtime advisories — remediated

The original release graph resolved `com.google.guava:guava:31.1-android` through Health Connect.
OSV returned:

- `GHSA-7g45-4rm6-3mm3` — medium severity, insecure temporary-directory use.
- `GHSA-5mg8-w23w-74h3` — low severity, temporary-directory information disclosure.

The explicit `32.0.1-android` constraint is the first recommended patched Android release and
Gradle dependency insight confirms that it wins over the transitive 31.x requests.

### Kotlin Gradle Plugin advisory — mitigated, not runtime code

OSV reports `GHSA-r937-wjx7-w2jp` against Kotlin Gradle Plugin 2.0.20: unsafe deserialization of
build-cache metadata. The first patched version is `2.4.20-Beta1`, which is prerelease and cannot
be adopted independently with the current Hilt/Room annotation processors.

This repository has no remote build-cache configuration and does not enable Gradle's build cache.
`org.gradle.caching=false` now makes that mitigation explicit for local and CI builds. Reconsider
the Kotlin/Hilt/Room toolchain together once a compatible stable patched Kotlin release is
available. This advisory does not ship in the APK.

### Scan record

- GitHub Dependabot open alerts: none on the audit date.
- OSV batch query: 35 direct build, production, test, and critical resolved artifacts.
- Post-change OSV runtime findings: none among the queried artifacts.
- Repositories are restricted to Google Maven, Maven Central, and the Gradle Plugin Portal.
- Direct versions are fixed; no dynamic `+` version is used.

The repository does not yet commit Gradle dependency-verification metadata or dependency locks.
That is useful future supply-chain hardening, but is not a v1 release blocker given fixed versions,
restricted repositories, clean CI resolution, and the scan results above.

## Verification commands

```text
./gradlew testDebugUnitTest
./gradlew lintDebug
./gradlew assembleDebug
./gradlew processReleaseMainManifest
./gradlew :app:dependencyInsight --configuration releaseRuntimeClasspath --dependency com.google.guava:guava
```

Authoritative references:

- [Android Kotlin/AGP compatibility](https://developer.android.com/build/kotlin-support)
- [Health Connect releases](https://developer.android.com/jetpack/androidx/releases/health-connect)
- [WorkManager releases](https://developer.android.com/jetpack/androidx/releases/work)
- [Guava medium advisory](https://github.com/advisories/GHSA-7g45-4rm6-3mm3)
- [Guava low advisory](https://github.com/advisories/GHSA-5mg8-w23w-74h3)
- [Kotlin build-cache advisory](https://github.com/advisories/GHSA-r937-wjx7-w2jp)
