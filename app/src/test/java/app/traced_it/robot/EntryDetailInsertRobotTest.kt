package app.traced_it.robot

import androidx.compose.ui.test.assertAll
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.isNotSelected
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import app.traced_it.data.database.ClothingSizeUnit
import app.traced_it.data.database.FractionUnit
import app.traced_it.data.extensions.plusDateOffset
import app.traced_it.data.extensions.plusHourOffset
import app.traced_it.data.extensions.plusMinuteOffset
import app.traced_it.data.extensions.plusOffset
import app.traced_it.ui.components.TestTags
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Test

@HiltAndroidTest
class EntryDetailInsertRobotTest : BaseRobotTest() {

    @Test
    fun `when the insert button is clicked, then the form is open and it is empty`() {
        launch()
        // Click insert
        composeRule.onNodeWithTag(TestTags.ENTRY_LIST_INSERT).performClick()
        // Check detail
        onDetailContent("").assertExists()
        onDetailUnitChoices(ClothingSizeUnit).assertCountEquals(5).assertAll(isNotSelected())
        onDetailCreatedAtDate(now.date).performScrollTo().assertIsDisplayed()
        onDetailCreatedAtHour(now.hour).assertIsDisplayed()
        onDetailCreatedAtMinute(now.minute).assertIsDisplayed()
    }

    @Test
    fun `when the insert button is clicked and the form is changed and saved, then the list shows a new entry`() {
        launch()
        // Click insert
        composeRule.onNodeWithTag(TestTags.ENTRY_LIST_INSERT).performClick()
        // Change detail
        onDetailContent().performTextInput("Coconut")
        onDetailUnitChoice(ClothingSizeUnit, "M").performClick()
        onDetailCreatedAtDate().performScrollTo().performVerticalScroll(3)
        onDetailCreatedAtHour().performVerticalScroll(-3)
        onDetailCreatedAtMinute().performVerticalScroll(-3)
        // Save detail
        composeRule.onNodeWithTag(TestTags.ENTRY_DETAIL_SAVE_INSERT).performClick()
        // Check list
        onListItem(
            "Coconut (M)",
            createdAt = now.plusOffset(dateOffset = -3, hourOffset = -3, minuteOffset = -3)
        ).assertExists()
    }

    @Test
    fun `when the insert button is clicked and the form is changed and saved and the insert button is clicked again, then the form is open and it is empty`() {
        launch()
        // Click insert
        composeRule.onNodeWithTag(TestTags.ENTRY_LIST_INSERT).performClick()
        // Change detail
        onDetailContent().performTextInput("Coconut")
        onDetailUnitChoice(ClothingSizeUnit, "M").performClick()
        onDetailCreatedAtDate().performScrollTo().performVerticalScroll(-3)
        onDetailCreatedAtHour().performVerticalScroll(-3)
        onDetailCreatedAtMinute().performVerticalScroll(-3)
        // Save detail
        composeRule.onNodeWithTag(TestTags.ENTRY_DETAIL_SAVE_INSERT).performClick()
        // Click insert
        composeRule.onNodeWithTag(TestTags.ENTRY_LIST_INSERT).performClick()
        // Check detail
        onDetailContent("").assertExists()
        onDetailUnitChoices(ClothingSizeUnit).assertCountEquals(5).assertAll(isNotSelected())
        onDetailCreatedAtDate(now.date).performScrollTo().assertIsDisplayed()
        onDetailCreatedAtHour(now.hour).assertIsDisplayed()
        onDetailCreatedAtMinute(now.minute).assertIsDisplayed()
    }

    @Test
    fun `when the insert button is clicked and the form is dismissed, then the list does not show a new entry`() {
        launch()
        // Click insert
        composeRule.onNodeWithTag(TestTags.ENTRY_LIST_INSERT).performClick()
        // Dismiss detail
        composeRule.onNodeWithTag(TestTags.ENTRY_DETAIL_DISMISS).performClick()
        // Check list
        onListTitle(totalCount = 0).assertExists()
    }

    @Test
    fun `when the insert button is clicked and the back gesture is performed, then the list does not show a new entry`() {
        launch()
        // Click insert
        composeRule.onNodeWithTag(TestTags.ENTRY_LIST_INSERT).performClick()
        // Dismiss detail
        waitForDetail()
        pressBack()
        // Check list
        onListTitle(totalCount = 0).assertExists()
    }

    @Test
    fun `when the insert button is clicked and the app is restarted, then the form is open and it is empty`() {
        launch()
        // Click insert
        composeRule.onNodeWithTag(TestTags.ENTRY_LIST_INSERT).performClick()
        // Restart app
        restartActivity()
        // Check detail
        onDetailContent("").assertExists()
        onDetailUnitChoices(ClothingSizeUnit).assertCountEquals(5).assertAll(isNotSelected())
        onDetailCreatedAtDate(now.date).performScrollTo().assertIsDisplayed()
        onDetailCreatedAtHour(now.hour).assertIsDisplayed()
        onDetailCreatedAtMinute(now.minute).assertIsDisplayed()
    }

    @Test
    fun `when the insert button is clicked and the form is changed and the app is restarted and the form is saved, then the list shows a new entry`() {
        launch()
        // Click insert
        composeRule.onNodeWithTag(TestTags.ENTRY_LIST_INSERT).performClick()
        // Change detail
        onDetailContent().performTextInput("Fig")
        composeRule.onNodeWithTag(TestTags.ENTRY_DETAIL_UNIT_MENU).performClick()
        composeRule.onNodeWithTag(TestTags.entryDetailUnitMenuItem(FractionUnit)).performClick()
        onDetailUnitChoice(FractionUnit, "½").performClick()
        onDetailCreatedAtDate().performScrollTo().performVerticalScroll(-3)
        onDetailCreatedAtHour().performVerticalScroll(-3)
        onDetailCreatedAtMinute().performVerticalScroll(-3)
        // Restart app
        restartActivity()
        // Check detail
        onDetailContent("Fig").assertExists()
        onDetailUnitChoice(FractionUnit, "½").assertIsSelected()
        onDetailCreatedAtDate(now.date.plusDateOffset(-3)).performScrollTo().assertIsDisplayed()
        onDetailCreatedAtHour(now.hour.plusHourOffset(-3)).assertIsDisplayed()
        onDetailCreatedAtMinute(now.minute.plusMinuteOffset(-3)).assertIsDisplayed()
        // Save detail
        composeRule.onNodeWithTag(TestTags.ENTRY_DETAIL_SAVE_INSERT).performClick()
        // Check list
        onListItem(
            "Fig (½)",
            createdAt = now.plusOffset(dateOffset = -3, hourOffset = -3, minuteOffset = -3),
        ).assertExists()
    }

    @Test
    fun `when the insert button is clicked and the form is changed and the app is restarted and the form is changed and the form is saved, then the list shows a new entry`() {
        launch()
        // Click insert
        composeRule.onNodeWithTag(TestTags.ENTRY_LIST_INSERT).performClick()
        // Change detail
        onDetailContent().performTextInput("Fig")
        composeRule.onNodeWithTag(TestTags.ENTRY_DETAIL_UNIT_MENU).performClick()
        composeRule.onNodeWithTag(TestTags.entryDetailUnitMenuItem(FractionUnit)).performClick()
        onDetailUnitChoice(FractionUnit, "½").performClick()
        onDetailCreatedAtDate().performScrollTo().performVerticalScroll(-3)
        onDetailCreatedAtHour().performVerticalScroll(-3)
        onDetailCreatedAtMinute().performVerticalScroll(-3)
        // Restart app
        restartActivity()
        // Change detail
        onDetailContent().performTextInput(" edited")
        onDetailUnitChoice(FractionUnit, "¾").performClick()
        onDetailCreatedAtDate().performScrollTo().performVerticalScroll(-2)
        onDetailCreatedAtHour().performVerticalScroll(-2)
        onDetailCreatedAtMinute().performVerticalScroll(-2)
        // Save detail
        composeRule.onNodeWithTag(TestTags.ENTRY_DETAIL_SAVE_INSERT).performClick()
        // Check list
        onListItem(
            "Fig edited (¾)",
            createdAt = now.plusOffset(dateOffset = -5, hourOffset = -5, minuteOffset = -5),
        ).assertExists()
    }

    @Test
    fun `when the insert button is clicked and the form is changed and the app is restarted and the form is dismissed, then the list does not show a new entry`() {
        launch()
        // Click insert
        composeRule.onNodeWithTag(TestTags.ENTRY_LIST_INSERT).performClick()
        // Change detail
        onDetailContent().performTextInput("Fig")
        composeRule.onNodeWithTag(TestTags.ENTRY_DETAIL_UNIT_MENU).performClick()
        composeRule.onNodeWithTag(TestTags.entryDetailUnitMenuItem(FractionUnit)).performClick()
        onDetailUnitChoice(FractionUnit, "½").performClick()
        onDetailCreatedAtDate().performScrollTo().performVerticalScroll(-3)
        onDetailCreatedAtHour().performVerticalScroll(-3)
        onDetailCreatedAtMinute().performVerticalScroll(-3)
        // Restart app
        restartActivity()
        // Dismiss detail
        composeRule.onNodeWithTag(TestTags.ENTRY_DETAIL_DISMISS).performClick()
        // Check list
        onListTitle(totalCount = 0).assertExists()
    }

    @Test
    fun `when the insert button is clicked and the form is changed and dismissed and the app is restarted, then the form is not open`() {
        launch()
        // Click insert
        composeRule.onNodeWithTag(TestTags.ENTRY_LIST_INSERT).performClick()
        // Change detail
        onDetailContent().performTextInput("Fig")
        composeRule.onNodeWithTag(TestTags.ENTRY_DETAIL_UNIT_MENU).performClick()
        composeRule.onNodeWithTag(TestTags.entryDetailUnitMenuItem(FractionUnit)).performClick()
        onDetailUnitChoice(FractionUnit, "½").performClick()
        onDetailCreatedAtDate().performScrollTo().performVerticalScroll(-3)
        onDetailCreatedAtHour().performVerticalScroll(-3)
        onDetailCreatedAtMinute().performVerticalScroll(-3)
        // Dismiss detail
        composeRule.onNodeWithTag(TestTags.ENTRY_DETAIL_DISMISS).performClick()
        // Restart app
        restartActivity()
        // Check list
        onListTitle(totalCount = 0).assertExists()
    }
}
