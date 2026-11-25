package app.traced_it.robot

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.SemanticsNodeInteractionCollection
import androidx.compose.ui.test.hasAnyChild
import androidx.compose.ui.test.hasAnySibling
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.isEnabled
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollToKey
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.text.AnnotatedString
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.traced_it.MainScreenTestActivity
import app.traced_it.data.DefaultEntryRepository
import app.traced_it.data.database.AppDatabase
import app.traced_it.data.database.ClothingSizeUnit
import app.traced_it.data.database.DoubleUnit
import app.traced_it.data.database.Entry
import app.traced_it.data.database.EntryUnit
import app.traced_it.data.database.FractionUnit
import app.traced_it.data.database.NoneUnit
import app.traced_it.data.extensions.toTwoDigitString
import app.traced_it.testing.TestClock
import app.traced_it.ui.components.TestTags
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltTestApplication
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlinx.datetime.toInstant
import kotlinx.datetime.toJavaLocalDateTime
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import javax.inject.Inject
import kotlin.math.abs
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@RunWith(AndroidJUnit4::class)
@Config(
    application = HiltTestApplication::class,
    sdk = [36], // Use SDK <= 36 to prevent "NoSuchMethodException: android.hardware.input.InputManager.getInstance"
)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
abstract class BaseRobotTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @Suppress("DEPRECATION")
    @get:Rule(order = 1)
    val composeRule = createEmptyComposeRule()

    @Before
    fun baseSetUp() = hiltRule.inject()

    // Clock

    protected val now: LocalDateTime get() = TestClock.now
    protected val timeZone: TimeZone get() = TestClock.timeZone

    // Database

    @Inject
    lateinit var db: AppDatabase

    @Inject
    lateinit var entryRepository: DefaultEntryRepository

    // Launch

    private lateinit var scenario: ActivityScenario<MainScreenTestActivity>

    protected fun launch() {
        scenario = ActivityScenario.launch(MainScreenTestActivity::class.java)
    }

    @After
    fun tearDownActivity() {
        if (::scenario.isInitialized) {
            scenario.close()
        }
        db.close()
    }

    // Helpers

    protected fun onListTitle(totalCount: Int): SemanticsNodeInteraction =
        composeRule.onNode(hasTestTag(TestTags.ENTRY_LIST_TITLE) and hasText("($totalCount)", substring = true))

    @OptIn(ExperimentalTestApi::class)
    protected fun waitForListTitle(totalCount: Int) {
        composeRule.waitUntilExactlyOneExists(
            hasTestTag(TestTags.ENTRY_LIST_TITLE) and hasText("($totalCount)", substring = true)
        )
    }

    @Suppress("SameParameterValue")
    protected fun onListFilter(itemCount: Int, totalCount: Int): SemanticsNodeInteraction =
        composeRule.onNodeWithTag(TestTags.entryListFilterStatus(itemCount, totalCount))

    protected fun onListItem(text: String): SemanticsNodeInteraction =
        composeRule.onNode(
            hasTestTag(TestTags.ENTRY_LIST_ITEM) and
                hasAnyChild(hasTestTag(TestTags.ENTRY_LIST_ITEM_CONTENT) and hasText(text)),
            useUnmergedTree = true,
        )

    @Suppress("SameParameterValue")
    protected fun onListItemDeleteButton(text: String): SemanticsNodeInteraction =
        composeRule.onNode(
            hasTestTag(TestTags.ENTRY_LIST_ITEM_DELETE) and
                hasAnySibling(
                    hasTestTag(TestTags.ENTRY_LIST_ITEM) and
                        hasAnyChild(hasTestTag(TestTags.ENTRY_LIST_ITEM_CONTENT) and hasText(text))
                ),
            useUnmergedTree = true,
        )

    @Suppress("SameParameterValue")
    protected fun onListItemRepeatButton(text: String): SemanticsNodeInteraction =
        composeRule.onNode(
            hasTestTag(TestTags.ENTRY_LIST_ITEM_REPEAT) and
                hasAnySibling(hasTestTag(TestTags.ENTRY_LIST_ITEM_CONTENT) and hasText(text)),
            useUnmergedTree = true,
        )

    @Suppress("SameParameterValue")
    protected fun onListItemUpdateButton(text: String): SemanticsNodeInteraction =
        composeRule.onNode(
            hasTestTag(TestTags.ENTRY_LIST_ITEM_UPDATE) and
                hasAnySibling(
                    hasTestTag(TestTags.ENTRY_LIST_ITEM) and
                        hasAnyChild(hasTestTag(TestTags.ENTRY_LIST_ITEM_CONTENT) and hasText(text))
                ),
            useUnmergedTree = true,
        )

    protected fun onListItem(
        text: String,
        hoursAgo: Int = 0,
        minutesAgo: Int = 0,
        secondsAgo: Int = 0,
    ): SemanticsNodeInteraction {
        val timeText = if (hoursAgo != 0) {
            "${hoursAgo} h ${minutesAgo.toString().padStart(2, '0')} min ago"
        } else if (minutesAgo != 0) {
            "${minutesAgo} min ago"
        } else {
            "${secondsAgo} s ago"
        }
        return composeRule.onNode(
            hasTestTag(TestTags.ENTRY_LIST_ITEM) and
                hasAnyChild(hasTestTag(TestTags.ENTRY_LIST_ITEM_CONTENT) and hasText(text)) and
                hasAnyChild(hasTestTag(TestTags.ENTRY_LIST_ITEM_TIME) and hasText(timeText)),
            useUnmergedTree = true,
        )
    }

    protected fun onListItem(text: String, createdAt: LocalDateTime): SemanticsNodeInteraction {
        val formattedTime = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
            .withLocale(Locale.getDefault())
            .format(createdAt.toJavaLocalDateTime())
        val timeText = "at $formattedTime"
        return composeRule.onNode(
            hasTestTag(TestTags.ENTRY_LIST_ITEM) and
                hasAnyChild(hasTestTag(TestTags.ENTRY_LIST_ITEM_CONTENT) and hasText(text)) and
                hasAnyChild(hasTestTag(TestTags.ENTRY_LIST_ITEM_TIME) and hasText(timeText)),
            useUnmergedTree = true,
        )
    }

    protected fun SemanticsNodeInteraction.swipeListItemLeft() {
        performTouchInput { swipeLeft(startX = centerX, endX = left) }
    }

    protected fun SemanticsNodeInteraction.swipeListItemRight() {
        performTouchInput { swipeRight(startX = centerX, endX = right) }
    }

    protected fun onListHeading(): SemanticsNodeInteraction =
        composeRule.onNode(hasTestTag(TestTags.ENTRY_LIST_HEADING))

    protected fun onListHeading(dateTime: LocalDate): SemanticsNodeInteraction {
        val formattedDate = DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG)
            .withLocale(Locale.getDefault())
            .format(dateTime.atTime(12, 0).toJavaLocalDateTime())
        return composeRule
            .onNode(hasTestTag(TestTags.ENTRY_LIST_HEADING) and hasText(formattedDate), useUnmergedTree = true)
    }

    @Suppress("SameParameterValue")
    protected fun onDetailAmount(amountUnit: EntryUnit, amount: Double?): SemanticsNodeInteraction =
        composeRule.onNode(
            hasTestTag(TestTags.entryDetailAmount(amountUnit)) and isEnabled() and
                hasEditableText(amount?.toString().orEmpty())
        )

    protected fun onDetailContent(): SemanticsNodeInteraction {
        waitForDetail()
        return composeRule.onNodeWithTag(TestTags.ENTRY_DETAIL_CONTENT)
    }

    protected fun onDetailContent(content: String): SemanticsNodeInteraction {
        waitForDetail()
        return composeRule.onNode(
            hasTestTag(TestTags.ENTRY_DETAIL_CONTENT) and isEnabled() and hasEditableText(content),
            useUnmergedTree = true,
        )
    }

    private val dateFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG)

    protected fun onDetailCreatedAtDate(): SemanticsNodeInteraction =
        composeRule.onNodeWithTag(TestTags.DATE_TIME_PICKER_DATE)

    protected fun onDetailCreatedAtDate(date: LocalDate): SemanticsNodeInteraction {
        val text = if (date == now.date) {
            "today"
        } else {
            dateFormatter
                .withLocale(Locale.getDefault())
                .format(date.atTime(hour = 12, minute = 0).toJavaLocalDateTime())
        }
        return composeRule.onNode(
            hasTestTag(TestTags.DATE_TIME_PICKER_DATE) and
                hasAnyChild(hasTestTag(TestTags.DATE_TIME_PICKER_ITEM) and hasText(text))
        )
    }

    protected fun onDetailCreatedAtHour(): SemanticsNodeInteraction =
        composeRule.onNodeWithTag(TestTags.DATE_TIME_PICKER_HOUR)

    protected fun onDetailCreatedAtHour(hour: Int): SemanticsNodeInteraction =
        composeRule.onNode(
            hasTestTag(TestTags.DATE_TIME_PICKER_HOUR) and
                hasAnyChild(
                    hasTestTag(TestTags.DATE_TIME_PICKER_ITEM) and hasText(hour.toTwoDigitString())
                )
        )

    protected fun onDetailCreatedAtMinute(): SemanticsNodeInteraction =
        composeRule.onNodeWithTag(TestTags.DATE_TIME_PICKER_MINUTE)

    protected fun onDetailCreatedAtMinute(minute: Int): SemanticsNodeInteraction =
        composeRule.onNode(
            hasTestTag(TestTags.DATE_TIME_PICKER_MINUTE) and
                hasAnyChild(
                    hasTestTag(TestTags.DATE_TIME_PICKER_ITEM) and hasText(minute.toTwoDigitString())
                )
        )

    /**
     * Scroll by number of [steps] up if the number is negative or down if the number is positive. One step is half of
     * the node height.
     *
     * This method is used to scroll the date picker segments such as [onDetailCreatedAtDate], because it is much faster
     * than [performScrollToKey].
     */
    protected fun SemanticsNodeInteraction.performVerticalScroll(steps: Int) {
        when {
            steps < 0 -> repeat(abs(steps)) {
                performTouchInput { swipeDown(centerY, bottom) }
            }

            steps > 0 -> repeat(steps) {
                performTouchInput { swipeUp(centerY, top) }
            }
        }
    }

    @Suppress("SameParameterValue")
    protected fun onDetailUnitChoice(amountUnit: EntryUnit, choiceName: String): SemanticsNodeInteraction =
        composeRule.onNode(
            hasTestTag(TestTags.entryDetailUnitChoice(amountUnit)) and isEnabled() and
                hasText(choiceName)
        )

    protected fun onDetailUnitChoices(amountUnit: EntryUnit): SemanticsNodeInteractionCollection =
        composeRule.onAllNodes(hasTestTag(TestTags.entryDetailUnitChoice(amountUnit)) and isEnabled())

    @Suppress("SameParameterValue", "UnusedReceiverParameter")
    protected fun ComposeTestRule.waitUntil(matcher: SemanticsMatcher, useUnmergedTree: Boolean) {
        composeRule.waitUntil {
            composeRule.onAllNodes(matcher, useUnmergedTree = useUnmergedTree).fetchSemanticsNodes().isNotEmpty()
        }
    }

    @OptIn(ExperimentalTestApi::class)
    protected fun waitForDetail() {
        composeRule.waitUntilAtLeastOneExists(hasTestTag(TestTags.ENTRY_DETAIL_CONTENT))
    }

    @OptIn(ExperimentalTestApi::class)
    protected fun waitForList() {
        composeRule.waitUntilAtLeastOneExists(hasTestTag(TestTags.ENTRY_LIST_INSERT))
    }

    protected fun waitForListItem(content: String) {
        composeRule.waitUntil(hasTestTag(TestTags.ENTRY_LIST_ITEM_CONTENT) and hasText(content), useUnmergedTree = true)
    }

    protected fun SemanticsNodeInteraction.waitUntilDisplayed() {
        composeRule.waitUntil { isDisplayed() }
    }

    protected fun pressBack() {
        scenario.onActivity { activity -> activity.onBackPressedDispatcher.onBackPressed() }
        composeRule.waitForIdle()
    }

    protected fun restartActivity() {
        scenario.moveToState(Lifecycle.State.DESTROYED)
        launch()
    }

    protected fun rotateDevice() {
        RuntimeEnvironment.setQualifiers("+land")
        scenario.recreate()
        composeRule.waitForIdle()
    }

    protected fun hasEditableText(text: String): SemanticsMatcher =
        SemanticsMatcher.expectValue(SemanticsProperties.EditableText, AnnotatedString(text))

    // Entries

    protected val entryWithNoneUnit = Entry(
        amount = 0.0,
        amountUnit = NoneUnit,
        content = "Nectarine",
        createdAt = now.toInstant(timeZone).toEpochMilliseconds(),
        uid = 1,
    )
    protected val entryWithClothingSizeUnit = Entry(
        amount = 1.0,
        amountUnit = ClothingSizeUnit,
        content = "Coconut",
        createdAt = now.toInstant(timeZone).toEpochMilliseconds(),
        uid = 2,
    )
    protected val entryWithDoubleUnit = Entry(
        amount = 3.14,
        amountUnit = DoubleUnit,
        content = "Dewberry",
        createdAt = now.toInstant(timeZone).minus(1.days).minus(1.hours).minus(1.minutes)
            .toEpochMilliseconds(),
        uid = 3,
    )
    protected val entryWithFractionUnit = Entry(
        amount = 0.5,
        amountUnit = FractionUnit,
        content = "Fig",
        createdAt = now.toInstant(timeZone).minus(3.seconds).toEpochMilliseconds(),
        uid = 4,
    )
}
