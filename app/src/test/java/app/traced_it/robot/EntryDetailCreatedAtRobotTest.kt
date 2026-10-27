package app.traced_it.robot

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import app.traced_it.data.extensions.plusDateOffset
import app.traced_it.data.extensions.plusHourOffset
import app.traced_it.data.extensions.plusMinuteOffset
import app.traced_it.ui.components.TestTags
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Test

@HiltAndroidTest
class EntryDetailCreatedAtRobotTest : BaseRobotTest() {

    @Test
    fun `when created-at is not set in the database and the created-at field is changed and configuration changes, then the created-at field does not change`() {
        launch()
        // Click insert
        composeRule.onNodeWithTag(TestTags.ENTRY_LIST_INSERT).performClick()
        // Change created-at field
        onDetailCreatedAtDate().performScrollTo().performVerticalScroll(-3)
        onDetailCreatedAtHour().performVerticalScroll(-3)
        onDetailCreatedAtMinute().performVerticalScroll(-3)
        // Change configuration
        rotateDevice()
        // Check created-at field
        onDetailCreatedAtDate(now.date.plusDateOffset(-3)).performScrollTo().assertIsDisplayed()
        onDetailCreatedAtHour(now.hour.plusHourOffset(-3)).assertIsDisplayed()
        onDetailCreatedAtMinute(now.minute.plusMinuteOffset(-3)).assertIsDisplayed()
    }
}
