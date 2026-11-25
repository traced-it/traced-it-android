package app.traced_it.robot

import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import app.traced_it.ui.components.TestTags
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.toLocalDateTime
import org.junit.Test
import kotlin.time.Instant

@HiltAndroidTest
class EntryListSelectRobotTest : BaseRobotTest() {

    @Test
    fun `when a list item is clicked, then the list item is selected and shows exact time and the selected entry bar is open`() {
        runBlocking { entryRepository.insert(entryWithDoubleUnit) }
        launch()
        // Select list item
        waitForListItem("Dewberry (3.14)")
        onListItem("Dewberry (3.14)").performClick()
        // Check list
        onListItem(
            "Dewberry (3.14)",
            createdAt = Instant.fromEpochMilliseconds(entryWithDoubleUnit.createdAt).toLocalDateTime(timeZone)
        )
            .assertExists()
            .assertIsSelected()
        // Check selected bar
        composeRule.onNodeWithTag(TestTags.ENTRY_LIST_SELECTED_TITLE).assertExists()
    }

    @Test
    fun `when a list item is clicked and the update button is clicked, then the form is open`() {
        runBlocking { entryRepository.insert(entryWithDoubleUnit) }
        launch()
        // Select list item
        waitForListItem("Dewberry (3.14)")
        onListItem("Dewberry (3.14)").performClick()
        // Click update
        composeRule.onNode(hasTestTag(TestTags.ENTRY_LIST_SELECTED_UPDATE)).performClick()
        // Check detail
        onDetailContent("Dewberry").assertExists()
    }

    @Test
    fun `when a list item is clicked and the update button is clicked and the form is filled and saved, then the entry is still selected`() {
        runBlocking { entryRepository.insert(entryWithDoubleUnit) }
        launch()
        // Select list item
        waitForListItem("Dewberry (3.14)")
        onListItem("Dewberry (3.14)").performClick()
        // Click update
        composeRule.onNode(hasTestTag(TestTags.ENTRY_LIST_SELECTED_UPDATE)).performClick()
        // Change detail
        onDetailContent().performTextInput(" edited")
        // Save detail
        composeRule.onNodeWithTag(TestTags.ENTRY_DETAIL_SAVE_UPDATE).performClick()
        // Check list
        onListItem("Dewberry edited (3.14)").assertExists().assertIsSelected()
        // Check selected bar
        composeRule.onNodeWithTag(TestTags.ENTRY_LIST_SELECTED_TITLE).assertExists()
    }

    @Test
    fun `when a list item is clicked and the update button is clicked and the form is dismissed, then the entry is still selected`() {
        runBlocking { entryRepository.insert(entryWithDoubleUnit) }
        launch()
        // Select list item
        waitForListItem("Dewberry (3.14)")
        onListItem("Dewberry (3.14)").performClick()
        // Click update
        composeRule.onNode(hasTestTag(TestTags.ENTRY_LIST_SELECTED_UPDATE)).performClick()
        // Check detail
        onDetailContent("Dewberry").assertExists()
        // Dismiss detail
        composeRule.onNodeWithTag(TestTags.ENTRY_DETAIL_DISMISS).performClick()
        // Check list
        onListItem("Dewberry (3.14)").assertExists().assertIsSelected()
        // Check selected bar
        composeRule.onNodeWithTag(TestTags.ENTRY_LIST_SELECTED_TITLE).assertExists()
    }

    @Test
    fun `when a list item is clicked and the delete button is clicked, then the list does not show the deleted entry`() {
        runBlocking { entryRepository.insert(entryWithDoubleUnit) }
        launch()
        // Select list item
        waitForListItem("Dewberry (3.14)")
        onListItem("Dewberry (3.14)").performClick()
        // Click delete
        composeRule.onNodeWithTag(TestTags.ENTRY_LIST_SELECTED_DELETE).performClick()
        // Check list
        onListTitle(totalCount = 0).assertExists()
        onListItem("Dewberry (3.14)").assertDoesNotExist()
        // Check selected bar
        composeRule.onNodeWithTag(TestTags.ENTRY_LIST_SELECTED_TITLE).assertDoesNotExist()
    }

    @Test
    fun `when a list item is clicked and the repeat button is clicked, then the form is open`() {
        runBlocking { entryRepository.insert(entryWithDoubleUnit) }
        launch()
        // Select list item
        waitForListItem("Dewberry (3.14)")
        onListItem("Dewberry (3.14)").performClick()
        // Click repeat
        composeRule.onNodeWithTag(TestTags.ENTRY_LIST_SELECTED_MENU).performClick()
        composeRule.onNodeWithTag(TestTags.ENTRY_LIST_SELECTED_MENU_REPEAT).performClick()
        // Check detail
        onDetailContent("Dewberry").assertExists()
        composeRule.onNodeWithTag(TestTags.ENTRY_DETAIL_SAVE_INSERT).assertExists()
    }

    @Test
    fun `when a list item is clicked and the filter similar button is clicked, then the list shows filtered entries and the entry is deselected`() {
        runBlocking {
            entryRepository.insert(entryWithDoubleUnit)
            entryRepository.insert(entryWithFractionUnit)
        }
        launch()
        // Check list
        waitForList()
        onListTitle(totalCount = 2).assertExists()
        onListItem("Fig (½)").assertExists()
        // Select list item
        onListItem("Dewberry (3.14)").performClick()
        // Click filter similar
        composeRule.onNodeWithTag(TestTags.ENTRY_LIST_SELECTED_MENU).performClick()
        composeRule.onNodeWithTag(TestTags.ENTRY_LIST_SELECTED_MENU_FILTER_SIMILAR).performClick()
        // Check list
        onListFilter(itemCount = 1, totalCount = 2).assertExists()
        onListItem("Dewberry (3.14)").assertExists().assertIsNotSelected()
        onListItem("Fig (½)").assertDoesNotExist()
        // Check filter bar
        composeRule.onNode(hasTestTag(TestTags.ENTRY_LIST_FILTER_QUERY) and hasText("Dewberry")).assertExists()
        // Check selected bar
        composeRule.onNodeWithTag(TestTags.ENTRY_LIST_SELECTED_TITLE).assertDoesNotExist()
    }

    @Test
    fun `when a list item is clicked and the back button is clicked, then the entry is deselected and the selected entry bar is closed`() {
        runBlocking { entryRepository.insert(entryWithDoubleUnit) }
        launch()
        // Select list item
        waitForListItem("Dewberry (3.14)")
        onListItem("Dewberry (3.14)").performClick()
        // Click back
        composeRule.onNodeWithTag(TestTags.ENTRY_LIST_SELECTED_BACK).performClick()
        // Check list
        onListItem("Dewberry (3.14)").assertIsNotSelected()
        // Check selected bar
        composeRule.onNodeWithTag(TestTags.ENTRY_LIST_SELECTED_TITLE).assertDoesNotExist()
    }

    @Test
    fun `when a list item is clicked and the back gesture is performed, then the entry is deselected and the selected entry bar is closed`() {
        runBlocking { entryRepository.insert(entryWithDoubleUnit) }
        launch()
        // Select list item
        waitForListItem("Dewberry (3.14)")
        onListItem("Dewberry (3.14)").performClick()
        // Check list
        onListItem("Dewberry (3.14)").assertIsSelected()
        // Press back
        pressBack()
        // Check list
        onListItem("Dewberry (3.14)").assertIsNotSelected()
        // Check selected bar
        composeRule.onNodeWithTag(TestTags.ENTRY_LIST_SELECTED_TITLE).assertDoesNotExist()
    }

    @Test
    fun `when a list item is clicked twice, then the entry is deselected and the selected entry bar is closed`() {
        runBlocking { entryRepository.insert(entryWithDoubleUnit) }
        launch()
        // Select list item
        waitForListItem("Dewberry (3.14)")
        onListItem("Dewberry (3.14)").performClick()
        // Check list
        onListItem("Dewberry (3.14)").assertIsSelected()
        // Deselect list item
        onListItem("Dewberry (3.14)").performClick()
        // Check list
        onListItem("Dewberry (3.14)").assertIsNotSelected()
        // Check selected bar
        composeRule.onNodeWithTag(TestTags.ENTRY_LIST_SELECTED_TITLE).assertDoesNotExist()
    }

    @Test
    fun `when a list item is clicked and a second item is clicked, then the first item is deselected and the second item is selected`() {
        runBlocking {
            entryRepository.insert(entryWithDoubleUnit)
            entryRepository.insert(entryWithFractionUnit)
        }
        launch()
        // Select first list item
        waitForListItem("Dewberry (3.14)")
        onListItem("Dewberry (3.14)").performClick()
        // Check list
        onListItem("Dewberry (3.14)").assertIsSelected()
        onListItem("Fig (½)").assertIsNotSelected()
        // Select second list item
        onListItem("Fig (½)").performClick()
        // Check list
        onListItem("Fig (½)").assertIsSelected()
        // Check list
        onListItem("Dewberry (3.14)").assertIsNotSelected()
    }
}
