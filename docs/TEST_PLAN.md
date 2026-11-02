# Test plan

Automated checks include scheduler unit tests and a Compose startup smoke test. Run `./gradlew.bat testDebugUnitTest` and, with an emulator/device, `./gradlew.bat connectedDebugAndroidTest`.

Manual release blockers:

1. Enable airplane mode, launch, add `der Apfel`, edit it, study it through all single/double-tap faces, grade it, close/reopen, then find its new due date.
2. Create a folder and a card; trash, restore, duplicate and permanently delete a copy. Verify the original review history remains.
3. Create non-ASCII cards (`größer`, `Straße`, `Mädchen`), export, import into a second install, and compare all content/due dates.
4. Rapidly tap and double tap a study card. Verify only one visible gesture transition occurs and review history changes only after a schedule button.
5. Test narrow phone layout, TalkBack labels/visible controls, text scaling, empty study, and a few thousand imported cards.
6. On a configured cloud test account, switch Wi-Fi to cellular during a sync/generation request. Confirm pending work remains queued and the flashcard flow stays usable.
