# Build the APK online — no Android Studio

This route uses GitHub's computers to build the installable APK. Nothing needs to be installed on your Windows computer.

## One-time browser steps

1. Sign in to GitHub and create a new **private** repository.
2. Unzip this project on your computer.
3. On the empty repository page, choose **uploading an existing file**.
4. Drag all contents of the `MathWakeAlarm_online_build` folder onto the upload page, including the hidden `.github` folder.
   - On Windows, hidden folders may not appear. The easiest method is to drag the whole selected contents from File Explorer after enabling **View → Show → Hidden items**.
5. Commit the upload to the `main` branch.
6. Open the repository's **Actions** tab.
7. Select **Build Installable APK**.
8. Choose **Run workflow**.
9. When the run is complete, open it and download the artifact named **MathWakeAlarm-installable-APK**.
10. Unzip the downloaded artifact to obtain `MathWakeAlarm-v1.0.0.apk`.

## Install it on your phone

1. Send the APK to your phone or download it there.
2. Open the APK from Downloads or Files.
3. When Android asks, allow that browser or Files app to **Install unknown apps**.
4. Install and open **Math Wake Alarm**.
5. Grant Notifications, Alarms & reminders, and full-screen alarm access when the app prompts you.
6. Use the built-in ten-second test before relying on it overnight.

## Keep this project safe

`personal-release.keystore` is the signing identity for this private app. Keep this project ZIP. Future versions must use the same keystore to install as an update without uninstalling the existing app.

The included key is intended only for this personal app. Do not reuse it for Play Store or commercial applications.
