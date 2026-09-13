# Bike Companion privacy policy

Effective: September 13, 2026

Bike Companion is a local bicycle mileage and maintenance tracker. This policy explains what the app accesses, why it is used, where it is stored, and how you can delete it.

## Location

Bike Companion accesses precise location only during a GPS ride that you start. Tracking may continue while the app is in the background using an Android foreground service and persistent notification.

Coordinates and altitude remain in memory only while the app calculates distance, speed, and elevation. Raw coordinates are not saved as a route, written to the database or active-ride checkpoint, included in notifications, or written to application logs.

When a ride is saved, Bike Companion stores only its timestamps and derived statistics, including distance, duration, average and maximum speed, and elevation gain and loss.

## Health Connect

When you choose **Import from Health Connect** and grant permission, Bike Companion reads cycling exercise sessions and distance from the previous 30 days. The app requests only read access to exercise sessions and distance; it does not write data to Health Connect or read data in the background.

During import, the app uses each cycling session's start and end time, duration, distance, and Health Connect record identifier. After you review and assign a bike, those values are stored as a local ride. The record identifier is retained to prevent the same session from being imported more than once.

Revoking Health Connect permission prevents future access but does not delete rides already imported into Bike Companion. Deleting an imported ride in Bike Companion does not delete the source record from Health Connect.

## Information stored on this device

Bike Companion stores the following information in Android app-private storage:

- Bike details, descriptions, notes, odometer baselines, and accumulated ride statistics.
- Component details, serial numbers, purchase information, notes, service settings and history, lifecycle history, and accumulated statistics.
- Ride timestamps, source, bike assignment, and derived ride statistics.
- Health Connect record identifiers for duplicate prevention.
- App settings and dismissed or snoozed reminders.
- A temporary active-ride checkpoint containing timestamps and derived statistics so an interrupted ride can be recovered. It never contains raw coordinates.

## Network access and sharing

Bike Companion v1 has no account, cloud sync, advertising, analytics, crash-reporting service, or application-operated server. The application does not request Android's internet permission and does not transmit your ride, location, bike, component, or Health Connect data to the developer or other companies.

Health Connect and the Android location provider are device services governed by their own settings and policies. Bike Companion communicates with them through their Android APIs to perform the user-requested import and ride-tracking features.

## Android backup

Android cloud backup and device-to-device transfer are disabled for Bike Companion v1. The manifest disables backup, and explicit backup rules exclude all application storage domains on both older and current Android versions.

## Notifications

During an active ride, a persistent notification can show ride status and distance and provide pause, resume, and stop controls. Optional maintenance notifications can show component names. Both notification types are marked private so Android can hide their detailed content on a secure lock screen or during screen sharing. You can further control notification visibility in Android settings.

## Retention and deletion

Saved records remain on the device until you delete them in the app, clear Bike Companion storage in Android settings, or uninstall the app. Active-ride checkpoints are cleared after a ride is completed or discarded.

Clearing application storage or uninstalling Bike Companion removes its local data, but does not remove source records held by Health Connect. Because Android backup is disabled, Bike Companion data is not intentionally restored after reinstallation or copied to a replacement device.

## Security

Data is kept in Android app-private storage and protected by the Android application sandbox and device security. Bike Companion does not add separate database encryption. Avoid sharing an unlocked device with people who should not see your ride or maintenance information.

## Your choices

You can decline or revoke location and Health Connect permissions in Android settings. GPS recording and Health Connect import will not work without their respective permissions, but bike and maintenance tracking remain available.

## Contact

For privacy questions, contact the Bike Companion developer through the [project issue tracker](https://github.com/Clinton-Cochrane/Bike_CompanionVersion1.0/issues).

Material changes to these practices will be reflected in this policy and in the policy shown inside the app.
