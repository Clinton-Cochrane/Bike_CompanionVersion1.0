# Bike Companion – Manual Test Checklist

Use this checklist after builds to verify core flows work.

## Prerequisites
- [ ] App installs and launches without crashing
- [ ] Device/emulator has location permissions granted (or will prompt)
- [ ] For Health Connect import: Health Connect app installed (Android 14+)

---

## 1. Garage (Bikes)
- [ ] **View empty garage** – Open Garage tab, see empty state
- [ ] **Add bike** – Tap FAB, fill form, save; bike appears in list
- [ ] **Edit bike** – Tap bike → edit icon, change details, save
- [ ] **Add component** – On bike detail, tap “Add component”, pick type; component card appears
- [ ] **Component actions** – On component card, tap Replaced / Snooze / Alerts off (no crash)
- [ ] **View rides** – Bike detail shows past rides section

---

## 2. Trip (Start / Run Ride)
- [ ] **Select bike** – On Trip screen, pick a bike from dropdown (if bikes exist)
- [ ] **Start trip** – Tap “Start new trip”; permissions prompt if needed; Active Ride screen opens
- [ ] **Active ride UI** – Distance, duration, pause/resume, stop buttons visible
- [ ] **Pause** – Tap Pause; button label changes to Resume
- [ ] **Resume** – Tap Resume; tracking continues
- [ ] **Stop ride** – Tap Stop; returns to Trip screen; ride appears in past rides
- [ ] **“View current trip” when running** – (Future) With ride in progress, button shows “View current trip” instead of “Start new trip”

---

## 3. Stats
- [ ] **Stats screen** – Opens without crash
- [ ] **With data** – Add bikes/rides, confirm stats reflect totals
- [ ] **Empty state** – With no data, message shows appropriately

---

## 4. AI
- [ ] **AI screen** – Opens without crash
- [ ] **Send message** – Type, tap send; message appears in chat
- [ ] **Response** – Placeholder or real response appears (depends on AI config)

---

## 5. Navigation
- [ ] **Bottom nav** – Trip, Garage, Stats, AI switch correctly
- [ ] **Back from detail** – Back from bike detail, add/edit bike returns correctly
- [ ] **Deep link to ride** – (Future) Notification opens active ride screen

---

## 6. Notifications
- [ ] **Foreground notification** – Start ride; notification appears during tracking
- [ ] **Tap notification** – (Future) Opens active ride screen; currently may not re-enter ride
- [ ] **Pause/Stop from notification** – (Future) Notification actions for pause, stop, reset

---

## 7. Data & Persistence
- [ ] **Survive app kill** – Start ride, force-close app; reopen; ride state handled (or graceful handling)
- [ ] **Bike total distance** – After stopping ride, bike’s total km increases
- [ ] **Health Connect import** – (If supported) Import cycling sessions; rides appear

---

## Quick smoke test (minimum)
1. Launch app
2. Add a bike in Garage
3. Start a trip, let it run a few seconds, stop
4. Confirm ride appears in Trip past rides and bike total updates
