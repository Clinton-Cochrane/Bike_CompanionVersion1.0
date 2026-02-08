# Bike Companion – Enhancement Ideas

Captured from real-world use. Prioritize as needed.

---

## 1. Return to current ride
**Issue:** No way to get back into the current ride from the notification (or anywhere).

**Idea:** 
- Tap on ride notification opens Active Ride screen
- Add “View current trip” or similar entry point on Trip screen when a ride is in progress
- Ensure foreground service + notification correctly deep-link to `ActiveRideActivity`

---

## 2. Ride timer
**Issue:** Ride screen needs a visible timer.

**Idea:** Show elapsed time prominently (e.g. MM:SS or HH:MM:SS) that updates while ride is active.

---

## 3. Notification + trip time features
**Issue:** Notification needs pause, stop, reset; save total time for trips; add total ride time per bike.

**Ideas:**
- Add notification actions: **Pause**, **Stop**, **Reset**
- Persist total ride time (duration) for each trip in `RideEntity`
- Add `totalRideTimeMs` (or similar) per bike in `BikeEntity` for aggregate stats
- “Reset” could zero the current trip timer but keep the session (or clarify intended behavior)

---

## 4. Max speed + location
**Issue:** Track max speed for the bike with a location marker for reference.

**Ideas:**
- Compute speed from location updates (distance / time)
- Store `maxSpeedKmh` and `maxSpeedLocationLat`, `maxSpeedLocationLng` on ride or bike
- Show max speed in ride summary and/or bike stats
- Optional: show marker on map where max speed was reached

---

## 5. “View current trip” when ride is running
**Issue:** “Start new trip” is confusing when a ride is already in progress.

**Idea:** When a ride is running:
- Change button text to “View current trip” (or similar)
- Tapping it opens Active Ride screen instead of starting a new ride
- Optionally disable starting a second ride while one is active
