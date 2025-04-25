package app.traced_it.test

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
open class MainActivityBehaviorTest {

    protected lateinit var device: UiDevice

    protected val packageName = "app.traced_it.debug"
    protected val launchTimeout = 10_000L
    protected val timeout = 5_000L

    @Before
    fun goToLauncher() {
        device = UiDevice.getInstance(
            InstrumentationRegistry.getInstrumentation()
        )
        device.pressHome()
        device.executeShellCommand("monkey -p $packageName 1")
        device.wait(
            Until.hasObject(By.pkg(packageName).depth(0)),
            launchTimeout
        )
    }

    @Test
    fun createsEntry() {
        // Check entries count
        assertTrue(
            device.wait(Until.hasObject(By.text("Your notes (0)")), timeout)
        )

        // New entry
        device.findObject(By.res("entryListNewEntryButton"))?.click()

        // Set content
        device.findObject(By.res("entryDetailContentTextField"))
            ?.text = "Test entry 1"

        // Change unit
        device.findObject(By.res("unitSelectButton"))?.click()
        By.res("unitSelectDropdownMenuItem").hasChild(By.text("fraction")).let {
            assertTrue(device.wait(Until.hasObject(it), timeout))
            device.findObject(it)?.click()
        }

        // Select unit choice
        By.res("unitSelectChoiceText").text("½").let {
            assertTrue(device.wait(Until.hasObject(it), timeout))
            device.findObject(it)?.click()
        }

        // Save
        device.findObject(By.res("entryDetailSaveButton"))?.click()

        // Check entry list
        val listItemSelector =
            By.res("entryListItem").hasChild(By.text("Test entry 1 (½)"))
        assertTrue(device.wait(Until.hasObject(listItemSelector), timeout))

        // Check entries count
        assertTrue(
            device.wait(Until.hasObject(By.text("Your notes (1)")), timeout)
        )
    }

    @Test
    fun createsEntryFromExistingEntry() {
        // Create entry
        device.findObject(By.res("entryListNewEntryButton"))?.click()
        device.findObject(By.res("entryDetailContentTextField"))
            ?.text = "Test entry 3"
        device.findObject(By.res("unitSelectButton"))?.click()
        By.res("unitSelectDropdownMenuItem").hasChild(By.text("fraction")).let {
            assertTrue(device.wait(Until.hasObject(it), timeout))
            device.findObject(it)?.click()
        }
        By.res("unitSelectChoiceText").text("⅓").let {
            assertTrue(device.wait(Until.hasObject(it), timeout))
            device.findObject(it)?.click()
        }
        device.findObject(By.res("entryDetailSaveButton"))?.click()
        val listItemSelector =
            By.res("entryListItem").hasChild(By.text("Test entry 3 (⅓)"))
        assertTrue(device.wait(Until.hasObject(listItemSelector), timeout))

        // Create new entry from an existing entry
        device.findObject(listItemSelector)
            ?.findObject(By.res("entryListItemAddButton"))
            ?.click()

        // Check content
        By.res("entryDetailContentTextField").let {
            assertTrue(device.wait(Until.hasObject(it), timeout))
            assertEquals("Test entry 3", device.findObject(it)?.text)
        }

        // Check and set unit
        assertTrue(device.hasObject(By.res("unitSelectChoiceText").text("⅓")))
        device.findObject(By.res("unitSelectChoiceText").text("¾"))?.click()

        // Save entry
        device.findObject(By.res("entryDetailSaveButton"))?.click()

        // Check entry list
        assertTrue(device.wait(Until.hasObject(listItemSelector), timeout))
        val listItemAddedSelector =
            By.res("entryListItem").hasChild(By.text("Test entry 3 (¾)"))
        assertTrue(device.wait(Until.hasObject(listItemAddedSelector), timeout))

        // Check entries count
        assertTrue(
            device.wait(Until.hasObject(By.text("Your notes (2)")), timeout)
        )
    }

    @Test
    fun updatesEntry() {
        // Create entry
        device.findObject(By.res("entryListNewEntryButton"))?.click()
        device.findObject(By.res("entryDetailContentTextField"))
            ?.text = "Test entry 2"
        By.res("unitSelectChoiceText").text("L").let {
            assertTrue(device.wait(Until.hasObject(it), timeout))
            device.findObject(it)?.click()
        }
        device.findObject(By.res("entryDetailSaveButton"))?.click()
        val listItemSelector =
            By.res("entryListItem").hasChild(By.text("Test entry 2 (L)"))
        assertTrue(device.wait(Until.hasObject(listItemSelector), timeout))

        // Edit entry
        device.findObject(listItemSelector)?.swipe(Direction.RIGHT, 0.5f)
        By.res("entryListItemEditButton").let {
            assertTrue(device.wait(Until.hasObject(it), timeout))
            device.findObject(it)?.click()
        }

        // Set content
        device.findObject(By.res("entryDetailContentTextField"))
            ?.text += " edited"

        // Set entry unit
        device.findObject(By.res("unitSelectChoiceText").text("M"))?.click()

        // Save entry
        device.findObject(By.res("entryDetailSaveButton"))?.click()

        // Check entry list
        By.res("entryListItem").hasChild(By.text("Test entry 2 edited (M)"))
            .let {
                assertTrue(device.wait(Until.hasObject(it), timeout))
            }
    }

    @Test
    fun deletesEntry() {
        // Create entry
        device.findObject(By.res("entryListNewEntryButton"))?.click()
        device.findObject(By.res("entryDetailContentTextField"))
            ?.text = "Test entry 4"
        device.findObject(By.res("entryDetailSaveButton"))?.click()
        val listItemSelector =
            By.res("entryListItem").hasChild(By.text("Test entry 4"))
        assertTrue(device.wait(Until.hasObject(listItemSelector), timeout))

        // Delete entry
        device.findObject(listItemSelector)?.swipe(Direction.LEFT, 0.5f)
        By.res("entryListItemDeleteButton").let {
            assertTrue(device.wait(Until.hasObject(it), timeout))
            device.findObject(it)?.click()
        }

        // Confirm deletion
        By.res("confirmationDialogConfirmButton").let {
            assertTrue(device.wait(Until.hasObject(it), timeout))
            device.findObject(it)?.click()
        }

        // Check entry list
        assertTrue(device.wait(Until.gone(listItemSelector), timeout))

        // Check entries count
        assertTrue(
            device.wait(Until.hasObject(By.text("Your notes (0)")), timeout)
        )
    }

    @Test
    fun filtersEntries() {
        // Create first entry
        device.findObject(By.res("entryListNewEntryButton"))?.click()
        By.res("entryDetailContentTextField").let {
            device.wait(Until.hasObject(it), timeout)
            device.findObject(it)?.text = "Apples Oranges"
        }
        device.findObject(By.res("entryDetailSaveButton"))?.click()
        val listItem1Selector =
            By.res("entryListItem").hasChild(By.text("Apples Oranges"))
        assertTrue(device.wait(Until.hasObject(listItem1Selector), timeout))

        // Create second entry
        device.findObject(By.res("entryListNewEntryButton"))?.click()
        By.res("entryDetailContentTextField").let {
            device.wait(Until.hasObject(it), timeout)
            device.findObject(it)?.text = "Oranges Bananas"
        }
        device.findObject(By.res("entryDetailSaveButton"))?.click()
        val listItem2Selector =
            By.res("entryListItem").hasChild(By.text("Oranges Bananas"))
        assertTrue(
            device.wait(Until.hasObject(listItem2Selector), timeout)
        )

        // Expand filter
        By.res("entryListFilterExpandButton").let {
            device.wait(Until.hasObject(it), timeout)
            device.findObject(it)?.click()
        }

        // Filter by a term that only the first entry contains
        device.findObject(By.res("entryListFilterQueryTextField"))
            ?.text = "apple"

        // Check that only the first entry is displayed
        assertTrue(device.wait(Until.gone(listItem2Selector), timeout))
        assertTrue(device.wait(Until.hasObject(listItem1Selector), timeout))
        assertTrue(
            device.wait(Until.hasObject(By.text("1 of 2 notes")), timeout)
        )

        // Filter by a term that only the second entry contains
        device.findObject(By.res("entryListFilterQueryTextField"))
            ?.text = "banana"

        // Check that only the second entry is displayed
        assertTrue(device.wait(Until.gone(listItem1Selector), timeout))
        assertTrue(device.wait(Until.hasObject(listItem2Selector), timeout))
        assertTrue(
            device.wait(Until.hasObject(By.text("1 of 2 notes")), timeout)
        )

        // Filter by a term that both entries contain
        device.findObject(By.res("entryListFilterQueryTextField"))
            ?.text = "orange"

        // Check that both entries are displayed
        assertTrue(device.wait(Until.hasObject(listItem1Selector), timeout))
        assertTrue(device.wait(Until.hasObject(listItem2Selector), timeout))
        assertTrue(
            device.wait(Until.hasObject(By.text("2 of 2 notes")), timeout)
        )

        // Filter by a term that no entry contains
        device.findObject(By.res("entryListFilterQueryTextField"))
            ?.text = "spam"

        // Check that no entries are displayed
        assertTrue(device.wait(Until.gone(listItem1Selector), timeout))
        assertTrue(device.wait(Until.gone(listItem2Selector), timeout))
        assertTrue(
            device.wait(Until.hasObject(By.text("0 of 2 notes")), timeout)
        )
    }
}
