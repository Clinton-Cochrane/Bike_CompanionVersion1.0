# Bike Companion v1 Google Play listing

Status: ready for Play Console entry and final-release asset capture. This document covers issue
#69 only; Data Safety, Health Connect declarations, and foreground-service declarations remain in
`docs/play-console-declarations.md` (#65).

## Copy/paste store copy

### App name

Bike Companion

### Short description

Track bike mileage, rides, components, and maintenance locally.

### Full description

Bike Companion is a local-first bicycle mileage and maintenance tracker.

Keep bikes, rides, and components together in one place:

- Add bikes and the components you want to track.
- Record a ride with GPS, or add completed mileage manually.
- Import cycling sessions from Health Connect when you choose to connect it.
- Review ride history and per-bike mileage, time, ride, and service totals.
- Track component mileage, service intervals, inspections, replacements, and lifecycle history.
- Move components between bikes or keep retired components in their history.
- Turn on optional maintenance reminders for components that may need attention.

Bike Companion stores its records on your device. It has no account or cloud sync. GPS location is
used only during a ride you start. Health Connect access is optional and used only when you choose
to import cycling sessions.

Maintenance intervals and reminders are advisory. Inspect actual component condition and follow
manufacturer guidance.

### v1 release notes

Bike Companion v1 is here.

- Track bikes, components, mileage, and service history.
- Add manual rides, record GPS rides, or import cycling sessions from Health Connect.
- Review ride history and per-bike statistics.
- Get optional advisory maintenance reminders.

## Store settings and owner-entered metadata

| Play Console field | Recommended value or owner action |
| --- | --- |
| App or game | App |
| Category | Health & Fitness |
| Tags | Select **Cycling** and **Fitness tracking** only if those exact relevant tags are offered in the current Console. Do not add unrelated tags just to fill the field. |
| Default language | English (United States) |
| App name, short description, full description, release notes | Use the exact copy above. |
| Support email | **Owner action:** enter a monitored support email address. This is required. |
| Support website | **Owner action:** enter a public support URL if available; a GitHub issue tracker may be used only if Clinton intends to support users there. |
| Support phone | Optional; leave blank unless Clinton wants it published. |
| Privacy policy URL | **Owner action:** publish the current `PRIVACY.md` at a stable, public HTTPS URL and enter that exact URL. The in-app policy and hosted policy must remain identical in substance. |
| App access | No login or special access is required. Do not provide credentials. |
| Pricing | Set the actual intended price before the first rollout. The current app has no in-app billing, ads, or account feature. |
| Countries/regions | **Owner action:** choose the intended release countries/regions and confirm any local legal/tax obligations. Do not imply worldwide availability until selected. |
| Target audience and content rating | **Owner action:** complete truthfully in Play Console for the final release; do not select a children audience unless that is the intended product audience. |
| Data Safety, Health Connect, foreground-service declarations | Out of scope for #69; complete #65 using `docs/play-console-declarations.md`. |

## Asset checklist

| Asset | Current Play requirement | Repo asset status | Owner action |
| --- | --- | --- | --- |
| Store app icon | Required. 512 x 512 px, 32-bit PNG with alpha, 1 MB or smaller. | The launcher artwork exists as `app/src/main/res/mipmap-*/ic_launcher*.png`, but the largest checked-in file is 192 x 192 px, so it cannot be uploaded directly. | Export or recreate the same final icon artwork at 512 x 512; do not simply upload a scaled launcher PNG. |
| Feature graphic | Required. 1024 x 500 px, JPEG or 24-bit PNG, no alpha. | No compliant feature graphic is in the repo. | Create and supply one based on final branding and the actual app. Keep it simple; do not use rankings, price/promotion claims, or unsupported features. |
| Phone screenshots | Required: at least 2 total. JPEG or 24-bit PNG, no alpha; each dimension 320–3840 px and the long side no more than twice the short side. Up to 8 may be supplied for phones. Four 1080 px-or-larger 9:16 portraits are recommended for promotional eligibility. | None in repo. | Capture the six phone screens below from the final release build. Use 1080 x 1920 px or the device's native 9:16 portrait capture where possible. |
| Screenshot alt text | Add concise alt text in Play Console for every uploaded screenshot. | Not applicable. | Use the suggested descriptions in the capture plan, editing them if the final screen differs. |
| Promotional video | Optional; no video is required for the listing. | None. | Omit unless Clinton wants one. A separate reviewer-accessible GPS foreground-service demonstration may still be required by #65. |
| Tablet, Chromebook, TV, Wear OS, Automotive, XR graphics | Required only when distributing to those form factors. | The current v1 scope is phone Android; no corresponding app assets are present. | Do not add device-specific artwork unless the release is intentionally distributed to that form factor and its Console requirements are met. |

Official asset requirements: <https://support.google.com/googleplay/android-developer/answer/9866151>.

## Final-release screenshot capture plan

Capture from the signed final release build on a clean, realistic demo profile. Use fictional bike
and component names, and remove notification shade, personal account information, and private
Health Connect history before each capture. Do not use device frames, marketing overlays, test
data labels, or a Health Connect permission dialog as listing artwork.

1. **Garage overview:** one named bike with several tracked components and meaningful mileage.
   Alt text: `Garage overview showing a bike and its tracked components.`
2. **Bike detail / component tracking:** show component mileage and an advisory service interval.
   Alt text: `Bike details with component mileage and maintenance intervals.`
3. **Rides history:** show several completed rides assigned to the demo bike, including an
   imported or manual ride only if its source is visibly represented by the final UI.
   Alt text: `Ride history with completed cycling rides and distances.`
4. **Completed GPS ride detail:** save a short real or carefully controlled demo ride, then show
   its saved distance, duration, speed, and elevation statistics.
   Alt text: `Completed GPS ride with distance, duration, speed, and elevation statistics.`
5. **Service due workflow:** show the service list or a component detail where an advisory
   inspection/replacement interval is due; never imply that it diagnoses a fault or guarantees
   safety.
   Alt text: `Advisory maintenance list for bicycle components due for attention.`
6. **Stats:** show the per-bike statistics screen with distance, ride time, ride count, and
   service total.
   Alt text: `Per-bike statistics showing distance, ride time, rides, and services.`

Health Connect import is an optional seventh screenshot only after it has been tested on the final
release candidate. If used, capture the in-app review-and-assign state with fictional cycling data,
not the Health Connect provider UI. Do not show the active GPS recording screen unless its values
are stable and no location, notification, or personal-data disclosure is visible.

## Claim check

The copy above advertises only verified v1 behavior: local bike/component records; manual and GPS
ride recording; optional, user-started Health Connect cycling import; ride history and statistics;
maintenance/service tracking; component lifecycle history; and optional advisory reminders.

It deliberately does not advertise accounts, cloud sync, backup/export, crash detection,
emergency contacts, AI/Gemini, photos, unit switching, failure prediction/analytics, navigation,
medical use, or any safety guarantee. The current source removes the deferred v1 photo flow; no
photo claim or screenshot is included.

## #69 blockers

There is no application-code or listing-copy blocker created by #69. Before Play submission,
Clinton still must provide the support email, a stable public privacy-policy URL, a compliant
512 px store icon, a 1024 x 500 feature graphic, and final-release screenshots. The separate #65
declarations and the remaining release gates are not part of this issue and must be completed by
their owners.
