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
        device =
            UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        device.pressHome()
        device.executeShellCommand("monkey -p $packageName 1")
        device.wait(
            Until.hasObject(By.pkg(packageName).depth(0)),
            launchTimeout
        )
    }

    @Test
    fun createsEntry() {
        // New entry
        device.findObject(By.res("entryListNewEntryButton"))?.click()

        // Set content
        device.findObject(By.res("entryDetailContentTextField"))
            ?.text = "Test entry 1"

        // Change unit
        device.findObject(By.res("unitSelectButton"))?.click()
        val unitMenuItemSelector = By.res("unitSelectDropdownMenuItem").hasChild(
            By.text("portion")
        )
        assertTrue(
            device.wait(Until.hasObject(unitMenuItemSelector), timeout)
        )
        device.findObject(unitMenuItemSelector)?.click()

        // Select unit choice
        val unitChoiceTextSelector = By.res("unitSelectChoiceText").text("2x")
        assertTrue(
            device.wait(Until.hasObject(unitChoiceTextSelector), timeout)
        )
        device.findObject(unitChoiceTextSelector)?.click()

        // Save
        device.findObject(By.res("entryDetailSaveButton"))?.click()

        // Check entry list
        val listItemSelector = By.res("entryListItem").hasChild(
            By.text("Test entry 1 (2x)")
        )
        assertTrue(
            device.wait(Until.hasObject(listItemSelector), timeout)
        )
    }

    @Test
    fun createsEntryFromExistingEntry() {
        // Create entry
        device.findObject(By.res("entryListNewEntryButton"))?.click()
        device.findObject(By.res("entryDetailContentTextField"))
            ?.text = "Test entry 3"
        device.findObject(By.res("unitSelectButton"))?.click()
        val unitMenuItemSelector = By.res("unitSelectDropdownMenuItem").hasChild(
            By.text("portion")
        )
        assertTrue(
            device.wait(Until.hasObject(unitMenuItemSelector), timeout)
        )
        device.findObject(unitMenuItemSelector)?.click()
        val unitChoiceTextSelector = By.res("unitSelectChoiceText").text("3x")
        assertTrue(
            device.wait(Until.hasObject(unitChoiceTextSelector), timeout)
        )
        device.findObject(unitChoiceTextSelector)?.click()
        device.findObject(By.res("entryDetailSaveButton"))?.click()
        val listItemSelector = By.res("entryListItem").hasChild(
            By.text("Test entry 3 (3x)")
        )
        assertTrue(
            device.wait(Until.hasObject(listItemSelector), timeout)
        )

        // Create new entry form an existing entry
        device.findObject(listItemSelector)
            ?.findObject(By.res("entryListItemAddButton"))
            ?.click()

        // Check content
        val contentSelector = By.res("entryDetailContentTextField")
        assertTrue(
            device.wait(Until.hasObject(contentSelector), timeout)
        )
        assertEquals("Test entry 3", device.findObject(contentSelector)?.text)

        // Check and set unit
        assertTrue(device.hasObject(By.res("unitSelectChoiceText").text("3x")))
        device.findObject(By.res("unitSelectChoiceText").text("4x"))?.click()

        // Save entry
        device.findObject(By.res("entryDetailSaveButton"))?.click()

        // Check entry list
        assertTrue(
            device.wait(Until.hasObject(listItemSelector), timeout)
        )
        val listItemAddedSelector = By.res("entryListItem").hasChild(
            By.text("Test entry 3 (4x)")
        )
        assertTrue(
            device.wait(Until.hasObject(listItemAddedSelector), timeout)
        )
    }

    @Test
    fun updatesEntry() {
        // Create entry
        device.findObject(By.res("entryListNewEntryButton"))?.click()
        device.findObject(By.res("entryDetailContentTextField"))
            ?.text = "Test entry 2"
        val unitChoiceTextSelector = By.res("unitSelectChoiceText").text("L")
        assertTrue(
            device.wait(Until.hasObject(unitChoiceTextSelector), timeout)
        )
        device.findObject(unitChoiceTextSelector)?.click()
        device.findObject(By.res("entryDetailSaveButton"))?.click()
        val listItemSelector = By.res("entryListItem").hasChild(
            By.text("Test entry 2 (L)")
        )
        assertTrue(
            device.wait(Until.hasObject(listItemSelector), timeout)
        )

        // Edit entry
        device.findObject(listItemSelector)?.swipe(Direction.RIGHT, 0.5f)
        val editButtonSelector = By.res("entryListItemEditButton")
        assertTrue(
            device.wait(Until.hasObject(editButtonSelector), timeout)
        )
        device.findObject(editButtonSelector)?.click()

        // Set content
        device.findObject(By.res("entryDetailContentTextField"))
            ?.text += " edited"

        // Set entry unit
        device.findObject(By.res("unitSelectChoiceText").text("M"))?.click()

        // Save entry
        device.findObject(By.res("entryDetailSaveButton"))?.click()

        // Check entry list
        val listItemEditedSelector = By.res("entryListItem").hasChild(
            By.text("Test entry 2 edited (M)")
        )
        assertTrue(
            device.wait(Until.hasObject(listItemEditedSelector), timeout)
        )
    }

    @Test
    fun deletesEntry() {
        // Create entry
        device.findObject(By.res("entryListNewEntryButton"))?.click()
        device.findObject(By.res("entryDetailContentTextField"))
            ?.text = "Test entry 4"
        device.findObject(By.res("entryDetailSaveButton"))?.click()
        val listItemSelector = By.res("entryListItem").hasChild(
            By.text("Test entry 4")
        )
        assertTrue(
            device.wait(Until.hasObject(listItemSelector), timeout)
        )

        // Delete entry
        device.findObject(listItemSelector)?.swipe(Direction.LEFT, 0.5f)
        val deleteButtonSelector = By.res("entryListItemDeleteButton")
        assertTrue(
            device.wait(Until.hasObject(deleteButtonSelector), timeout)
        )
        device.findObject(deleteButtonSelector)?.click()

        // Confirm deletion
        val dialogConfirmButton = By.res("confirmationDialogConfirmButton")
        assertTrue(
            device.wait(Until.hasObject(dialogConfirmButton), timeout)
        )
        device.findObject(dialogConfirmButton)?.click()

        // Check entry list
        assertTrue(
            device.wait(Until.gone(listItemSelector), timeout)
        )
    }
}
