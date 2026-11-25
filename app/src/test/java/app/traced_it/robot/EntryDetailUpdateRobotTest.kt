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
import app.traced_it.data.extensions.plusDateOffset
import app.traced_it.data.extensions.plusHourOffset
import app.traced_it.data.extensions.plusMinuteOffset
import app.traced_it.data.extensions.plusOffset
import app.traced_it.ui.components.TestTags
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.toLocalDateTime
import org.junit.Test
import kotlin.time.Instant

@HiltAndroidTest
class EntryDetailUpdateRobotTest : BaseRobotTest() {

    private val createdAt = Instant.fromEpochMilliseconds(entryWithDoubleUnit.createdAt).toLocalDateTime(timeZone)

    @Test
    fun `when a list item is swiped and the update button is clicked, then the form is open and it is filled`() {
        runBlocking { entryRepository.insert(entryWithDoubleUnit) }
        launch()
        // Click update
        onListItem("Dewberry (3.14)").swipeListItemRight()
        onListItemUpdateButton("Dewberry (3.14)").performClick()
        // Check form
        onDetailContent("Dewberry").assertExists()
        onDetailAmount(DoubleUnit, 3.14).assertExists()
        onDetailCreatedAtDate(createdAt.date).performScrollTo().assertIsDisplayed()
        onDetailCreatedAtHour(createdAt.hour).assertIsDisplayed()
        onDetailCreatedAtMinute(createdAt.minute).assertIsDisplayed()
    }

    @Test
    fun `when a list item is swiped and the update button is clicked and the form is changed and saved, then the list shows the updated entry`() {
        runBlocking { entryRepository.insert(entryWithDoubleUnit) }
        launch()
        // Click update
        onListItem("Dewberry (3.14)").swipeListItemRight()
        onListItemUpdateButton("Dewberry (3.14)").performClick()
        // Change detail
        onDetailContent().performTextInput(" edited")
        composeRule.onNodeWithTag(TestTags.ENTRY_DETAIL_UNIT_MENU).performClick()
        composeRule.onNodeWithTag(TestTags.entryDetailUnitMenuItem(ClothingSizeUnit)).performClick()
        onDetailUnitChoice(ClothingSizeUnit, "M").performClick()
        onDetailCreatedAtDate().performScrollTo().performVerticalScroll(-3)
        onDetailCreatedAtHour().performVerticalScroll(-3)
        onDetailCreatedAtMinute().performVerticalScroll(-3)
        // Save detail
        composeRule.onNodeWithTag(TestTags.ENTRY_DETAIL_SAVE_UPDATE).performClick()
        // Check list
        onListItem(
            "Dewberry edited (M)",
            createdAt = createdAt.plusOffset(dateOffset = -3, hourOffset = -3, minuteOffset = -3),
        ).assertExists()
    }

    @Test
    fun `when a list item is swiped and the update button is clicked and the form is changed and saved and the update button is clicked again, then the form is open and it is filled`() {
        runBlocking { entryRepository.insert(entryWithDoubleUnit) }
        launch()
        // Click update
        onListItem("Dewberry (3.14)").swipeListItemRight()
        onListItemUpdateButton("Dewberry (3.14)").performClick()
        // Change detail
        onDetailContent().performTextInput(" edited")
        composeRule.onNodeWithTag(TestTags.ENTRY_DETAIL_UNIT_MENU).performClick()
        composeRule.onNodeWithTag(TestTags.entryDetailUnitMenuItem(ClothingSizeUnit)).performClick()
        onDetailUnitChoice(ClothingSizeUnit, "M").performClick()
        onDetailCreatedAtDate().performScrollTo().performVerticalScroll(-3)
        onDetailCreatedAtHour().performVerticalScroll(-3)
        onDetailCreatedAtMinute().performVerticalScroll(-3)
        // Save detail
        composeRule.onNodeWithTag(TestTags.ENTRY_DETAIL_SAVE_UPDATE).performClick()
        // Click update
        onListItem("Dewberry edited (M)").swipeListItemRight()
        onListItemUpdateButton("Dewberry edited (M)").performClick()
        // Check detail
        onDetailContent("Dewberry edited").assertExists()
        onDetailUnitChoice(ClothingSizeUnit, "M").assertIsSelected()
        onDetailCreatedAtDate(createdAt.date.plusDateOffset(-3)).performScrollTo().assertIsDisplayed()
        onDetailCreatedAtHour(createdAt.hour.plusHourOffset(-3)).assertIsDisplayed()
        onDetailCreatedAtMinute(createdAt.minute.plusMinuteOffset(-3)).assertIsDisplayed()
    }

    @Test
    fun `when a list item is swiped and the update button is clicked and the form is changed and saved and a different update button is clicked, then the form is open and it is filled with different values`() {
        runBlocking {
            entryRepository.insert(entryWithDoubleUnit)
            entryRepository.insert(entryWithFractionUnit)
        }
        launch()
        // Click update
        onListItem("Dewberry (3.14)").swipeListItemRight()
        onListItemUpdateButton("Dewberry (3.14)").performClick()
        // Change detail
        onDetailContent().performTextInput(" edited")
        composeRule.onNodeWithTag(TestTags.ENTRY_DETAIL_UNIT_MENU).performClick()
        composeRule.onNodeWithTag(TestTags.entryDetailUnitMenuItem(ClothingSizeUnit)).performClick()
        onDetailUnitChoice(ClothingSizeUnit, "M").performClick()
        onDetailCreatedAtDate().performScrollTo().performVerticalScroll(-3)
        onDetailCreatedAtHour().performVerticalScroll(-3)
        onDetailCreatedAtMinute().performVerticalScroll(-3)
        // Save detail
        composeRule.onNodeWithTag(TestTags.ENTRY_DETAIL_SAVE_UPDATE).performClick()
        // Click update
        onListItem("Fig (½)").swipeListItemRight()
        onListItemUpdateButton("Fig (½)").performClick()
        // Check detail
        onDetailContent("Fig").assertExists()
        onDetailUnitChoice(FractionUnit, "½").assertIsSelected()
        val secondCreatedAt = Instant.fromEpochMilliseconds(entryWithFractionUnit.createdAt).toLocalDateTime(timeZone)
        onDetailCreatedAtDate(secondCreatedAt.date).performScrollTo().assertIsDisplayed()
        onDetailCreatedAtHour(secondCreatedAt.hour).assertIsDisplayed()
        onDetailCreatedAtMinute(secondCreatedAt.minute).assertIsDisplayed()
    }


    @Test
    fun `when a list item is swiped and the update button is clicked and the form is dismissed, then the list shows the unchanged entry`() {
        runBlocking { entryRepository.insert(entryWithDoubleUnit) }
        launch()
        // Click update
        onListItem("Dewberry (3.14)").swipeListItemRight()
        onListItemUpdateButton("Dewberry (3.14)").performClick()
        // Dismiss detail
        waitForDetail()
        composeRule.onNodeWithTag(TestTags.ENTRY_DETAIL_DISMISS).performClick()
        // Check list
        onListItem("Dewberry (3.14)").assertExists()
    }

    @Test
    fun `when a list item is swiped and the update button is clicked and the back gesture is performed, then the list shows the unchanged entry`() {
        runBlocking { entryRepository.insert(entryWithDoubleUnit) }
        launch()
        // Click update
        onListItem("Dewberry (3.14)").swipeListItemRight()
        onListItemUpdateButton("Dewberry (3.14)").performClick()
        // Dismiss detail
        waitForDetail()
        pressBack()
        // Check list
        onListItem("Dewberry (3.14)").assertExists()
    }

    @Test
    fun `when a list item is swiped and the update button is clicked and the app is restarted, then the form is open and it is filled`() {
        runBlocking { entryRepository.insert(entryWithDoubleUnit) }
        launch()
        // Click update
        onListItem("Dewberry (3.14)").swipeListItemRight()
        onListItemUpdateButton("Dewberry (3.14)").performClick()
        // Restart app
        waitForDetail()
        restartActivity()
        // Check detail
        onDetailContent("Dewberry").assertExists()
        onDetailAmount(DoubleUnit, 3.14)
        onDetailCreatedAtDate(createdAt.date).performScrollTo().assertIsDisplayed()
        onDetailCreatedAtHour(createdAt.hour).assertIsDisplayed()
        onDetailCreatedAtMinute(createdAt.minute).assertIsDisplayed()
    }

    @Test
    fun `when a list item is swiped and the update button is clicked and the form is changed and the app is restarted and the form is saved, then the list shows the updated entry`() {
        runBlocking { entryRepository.insert(entryWithDoubleUnit) }
        launch()
        // Click update
        onListItem("Dewberry (3.14)").swipeListItemRight()
        onListItemUpdateButton("Dewberry (3.14)").performClick()
        // Change detail
        onDetailContent().performTextInput(" edited")
        composeRule.onNodeWithTag(TestTags.ENTRY_DETAIL_UNIT_MENU).performClick()
        composeRule.onNodeWithTag(TestTags.entryDetailUnitMenuItem(ClothingSizeUnit)).performClick()
        onDetailUnitChoice(ClothingSizeUnit, "M").performClick()
        onDetailCreatedAtDate().performScrollTo().performVerticalScroll(-3)
        onDetailCreatedAtHour().performVerticalScroll(-3)
        onDetailCreatedAtMinute().performVerticalScroll(-3)
        // Restart app
        restartActivity()
        // Check detail
        onDetailContent("Dewberry edited").assertExists()
        onDetailUnitChoice(ClothingSizeUnit, "M").assertIsSelected()
        onDetailCreatedAtDate(createdAt.date.plusDateOffset(-3)).performScrollTo().assertIsDisplayed()
        onDetailCreatedAtHour(createdAt.hour.plusHourOffset(-3)).assertIsDisplayed()
        onDetailCreatedAtMinute(createdAt.minute.plusMinuteOffset(-3)).assertIsDisplayed()
        // Save detail
        composeRule.onNodeWithTag(TestTags.ENTRY_DETAIL_SAVE_UPDATE).performClick()
        // Check list
        onListItem(
            "Dewberry edited (M)",
            createdAt = createdAt.plusOffset(dateOffset = -3, hourOffset = -3, minuteOffset = -3),
        ).assertExists()
    }

    @Test
    fun `when a list item is swiped and the update button is clicked and the form is changed and the app is restarted and the form is changed and the form is saved, then the list shows the updated entry`() {
        runBlocking { entryRepository.insert(entryWithDoubleUnit) }
        launch()
        // Click update
        onListItem("Dewberry (3.14)").swipeListItemRight()
        onListItemUpdateButton("Dewberry (3.14)").performClick()
        // Change detail
        onDetailContent().performTextInput(" edited")
        composeRule.onNodeWithTag(TestTags.ENTRY_DETAIL_UNIT_MENU).performClick()
        composeRule.onNodeWithTag(TestTags.entryDetailUnitMenuItem(ClothingSizeUnit)).performClick()
        onDetailUnitChoice(ClothingSizeUnit, "M").performClick()
        onDetailCreatedAtDate().performScrollTo().performVerticalScroll(-3)
        onDetailCreatedAtHour().performVerticalScroll(-3)
        onDetailCreatedAtMinute().performVerticalScroll(-3)
        // Restart app
        restartActivity()
        // Change detail
        onDetailContent().performTextInput(" 2")
        onDetailUnitChoice(ClothingSizeUnit, "XL").performClick()
        onDetailCreatedAtDate().performScrollTo().performVerticalScroll(-2)
        onDetailCreatedAtHour().performVerticalScroll(-2)
        onDetailCreatedAtMinute().performVerticalScroll(-2)
        // Save detail
        composeRule.onNodeWithTag(TestTags.ENTRY_DETAIL_SAVE_UPDATE).performClick()
        // Check list
        onListItem(
            "Dewberry edited 2 (XL)",
            createdAt = createdAt.plusOffset(dateOffset = -5, hourOffset = -5, minuteOffset = -5),
        ).assertExists()
    }

    @Test
    fun `when a list item is swiped and the update button is clicked and the form is changed and the app is restarted and the form is dismissed, then the list shows the unchanged entry`() {
        runBlocking { entryRepository.insert(entryWithDoubleUnit) }
        launch()
        // Click update
        onListItem("Dewberry (3.14)").swipeListItemRight()
        onListItemUpdateButton("Dewberry (3.14)").performClick()
        // Change detail
        onDetailContent().performTextInput(" edited")
        composeRule.onNodeWithTag(TestTags.ENTRY_DETAIL_UNIT_MENU).performClick()
        composeRule.onNodeWithTag(TestTags.entryDetailUnitMenuItem(ClothingSizeUnit)).performClick()
        onDetailUnitChoice(ClothingSizeUnit, "M").performClick()
        onDetailCreatedAtDate().performScrollTo().performVerticalScroll(-3)
        onDetailCreatedAtHour().performVerticalScroll(-3)
        onDetailCreatedAtMinute().performVerticalScroll(-3)
        // Restart app
        restartActivity()
        // Dismiss detail
        composeRule.onNodeWithTag(TestTags.ENTRY_DETAIL_DISMISS).performClick()
        // Check list
        onListItem(
            "Dewberry (3.14)",
            createdAt = entryWithDoubleUnit.createdAtInstant.toLocalDateTime(timeZone),
        ).assertExists()
    }

    @Test
    fun `when a list item is swiped and the update button is clicked and the form is changed and dismissed and the app is restarted, then the form is not open`() {
        runBlocking { entryRepository.insert(entryWithDoubleUnit) }
        launch()
        // Click update
        onListItem("Dewberry (3.14)").swipeListItemRight()
        onListItemUpdateButton("Dewberry (3.14)").performClick()
        // Change detail
        onDetailContent().performTextInput(" edited")
        composeRule.onNodeWithTag(TestTags.ENTRY_DETAIL_UNIT_MENU).performClick()
        composeRule.onNodeWithTag(TestTags.entryDetailUnitMenuItem(ClothingSizeUnit)).performClick()
        onDetailUnitChoice(ClothingSizeUnit, "M").performClick()
        onDetailCreatedAtDate().performScrollTo().performVerticalScroll(-3)
        onDetailCreatedAtHour().performVerticalScroll(-3)
        onDetailCreatedAtMinute().performVerticalScroll(-3)
        // Dismiss detail
        composeRule.onNodeWithTag(TestTags.ENTRY_DETAIL_DISMISS).performClick()
        // Restart app
        restartActivity()
        // Check list
        onListItem(
            "Dewberry (3.14)",
            createdAt = entryWithDoubleUnit.createdAtInstant.toLocalDateTime(timeZone),
        ).assertExists()
    }
}
