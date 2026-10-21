# SK Flashlight

A dark-themed Kotlin Android Studio flashlight starter project with torch on/off, supported-device brightness control, SOS/strobe patterns, timer options, About dialog, AdMob banner/interstitial/App Open ad integration, and a GitHub Actions APK build workflow.

## Included

- Camera flash torch ON/OFF
- Torch strength control where Android 13+ hardware supports it
- SOS and strobe patterns
- 1-minute and 5-minute timer
- Banner ad at the bottom of the screen
- Interstitial ad at the About-screen transition (if an ad is loaded)
- About dialog with app contact details
- GitHub Actions debug APK workflow
- MIT License

## AdMob IDs supplied for this project

- App ID: `ca-app-pub-1692534879845424~2737314632`
- Banner Ad Unit ID: `ca-app-pub-1692534879845424/4022476142`
- Interstitial Ad Unit ID: `ca-app-pub-1692534879845424/4429670256`
- App Open Ad Unit ID: `ca-app-pub-1692534879845424/3134016448`

**Testing warning:** these are the production IDs you supplied. For development, replace the banner and interstitial IDs with Google's official test IDs before running the app. Do not click your own live ads; Google warns that testing against live ads can risk account suspension. Verify consent/privacy requirements and AdMob app status before release.

## Build

Open the project folder in Android Studio with JDK 17 and Android SDK Platform 35 installed. Sync Gradle, then run the app on a physical Android phone with a camera flash. GitHub Actions uses Gradle 8.11.1 and Kotlin Gradle Plugin 2.3.0 (to match the Kotlin metadata in Google Mobile Ads SDK 25.5.0) to build the debug APK and uploads it as an artifact.

## Limitations

- Flash strength control only works on compatible devices; many phones support only ON/OFF.
- SOS/strobe timing and torch availability depend on Android/device behavior.
- This is a new project scaffold, not a modification of an existing Sketchware APK or source project. Existing app features/design cannot be preserved without the original project export.
- This ZIP does not contain a prebuilt APK; the workflow builds one in GitHub Actions. The Kotlin plugin version was updated to 2.3.0 to fix the reported Kotlin metadata mismatch from play-services-ads 25.5.0.
