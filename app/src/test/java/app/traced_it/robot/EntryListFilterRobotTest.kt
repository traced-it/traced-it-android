package app.traced_it.robot

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.printToString
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.test.swipeUp
import app.traced_it.data.database.Entry
import app.traced_it.data.database.NoneUnit
import app.traced_it.ui.components.TestTags
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.toInstant
import org.junit.Test
import kotlin.time.Duration.Companion.minutes

@OptIn(ExperimentalTestApi::class)
@HiltAndroidTest
class EntryListFilterRobotTest : BaseRobotTest() {

    private val entries = listOf(
        Entry(
            amount = 0.0,
            amountUnit = NoneUnit,
            content = "Apples and oranges",
            createdAt = now.toInstant(timeZone).toEpochMilliseconds(),
        ),
        Entry(
            amount = 0.0,
            amountUnit = NoneUnit,
            content = "Oranges and bananas",
            createdAt = now.toInstant(timeZone).toEpochMilliseconds(),
        ),
    )
    private val orderedEntries = List(15) { i ->
        val minutes = i * 2
        Entry(
            amount = 0.0,
            amountUnit = NoneUnit,
            content = "Entry $minutes minutes ago",
            createdAt = now.toInstant(timeZone).minus(minutes.minutes).toEpochMilliseconds(),
            uid = 1000 + minutes,
        )
    }

    @Test
    fun `when the filter button is clicked, then the filter bar is open and the list shows all entries`() {
        runBlocking { entries.forEach { entryRepository.insert(it) } }
        launch()
        // Check list
        waitForListTitle(totalCount = 2)
        // Expand filter bar
        composeRule.onNodeWithTag(TestTags.ENTRY_LIST_FILTER_EXPAND).performClick()
        // Check filter bar
        composeRule.onNodeWithTag(TestTags.ENTRY_LIST_FILTER_QUERY).assertExists().assertIsFocused()
        // Check list
        composeRule.waitUntilAtLeastOneExists(hasTestTag(TestTags.entryListFilterStatus(itemCount = 2, totalCount = 2)))
        onListFilter(itemCount = 2, totalCount = 2).assertExists()
        onListItem("Apples and oranges").assertExists()
        onListItem("Oranges and bananas").assertExists()
    }

    @Test
    fun `when the query is empty and the back button is clicked, then the filter bar is closed`() {
        runBlocking { entries.forEach { entryRepository.insert(it) } }
        launch()
        // Check list
        waitForListTitle(totalCount = 2)
        // Expand filter bar
        composeRule.onNodeWithTag(TestTags.ENTRY_LIST_FILTER_EXPAND).performClick()
        // Click back
        composeRule.onNodeWithTag(TestTags.ENTRY_LIST_FILTER_BACK).performClick()
        // Check list
        onListTitle(totalCount = 2).assertExists()
        // Check filter bar
        composeRule.onNodeWithTag(TestTags.ENTRY_LIST_FILTER_QUERY).assertDoesNotExist()
    }

    @Test
    fun `when the query is empty and the back gesture is performed, then the filter bar is closed`() {
        runBlocking { entries.forEach { entryRepository.insert(it) } }
        launch()
        // Check list
        waitForListTitle(totalCount = 2)
        // Expand filter bar
        composeRule.onNodeWithTag(TestTags.ENTRY_LIST_FILTER_EXPAND).performClick()
        // Hide IME by focusing another node
        composeRule.onNodeWithTag(TestTags.ENTRY_LIST_FILTER_CLEAR).requestFocus()
        // Press back
        pressBack()
        // Check list
        onListTitle(totalCount = 2).assertExists()
        // Check filter bar
        composeRule.onNodeWithTag(TestTags.ENTRY_LIST_FILTER_QUERY).assertDoesNotExist()
    }

    @Test
    fun `when the query is set and there are matching entries, then the list shows filtered entries`() {
        runBlocking { entries.forEach { entryRepository.insert(it) } }
        launch()
        // Check list
        waitForListTitle(totalCount = 2)
        // Expand filter bar
        composeRule.onNodeWithTag(TestTags.ENTRY_LIST_FILTER_EXPAND).performClick()
        // Set query to one that only the first entry matches
        composeRule.onNodeWithTag(TestTags.ENTRY_LIST_FILTER_QUERY).apply {
            performTextClearance()
            performTextInput("apples")
        }
        // Check list
        composeRule.waitUntilAtLeastOneExists(hasTestTag(TestTags.entryListFilterStatus(itemCount = 1, totalCount = 2)))
        onListItem("Apples and oranges").assertExists()
        onListItem("Oranges and bananas").assertDoesNotExist()
        // Set query to one that only the second entry matches
        composeRule.onNodeWithTag(TestTags.ENTRY_LIST_FILTER_QUERY).apply {
            performTextClearance()
            performTextInput("bananas")
        }
        // Check list
        composeRule.waitUntilAtLeastOneExists(hasTestTag(TestTags.entryListFilterStatus(itemCount = 1, totalCount = 2)))
        onListItem("Apples and oranges").assertDoesNotExist()
        onListItem("Oranges and bananas").assertExists()
        // Set query to one that both entries matches
        composeRule.onNodeWithTag(TestTags.ENTRY_LIST_FILTER_QUERY).apply {
            performTextClearance()
            performTextInput("oranges")
        }
        // Check list
        composeRule.waitUntilAtLeastOneExists(hasTestTag(TestTags.entryListFilterStatus(itemCount = 2, totalCount = 2)))
        onListItem("Apples and oranges").assertExists()
        onListItem("Oranges and bananas").assertExists()
    }

    @Test
    fun `when the query is set and there are no matching entries, then the list is empty`() {
        runBlocking { entries.forEach { entryRepository.insert(it) } }
        launch()
        // Check list
        waitForListTitle(totalCount = 2)
        // Expand filter bar
        composeRule.onNodeWithTag(TestTags.ENTRY_LIST_FILTER_EXPAND).performClick()
        // Set query
        composeRule.onNodeWithTag(TestTags.ENTRY_LIST_FILTER_QUERY).apply {
            performTextClearance()
            performTextInput("Spam")
        }
        // Check list
        composeRule.waitUntilAtLeastOneExists(hasTestTag(TestTags.entryListFilterStatus(itemCount = 0, totalCount = 2)))
        onListItem("Apples and oranges").assertDoesNotExist()
        onListItem("Oranges and bananas").assertDoesNotExist()
    }

    @Test
    fun `when the query is set and the clear button is clicked, then the list shows all entries and the query is cleared`() {
        runBlocking { entries.forEach { entryRepository.insert(it) } }
        launch()
        // Check list
        waitForListTitle(totalCount = 2)
        // Expand filter bar
        composeRule.onNodeWithTag(TestTags.ENTRY_LIST_FILTER_EXPAND).performClick()
        // Set query
        composeRule.onNodeWithTag(TestTags.ENTRY_LIST_FILTER_QUERY).apply {
            performTextClearance()
            performTextInput("apple")
        }
        // Check list
        composeRule.waitUntilAtLeastOneExists(hasTestTag(TestTags.entryListFilterStatus(itemCount = 1, totalCount = 2)))
        onListItem("Apples and oranges").assertExists()
        onListItem("Oranges and bananas").assertDoesNotExist()
        // Clear query
        composeRule.onNodeWithTag(TestTags.ENTRY_LIST_FILTER_CLEAR).performClick()
        // Check list
        composeRule.waitUntilAtLeastOneExists(hasTestTag(TestTags.entryListFilterStatus(itemCount = 2, totalCount = 2)))
        onListItem("Apples and oranges").assertExists()
        onListItem("Oranges and bananas").assertExists()
    }

    @Test
    fun `when the query is set and the back button is clicked, then the list shows all entries and the query is cleared`() {
        runBlocking { entries.forEach { entryRepository.insert(it) } }
        launch()
        // Check list
        waitForListTitle(totalCount = 2)
        // Expand filter bar
        composeRule.onNodeWithTag(TestTags.ENTRY_LIST_FILTER_EXPAND).performClick()
        // Set query
        composeRule.onNodeWithTag(TestTags.ENTRY_LIST_FILTER_QUERY).apply {
            performTextClearance()
            performTextInput("apple")
        }
        // Check list
        composeRule.waitUntilAtLeastOneExists(hasTestTag(TestTags.entryListFilterStatus(itemCount = 1, totalCount = 2)))
        onListItem("Apples and oranges").assertExists()
        onListItem("Oranges and bananas").assertDoesNotExist()
        // Click back
        composeRule.onNodeWithTag(TestTags.ENTRY_LIST_FILTER_BACK).performClick()
        // Check list
        onListTitle(totalCount = 2).assertExists()
        onListItem("Apples and oranges").assertExists()
        onListItem("Oranges and bananas").assertExists()
    }

    @Test
    fun `when the query is set and the back gesture is performed, then the list shows all entries and the query is cleared`() {
        runBlocking { entries.forEach { entryRepository.insert(it) } }
        launch()
        // Check list
        waitForListTitle(totalCount = 2)
        // Expand filter bar
        composeRule.onNodeWithTag(TestTags.ENTRY_LIST_FILTER_EXPAND).performClick()
        // Set query
        composeRule.onNodeWithTag(TestTags.ENTRY_LIST_FILTER_QUERY).apply {
            performTextClearance()
            performTextInput("apple")
        }
        // Check list
        composeRule.waitUntilAtLeastOneExists(hasTestTag(TestTags.entryListFilterStatus(itemCount = 1, totalCount = 2)))
        onListItem("Apples and oranges").assertExists()
        onListItem("Oranges and bananas").assertDoesNotExist()
        // Press back
        pressBack()
        // Check list
        composeRule.waitUntilAtLeastOneExists(hasTestTag(TestTags.entryListFilterStatus(itemCount = 2, totalCount = 2)))
        onListItem("Apples and oranges").assertExists()
        onListItem("Oranges and bananas").assertExists()
        // Check filter bar
        composeRule.onNode(hasTestTag(TestTags.ENTRY_LIST_FILTER_QUERY) and hasText("apple")).assertDoesNotExist()
    }

    @Test
    fun `when the query is set and the update button is clicked the form is filled and saved, then the filter bar is still open and the list shows filtered entries with the inserted entry`() {
        runBlocking { entries.forEach { entryRepository.insert(it) } }
        launch()
        // Check list
        waitForListTitle(totalCount = 2)
        // Expand filter bar
        composeRule.onNodeWithTag(TestTags.ENTRY_LIST_FILTER_EXPAND).performClick()
        // Set query
        composeRule.onNodeWithTag(TestTags.ENTRY_LIST_FILTER_QUERY).apply {
            performTextClearance()
            performTextInput("apple")
        }
        // Check list
        composeRule.waitUntilAtLeastOneExists(hasTestTag(TestTags.entryListFilterStatus(itemCount = 1, totalCount = 2)))
        onListItem("Apples and oranges").assertExists()
        onListItem("Oranges and bananas").assertDoesNotExist()
        // Select list item
        onListItem("Apples and oranges").performClick()
        // Click update
        composeRule.onNode(hasTestTag(TestTags.ENTRY_LIST_SELECTED_UPDATE)).performClick()
        // Change detail
        onDetailContent().performTextInput(" edited")
        // Save detail
        composeRule.onNodeWithTag(TestTags.ENTRY_DETAIL_SAVE_UPDATE).performClick()
        // Deselect list item
        onListItem("Apples and oranges edited").performClick()
        // Check list
        composeRule.waitUntilAtLeastOneExists(hasTestTag(TestTags.entryListFilterStatus(itemCount = 1, totalCount = 2)))
        onListItem("Apples and oranges edited").assertExists()
        onListItem("Oranges and bananas").assertDoesNotExist()
        // Check filter bar
        composeRule.onNode(hasTestTag(TestTags.ENTRY_LIST_FILTER_QUERY) and hasText("apple")).assertExists()
    }

    @Test
    fun `when the query is set and the update button is clicked the form is dismissed, then the filter bar is still open and the list shows filtered entries`() {
        runBlocking { entries.forEach { entryRepository.insert(it) } }
        launch()
        // Check list
        waitForListTitle(totalCount = 2)
        // Expand filter bar
        composeRule.onNodeWithTag(TestTags.ENTRY_LIST_FILTER_EXPAND).performClick()
        // Set query
        composeRule.onNodeWithTag(TestTags.ENTRY_LIST_FILTER_QUERY).apply {
            performTextClearance()
            performTextInput("apple")
        }
        // Check list
        composeRule.waitUntilAtLeastOneExists(hasTestTag(TestTags.entryListFilterStatus(itemCount = 1, totalCount = 2)))
        onListItem("Apples and oranges").assertExists()
        onListItem("Oranges and bananas").assertDoesNotExist()
        // Select list item
        onListItem("Apples and oranges").performClick()
        // Click update
        composeRule.onNode(hasTestTag(TestTags.ENTRY_LIST_SELECTED_UPDATE)).performClick()
        // Check detail
        onDetailContent("Apples and oranges").assertExists()
        // Dismiss detail
        composeRule.onNodeWithTag(TestTags.ENTRY_DETAIL_DISMISS).performClick()
        // Deselect list item
        onListItem("Apples and oranges").performClick()
        // Check list
        composeRule.waitUntilAtLeastOneExists(hasTestTag(TestTags.entryListFilterStatus(itemCount = 1, totalCount = 2)))
        onListItem("Apples and oranges").assertExists()
        onListItem("Oranges and bananas").assertDoesNotExist()
        // Check filter bar
        composeRule.onNode(hasTestTag(TestTags.ENTRY_LIST_FILTER_QUERY) and hasText("apple")).assertExists()
    }

    @Test
    fun `when the query is set and an entry is selected and the back button is clicked, then the entry is deselected and the list still shows filtered entries`() {
        runBlocking { entries.forEach { entryRepository.insert(it) } }
        launch()
        // Check list
        waitForListTitle(totalCount = 2)
        // Expand filter bar
        composeRule.onNodeWithTag(TestTags.ENTRY_LIST_FILTER_EXPAND).performClick()
        // Set query
        composeRule.onNodeWithTag(TestTags.ENTRY_LIST_FILTER_QUERY).apply {
            performTextClearance()
            performTextInput("apple")
        }
        // Check list
        composeRule.waitUntilAtLeastOneExists(hasTestTag(TestTags.entryListFilterStatus(itemCount = 1, totalCount = 2)))
        onListItem("Apples and oranges").assertExists()
        onListItem("Oranges and bananas").assertDoesNotExist()
        // Select list item
        onListItem("Apples and oranges").performClick()
        onListItem("Apples and oranges").assertIsSelected()
        // Click back
        composeRule.onNodeWithTag(TestTags.ENTRY_LIST_SELECTED_BACK).performClick()
        // Check list
        composeRule.waitUntilAtLeastOneExists(hasTestTag(TestTags.entryListFilterStatus(itemCount = 1, totalCount = 2)))
        onListItem("Apples and oranges").assertExists().assertIsNotSelected()
        onListItem("Oranges and bananas").assertDoesNotExist()
        // Check filter bar
        composeRule.onNode(hasTestTag(TestTags.ENTRY_LIST_FILTER_QUERY) and hasText("apple")).assertExists()
    }

    @Test
    fun `when the query is set, then the list scrolls to top`() {
        runBlocking { orderedEntries.forEach { entryRepository.insert(it) } }
        launch()
        // Scroll to middle
        composeRule.onNodeWithTag(TestTags.ENTRY_LIST_COLUMN).performTouchInput { swipeUp(bottom, centerY) }
        // Check list
        onListItem("Entry 0 minutes ago").assertIsNotDisplayed()
        onListItem("Entry 2 minutes ago").assertIsNotDisplayed()
        onListItem("Entry 4 minutes ago").assertIsDisplayed()
        onListItem("Entry 6 minutes ago").assertIsDisplayed()
        onListItem("Entry 8 minutes ago").assertIsDisplayed()
        onListItem("Entry 10 minutes ago").assertIsDisplayed()
        onListItem("Entry 12 minutes ago").assertIsDisplayed()
        onListItem("Entry 14 minutes ago").assertIsNotDisplayed()
        onListItem("Entry 16 minutes ago").assertIsNotDisplayed()
        // Expand filter bar
        composeRule.onNodeWithTag(TestTags.ENTRY_LIST_FILTER_EXPAND).performClick()
        // Set query
        composeRule.onNodeWithTag(TestTags.ENTRY_LIST_FILTER_QUERY).apply {
            performTextClearance()
            performTextInput("entry")
        }
        // Check list
        println(composeRule.onRoot().printToString())
        onListItem("Entry 0 minutes ago").assertIsDisplayed()
        onListItem("Entry 2 minutes ago").assertIsDisplayed()
        onListItem("Entry 4 minutes ago").assertIsDisplayed()
        onListItem("Entry 6 minutes ago").assertIsNotDisplayed()
        onListItem("Entry 8 minutes ago").assertIsNotDisplayed()
    }

    @Test
    fun `when the query is changed, then the list scrolls to top`() {
        runBlocking { orderedEntries.forEach { entryRepository.insert(it) } }
        launch()
        // Expand filter bar
        composeRule.onNodeWithTag(TestTags.ENTRY_LIST_FILTER_EXPAND).performClick()
        // Set query
        composeRule.onNodeWithTag(TestTags.ENTRY_LIST_FILTER_QUERY).apply {
            performTextClearance()
            performTextInput("entry")
        }
        // Scroll to middle
        composeRule.onNodeWithTag(TestTags.ENTRY_LIST_COLUMN).performTouchInput { swipeUp(bottom, centerY) }
        // Check list
        onListItem("Entry 0 minutes ago").assertIsNotDisplayed()
        onListItem("Entry 2 minutes ago").assertIsNotDisplayed()
        onListItem("Entry 4 minutes ago").assertIsDisplayed()
        onListItem("Entry 6 minutes ago").assertIsDisplayed()
        onListItem("Entry 8 minutes ago").assertIsDisplayed()
        onListItem("Entry 10 minutes ago").assertIsDisplayed()
        onListItem("Entry 12 minutes ago").assertIsNotDisplayed()
        onListItem("Entry 14 minutes ago").assertIsNotDisplayed()
        onListItem("Entry 16 minutes ago").assertIsNotDisplayed()
        // Change query
        composeRule.onNodeWithTag(TestTags.ENTRY_LIST_FILTER_QUERY).apply {
            performTextClearance()
            performTextInput("entry")
        }
        // Check list
        onListItem("Entry 0 minutes ago").assertIsDisplayed()
        onListItem("Entry 2 minutes ago").assertIsDisplayed()
        onListItem("Entry 4 minutes ago").assertIsDisplayed()
        onListItem("Entry 6 minutes ago").assertIsNotDisplayed()
        onListItem("Entry 8 minutes ago").assertIsNotDisplayed()
    }
}
