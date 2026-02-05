# Real-world usage review checklist

Use this list before release or when testing the app in production-like conditions.

## Location & ride tracking

- [ ] **Location permission** – Test “deny” / “don’t ask again”; app should explain why permission is needed and degrade gracefully (e.g. can’t start trip).
- [ ] **Background tracking** – Start a ride, send app to background or lock device; confirm tracking continues and notification shows pause/stop.
- [ ] **Battery** – Run a long ride (or simulate); check battery impact and that the foreground service isn’t over-sampling.
- [ ] **GPS accuracy** – Test in poor signal (indoor start, tunnel); confirm “Waiting for GPS” and that distance doesn’t jump unrealistically.
- [ ] **Process death** – Force-stop app or kill process during a ride; confirm behavior (e.g. re-open app, notification still works or clear messaging).

## Health Connect

- [ ] **Not installed** – On a device/emulator without Health Connect; confirm install prompt or clear error (no crash).
- [ ] **Permission denied** – Deny Health Connect permission; import should fail with a clear message (e.g. trip_import_error).
- [ ] **Empty data** – Grant permission but no cycling sessions; confirm “No cycling sessions found” (or equivalent) and no crash.
- [ ] **Import attribution** – Import rides and confirm selected bike’s total distance and component distances update correctly.

## Notifications

- [ ] **Notification channel** – Ride tracking notification appears and is grouped/clear; user can change channel settings.
- [ ] **Component alerts** – Trigger a component past threshold (e.g. chain near lifespan); confirm notification and in-app state (e.g. garage badge).
- [ ] **Post-notification permission (Android 13+)** – On fresh install, confirm notification permission is requested when needed (e.g. before first ride or first alert).

## Data & storage

- [ ] **No bikes** – Fresh install; all flows that assume “at least one bike” (e.g. start trip, import) show “Add a bike first” or equivalent.
- [ ] **No rides** – Bike added but no rides; Trip list empty, Stats show “No ride data”, no crashes.
- [ ] **Large data** – Many bikes/rides/components; list scrolling and stats remain responsive.
- [ ] **Backup** – If using Auto Backup, confirm excluded files (e.g. DB) if you don’t want DB in backup; or confirm DB is backed up if desired.

## Accessibility (A11y)

- [ ] **TalkBack** – Navigate Trip, Garage, Stats, AI, active ride with TalkBack; every interactive element has a sensible label and order.
- [ ] **Touch targets** – Buttons and list items meet minimum size (e.g. 48dp); test on small screen.
- [ ] **Font scaling** – Increase system font size; text scales and layout doesn’t break.
- [ ] **Contrast** – Dark (and light if used) theme has sufficient contrast for text and controls.

## AI feature

- [ ] **API key** – With no API key, sending a message shows the “configure API key” placeholder (no crash, no leak).
- [ ] **Network failure** – With API configured, simulate no network; confirm error handling (e.g. Snackbar) and no crash.
- [ ] **Trip readiness** – With components and bikes present, ask a trip-readiness question; confirm component health is reflected in behavior when API is wired.

## Privacy & compliance

- [ ] **PRIVACY.md** – Matches actual behavior (location use, Health Connect read, retention).
- [ ] **No PII in logs** – Grep/code review: no coordinates or user identifiers in Log.* in release code paths.
- [ ] **Store listing** – Privacy policy URL and data disclosure (location, Health Connect) consistent with PRIVACY.md.

## Store & release

- [ ] **Signing** – Release build signed with upload key; versionCode/versionName set for release.
- [ ] **ProGuard/R8** – If minifyEnabled true, test release build and Room/Hilt/Reflection paths.
- [ ] **Screenshots** – Dark (and light if supported) for store listing.
- [ ] **Target SDK** – Meets current Play target API level; all permission and foreground service declarations correct for that level.

## Devices & OS

- [ ] **Min SDK (26)** – Test on API 26 device/emulator; no use of APIs above 26 without checks.
- [ ] **Target SDK (34)** – Test on API 34; notification permission, edge-to-edge, and any new requirements.
- [ ] **Different manufacturers** – If possible, test on Samsung, Pixel, etc. for background and notification differences.

## Edge cases

- [ ] **Ride save failure** – Simulate DB or disk full on “Stop ride”; user sees error and isn’t left in a broken state.
- [ ] **Duplicate / back stack** – Navigate Add Bike → Back, Bike Detail → Back; no duplicate entries or wrong screen.
- [ ] **Rotation** – Rotate during Trip, Garage, active ride; state and UI recover correctly (ViewModel / saved state).

---

*Add or remove items as your app and policies evolve.*
