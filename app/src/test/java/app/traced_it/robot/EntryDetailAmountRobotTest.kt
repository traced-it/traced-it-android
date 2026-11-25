package app.traced_it.robot

import androidx.compose.ui.test.assertAll
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.isNotSelected
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import app.traced_it.data.database.ClothingSizeUnit
import app.traced_it.data.database.DoubleUnit
import app.traced_it.data.database.FractionUnit
import app.traced_it.ui.components.TestTags
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.runBlocking
import org.junit.Test

@HiltAndroidTest
class EntryDetailAmountRobotTest : BaseRobotTest() {

    @Test
    fun `when unit and amount are set in the database, then the correct unit is selected and the amount field is set`() {
        runBlocking {
            entryRepository.insert(entryWithClothingSizeUnit)
            entryRepository.insert(entryWithDoubleUnit)
            entryRepository.insert(entryWithFractionUnit)
        }
        launch()

        // Clothing size: Click update
        onListItem("Coconut (S)").swipeListItemRight()
        onListItemUpdateButton("Coconut (S)").performClick()
        // Clothing size: Check unit
        waitForDetail()
        onDetailUnitChoice(ClothingSizeUnit, "S").assertIsSelected()
        // Clothing size: Dismiss detail
        composeRule.onNodeWithTag(TestTags.ENTRY_DETAIL_DISMISS).performClick()

        // Double: Click update
        onListItem("Dewberry (3.14)").swipeListItemRight()
        onListItemUpdateButton("Dewberry (3.14)").performClick()
        // Double: Check unit
        waitForDetail()
        onDetailAmount(DoubleUnit, 3.14).assertExists()
        // Double: Dismiss detail
        composeRule.onNodeWithTag(TestTags.ENTRY_DETAIL_DISMISS).performClick()

        // Fraction: Click update
        onListItem("Fig (½)").swipeListItemRight()
        onListItemUpdateButton("Fig (½)").performClick()
        // Fraction: Check unit
        waitForDetail()
        onDetailUnitChoice(FractionUnit, "½").assertIsSelected()
    }

    @Test
    fun `when unit and amount are not set in the database, then the last selected unit is the default unit`() {
        launch()

        // Click insert
        composeRule.onNodeWithTag(TestTags.ENTRY_LIST_INSERT).performClick()
        // Check unit
        onDetailUnitChoices(ClothingSizeUnit).assertCountEquals(5).assertAll(isNotSelected())
        // Change unit to double
        composeRule.onNodeWithTag(TestTags.ENTRY_DETAIL_UNIT_MENU).performClick()
        composeRule.onNodeWithTag(TestTags.entryDetailUnitMenuItem(DoubleUnit)).performClick()
        // Dismiss detail
        composeRule.onNodeWithTag(TestTags.ENTRY_DETAIL_DISMISS).performClick()
        // Check list
        onListTitle(totalCount = 0).assertExists()

        // Click insert
        composeRule.onNodeWithTag(TestTags.ENTRY_LIST_INSERT).performClick()
        // Check unit
        onDetailAmount(DoubleUnit, null).assertExists()
        // Change unit to fraction
        composeRule.onNodeWithTag(TestTags.ENTRY_DETAIL_UNIT_MENU).performClick()
        composeRule.onNodeWithTag(TestTags.entryDetailUnitMenuItem(FractionUnit)).performClick()
        // Dismiss detail
        composeRule.onNodeWithTag(TestTags.ENTRY_DETAIL_DISMISS).performClick()
        // Check list
        onListTitle(totalCount = 0).assertExists()

        // Click insert
        composeRule.onNodeWithTag(TestTags.ENTRY_LIST_INSERT).performClick()
        // Check unit
        onDetailUnitChoices(FractionUnit).assertCountEquals(5).assertAll(isNotSelected())
        // Change unit to clothing size
        composeRule.onNodeWithTag(TestTags.ENTRY_DETAIL_UNIT_MENU).performClick()
        composeRule.onNodeWithTag(TestTags.entryDetailUnitMenuItem(ClothingSizeUnit)).performClick()
        // Dismiss detail
        composeRule.onNodeWithTag(TestTags.ENTRY_DETAIL_DISMISS).performClick()
        // Check list
        onListTitle(totalCount = 0).assertExists()

        // Click insert
        composeRule.onNodeWithTag(TestTags.ENTRY_LIST_INSERT).performClick()
        // Check unit
        onDetailUnitChoices(ClothingSizeUnit).assertCountEquals(5).assertAll(isNotSelected())
    }

    @Test
    fun `when unit and amount are not set in the database and default unit is not set, then the default unit is selected and the amount field is empty`() {
        runBlocking {
            entryRepository.insert(entryWithNoneUnit)
        }
        launch()
        // Click update
        onListItem("Nectarine").swipeListItemRight()
        onListItemUpdateButton("Nectarine").performClick()
        // Check unit
        waitForDetail()
        onDetailUnitChoices(ClothingSizeUnit).assertCountEquals(5).assertAll(isNotSelected())
        // Check amount
        composeRule.onNodeWithTag(TestTags.entryDetailAmount(DoubleUnit)).assertDoesNotExist()
    }

    @Test
    fun `when unit and amount are set in the database and the selected unit is changed, then the amount field is cleared and the choice is deselected`() {
        runBlocking {
            entryRepository.insert(entryWithClothingSizeUnit)
            entryRepository.insert(entryWithDoubleUnit)
            entryRepository.insert(entryWithFractionUnit)
        }
        launch()

        // Clothing size: Click update
        onListItem("Coconut (S)").swipeListItemRight()
        onListItemUpdateButton("Coconut (S)").performClick()
        // Clothing size: Change unit to double
        waitForDetail()
        composeRule.onNodeWithTag(TestTags.ENTRY_DETAIL_UNIT_MENU).performClick()
        composeRule.onNodeWithTag(TestTags.entryDetailUnitMenuItem(DoubleUnit)).performClick()
        // Clothing size: Check unit
        onDetailAmount(DoubleUnit, null).assertExists()
        // Clothing size: Dismiss detail
        composeRule.onNodeWithTag(TestTags.ENTRY_DETAIL_DISMISS).performClick()

        // Clothing size: Click update
        onListItem("Coconut (S)").swipeListItemRight()
        onListItemUpdateButton("Coconut (S)").performClick()
        // Clothing size: Change unit to fraction
        waitForDetail()
        composeRule.onNodeWithTag(TestTags.ENTRY_DETAIL_UNIT_MENU).performClick()
        composeRule.onNodeWithTag(TestTags.entryDetailUnitMenuItem(FractionUnit)).performClick()
        // Clothing size: Check unit
        onDetailUnitChoices(FractionUnit).assertCountEquals(5).assertAll(isNotSelected())
        // Clothing size: Dismiss detail
        composeRule.onNodeWithTag(TestTags.ENTRY_DETAIL_DISMISS).performClick()

        // Double: Click update
        onListItem("Dewberry (3.14)").swipeListItemRight()
        onListItemUpdateButton("Dewberry (3.14)").performClick()
        // Double: Change unit to clothing size
        waitForDetail()
        composeRule.onNodeWithTag(TestTags.ENTRY_DETAIL_UNIT_MENU).performClick()
        composeRule.onNodeWithTag(TestTags.entryDetailUnitMenuItem(ClothingSizeUnit)).performClick()
        // Double: Check unit
        onDetailUnitChoices(ClothingSizeUnit).assertCountEquals(5).assertAll(isNotSelected())
        // Double: Dismiss detail
        composeRule.onNodeWithTag(TestTags.ENTRY_DETAIL_DISMISS).performClick()

        // Double: Click update
        onListItem("Dewberry (3.14)").swipeListItemRight()
        onListItemUpdateButton("Dewberry (3.14)").performClick()
        // Double: Change unit to fraction
        waitForDetail()
        composeRule.onNodeWithTag(TestTags.ENTRY_DETAIL_UNIT_MENU).performClick()
        composeRule.onNodeWithTag(TestTags.entryDetailUnitMenuItem(FractionUnit)).performClick()
        // Double: Check unit
        onDetailUnitChoices(FractionUnit).assertCountEquals(5).assertAll(isNotSelected())
        // Double: Dismiss detail
        composeRule.onNodeWithTag(TestTags.ENTRY_DETAIL_DISMISS).performClick()

        // Fraction: Click update
        onListItem("Fig (½)").swipeListItemRight()
        onListItemUpdateButton("Fig (½)").performClick()
        // Fraction: Change unit to clothing size
        waitForDetail()
        composeRule.onNodeWithTag(TestTags.ENTRY_DETAIL_UNIT_MENU).performClick()
        composeRule.onNodeWithTag(TestTags.entryDetailUnitMenuItem(ClothingSizeUnit)).performClick()
        // Fraction: Check unit
        onDetailUnitChoices(ClothingSizeUnit).assertCountEquals(5).assertAll(isNotSelected())
        // Fraction: Dismiss detail
        composeRule.onNodeWithTag(TestTags.ENTRY_DETAIL_DISMISS).performClick()

        // Fraction: Click update
        onListItem("Fig (½)").swipeListItemRight()
        onListItemUpdateButton("Fig (½)").performClick()
        // Fraction: Change unit to double
        waitForDetail()
        composeRule.onNodeWithTag(TestTags.ENTRY_DETAIL_UNIT_MENU).performClick()
        composeRule.onNodeWithTag(TestTags.entryDetailUnitMenuItem(DoubleUnit)).performClick()
        // Fraction: Check unit
        onDetailAmount(DoubleUnit, null).assertExists()
    }

    @Test
    fun `when unit and amount are not set in the database and the selected unit is changed and the amount is not changed and the form is saved, then the database unit and amount are not updated`() {
        launch()

        // Click insert
        composeRule.onNodeWithTag(TestTags.ENTRY_LIST_INSERT).performClick()
        // Change content
        composeRule.onNodeWithTag(TestTags.ENTRY_DETAIL_CONTENT).performTextInput("Coconut")
        // Change unit to clothing size (first to double, because clothing size is the default)
        composeRule.onNodeWithTag(TestTags.ENTRY_DETAIL_UNIT_MENU).performClick()
        composeRule.onNodeWithTag(TestTags.entryDetailUnitMenuItem(DoubleUnit)).performClick()
        composeRule.onNodeWithTag(TestTags.ENTRY_DETAIL_UNIT_MENU).performClick()
        composeRule.onNodeWithTag(TestTags.entryDetailUnitMenuItem(ClothingSizeUnit)).performClick()
        // Save detail
        composeRule.onNodeWithTag(TestTags.ENTRY_DETAIL_SAVE_INSERT).performClick()
        // Check list
        waitForListItem("Coconut")
        onListItem("Coconut").assertExists()

        // Click insert
        composeRule.onNodeWithTag(TestTags.ENTRY_LIST_INSERT).performClick()
        // Change content
        composeRule.onNodeWithTag(TestTags.ENTRY_DETAIL_CONTENT).performTextInput("Dewberry")
        // Change unit to double
        composeRule.onNodeWithTag(TestTags.ENTRY_DETAIL_UNIT_MENU).performClick()
        composeRule.onNodeWithTag(TestTags.entryDetailUnitMenuItem(DoubleUnit)).performClick()
        // Save detail
        composeRule.onNodeWithTag(TestTags.ENTRY_DETAIL_SAVE_INSERT).performClick()
        // Check list
        waitForListItem("Dewberry")
        onListItem("Dewberry").assertExists()

        // Click insert
        composeRule.onNodeWithTag(TestTags.ENTRY_LIST_INSERT).performClick()
        // Change content
        composeRule.onNodeWithTag(TestTags.ENTRY_DETAIL_CONTENT).performTextInput("Fig")
        // Change unit to fraction
        composeRule.onNodeWithTag(TestTags.ENTRY_DETAIL_UNIT_MENU).performClick()
        composeRule.onNodeWithTag(TestTags.entryDetailUnitMenuItem(FractionUnit)).performClick()
        // Save detail
        composeRule.onNodeWithTag(TestTags.ENTRY_DETAIL_SAVE_INSERT).performClick()
        // Check list
        waitForListItem("Fig")
        onListItem("Fig").assertExists()
    }

    @Test
    fun `when unit and amount are not set in the database and the selected unit is changed and the amount is changed and the form is saved, then the database unit and amount are updated`() {
        launch()

        // Click insert
        composeRule.onNodeWithTag(TestTags.ENTRY_LIST_INSERT).performClick()
        // Change content
        composeRule.onNodeWithTag(TestTags.ENTRY_DETAIL_CONTENT).performTextInput("Coconut")
        // Change unit to clothing size (first to double, because clothing size is the default)
        composeRule.onNodeWithTag(TestTags.ENTRY_DETAIL_UNIT_MENU).performClick()
        composeRule.onNodeWithTag(TestTags.entryDetailUnitMenuItem(DoubleUnit)).performClick()
        composeRule.onNodeWithTag(TestTags.ENTRY_DETAIL_UNIT_MENU).performClick()
        composeRule.onNodeWithTag(TestTags.entryDetailUnitMenuItem(ClothingSizeUnit)).performClick()
        // Change amount
        onDetailUnitChoice(ClothingSizeUnit, "XL").performClick()
        // Save detail
        composeRule.onNodeWithTag(TestTags.ENTRY_DETAIL_SAVE_INSERT).performClick()
        // Check list
        waitForListItem("Coconut (XL)")
        onListItem("Coconut (XL)").assertExists()

        // Click insert
        composeRule.onNodeWithTag(TestTags.ENTRY_LIST_INSERT).performClick()
        // Change content
        composeRule.onNodeWithTag(TestTags.ENTRY_DETAIL_CONTENT).performTextInput("Dewberry")
        // Change unit to double
        composeRule.onNodeWithTag(TestTags.ENTRY_DETAIL_UNIT_MENU).performClick()
        composeRule.onNodeWithTag(TestTags.entryDetailUnitMenuItem(DoubleUnit)).performClick()
        // Change amount
        onDetailAmount(DoubleUnit, null).performTextInput("7.89")
        // Save detail
        composeRule.onNodeWithTag(TestTags.ENTRY_DETAIL_SAVE_INSERT).performClick()
        // Check list
        waitForListItem("Dewberry (7.89)")
        onListItem("Dewberry (7.89)").assertExists()

        // Click insert
        composeRule.onNodeWithTag(TestTags.ENTRY_LIST_INSERT).performClick()
        // Change content
        composeRule.onNodeWithTag(TestTags.ENTRY_DETAIL_CONTENT).performTextInput("Fig")
        // Change unit to fraction
        composeRule.onNodeWithTag(TestTags.ENTRY_DETAIL_UNIT_MENU).performClick()
        composeRule.onNodeWithTag(TestTags.entryDetailUnitMenuItem(FractionUnit)).performClick()
        // Change amount
        onDetailUnitChoice(FractionUnit, "¼").performClick()
        // Save detail
        composeRule.onNodeWithTag(TestTags.ENTRY_DETAIL_SAVE_INSERT).performClick()
        // Check list
        waitForListItem("Fig (¼)")
        onListItem("Fig (¼)").assertExists()
    }

    @Test
    fun `when unit and amount are set in the database and the selected unit is changed and the amount is not changed and the form is saved, then the database unit and amount are set to none`() {
        runBlocking {
            entryRepository.insert(entryWithClothingSizeUnit)
            entryRepository.insert(entryWithDoubleUnit)
            entryRepository.insert(entryWithFractionUnit)
        }
        launch()

        // Clothing size: Click update
        onListItem("Coconut (S)").swipeListItemRight()
        onListItemUpdateButton("Coconut (S)").performClick()
        // Clothing size: Change unit to double
        waitForDetail()
        composeRule.onNodeWithTag(TestTags.ENTRY_DETAIL_UNIT_MENU).performClick()
        composeRule.onNodeWithTag(TestTags.entryDetailUnitMenuItem(DoubleUnit)).performClick()
        // Clothing size: Save detail
        composeRule.onNodeWithTag(TestTags.ENTRY_DETAIL_SAVE_UPDATE).performClick()
        // Clothing size: Check list
        waitForListItem("Coconut")
        onListItem("Coconut").assertExists()

        // Double: Click update
        onListItem("Dewberry (3.14)").swipeListItemRight()
        onListItemUpdateButton("Dewberry (3.14)").performClick()
        // Double: Change unit to fraction
        waitForDetail()
        composeRule.onNodeWithTag(TestTags.ENTRY_DETAIL_UNIT_MENU).performClick()
        composeRule.onNodeWithTag(TestTags.entryDetailUnitMenuItem(FractionUnit)).performClick()
        // Double: Save detail
        composeRule.onNodeWithTag(TestTags.ENTRY_DETAIL_SAVE_UPDATE).performClick()
        // Double: Check list
        waitForListItem("Dewberry")
        onListItem("Dewberry").assertExists()

        // Fraction: Click update
        onListItem("Fig (½)").swipeListItemRight()
        onListItemUpdateButton("Fig (½)").performClick()
        // Fraction: Change unit to clothing size
        waitForDetail()
        composeRule.onNodeWithTag(TestTags.ENTRY_DETAIL_UNIT_MENU).performClick()
        composeRule.onNodeWithTag(TestTags.entryDetailUnitMenuItem(ClothingSizeUnit)).performClick()
        // Fraction: Save detail
        composeRule.onNodeWithTag(TestTags.ENTRY_DETAIL_SAVE_UPDATE).performClick()
        // Fraction: Check list
        waitForListItem("Fig")
        onListItem("Fig").assertExists()
    }

    @Test
    fun `when unit and amount are set in the database and the selected unit is changed and the amount is changed and the form is saved, then the database unit and amount are updated`() {
        runBlocking {
            entryRepository.insert(entryWithClothingSizeUnit)
        }
        launch()

        // Clothing size: Click update
        onListItem("Coconut (S)").swipeListItemRight()
        onListItemUpdateButton("Coconut (S)").performClick()
        // Clothing size: Change unit to double
        waitForDetail()
        composeRule.onNodeWithTag(TestTags.ENTRY_DETAIL_UNIT_MENU).performClick()
        composeRule.onNodeWithTag(TestTags.entryDetailUnitMenuItem(DoubleUnit)).performClick()
        // Clothing size: Change amount
        onDetailAmount(DoubleUnit, null).performTextInput("7.89")
        // Clothing size: Save detail
        composeRule.onNodeWithTag(TestTags.ENTRY_DETAIL_SAVE_UPDATE).performClick()
        // Clothing size: Check list
        waitForListItem("Coconut (7.89)")
        onListItem("Coconut (7.89)").assertExists()

        // Double: Click update
        onListItem("Coconut (7.89)").swipeListItemRight()
        onListItemUpdateButton("Coconut (7.89)").performClick()
        // Double: Change unit to fraction
        waitForDetail()
        composeRule.onNodeWithTag(TestTags.ENTRY_DETAIL_UNIT_MENU).performClick()
        composeRule.onNodeWithTag(TestTags.entryDetailUnitMenuItem(FractionUnit)).performClick()
        // Double: Change amount
        onDetailUnitChoice(FractionUnit, "¼").performClick()
        // Double: Save detail
        composeRule.onNodeWithTag(TestTags.ENTRY_DETAIL_SAVE_UPDATE).performClick()
        // Double: Check list
        waitForListItem("Coconut (¼)")
        onListItem("Coconut (¼)").assertExists()

        // Fraction: Click update
        onListItem("Coconut (¼)").swipeListItemRight()
        onListItemUpdateButton("Coconut (¼)").performClick()
        // Fraction: Change unit to clothing size
        waitForDetail()
        composeRule.onNodeWithTag(TestTags.ENTRY_DETAIL_UNIT_MENU).performClick()
        composeRule.onNodeWithTag(TestTags.entryDetailUnitMenuItem(ClothingSizeUnit)).performClick()
        // Fraction: Change amount
        onDetailUnitChoice(ClothingSizeUnit, "XL").performClick()
        // Fraction: Save detail
        composeRule.onNodeWithTag(TestTags.ENTRY_DETAIL_SAVE_UPDATE).performClick()
        // Fraction: Check list
        waitForListItem("Coconut (XL)")
        onListItem("Coconut (XL)").assertExists()

        // Clothing size: Click update
        onListItem("Coconut (XL)").swipeListItemRight()
        onListItemUpdateButton("Coconut (XL)").performClick()
        // Clothing size: Change unit to fraction
        waitForDetail()
        composeRule.onNodeWithTag(TestTags.ENTRY_DETAIL_UNIT_MENU).performClick()
        composeRule.onNodeWithTag(TestTags.entryDetailUnitMenuItem(FractionUnit)).performClick()
        // Clothing size: Change amount
        onDetailUnitChoice(FractionUnit, "¼").performClick()
        // Clothing size: Save detail
        composeRule.onNodeWithTag(TestTags.ENTRY_DETAIL_SAVE_UPDATE).performClick()
        // Clothing size: Check list
        waitForListItem("Coconut (¼)")
        onListItem("Coconut (¼)").assertExists()

        // Fraction: Click update
        onListItem("Coconut (¼)").swipeListItemRight()
        onListItemUpdateButton("Coconut (¼)").performClick()
        // Fraction: Change unit to double
        waitForDetail()
        composeRule.onNodeWithTag(TestTags.ENTRY_DETAIL_UNIT_MENU).performClick()
        composeRule.onNodeWithTag(TestTags.entryDetailUnitMenuItem(DoubleUnit)).performClick()
        // Fraction: Change amount
        onDetailAmount(DoubleUnit, null).performTextInput("7.89")
        // Fraction: Save detail
        composeRule.onNodeWithTag(TestTags.ENTRY_DETAIL_SAVE_UPDATE).performClick()
        // Fraction: Check list
        waitForListItem("Coconut (7.89)")
        onListItem("Coconut (7.89)").assertExists()

        // Double: Click update
        onListItem("Coconut (7.89)").swipeListItemRight()
        onListItemUpdateButton("Coconut (7.89)").performClick()
        // Double: Change unit to clothing size
        waitForDetail()
        composeRule.onNodeWithTag(TestTags.ENTRY_DETAIL_UNIT_MENU).performClick()
        composeRule.onNodeWithTag(TestTags.entryDetailUnitMenuItem(ClothingSizeUnit)).performClick()
        // Double: Change amount
        onDetailUnitChoice(ClothingSizeUnit, "XL").performClick()
        // Double: Save detail
        composeRule.onNodeWithTag(TestTags.ENTRY_DETAIL_SAVE_UPDATE).performClick()
        // Double: Check list
        waitForListItem("Coconut (XL)")
        onListItem("Coconut (XL)").assertExists()
    }

    @Test
    fun `when unit and amount are not set in the database and a selected unit is changed and the amount is changed and configuration changes, then the selected unit and amount field do not change`() {
        launch()

        // Click insert
        composeRule.onNodeWithTag(TestTags.ENTRY_LIST_INSERT).performClick()
        // Change content
        composeRule.onNodeWithTag(TestTags.ENTRY_DETAIL_CONTENT).performTextInput("Coconut")
        // Change unit to clothing size (first to double, because clothing size is the default)
        composeRule.onNodeWithTag(TestTags.ENTRY_DETAIL_UNIT_MENU).performClick()
        composeRule.onNodeWithTag(TestTags.entryDetailUnitMenuItem(DoubleUnit)).performClick()
        composeRule.onNodeWithTag(TestTags.ENTRY_DETAIL_UNIT_MENU).performClick()
        composeRule.onNodeWithTag(TestTags.entryDetailUnitMenuItem(ClothingSizeUnit)).performClick()
        // Change amount
        onDetailUnitChoice(ClothingSizeUnit, "XL").performClick()
        // Change configuration
        rotateDevice()
        // Check unit
        onDetailUnitChoice(ClothingSizeUnit, "XL").assertIsSelected()
        // Dismiss detail
        composeRule.onNodeWithTag(TestTags.ENTRY_DETAIL_DISMISS).performClick()

        // Click insert
        composeRule.onNodeWithTag(TestTags.ENTRY_LIST_INSERT).performClick()
        // Change content
        composeRule.onNodeWithTag(TestTags.ENTRY_DETAIL_CONTENT).performTextInput("Dewberry")
        // Change unit to double
        composeRule.onNodeWithTag(TestTags.ENTRY_DETAIL_UNIT_MENU).performClick()
        composeRule.onNodeWithTag(TestTags.entryDetailUnitMenuItem(DoubleUnit)).performClick()
        // Change amount
        onDetailAmount(DoubleUnit, null).performTextInput("7.89")
        // Change configuration
        rotateDevice()
        // Check unit
        onDetailAmount(DoubleUnit, 7.89).assertExists()
        // Dismiss detail
        composeRule.onNodeWithTag(TestTags.ENTRY_DETAIL_DISMISS).performClick()

        // Click insert
        composeRule.onNodeWithTag(TestTags.ENTRY_LIST_INSERT).performClick()
        // Change content
        composeRule.onNodeWithTag(TestTags.ENTRY_DETAIL_CONTENT).performTextInput("Fig")
        // Change unit to fraction
        composeRule.onNodeWithTag(TestTags.ENTRY_DETAIL_UNIT_MENU).performClick()
        composeRule.onNodeWithTag(TestTags.entryDetailUnitMenuItem(FractionUnit)).performClick()
        // Change amount
        onDetailUnitChoice(FractionUnit, "¼").performClick()
        // Change configuration
        rotateDevice()
        // Check unit
        onDetailUnitChoice(FractionUnit, "¼").assertIsSelected()
    }
}
