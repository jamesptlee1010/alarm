# Math Wake Alarm

A personal Android alarm that keeps ringing until you solve the configured number of maths questions.

## Version 1 features

- One recurring alarm with selectable weekdays
- Full-screen alarm activity over the lock screen
- Built-in looping alarm sound
- Starts quietly and rises by 10 percentage points every 10 seconds
- Addition, subtraction, multiplication and exact division questions
- Decimal answers accepted with a dot or comma
- Unlimited question skipping, with no credit for a skip
- Requires 3–10 correct answers (default: 5)
- Optional vibration
- Re-schedules after reboot, timezone change, clock change and app update
- Test alarm scheduled 10 seconds ahead
- Clear permission checks for exact alarms, notifications and full-screen alarms

## Open and run

1. Install the current stable Android Studio.
2. Run `PREPARE_PROJECT.bat` once. It downloads Gradle 8.13 and creates the standard Gradle wrapper.
3. Open the `MathWakeAlarm` folder in Android Studio and allow Gradle sync to complete.
4. Connect your Android phone with USB debugging enabled.
5. Press **Run** and select your phone.
6. In the app, grant all three alarm permissions.
7. Press **TEST IN 10 SECONDS**, lock the phone and confirm the full-screen challenge appears.

Alternatively, `BUILD_DEBUG_APK.bat` builds and tests the debug APK directly. Afterward, `INSTALL_ON_CONNECTED_PHONE.bat` installs it through ADB when your phone is connected.

The project targets Android API 36 and supports Android 8.0 (API 26) and newer.

## Building an APK

In Android Studio:

- Debug APK: **Build → Build APK(s)**
- Signed release APK: **Build → Generate Signed App Bundle or APK → APK**

A debug APK can be installed directly on your own phone. Keep your release keystore safe if you generate a signed release, because Android requires the same signing key for later updates.

## Reliability checklist

Before relying on the app as your only morning alarm:

1. Confirm the app reports **Exact alarms: Granted**. Android 13 and newer grant this automatically to this alarm-clock app; Android 12 may show an Alarms & reminders setting.
2. Confirm notifications and full-screen alarm access are granted.
3. Set the system alarm volume above zero.
4. Exclude the app from manufacturer-specific battery optimisation if your phone aggressively stops background apps.
5. Test while the phone is locked, charging and unplugged.
6. Test after restarting the phone.
7. Keep a backup alarm until you have tested several mornings on your exact handset.

## Android limitation

No normal Android app can prevent its owner from force-stopping or uninstalling it. The app has no in-app dismiss or snooze action, but Android's system-level controls remain available.

## No Android Studio option

See `BUILD_ONLINE_NO_ANDROID_STUDIO.md`. GitHub Actions can build the signed APK entirely online.
