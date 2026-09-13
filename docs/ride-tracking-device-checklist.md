# Ride tracking foreground-service device checklist

Run this checklist on the Pixel 5 and Pixel 8 release targets with location permission granted
while the app is in use. It covers foreground-service behavior that cannot be reliably exercised
by JVM tests.

- Start a GPS ride from the visible trip-start flow. Confirm the persistent ride notification
  appears immediately and Logcat contains no foreground-service start or location permission
  exception.
- Background the app, wait at least one location update interval, then reopen it from the
  launcher and from the notification. Confirm one active ride is shown and distance continues
  increasing only once per physical movement.
- Tap Pause and Resume repeatedly from both the activity and notification. Confirm exactly one
  notification action is shown, no distance accumulates while paused, and resumed tracking does
  not double-count distance.
- Stop from the activity and from the notification. Confirm location access ends, the notification
  disappears, and the service is absent from `adb shell dumpsys activity services`.
- While an active ride is running, remove the app process with `adb shell am kill
  com.clintoncochrane.bikecompanion`. Confirm Android does not restart a blank tracking service
  or fabricate a new ride. Unfinished-ride restoration is intentionally verified by the #44/#45
  checkpoint-recovery work.
- Force-stop the app with `adb shell am force-stop com.clintoncochrane.bikecompanion`. Confirm no
  service or notification remains. A force-stop clears app execution state; it must not be treated
  as recoverable ride interruption.
