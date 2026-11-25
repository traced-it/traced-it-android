package app.traced_it.robot

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import app.traced_it.data.database.ClothingSizeUnit
import app.traced_it.data.database.DoubleUnit
import app.traced_it.data.database.FractionUnit
import app.traced_it.data.extensions.plusOffset
import app.traced_it.ui.components.TestTags
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.runBlocking
import org.junit.Test

@HiltAndroidTest
class EntryDetailRepeatRobotTest : BaseRobotTest() {

    @Test
    fun `when the repeat button is clicked, then the form is open and it is filled`() {
        runBlocking { entryRepository.insert(entryWithDoubleUnit) }
        launch()
        // Click repeat
        onListItemRepeatButton("Dewberry (3.14)").performClick()
        // Check detail
        onDetailContent("Dewberry").assertExists()
        onDetailAmount(DoubleUnit, 3.14).assertExists()
        onDetailCreatedAtDate(now.date).performScrollTo().assertIsDisplayed()
        onDetailCreatedAtHour(now.hour).assertIsDisplayed()
        onDetailCreatedAtMinute(now.minute).assertIsDisplayed()
        composeRule.onNodeWithTag(TestTags.ENTRY_DETAIL_SAVE_INSERT).assertExists()
    }

    @Test
    fun `when the repeat button is clicked and the form is dismissed, then the list does not show a new entry`() {
        runBlocking { entryRepository.insert(entryWithDoubleUnit) }
        launch()
        // Click repeat
        onListItemRepeatButton("Dewberry (3.14)").performClick()
        // Dismiss detail
        waitForDetail()
        composeRule.onNodeWithTag(TestTags.ENTRY_DETAIL_DISMISS).performClick()
        // Check list
        onListTitle(totalCount = 1).assertExists()
    }

    @Test
    fun `when the repeat button is clicked and the back gesture is performed, then the list does not show a new entry`() {
        runBlocking { entryRepository.insert(entryWithDoubleUnit) }
        launch()
        // Click repeat
        onListItemRepeatButton("Dewberry (3.14)").performClick()
        // Dismiss detail
        waitForDetail()
        pressBack()
        // Check list
        onListTitle(totalCount = 1).assertExists()
    }

    @Test
    fun `when the repeat button is clicked and the form is changed and saved, then the list shows a new entry`() {
        runBlocking { entryRepository.insert(entryWithDoubleUnit) }
        launch()
        // Click repeat
        onListItemRepeatButton("Dewberry (3.14)").performClick()
        // Change detail
        onDetailContent().performTextInput(" repeated")
        composeRule.onNodeWithTag(TestTags.ENTRY_DETAIL_UNIT_MENU).performClick()
        composeRule.onNodeWithTag(TestTags.entryDetailUnitMenuItem(ClothingSizeUnit)).performClick()
        onDetailUnitChoice(ClothingSizeUnit, "M").performClick()
        onDetailCreatedAtDate().performScrollTo().performVerticalScroll(-3)
        onDetailCreatedAtHour().performVerticalScroll(-3)
        onDetailCreatedAtMinute().performVerticalScroll(-3)
        // Save detail
        composeRule.onNodeWithTag(TestTags.ENTRY_DETAIL_SAVE_INSERT).performClick()
        // Check list
        onListTitle(totalCount = 2).assertExists()
        onListItem(
            "Dewberry repeated (M)",
            createdAt = now.plusOffset(dateOffset = -3, hourOffset = -3, minuteOffset = -3)
        )
            .assertExists()
    }

    @Test
    fun `when the repeat button is clicked and the form is changed and saved and a different repeat button is clicked, then the form is open and it is filled with different values`() {
        runBlocking {
            entryRepository.insert(entryWithDoubleUnit)
            entryRepository.insert(entryWithFractionUnit)
        }
        launch()
        // Click repeat
        onListItemRepeatButton("Dewberry (3.14)").performClick()
        // Change detail
        onDetailContent().performTextInput(" repeated")
        composeRule.onNodeWithTag(TestTags.ENTRY_DETAIL_UNIT_MENU).performClick()
        composeRule.onNodeWithTag(TestTags.entryDetailUnitMenuItem(ClothingSizeUnit)).performClick()
        onDetailUnitChoice(ClothingSizeUnit, "M").performClick()
        onDetailCreatedAtDate().performScrollTo().performVerticalScroll(-3)
        onDetailCreatedAtHour().performVerticalScroll(-3)
        onDetailCreatedAtMinute().performVerticalScroll(-3)
        // Save detail
        composeRule.onNodeWithTag(TestTags.ENTRY_DETAIL_SAVE_INSERT).performClick()
        // Click repeat
        onListItemRepeatButton("Fig (½)").performClick()
        // Check detail
        onDetailContent("Fig").assertExists()
        onDetailUnitChoice(FractionUnit, "½").assertIsSelected()
        onDetailCreatedAtDate(now.date).performScrollTo().assertIsDisplayed()
        onDetailCreatedAtHour(now.hour).assertIsDisplayed()
        onDetailCreatedAtMinute(now.minute).assertIsDisplayed()
    }
}
