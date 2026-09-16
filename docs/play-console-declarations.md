# Google Play v1 declarations

- Status: **Prepared; complete the release-candidate evidence below before submission**
- Engineering baseline: `56693b1` (`main`, September 16, 2026)
- Privacy audit baseline: issue #64 / PR #99
- Last reviewed: September 16, 2026

This checklist records the answers for package `com.clintoncochrane.bikecompanion`.
Revalidate it against the release AAB, not only the source manifest, immediately before
submission. Google Play declarations apply to every active artifact for the package.

## Remaining submission prerequisites

- Publish the final privacy policy at one stable, active, publicly accessible, non-geofenced,
  non-editable HTML URL. Enter that exact URL in Play Console. The hosted page and the in-app
  policy opened from Settings and Health Connect must state the same facts and effective date.
- Record a public or reviewer-accessible video URL for the foreground-service declaration. The
  video must show a user starting a GPS ride, the persistent ride notification, continued
  tracking while Bike Companion is not visible, and the user stopping the ride.
- Complete the remaining Pixel 5 and Pixel 8 checks in `docs/privacy-release-checklist.md` against
  the release candidate. Record the release commit, device/Android versions, and results.

### Verified non-blockers

- Issue #101 removed the deferred v1 photo picker, thumbnail display, and photo persistence.
- Maintenance notification permission is requested contextually from Settings rather than at
  first launch (#56, closed). It does not add a Data Safety data type; recheck only if notification
  behavior changes before release.

Issue #59 remains open administratively, but its required Health Connect review, explicit bike
assignment, cancel-without-save, and exactly-once save behavior is present on this baseline in
commit `cab8a57` and has focused unit coverage.

## Data Safety form

Google Play defines collection as transmission of user data off the device. Data that is accessed
and processed only on the user's device is not disclosed as collected. The audited v1 artifact has
no `INTERNET` permission, application server, analytics, advertising, crash reporting, or SDK data
transmission. It does not transfer user data to another app. User-triggered intents open Android
settings or the Health Connect provider listing and do not include Bike Companion user data.

### Exact top-level answers

| Play Console question | Answer to enter | Basis |
|---|---|---|
| Does your app collect or share any of the required user data types? | **No** | All app user data is processed and retained only in app-private storage on the device. |
| Is all collected user data encrypted in transit? | **Not shown / not applicable** | This follow-up is shown only after answering Yes to collection or sharing. Do not claim an in-transit encryption badge for a flow that does not exist. |
| Can users request that data is deleted? | **Not shown / not applicable** | The app has no account or developer-held data. Users delete individual local records in-app or all local data through Android clear-storage/uninstall. Do not represent this as a server-side deletion-request mechanism. |
| Data types | **Select none** | The table below documents local access; none meets Play's collection or sharing definition. |
| Independent security review | **Do not select** | No qualifying independent review is recorded. |

If Play Console presents separate account-deletion questions, answer that the app **does not allow
users to create an account**. Do not provide an account-deletion URL.

### Data-type answer matrix

The purpose and retention columns are an audit trail, not extra Data Safety selections. If any
release artifact adds transmission or another-app transfer, change the top-level answer to Yes and
complete the per-type form before release.

| Play data type | Actual v1 access and processing | Collected | Shared | Purpose | Retention/deletion |
|---|---|---:|---:|---|---|
| Approximate location | Requested with precise location as part of Android's location permission flow. The GPS ride feature requires precise fixes and does not use coarse fixes as a separate data product. | No | No | App functionality: user-started GPS ride recording. | Raw fixes remain in service memory only and are discarded when tracking ends. |
| Precise location | Latitude, longitude, altitude, accuracy, speed, and timestamps are read during a user-started ride to derive distance, speed, elevation, and movement state. Raw coordinates are not persisted. | No | No | App functionality: calculate the user's active ride statistics. | Raw fixes are replaced in memory and discarded when tracking ends. Derived ride statistics remain locally until the ride/app data is deleted. |
| Fitness info | On explicit import, reads 30 days of cycling exercise session times/type, duration, Health Connect record ID, and aggregate distance. Imported sessions require review and a bike assignment. | No | No | App functionality: create local cycling ride history, update bike/component mileage, and prevent duplicate imports. | The local imported ride and record ID remain until that ride or app data is deleted. Revoking permission stops future access but does not delete either local imports or source Health Connect records. |
| Other user-generated content | Bike/component names, descriptions, notes, serial number, settings, lifecycle/service history, and ride assignments are entered or generated locally. | No | No | App functionality: bicycle mileage and maintenance tracking. | Until the relevant local record or all app data is deleted. |
| Purchase history / other financial info | Optional component purchase date, price, seller, and purchase link are stored locally. | No | No | App functionality: local component maintenance records. | Until the component/context record or all app data is deleted. Opening a saved purchase link is a specific user-initiated browser action. |
| App interactions / other actions | Local preferences include maintenance threshold, dismissed/snoozed reminder IDs, and whether the Health Connect import disclaimer was shown. | No | No | App functionality: remember local UI and reminder state. | Until replaced by later interaction or all app data is deleted. |

### Security and deletion statements that are supported

- Data is stored in Android app-private Room and Preferences DataStore.
- Android cloud backup and device-to-device transfer are disabled by the manifest and both backup
  rule formats.
- The app adds no separate database encryption. Do not claim that it does.
- No user data is sent to the developer or another company by the app.
- Individual rides, bikes, and components have in-app deletion paths.
- Clearing app storage or uninstalling deletes all Bike Companion local data. It does not delete
  source records in Health Connect.
- There is no account, cloud copy, remote deletion request, or developer-held record to delete.

## Health apps and Health Connect declaration

### Health apps form

| Question | Answer to enter |
|---|---|
| Does the app provide health features or access health data? | **Yes** |
| Health feature | **Health and fitness > Activity and Fitness** only |
| Approved use case, if requested | **Fitness, wellness and coaching** |
| Medical categories | **Select none** |
| Medical device app | **No** |
| Human subjects research | **No** |

Feature explanation, if a general text field is shown:

> Bike Companion is a local bicycle mileage and maintenance tracker. Users can record a GPS ride
> or explicitly import cycling sessions from Health Connect. Imported sessions are reviewed and
> assigned to a bike before they are stored as local ride history and used to update bicycle and
> component mileage. The app is not a medical device and provides no diagnosis or treatment.

### Health Connect data-type access

Request approval for exactly these two read permissions. Exclude all write, route, background-read,
and older-history permissions.

| Health Connect data type | Manifest permission | Access | Rationale to paste |
|---|---|---|---|
| Exercise | `android.permission.health.READ_EXERCISE` | Read only | Bike Companion reads cycling exercise sessions from the previous 30 days only after the user selects Import from Health Connect and grants access. It filters for biking sessions and shows each session's date, time, and duration for review so the user can assign a bike before creating a local ride record. |
| Distance | `android.permission.health.READ_DISTANCE` | Read only | For each reviewed cycling session, Bike Companion reads aggregate distance for the same time range and data origin. The distance creates local ride mileage and updates the selected bike's component maintenance totals. The app does not read in the background, write to Health Connect, or transmit the data off the device. |

Confirm in the form and release manifest:

- Do not request `READ_HEALTH_DATA_IN_BACKGROUND`; imports run only from the visible, user-started
  flow.
- Do not request `READ_HEALTH_DATA_HISTORY`; the query is limited to the previous 30 days.
- Do not request exercise-route access. Raw routes are neither read nor stored.
- Do not request write access to Exercise or Distance. Deleting a Bike Companion import does not
  alter Health Connect.
- Do not request cycling cadence, speed, elevation, heart rate, calories, or any other Health
  Connect data type.
- The runtime permission request occurs only after the user taps Import from Health Connect and
  then chooses the permission action.

### Reviewer instructions

1. Launch Bike Companion; no account or sign-in is required.
2. Create a bike in Garage.
3. Open Trip and select **Import from Health Connect**.
4. If prompted, choose the permission action and grant Exercise and Distance read access.
5. Select a bike for each cycling session in the review dialog and save.
6. Verify the imported activity appears in normal ride history and updates local bike/component
   mileage. Canceling the review or leaving a session unassigned saves nothing.

The Play listing privacy-policy URL must display the same policy that opens through
`androidx.health.ACTION_SHOW_PERMISSIONS_RATIONALE`, Android's `VIEW_PERMISSION_USAGE` health
category, and Settings > Privacy and data.

## Foreground-service declaration

The release targets API 36 and declares one foreground service type: `location`.

| Play Console field | Answer to enter |
|---|---|
| Foreground service type | **Location** |
| App functionality | Bike Companion records a GPS bicycle ride only after the user selects a bike and taps Start ride. A location foreground service keeps calculating distance, speed, elevation, elapsed time, and recovery checkpoints while the app is not visible. A persistent private notification shows ride status and pause, resume, and stop controls. |
| Impact if start is deferred | The beginning of the user-started ride would be missed, producing incomplete distance, duration, speed, and elevation statistics. |
| Impact if interrupted | Movement during the interruption cannot be reconstructed, so the saved ride and bicycle/component maintenance mileage would be incomplete. The local checkpoint can recover statistics captured before interruption but cannot recover missed GPS fixes. |
| Demonstration video URL | **Required before submission — enter the URL described in Submission blockers.** |

## Permission-purpose inventory

| Manifest item | User-facing purpose | Console action |
|---|---|---|
| `ACCESS_COARSE_LOCATION` | Requested with precise location in Android's runtime flow for a user-started GPS ride. | Do not mark location as collected in Data Safety. Recheck the precise-location declaration when it becomes available. |
| `ACCESS_FINE_LOCATION` | Supplies the successive precise fixes needed to calculate cycling distance, speed, and elevation. | Current use case: **Live tracking**. Prepared rationale: coarse location is too imprecise for ride-distance and speed calculations, and the one-time Location Button cannot support continuous tracking for the duration of a user-started ride. Google says this declaration becomes available in November 2026; recheck if submission occurs then or later. |
| `FOREGROUND_SERVICE` | Keeps a user-started ride visibly active when the UI is not visible. | Covered by the foreground-service declaration above. |
| `FOREGROUND_SERVICE_LOCATION` | Allows the ride foreground service to receive location during the active ride. | Declare foreground service type **Location**. |
| `POST_NOTIFICATIONS` | Supports the persistent active-ride status/control notification and local maintenance alerts. | Revalidate after #56; do not claim maintenance opt-in until its contextual flow is implemented. |
| `health.READ_EXERCISE` | Reads user-selected cycling session metadata for review/import. | Declare Exercise read access and Activity and Fitness. |
| `health.READ_DISTANCE` | Reads aggregate cycling distance used for local ride and maintenance mileage. | Declare Distance read access and Activity and Fitness. |
| App-specific `DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION` | AndroidX-generated signature permission that prevents other apps from sending to non-exported dynamic receivers. It does not grant access to user data. | No Play declaration. Confirm the merged-manifest entry remains app-specific and signature-protected. |
| Health Connect provider package query | Detects/opens the Health Connect provider on Android versions where it is a separate app. It is package visibility, not user-data collection. | No Data Safety type. |

Confirm the merged release manifest contains no `INTERNET`, `ACCESS_BACKGROUND_LOCATION`, Health
Connect write, background-read, history, or exercise-route permission.

## Final submission record

Fill this in without changing the prepared answers silently:

- Release commit / AAB version code:
- Merged-manifest review result:
- Runtime dependency review result:
- Pixel 5 privacy checklist result:
- Pixel 8 privacy checklist result:
- Hosted privacy-policy URL:
- Foreground-service demonstration video URL:
- Photo mismatch follow-up resolution: #101 removed the deferred v1 photo flow and legacy photo storage.
- Notification-permission flow review: #56 closed; verified contextual Settings request on
  September 16, 2026.
- Play Console Data Safety submitted by / date:
- Play Console Health apps and Health Connect submitted by / date:
- Foreground-service declaration submitted by / date:
- Precise-location declaration status at submission date:
- Reviewer notes or deviations (with issue/PR link):

## Official references

- Google Play Data Safety form and definitions:
  https://support.google.com/googleplay/android-developer/answer/10787469
- Google Play User Data and privacy-policy requirements:
  https://support.google.com/googleplay/android-developer/answer/10144311
- Google Play Health apps declaration:
  https://support.google.com/googleplay/android-developer/answer/14738291
- Android Health Connect publishing declaration:
  https://developer.android.com/health-and-fitness/health-connect/publish
- Android health-permission guidance:
  https://support.google.com/googleplay/android-developer/answer/12991134
- Google Play foreground-service declaration:
  https://support.google.com/googleplay/android-developer/answer/13392821
- Google Play precise foreground-location declaration timeline:
  https://support.google.com/googleplay/android-developer/answer/17033915
