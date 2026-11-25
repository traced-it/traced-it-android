package app.traced_it.robot

import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.isEnabled
import androidx.compose.ui.test.isNotEnabled
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import app.traced_it.ui.components.TestTags
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.runBlocking
import org.junit.Test

@HiltAndroidTest
class EntryDetailContentRobotTest : BaseRobotTest() {

    @Test
    fun `when content is set in the database, then the content field is filled and focused`() {
        runBlocking { entryRepository.insert(entryWithDoubleUnit) }
        launch()
        // Click update
        onListItem("Dewberry (3.14)").swipeListItemRight()
        onListItemUpdateButton("Dewberry (3.14)").performClick()
        // Check content field
        onDetailContent("Dewberry").assertExists().assertIsFocused()
    }

    @Test
    fun `when content is not set in the database, then the content field is empty and focused`() {
        launch()
        // Click insert
        composeRule.onNodeWithTag(TestTags.ENTRY_LIST_INSERT).performClick()
        // Check content field
        onDetailContent("").assertExists().assertIsFocused()
    }

    @Test
    fun `when the content field is set, then the save button is enabled`() {
        launch()
        // Click insert
        composeRule.onNodeWithTag(TestTags.ENTRY_LIST_INSERT).performClick()
        // Check save button
        composeRule.onNode(hasTestTag(TestTags.ENTRY_DETAIL_SAVE_INSERT) and isNotEnabled()).assertExists()
        // Change content field
        onDetailContent().performTextInput("Dewberry")
        // Check save button
        composeRule.onNode(hasTestTag(TestTags.ENTRY_DETAIL_SAVE_INSERT) and isEnabled()).assertExists()
    }

    @Test
    fun `when the content field clear button is clicked, then the content field is cleared and the save button is disabled`() {
        launch()
        // Click insert
        composeRule.onNodeWithTag(TestTags.ENTRY_LIST_INSERT).performClick()
        // Change content field
        onDetailContent().performTextInput("Dewberry")
        // Click clear button
        composeRule.onNodeWithTag(TestTags.ENTRY_DETAIL_CONTENT_CLEAR).performClick()
        // Check content field
        onDetailContent("").assertExists()
        // Check save button
        composeRule.onNode(hasTestTag(TestTags.ENTRY_DETAIL_SAVE_INSERT) and isNotEnabled()).assertExists()
    }

    @Test
    fun `when content is not set in the database and the content field is set and configuration changes, then the content field does not change`() {
        launch()
        // Click insert
        composeRule.onNodeWithTag(TestTags.ENTRY_LIST_INSERT).performClick()
        // Change content field
        onDetailContent().performTextInput("Dewberry")
        // Change configuration
        rotateDevice()
        // Check content field
        onDetailContent("Dewberry").assertExists()
        // Check save button
        composeRule.onNode(hasTestTag(TestTags.ENTRY_DETAIL_SAVE_INSERT) and isEnabled()).assertExists()
    }
}
