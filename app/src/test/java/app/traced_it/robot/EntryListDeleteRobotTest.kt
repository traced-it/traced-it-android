package app.traced_it.robot

import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import app.traced_it.ui.components.TestTags
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.runBlocking
import org.junit.Test

@HiltAndroidTest
class EntryListDeleteRobotTest : BaseRobotTest() {

    @Test
    fun `when a list item is swiped and the delete button is clicked, then the list does not show the deleted entry`() {
        runBlocking {
            entryRepository.insert(entryWithDoubleUnit)
            entryRepository.insert(entryWithFractionUnit)
        }
        launch()
        // Check list
        waitForList()
        onListTitle(totalCount = 2).assertExists()
        onListItem("Fig (½)").assertExists()
        // Click delete
        onListItem("Dewberry (3.14)").swipeListItemLeft()
        onListItemDeleteButton("Dewberry (3.14)").performClick()
        // Check list
        composeRule.waitForIdle()
        onListTitle(totalCount = 1).assertExists()
        onListItem("Dewberry (3.14)").assertDoesNotExist()
        onListItem("Fig (½)").assertExists()
    }

    @Test
    fun `when a list item is swiped and the delete button is clicked and the restore button is clicked, then the list still shows the entry`() {
        runBlocking {
            entryRepository.insert(entryWithDoubleUnit)
            entryRepository.insert(entryWithFractionUnit)
        }
        launch()
        // Check list
        waitForList()
        onListTitle(totalCount = 2).assertExists()
        onListItem("Fig (½)").assertExists()
        // Click delete
        onListItem("Dewberry (3.14)").swipeListItemLeft()
        onListItemDeleteButton("Dewberry (3.14)").performClick()
        // Check list
        composeRule.waitForIdle()
        onListTitle(totalCount = 1).assertExists()
        onListItem("Dewberry (3.14)").assertDoesNotExist()
        onListItem("Fig (½)").assertExists()
        // Click undo
        composeRule.onNodeWithText("Undo").performClick()
        // Check list
        onListTitle(totalCount = 2).assertExists()
        onListItem("Dewberry (3.14)").assertExists()
        onListItem("Fig (½)").assertExists()
    }

    @Test
    fun `when a list item is swiped and the delete all button is clicked and the dialog is confirmed, then the list is empty`() {
        runBlocking {
            entryRepository.insert(entryWithDoubleUnit)
            entryRepository.insert(entryWithFractionUnit)
        }
        launch()
        // Check list
        waitForList()
        onListTitle(totalCount = 2).assertExists()
        onListItem("Dewberry (3.14)").assertExists()
        onListItem("Fig (½)").assertExists()
        // Click delete all
        composeRule.onNodeWithTag(TestTags.ENTRY_LIST_MENU).performClick()
        composeRule.onNodeWithTag(TestTags.ENTRY_LIST_MENU_DELETE_ALL).performClick()
        // Confirm delete all
        composeRule.onNodeWithTag(TestTags.CONFIRMATION_DIALOG_CONFIRM).performClick()
        // Check list
        onListTitle(totalCount = 0).assertExists()
        onListItem("Dewberry (3.14)").assertDoesNotExist()
        onListItem("Fig (½)").assertDoesNotExist()
    }

    @Test
    fun `when a list item is swiped and the delete all button is clicked and the dialog is confirmed and the restore button is clicked, then the list shows all entries`() {
        runBlocking {
            entryRepository.insert(entryWithDoubleUnit)
            entryRepository.insert(entryWithFractionUnit)
        }
        launch()
        // Check list
        waitForList()
        onListTitle(totalCount = 2).assertExists()
        onListItem("Dewberry (3.14)").assertExists()
        onListItem("Fig (½)").assertExists()
        // Click delete all
        composeRule.onNodeWithTag(TestTags.ENTRY_LIST_MENU).performClick()
        composeRule.onNodeWithTag(TestTags.ENTRY_LIST_MENU_DELETE_ALL).performClick()
        // Confirm delete all
        composeRule.onNodeWithTag(TestTags.CONFIRMATION_DIALOG_CONFIRM).performClick()
        // Check list
        onListTitle(totalCount = 0).assertExists()
        onListItem("Dewberry (3.14)").assertDoesNotExist()
        onListItem("Fig (½)").assertDoesNotExist()
        // Click undo
        composeRule.onNodeWithText("Undo").performClick()
        // Check list
        onListTitle(totalCount = 2).assertExists()
        onListItem("Dewberry (3.14)").assertExists()
        onListItem("Fig (½)").assertExists()
    }

    @Test
    fun `when a list item is swiped and the delete all button is clicked and the dialog is dismissed, then the list shows all entries`() {
        runBlocking {
            entryRepository.insert(entryWithDoubleUnit)
            entryRepository.insert(entryWithFractionUnit)
        }
        launch()
        // Check list
        waitForList()
        onListTitle(totalCount = 2).assertExists()
        onListItem("Dewberry (3.14)").assertExists()
        onListItem("Fig (½)").assertExists()
        // Click delete all
        composeRule.onNodeWithTag(TestTags.ENTRY_LIST_MENU).performClick()
        composeRule.onNodeWithTag(TestTags.ENTRY_LIST_MENU_DELETE_ALL).performClick()
        // Dismiss delete all
        composeRule.onNodeWithTag(TestTags.CONFIRMATION_DIALOG_DISMISS).performClick()
        // Check list
        onListTitle(totalCount = 2).assertExists()
        onListItem("Dewberry (3.14)").assertExists()
        onListItem("Fig (½)").assertExists()
    }
}
