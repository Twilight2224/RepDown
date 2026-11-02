# Android APK build and installation

The native Android project requires JDK 17+, Android SDK Platform 36, and the Android Gradle Plugin 8.13.2, Gradle 8.13 (from `gradle/wrapper/gradle-wrapper.properties`), and Android SDK Platform 36.

## Build using GitHub Actions

1. Create a private GitHub repository and upload the contents of this native project folder (`android-source/`).
2. Open **Actions → Android build** and select **Run workflow**.
3. Wait for the workflow to finish successfully, then open its run and download the `repdawn-debug-apk` artifact.
4. Extract the ZIP artifact. The file inside is `app-debug.apk`.
5. Transfer the APK to your Android phone, open it, and approve installation from that file app/browser when Android asks. Install the APK itself, not the artifact ZIP.

## Build locally

Install JDK 17+, Android Studio / SDK Platform 36, and Build Tools. From this directory run:

```powershell
.\\gradlew.bat testDebugUnitTest assembleDebug
```

The APK is created at `app/build/outputs/apk/debug/app-debug.apk` when the build succeeds.

This package was not compiled in the current chat environment because Android SDK Platform 36 was absent and the Gradle distribution could not be downloaded. The GitHub Actions workflow is included but has not been run here.

Do not uninstall RepDawn before exporting a ZIP backup: Android normally removes its private database on uninstall.
