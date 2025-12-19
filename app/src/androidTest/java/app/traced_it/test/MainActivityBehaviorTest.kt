package app.traced_it.test

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.onElement
import androidx.test.uiautomator.textAsString
import androidx.test.uiautomator.uiAutomator
import app.traced_it.data.local.database.fractionUnit
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
open class MainActivityBehaviorTest {

    companion object {
        const val PACKAGE_NAME = "app.traced_it.debug"
        const val ELEMENT_DOES_NOT_EXIST_TIMEOUT = 500L
    }

    @Before
    fun before() = uiAutomator {
        pressHome()
        // Use shell command instead of startActivity() to support Xiaomi
        device.executeShellCommand("monkey -p $PACKAGE_NAME 1")
        waitForAppToBeVisible(PACKAGE_NAME)
    }

    @Test
    fun createsEntry() = uiAutomator {
        // Check entries count
        onElement { textAsString() == "Your notes (0)" }

        // New entry
        onElement { viewIdResourceName == "entryListNewEntryButton" }.click()

        // Set content
        onElement { viewIdResourceName == "entryDetailContentTextField" }.setText("Test entry 1")
        pressBack() // Close IME

        // Set unit
        onElement { viewIdResourceName == "unitSelectButton" }.click()
        onElement { viewIdResourceName == "unitSelectDropdownMenuItem_${fractionUnit.id}" }.click()
        onElement { viewIdResourceName == "unitSelectChoiceText" && textAsString() == "½" }.click()

        // Save
        onElement { viewIdResourceName == "entryDetailSaveButton" }.click()

        // Check entry list
        onElement { textAsString() == "Your notes (1)" }
        onElement { viewIdResourceName == "entryListItem" && onElementOrNull { textAsString() == "Test entry 1 (½)" } != null }
    }

    @Test
    fun createsEntryFromExistingEntry() = uiAutomator {
        // Create entry
        onElement { viewIdResourceName == "entryListNewEntryButton" }.click()
        onElement { viewIdResourceName == "entryDetailContentTextField" }.setText("Test entry 3")
        pressBack() // Close IME
        onElement { viewIdResourceName == "unitSelectButton" }.click()
        onElement { viewIdResourceName == "unitSelectDropdownMenuItem_${fractionUnit.id}" }.click()
        onElement { viewIdResourceName == "unitSelectChoiceText" && textAsString() == "⅓" }.click()
        onElement { viewIdResourceName == "entryDetailSaveButton" }.click()

        // Create new entry from an existing entry
        onElement { viewIdResourceName == "entryListItem" && onElementOrNull { textAsString() == "Test entry 3 (⅓)" } != null }
            .onElement { viewIdResourceName == "entryListItemAddButton" }
            .click()

        // Check content
        onElement { viewIdResourceName == "entryDetailContentTextField" && textAsString() == "Test entry 3" }
        pressBack() // Close IME

        // Set unit
        onElement { viewIdResourceName == "unitSelectChoiceText" && textAsString() == "¾" }.click()

        // Save
        onElement { viewIdResourceName == "entryDetailSaveButton" }.click()

        // Check entry list
        onElement { textAsString() == "Your notes (2)" }
        onElement { viewIdResourceName == "entryListItem" && onElementOrNull { textAsString() == "Test entry 3 (⅓)" } != null }
        onElement { viewIdResourceName == "entryListItem" && onElementOrNull { textAsString() == "Test entry 3 (¾)" } != null }
    }

    @Test
    fun updatesEntry() = uiAutomator {
        // Create entry
        onElement { viewIdResourceName == "entryListNewEntryButton" }.click()
        onElement { viewIdResourceName == "entryDetailContentTextField" }.setText("Test entry 2")
        pressBack() // Close IME
        onElement { viewIdResourceName == "unitSelectChoiceText" && textAsString() == "L" }.click()
        onElement { viewIdResourceName == "entryDetailSaveButton" }.click()

        // Edit entry
        onElement { viewIdResourceName == "entryListItem" && onElementOrNull { textAsString() == "Test entry 2 (L)" } != null }
            .swipe(Direction.RIGHT, 0.5f)
        onElement { viewIdResourceName == "entryListItemEditButton" }.click()

        // Set content
        onElement { viewIdResourceName == "entryDetailContentTextField" }.apply { setText("$text edited") }
        pressBack() // Close IME

        // Set unit
        onElement { viewIdResourceName == "unitSelectChoiceText" && textAsString() == "M" }.click()

        // Set created at
        onElement { viewIdResourceName == "tracedTimePickerDaySegment" }
            .scroll(Direction.UP,0.5f) // Minus one day
        onElement { viewIdResourceName == "tracedTimePickerHourSegment" }
            .scroll(Direction.DOWN,0.5f) // Plus one hour
        onElement { viewIdResourceName == "tracedTimePickerMinuteSegment" }
            .scroll(Direction.DOWN,0.5f) // Plus one minute

        // Save
        onElement { viewIdResourceName == "entryDetailSaveButton" }.click()

        // Check entry list
        onElement { viewIdResourceName == "entryListItem" && onElementOrNull { textAsString() == "Test entry 2 edited (M)" } != null }
        onElement { viewIdResourceName == "entryListItem" && onElementOrNull { textAsString() == "22 h 59 min ago" } != null }
    }

    @Test
    fun deletesEntry() = uiAutomator {
        // Create entry
        onElement { viewIdResourceName == "entryListNewEntryButton" }.click()
        onElement { viewIdResourceName == "entryDetailContentTextField" }.setText("Test entry 4")
        pressBack() // Close IME
        onElement { viewIdResourceName == "entryDetailSaveButton" }.click()

        // Delete entry
        onElement { viewIdResourceName == "entryListItem" && onElementOrNull { textAsString() == "Test entry 4" } != null }
            .swipe(Direction.LEFT, 0.5f)
        onElement { viewIdResourceName == "entryListItemDeleteButton" }.click()

        // Check entry list
        onElement { textAsString() == "Your notes (0)" }
        assertNull(onElementOrNull(ELEMENT_DOES_NOT_EXIST_TIMEOUT) { viewIdResourceName == "entryListItem" && onElementOrNull { textAsString() == "Test entry 4" } != null })
    }

    @Test
    fun filtersEntries() = uiAutomator {
        // Create first entry
        onElement { viewIdResourceName == "entryListNewEntryButton" }.click()
        onElement { viewIdResourceName == "entryDetailContentTextField" }.setText("Apples Oranges")
        pressBack() // Close IME
        onElement { viewIdResourceName == "entryDetailSaveButton" }.click()

        // Create second entry
        onElement { viewIdResourceName == "entryListNewEntryButton" }.click()
        onElement { viewIdResourceName == "entryDetailContentTextField" }.setText("Oranges Bananas")
        pressBack() // Close IME
        onElement { viewIdResourceName == "entryDetailSaveButton" }.click()

        // Expand filter
        onElement { viewIdResourceName == "entryListFilterExpandButton" }.click()

        // Filter by a term that only the first entry contains
        onElement { viewIdResourceName == "entryListFilterQueryTextField" }.setText("apple")
        pressBack() // Close IME

        // Check that only the first entry is displayed
        onElement { textAsString() == "1 note out of 2" }
        onElement { viewIdResourceName == "entryListItem" && onElementOrNull { textAsString() == "Apples Oranges" } != null }
        assertNull(onElementOrNull(ELEMENT_DOES_NOT_EXIST_TIMEOUT) { viewIdResourceName == "entryListItem" && onElementOrNull { textAsString() == "Oranges Bananas" } != null })

        // Filter by a term that only the second entry contains
        onElement { viewIdResourceName == "entryListFilterQueryTextField" }.setText("banana")

        // Check that only the second entry is displayed
        onElement { textAsString() == "1 note out of 2" }
        assertNull(onElementOrNull(ELEMENT_DOES_NOT_EXIST_TIMEOUT) { viewIdResourceName == "entryListItem" && onElementOrNull { textAsString() == "Apples Oranges" } != null })
        onElement { viewIdResourceName == "entryListItem" && onElementOrNull { textAsString() == "Oranges Bananas" } != null }

        // Filter by a term that both entries contain
        onElement { viewIdResourceName == "entryListFilterQueryTextField" }.setText("orange")

        // Check that both entries are displayed
        onElement { textAsString() == "2 notes out of 2" }
        onElement { viewIdResourceName == "entryListItem" && onElementOrNull { textAsString() == "Apples Oranges" } != null }
        onElement { viewIdResourceName == "entryListItem" && onElementOrNull { textAsString() == "Oranges Bananas" } != null }

        // Filter by a term that no entry contains
        onElement { viewIdResourceName == "entryListFilterQueryTextField" }.setText("spam")

        // Check that no entries are displayed
        onElement { textAsString() == "0 notes out of 2" }
        assertNull(onElementOrNull(ELEMENT_DOES_NOT_EXIST_TIMEOUT) { viewIdResourceName == "entryListItem" && onElementOrNull { textAsString() == "Apples Oranges" } != null })
        assertNull(onElementOrNull(ELEMENT_DOES_NOT_EXIST_TIMEOUT) { viewIdResourceName == "entryListItem" && onElementOrNull { textAsString() == "Oranges Bananas" } != null })
    }
}
