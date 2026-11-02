package app.repdawn.android

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test

class SmokeTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()
    @Test fun homeShowsOfflinePromise() { rule.onNodeWithText("Learn German, locally first").assertExists() }
}
