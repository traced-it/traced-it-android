package app.traced_it

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.UiAutomatorTestScope
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.onElement
import androidx.test.uiautomator.onElementOrNull
import androidx.test.uiautomator.textAsString
import androidx.test.uiautomator.uiAutomator
import app.traced_it.data.database.ClothingSizeUnit
import app.traced_it.data.database.EntryUnit
import app.traced_it.data.database.FractionUnit
import app.traced_it.ui.components.TestTags
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.math.abs

@RunWith(AndroidJUnit4::class)
class MainBehaviorTest {

    @Before
    fun before() = uiAutomator {
        pressHome()
        // Use shell command instead of startActivity() to support Xiaomi
        device.executeShellCommand("monkey -p ${BuildConfig.APPLICATION_ID} 1")
        waitForAppToBeVisible(BuildConfig.APPLICATION_ID)
    }

    @Test
    fun insertsEntry() = uiAutomator {
        // Check list
        assertTotalCount(0)
        // Click insert
        onElement { viewIdResourceName == TestTags.ENTRY_LIST_INSERT }.click()
        // Change detail
        onElement { viewIdResourceName == TestTags.ENTRY_DETAIL_CONTENT }.setText("Test entry 1")
        pressBack() // Close IME
        onElement { viewIdResourceName == TestTags.ENTRY_DETAIL_UNIT_MENU }.click()
        onElement { viewIdResourceName == TestTags.entryDetailUnitMenuItem(FractionUnit) }.click()
        onDetailUnitChoice(FractionUnit, "½").click()
        // Save detail
        onElement { viewIdResourceName == TestTags.ENTRY_DETAIL_SAVE_INSERT }.click()
        // Check list
        assertTotalCount(1)
        assertListItemExists("Test entry 1 (½)")
    }

    @Test
    fun repeatsEntry() = uiAutomator {
        // Click insert
        onElement { viewIdResourceName == TestTags.ENTRY_LIST_INSERT }.click()
        // Change detail
        onElement { viewIdResourceName == TestTags.ENTRY_DETAIL_CONTENT }.setText("Test entry 3")
        pressBack() // Close IME
        onElement { viewIdResourceName == TestTags.ENTRY_DETAIL_UNIT_MENU }.click()
        onElement { viewIdResourceName == TestTags.entryDetailUnitMenuItem(FractionUnit) }.click()
        onDetailUnitChoice(FractionUnit, "⅓").click()
        // Save detail
        onElement { viewIdResourceName == TestTags.ENTRY_DETAIL_SAVE_INSERT }.click()
        // Click repeat
        onVisibleListItem("Test entry 3 (⅓)").repeatItem()
        // Change detail
        onElement { viewIdResourceName == TestTags.ENTRY_DETAIL_CONTENT && textAsString() == "Test entry 3" }
        pressBack() // Close IME
        onDetailUnitChoice(FractionUnit, "¾").click()
        // Save detail
        onElement { viewIdResourceName == TestTags.ENTRY_DETAIL_SAVE_INSERT }.click()
        // Check list
        assertTotalCount(2)
        assertListItemExists("Test entry 3 (⅓)")
        assertListItemExists("Test entry 3 (¾)")
    }

    @Test
    fun updatesEntry() = uiAutomator {
        // Click insert
        onElement { viewIdResourceName == TestTags.ENTRY_LIST_INSERT }.click()
        // Change detail
        onElement { viewIdResourceName == TestTags.ENTRY_DETAIL_CONTENT }.setText("Test entry 2")
        pressBack() // Close IME
        onDetailUnitChoice(ClothingSizeUnit, "L").click()
        // Save detail
        onElement { viewIdResourceName == TestTags.ENTRY_DETAIL_SAVE_INSERT }.click()
        // Click update
        onVisibleListItem("Test entry 2 (L)").updateItem()
        // Change detail
        onElement { viewIdResourceName == TestTags.ENTRY_DETAIL_CONTENT }.apply { setText("$text edited") }
        pressBack() // Close IME
        onDetailUnitChoice(ClothingSizeUnit, "M").click()
        onElement { viewIdResourceName == TestTags.DATE_TIME_PICKER_DATE }.scrollVertically(-1)
        onElement { viewIdResourceName == TestTags.DATE_TIME_PICKER_HOUR }.scrollVertically(1)
        onElement { viewIdResourceName == TestTags.DATE_TIME_PICKER_MINUTE }.scrollVertically(1)
        // Save detail
        onElement { viewIdResourceName == TestTags.ENTRY_DETAIL_SAVE_UPDATE }.click()
        // Check list
        onVisibleListItem("Test entry 2 edited (M)")
            .assertListItemTime(expectedHoursAgo = 22, expectedMinutesAgo = 59)
    }

    @Test
    fun deletesEntry() = uiAutomator {
        // Click insert
        onElement { viewIdResourceName == TestTags.ENTRY_LIST_INSERT }.click()
        // Change detail
        onElement { viewIdResourceName == TestTags.ENTRY_DETAIL_CONTENT }.setText("Test entry 4")
        pressBack() // Close IME
        // Save detail
        onElement { viewIdResourceName == TestTags.ENTRY_DETAIL_SAVE_INSERT }.click()
        // Click delete
        onVisibleListItem("Test entry 4").deleteItem()
        // Check list
        assertTotalCount(0)
        assertListItemDoesNotExist("Test entry 4")
    }

    @Test
    fun filtersEntries() = uiAutomator {
        // Click insert
        onElement { viewIdResourceName == TestTags.ENTRY_LIST_INSERT }.click()
        // Change detail
        onElement { viewIdResourceName == TestTags.ENTRY_DETAIL_CONTENT }.setText("Apples Oranges")
        pressBack() // Close IME
        // Save detail
        onElement { viewIdResourceName == TestTags.ENTRY_DETAIL_SAVE_INSERT }.click()
        // Click insert
        onElement { viewIdResourceName == TestTags.ENTRY_LIST_INSERT }.click()
        // Change detail
        onElement { viewIdResourceName == TestTags.ENTRY_DETAIL_CONTENT }.setText("Oranges Bananas")
        pressBack() // Close IME
        // Save detail
        onElement { viewIdResourceName == TestTags.ENTRY_DETAIL_SAVE_INSERT }.click()
        // Expand filter bar
        onElement { viewIdResourceName == TestTags.ENTRY_LIST_FILTER_EXPAND }.click()
        // Set query
        onElement { viewIdResourceName == TestTags.ENTRY_LIST_FILTER_QUERY }.setText("apple")
        pressBack() // Close IME
        waitForStableInActiveWindow()
        // Check list
        onElement { viewIdResourceName == TestTags.entryListFilterStatus(1, 2) }
        assertListItemExists("Apples Oranges")
        assertListItemDoesNotExist("Oranges Bananas")
    }

    @Test
    fun scrollsToEntry() = uiAutomator {
        // Insert entries below visible items
        repeat(7) { i ->
            val daysAgo = i * 2
            // Click insert
            onElement { viewIdResourceName == TestTags.ENTRY_LIST_INSERT }.click()
            // Change detail
            onElement { viewIdResourceName == TestTags.ENTRY_DETAIL_CONTENT }.setText("Test entry $daysAgo days ago")
            pressBack() // Close IME
            onElement { viewIdResourceName == TestTags.DATE_TIME_PICKER_DATE }.scrollVertically(-daysAgo)
            // Save detail
            onElement { viewIdResourceName == TestTags.ENTRY_DETAIL_SAVE_INSERT }.click()
            // Check list
            onVisibleListItem("Test entry $daysAgo days ago")
        }

        // Insert entry within visible items
        // Click insert
        onElement { viewIdResourceName == TestTags.ENTRY_LIST_INSERT }.click()
        // Change detail
        onElement { viewIdResourceName == TestTags.ENTRY_DETAIL_CONTENT }.setText("New entry 9 days ago")
        pressBack() // Close IME
        onElement { viewIdResourceName == TestTags.DATE_TIME_PICKER_DATE }.scrollVertically(-9)
        // Save detail
        onElement { viewIdResourceName == TestTags.ENTRY_DETAIL_SAVE_INSERT }.click()
        // Check list
        onVisibleListItem("New entry 9 days ago")

        // Insert entry above visible items
        // Click insert
        onElement { viewIdResourceName == TestTags.ENTRY_LIST_INSERT }.click()
        // Change detail
        onElement { viewIdResourceName == TestTags.ENTRY_DETAIL_CONTENT }.setText("New entry 1 day ago")
        pressBack() // Close IME
        onElement { viewIdResourceName == TestTags.DATE_TIME_PICKER_DATE }.scrollVertically(-1)
        // Save detail
        onElement { viewIdResourceName == TestTags.ENTRY_DETAIL_SAVE_INSERT }.click()
        // Check list
        onVisibleListItem("New entry 1 day ago")

        // Move entry above visible items
        // Click update
        onVisibleListItem("New entry 1 day ago").updateItem()
        // Change detail
        onElement { viewIdResourceName == TestTags.ENTRY_DETAIL_CONTENT }.setText("Updated entry 13 days ago")
        pressBack() // Close IME
        onElement { viewIdResourceName == TestTags.DATE_TIME_PICKER_DATE }.scrollVertically(-12)
        // Save detail
        onElement { viewIdResourceName == TestTags.ENTRY_DETAIL_SAVE_UPDATE }.click()
        // Check list
        onVisibleListItem("Updated entry 13 days ago")

        // Move entry within visible items
        // Click update
        onVisibleListItem("Updated entry 13 days ago").updateItem()
        // Change detail
        onElement { viewIdResourceName == TestTags.ENTRY_DETAIL_CONTENT }.setText("Updated entry 11 days ago")
        pressBack() // Close IME
        onElement { viewIdResourceName == TestTags.DATE_TIME_PICKER_DATE }.scrollVertically(2)
        // Save detail
        onElement { viewIdResourceName == TestTags.ENTRY_DETAIL_SAVE_UPDATE }.click()
        // Check list
        onVisibleListItem("Updated entry 11 days ago")

        // Move entry above visible items
        // Click update
        onVisibleListItem("Updated entry 11 days ago").updateItem()
        // Change detail
        onElement { viewIdResourceName == TestTags.ENTRY_DETAIL_CONTENT }.setText("Updated entry 3 days ago")
        pressBack() // Close IME
        onElement { viewIdResourceName == TestTags.DATE_TIME_PICKER_DATE }.scrollVertically(8)
        // Save detail
        onElement { viewIdResourceName == TestTags.ENTRY_DETAIL_SAVE_UPDATE }.click()
        // Check list
        onVisibleListItem("Updated entry 3 days ago")
    }

    private fun UiAutomatorTestScope.assertTotalCount(expectedTotalCount: Int) {
        onElement {
            viewIdResourceName == TestTags.ENTRY_LIST_TITLE &&
                textAsString()?.contains("($expectedTotalCount)") == true
        }
    }

    private fun UiAutomatorTestScope.assertListItemExists(expectedText: String) {
        onElement { viewIdResourceName == TestTags.ENTRY_LIST_ITEM_CONTENT && textAsString() == expectedText }
    }

    private fun UiAutomatorTestScope.assertListItemDoesNotExist(expectedText: String) {
        assertNull(onElementOrNull(ELEMENT_DOES_NOT_EXIST_TIMEOUT) {
            viewIdResourceName == TestTags.ENTRY_LIST_ITEM_CONTENT && textAsString() == expectedText
        })
    }

    @Suppress("SameParameterValue")
    private fun UiObject2.assertListItemTime(expectedHoursAgo: Int, expectedMinutesAgo: Int) {
        val pattern = Regex("""(^|\s)$expectedHoursAgo\s.*\s$expectedMinutesAgo(\s|$)""")
        val text = onElement { viewIdResourceName == TestTags.ENTRY_LIST_ITEM_TIME }.text
        assertTrue("Expected $text to contain $pattern", text.contains(pattern))
    }

    private fun UiAutomatorTestScope.onVisibleListItem(content: String): UiObject2 =
        onElement {
            viewIdResourceName == TestTags.ENTRY_LIST_ITEM &&
                isVisibleToUser &&
                onElementOrNull(50) {
                    viewIdResourceName == TestTags.ENTRY_LIST_ITEM_CONTENT && textAsString() == content
                } != null
        }

    private fun UiObject2.deleteItem() {
        swipe(Direction.LEFT, 0.5f)
        parent.onElement { viewIdResourceName == TestTags.ENTRY_LIST_ITEM_DELETE }.click()
    }

    private fun UiObject2.repeatItem() {
        onElement { viewIdResourceName == TestTags.ENTRY_LIST_ITEM_REPEAT }.click()
    }

    private fun UiObject2.updateItem() {
        swipe(Direction.RIGHT, 0.5f)
        parent.onElement { viewIdResourceName == TestTags.ENTRY_LIST_ITEM_UPDATE }.click()
    }

    private fun UiAutomatorTestScope.onDetailUnitChoice(amountUnit: EntryUnit, choiceName: String): UiObject2 =
        onElement {
            viewIdResourceName == TestTags.entryDetailUnitChoice(amountUnit) &&
                this.onElementOrNull(50) { textAsString() == choiceName } != null
        }

    private fun UiObject2.scrollVertically(steps: Int) {
        when {
            steps < 0 -> repeat(abs(steps)) {
                scroll(Direction.UP, 0.5f)
            }

            steps > 0 -> repeat(steps) {
                scroll(Direction.DOWN, 0.5f)
            }
        }
    }

    private companion object {
        private const val ELEMENT_DOES_NOT_EXIST_TIMEOUT = 500L
    }
}
