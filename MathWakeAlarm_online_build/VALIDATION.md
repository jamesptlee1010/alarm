# Validation performed

The project was checked before packaging as follows:

- Kotlin parser check across all application and unit-test `.kt` files
- XML parse check across the Android manifest and resources
- 10,000 generated maths questions verified against their stored answers
- Dot and comma decimal parsing verified (`12.8` and `12,8`)
- Brisbane next-alarm scheduling checks for same-day and next-selected-day behavior
- WAV alarm asset opened and validated as stereo, 44.1 kHz PCM
- ZIP integrity test

A full Android APK compilation was not run in the packaging environment because it does not contain the Android SDK or permit downloading the SDK archives. The included Windows and Android Studio build helpers perform the real Gradle unit tests and APK compilation on a machine with Android Studio installed.
