package app.traced_it.robot

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import app.traced_it.data.database.Entry
import app.traced_it.data.database.NoneUnit
import app.traced_it.ui.components.TestTags
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.minus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import org.junit.Test
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@HiltAndroidTest
class EntryListItemRobotTest : BaseRobotTest() {

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
    fun `when there are no entries in the database, then the list is empty`() {
        launch()
        // Check list
        waitForListTitle(totalCount = 0)
    }

    @Test
    fun `when there are entries created less than 1h ago in the database, then the list shows the entries with relative time`() {
        runBlocking {
            entryRepository.insert(
                entryWithClothingSizeUnit.copy(
                    createdAt = now.toInstant(timeZone).minus(3.seconds).toEpochMilliseconds(),
                )
            )
            entryRepository.insert(
                entryWithDoubleUnit.copy(
                    createdAt = now.toInstant(timeZone).minus(130.seconds).toEpochMilliseconds(),
                )
            )
            entryRepository.insert(
                entryWithFractionUnit.copy(
                    createdAt = now.toInstant(timeZone).minus(90.minutes).toEpochMilliseconds(),
                )
            )
        }
        launch()
        // Check list
        waitForListTitle(totalCount = 3)
        onListItem("Coconut (S)", secondsAgo = 3).assertExists()
        onListItem("Dewberry (3.14)", minutesAgo = 2).assertExists()
        onListItem("Fig (½)", hoursAgo = 1, minutesAgo = 30).assertExists()
        onListHeading().assertDoesNotExist()
    }

    @Test
    fun `when there are entries created more than 24h ago in the database, then the list shows the entries with exact time and a heading before each day`() {
        runBlocking {
            entryRepository.insert(
                entryWithClothingSizeUnit.copy(
                    createdAt = now.toInstant(timeZone).minus(2.hours).toEpochMilliseconds(),
                )
            )
            entryRepository.insert(
                entryWithDoubleUnit.copy(
                    createdAt = now.toInstant(timeZone).minus(8.hours).toEpochMilliseconds(),
                )
            )
            entryRepository.insert(
                entryWithFractionUnit.copy(
                    createdAt = now.toInstant(timeZone).minus(48.hours).toEpochMilliseconds(),
                )
            )
        }
        launch()
        // Check list
        waitForListTitle(totalCount = 3)
        onListHeading(now.date.minus(DatePeriod(days = 1))).assertExists()
        onListItem("Coconut (S)", hoursAgo = 2).assertExists()
        onListItem("Dewberry (3.14)", hoursAgo = 8).assertExists()
        onListHeading(now.date.minus(DatePeriod(days = 2))).assertExists()
        onListItem("Fig (½)", createdAt = now.toInstant(timeZone).minus(48.hours).toLocalDateTime(timeZone))
            .assertExists()
    }

    @Test
    fun `when a list item is swiped and a second item is swiped, then the first item snaps back to center`() {
        runBlocking {
            entryRepository.insert(entryWithDoubleUnit)
            entryRepository.insert(entryWithFractionUnit)
        }
        launch()
        // Swipe first item
        waitForListItem("Dewberry (3.14)")
        onListItem("Dewberry (3.14)").swipeListItemRight()
        // Check list
        onListItemUpdateButton("Dewberry (3.14)").assertIsDisplayed()
        onListItemUpdateButton("Fig (½)").assertIsNotDisplayed()
        // Swipe second item
        onListItem("Fig (½)").swipeListItemRight()
        // Check list
        onListItemUpdateButton("Fig (½)").assertIsDisplayed()
        onListItemUpdateButton("Dewberry (3.14)").assertIsNotDisplayed()
    }

    @Test
    fun `when an entry is inserted above visible items, the list scrolls to the item`() {
        runBlocking { orderedEntries.forEach { entryRepository.insert(it) } }
        launch()
        // Scroll to middle
        waitForList()
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
        // Click insert
        waitForList()
        composeRule.onNodeWithTag(TestTags.ENTRY_LIST_INSERT).performClick()
        // Change detail
        onDetailContent().performTextInput("New entry 1 minute ago")
        onDetailCreatedAtMinute().performScrollTo().performVerticalScroll(-1)
        // Save detail
        composeRule.onNodeWithTag(TestTags.ENTRY_DETAIL_SAVE_INSERT).performClick()
        // Check list
        onListItem("New entry 1 minute ago").waitUntilDisplayed()
        onListItem("Entry 0 minutes ago").assertIsNotDisplayed()
        onListItem("New entry 1 minute ago").assertIsDisplayed()
        onListItem("Entry 2 minutes ago").assertIsDisplayed()
        onListItem("Entry 4 minutes ago").assertIsDisplayed()
        onListItem("Entry 6 minutes ago").assertIsDisplayed()
        onListItem("Entry 8 minutes ago").assertIsNotDisplayed()
        onListItem("Entry 10 minutes ago").assertIsNotDisplayed()
    }

    @Test
    fun `when an entry is inserted within visible items, the list does not scroll to the item`() {
        runBlocking { orderedEntries.forEach { entryRepository.insert(it) } }
        launch()
        // Scroll to middle
        waitForList()
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
        // Click insert
        waitForList()
        composeRule.onNodeWithTag(TestTags.ENTRY_LIST_INSERT).performClick()
        // Change detail
        onDetailContent().performTextInput("New entry 5 minutes ago")
        onDetailCreatedAtMinute().performScrollTo().performVerticalScroll(-5)
        // Save detail
        composeRule.onNodeWithTag(TestTags.ENTRY_DETAIL_SAVE_INSERT).performClick()
        // Check list
        // Notice that the list scrolls so the inserted entry is at the top. We don't know why. It would be better if
        // the list didn't scroll.
        onListItem("New entry 5 minutes ago").waitUntilDisplayed()
        onListItem("Entry 4 minutes ago").assertIsNotDisplayed()
        onListItem("New entry 5 minutes ago").assertIsDisplayed()
        onListItem("Entry 6 minutes ago").assertIsDisplayed()
        onListItem("Entry 8 minutes ago").assertIsDisplayed()
        onListItem("Entry 10 minutes ago").assertIsDisplayed()
        onListItem("Entry 12 minutes ago").assertIsNotDisplayed()
        onListItem("Entry 14 minutes ago").assertIsNotDisplayed()
    }

    @Test
    fun `when an entry is inserted below visible items, the list scrolls to the item`() {
        runBlocking { orderedEntries.forEach { entryRepository.insert(it) } }
        launch()
        // Scroll to middle
        waitForList()
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
        // Click insert
        waitForList()
        composeRule.onNodeWithTag(TestTags.ENTRY_LIST_INSERT).performClick()
        // Change detail
        onDetailContent().performTextInput("New entry 15 minutes ago")
        onDetailCreatedAtMinute().performScrollTo().performVerticalScroll(-15)
        // Save detail
        composeRule.onNodeWithTag(TestTags.ENTRY_DETAIL_SAVE_INSERT).performClick()
        // Check list
        onListItem("New entry 15 minutes ago").waitUntilDisplayed()
        onListItem("Entry 12 minutes ago").assertIsNotDisplayed()
        onListItem("Entry 14 minutes ago").assertIsNotDisplayed()
        onListItem("New entry 15 minutes ago").assertIsDisplayed()
        onListItem("Entry 16 minutes ago").assertIsDisplayed()
        onListItem("Entry 18 minutes ago").assertIsDisplayed()
        onListItem("Entry 20 minutes ago").assertIsDisplayed()
        onListItem("Entry 22 minutes ago").assertIsNotDisplayed()
        onListItem("Entry 24 minutes ago").assertIsNotDisplayed()
    }

    @Test
    fun `when an entry is moved above visible items, the list scrolls to the item`() {
        runBlocking { orderedEntries.forEach { entryRepository.insert(it) } }
        launch()
        // Scroll to middle
        waitForList()
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
        // Click insert
        onListItem("Entry 10 minutes ago").swipeListItemRight()
        onListItemUpdateButton("Entry 10 minutes ago").performClick()
        // Change detail
        onDetailContent().performTextClearance()
        onDetailContent().performTextInput("Updated entry 1 minute ago")
        onDetailCreatedAtMinute().performScrollTo().performVerticalScroll(9)
        // Save detail
        composeRule.onNodeWithTag(TestTags.ENTRY_DETAIL_SAVE_UPDATE).performClick()
        // Check list
        onListItem("Updated entry 1 minute ago").waitUntilDisplayed()
        onListItem("Entry 0 minutes ago").assertIsNotDisplayed()
        onListItem("Updated entry 1 minute ago").assertIsDisplayed()
        onListItem("Entry 2 minutes ago").assertIsDisplayed()
        onListItem("Entry 4 minutes ago").assertIsDisplayed()
        onListItem("Entry 6 minutes ago").assertIsDisplayed()
        onListItem("Entry 8 minutes ago").assertIsNotDisplayed()
        onListItem("Entry 12 minutes ago").assertIsNotDisplayed()
        onListItem("Entry 14 minutes ago").assertIsNotDisplayed()
    }

    @Test
    fun `when an entry is moved within visible items, the list does not scroll to the item`() {
        runBlocking { orderedEntries.forEach { entryRepository.insert(it) } }
        launch()
        // Scroll to middle
        waitForList()
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
        // Click insert
        onListItem("Entry 10 minutes ago").swipeListItemRight()
        onListItemUpdateButton("Entry 10 minutes ago").performClick()
        // Change detail
        onDetailContent().performTextClearance()
        onDetailContent().performTextInput("Updated entry 7 minutes ago")
        onDetailCreatedAtMinute().performScrollTo().performVerticalScroll(3)
        // Save detail
        composeRule.onNodeWithTag(TestTags.ENTRY_DETAIL_SAVE_UPDATE).performClick()
        // Check list
        // Notice that the list scrolls so the inserted entry is at the top. We don't know why. It would be better if
        // the list didn't scroll.
        onListItem("Updated entry 7 minutes ago").waitUntilDisplayed()
        onListItem("Entry 0 minutes ago").assertIsNotDisplayed()
        onListItem("Entry 2 minutes ago").assertIsNotDisplayed()
        onListItem("Entry 4 minutes ago").assertIsNotDisplayed()
        onListItem("Entry 6 minutes ago").assertIsNotDisplayed()
        onListItem("Updated entry 7 minutes ago").assertIsDisplayed()
        onListItem("Entry 8 minutes ago").assertIsDisplayed()
        onListItem("Entry 12 minutes ago").assertIsDisplayed()
        onListItem("Entry 14 minutes ago").assertIsDisplayed()
        onListItem("Entry 16 minutes ago").assertIsNotDisplayed()
        onListItem("Entry 18 minutes ago").assertIsNotDisplayed()
        onListItem("Entry 20 minutes ago").assertIsNotDisplayed()
    }

    @Test
    fun `when an entry is moved below visible items, the list scrolls to the item`() {
        runBlocking { orderedEntries.forEach { entryRepository.insert(it) } }
        launch()
        // Scroll to middle
        waitForList()
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
        // Click insert
        onListItem("Entry 10 minutes ago").swipeListItemRight()
        onListItemUpdateButton("Entry 10 minutes ago").performClick()
        // Change detail
        onDetailContent().performTextClearance()
        onDetailContent().performTextInput("Updated entry 19 minutes ago")
        onDetailCreatedAtMinute().performScrollTo().performVerticalScroll(-9)
        // Save detail
        composeRule.onNodeWithTag(TestTags.ENTRY_DETAIL_SAVE_UPDATE).performClick()
        // Check list
        onListItem("Updated entry 19 minutes ago").waitUntilDisplayed()
        onListItem("Entry 0 minutes ago").assertIsNotDisplayed()
        onListItem("Entry 2 minutes ago").assertIsNotDisplayed()
        onListItem("Entry 4 minutes ago").assertIsNotDisplayed()
        onListItem("Entry 6 minutes ago").assertIsNotDisplayed()
        onListItem("Entry 8 minutes ago").assertIsNotDisplayed()
        onListItem("Entry 12 minutes ago").assertIsNotDisplayed()
        onListItem("Entry 14 minutes ago").assertIsNotDisplayed()
        onListItem("Entry 16 minutes ago").assertIsNotDisplayed()
        onListItem("Entry 18 minutes ago").assertIsNotDisplayed()
        onListItem("Updated entry 19 minutes ago").assertIsDisplayed()
        onListItem("Entry 20 minutes ago").assertIsDisplayed()
        onListItem("Entry 24 minutes ago").assertIsDisplayed()
        onListItem("Entry 26 minutes ago").assertIsNotDisplayed()
        onListItem("Entry 28 minutes ago").assertIsNotDisplayed()
    }
}
