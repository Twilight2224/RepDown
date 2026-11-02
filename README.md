# RepDawn — Native Android source

This is the Kotlin / Jetpack Compose Android project for RepDawn. It is local-first and uses a Room database; cards and repetition scheduling work without an internet connection.

## Included

- Add, edit, trash, restore, and permanently delete flashcards.
- Study in both German → meaning and meaning → German directions.
- Review scheduling with Again / Hard / Good / Easy grades and custom intervals.
- ZIP backup and merge restore, including card schedules and review logs.
- CSV export/import so vocabulary can be edited on a laptop and merged back into Android.

## Build requirements

JDK 17+, Android SDK Platform 36, Android Gradle Plugin 8.13.2, and Gradle 8.13. From this directory:

```powershell
.\\gradlew.bat testDebugUnitTest assembleDebug
```

Or use the included GitHub Actions workflow (`.github/workflows/android.yml`). See `docs/ANDROID_INSTALL.md`.

**Build status:** this chat environment had no Android SDK Platform 36 and could not download the Gradle distribution because outbound access was blocked. The source changes were reviewed, but no APK, emulator, or device build was executed here. Do not treat this folder as an APK; it is the source for producing one.

## Cloud sync

No cloud synchronization adapter is active. The app works offline; cross-device transfer is currently by CSV or ZIP backup. Actual automatic sync requires configuring a backend and adding its authenticated sync adapter.
