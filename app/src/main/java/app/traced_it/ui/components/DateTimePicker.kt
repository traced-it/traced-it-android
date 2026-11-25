package app.traced_it.ui.components

import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.gestures.stopScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.retain.retain
import androidx.compose.runtime.saveable.mapSaver
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.LayoutBoundsHolder
import androidx.compose.ui.layout.onVisibilityChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.traced_it.R
import app.traced_it.data.extensions.plusDateOffset
import app.traced_it.data.extensions.plusHourOffset
import app.traced_it.data.extensions.plusMinuteOffset
import app.traced_it.data.extensions.plusOffset
import app.traced_it.data.extensions.toTwoDigitString
import app.traced_it.ui.theme.AppTheme
import app.traced_it.ui.theme.Spacing
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlinx.datetime.toInstant
import kotlinx.datetime.toJavaLocalDateTime
import kotlinx.datetime.toLocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlin.math.roundToInt
import kotlin.time.Instant

/**
 * State for the [DateTimePicker] composable.
 *
 * It takes the initial time in [initialTimeInMillis] and provides the current time in [timeInMillis].
 *
 * To update the current time, call [setDateOffset], [setHourOffset] or [setMinuteOffset].
 *
 * When the state is recreated with different initial time -- for example when the entry detail for a different entry
 * loads -- then the [resetDate], [resetHour] and [resetMinute] properties will be set to true. The date time picker
 * composable must observe these properties and scroll itself to the initial position when they're true. This
 * method of resetting the date time picker results in smoother redrawing than if the composable was recreated.
 *
 * To implement a reset button, call [reset].
 */
@Stable
class DateTimePickerState(
    initialTimeInMillis: Long,
    val timeZone: TimeZone,
) {
    val initialDateTime = Instant.fromEpochMilliseconds(initialTimeInMillis).toLocalDateTime(timeZone)

    // Current values

    private var dateOffset by mutableIntStateOf(0)
    private var hourOffset by mutableIntStateOf(0)
    private var minuteOffset by mutableIntStateOf(0)

    val timeInMillis: Long
        get() = initialDateTime.plusOffset(dateOffset, hourOffset, minuteOffset).toInstant(timeZone)
            .toEpochMilliseconds()

    @JvmName("setDateOffsetMethod")
    fun setDateOffset(offset: Int) {
        dateOffset = offset
    }

    @JvmName("setHourOffsetMethod")
    fun setHourOffset(offset: Int) {
        hourOffset = offset
    }

    @JvmName("setMinuteOffsetMethod")
    fun setMinuteOffset(offset: Int) {
        minuteOffset = offset
    }

    // Reset

    var resetDate by mutableStateOf(true)
    var resetHour by mutableStateOf(true)
    var resetMinute by mutableStateOf(true)

    fun reset() {
        resetDate = true
        resetHour = true
        resetMinute = true
    }

    // Scroll in progress

    var dateScrollInProgress by mutableStateOf(false)
    var hourScrollInProgress by mutableStateOf(false)
    var minuteScrollInProgress by mutableStateOf(false)

    val isScrollInProgress: Boolean
        get() = dateScrollInProgress || hourScrollInProgress || minuteScrollInProgress

    companion object {
        @Suppress("unused")
        val Saver = run {
            val initialTimeInMillisKey = "initialTimeInMillis"
            val timeZoneIdKey = "timeZoneId"
            mapSaver(
                save = {
                    mapOf(initialTimeInMillisKey to it.timeInMillis, timeZoneIdKey to it.timeZone.id)
                },
                restore = {
                    val initialTimeInMillis = it[initialTimeInMillisKey] as? Long ?: return@mapSaver null
                    val timeZone = (it[timeZoneIdKey] as? String)?.let { zoneId -> TimeZone.of(zoneId) }
                        ?: return@mapSaver null
                    DateTimePickerState(initialTimeInMillis, timeZone)
                },
            )
        }
    }
}

@Composable
fun rememberDateTimePickerState(initialTimeInMillis: Long, timeZone: TimeZone): DateTimePickerState =
    retain(initialTimeInMillis, timeZone) {
        DateTimePickerState(initialTimeInMillis, timeZone)
    }

@Composable
fun DateTimePicker(
    state: DateTimePickerState,
    viewportBounds: LayoutBoundsHolder?,
    enabled: Boolean = true,
    height: Dp = Spacing.inputHeight * 1.25f,
    itemsPerHeight: Int = 3,
    borderColor: Color = MaterialTheme.colorScheme.outline,
    borderWidth: Dp = 1.dp,
) {
    val itemHeight = height / itemsPerHeight.toFloat()

    /**
     * Item text offset is used to shift which text is rendered for which item. For example when the initial date is
     * "November 4, 2026" and [itemTextOffset] is 0, then the texts are:
     *
     * - item 0 -> text "November 4, 2026"
     * - item 1 -> text "November 5, 2026"
     * - item 2 -> text "November 6, 2026"
     *
     * And when [itemTextOffset] is -1, then the texts are:
     *
     * - item 0 -> text "November 3, 2026"
     * - item 1 -> text "November 4, 2026"
     * - item 2 -> text "November 5, 2026"
     *
     * The offset is used when a segment shows more than one item at a time ([itemsPerHeight] > 1). Because in the code
     * the selected item is always the first one, but in the UI it should appear that the selected item is the middle
     * one. So we apply an offset to all texts. Another solution would be to recalculate item indexes, but that feels
     * more error-prone.
     */
    val itemTextOffset = itemsPerHeight / -2

    val locale = LocalLocale.current.platformLocale
    val today = LocalClock.current.now().toLocalDateTime(state.timeZone).date

    val dateFormatter = remember { DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG) }
    var userScrollEnabled by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier
            .drawWithContent {
                drawContent()
                drawRoundRect(
                    color = borderColor,
                    topLeft = Offset(0f, (height / itemsPerHeight).toPx()),
                    size = Size(width = size.width, height = (height / itemsPerHeight).toPx()),
                    cornerRadius = CornerRadius(4.dp.toPx()),
                    style = Stroke(borderWidth.toPx()),
                )
            }
            // Enable scrolling the date picker only when at least half of it is visible. It prevents the user from
            // accidentally changing the date picker value when scrolling a parent composable (e.g. the whole form).
            .onVisibilityChanged(minFractionVisible = 0.5f, viewportBounds = viewportBounds) { visible ->
                userScrollEnabled = visible
            },
    ) {
        Row {
            DateTimePickerSegment(
                height = height,
                reset = state.resetDate,
                onResetFinish = { state.resetDate = false },
                onScrollInProgress = { state.dateScrollInProgress = it },
                onValueChange = state::setDateOffset,
                modifier = Modifier
                    .weight(3f)
                    .rightBorder(borderColor, borderWidth)
                    .testTag(TestTags.DATE_TIME_PICKER_DATE),
                enabled = enabled,
                itemHeight = itemHeight,
                userScrollEnabled = userScrollEnabled,
            ) { offset ->
                val date = state.initialDateTime.date.plusDateOffset(offset + itemTextOffset)
                if (date == today) {
                    stringResource(R.string.detail_created_at_today)
                } else {
                    dateFormatter.withLocale(locale)
                        .format(date.atTime(hour = 12, minute = 0).toJavaLocalDateTime())
                }
            }
            DateTimePickerSegment(
                height = height,
                reset = state.resetHour,
                onResetFinish = { state.resetHour = false },
                onScrollInProgress = { state.hourScrollInProgress = it },
                onValueChange = state::setHourOffset,
                modifier = Modifier
                    .weight(1f)
                    .rightBorder(borderColor, borderWidth)
                    .testTag(TestTags.DATE_TIME_PICKER_HOUR),
                enabled = enabled,
                itemHeight = itemHeight,
                userScrollEnabled = userScrollEnabled,
            ) { offset ->
                state.initialDateTime.hour.plusHourOffset(offset + itemTextOffset).toTwoDigitString()
            }
            DateTimePickerSegment(
                height = height,
                reset = state.resetMinute,
                onResetFinish = { state.resetMinute = false },
                onScrollInProgress = { state.minuteScrollInProgress = it },
                onValueChange = state::setMinuteOffset,
                modifier = Modifier
                    .weight(1f)
                    .testTag(TestTags.DATE_TIME_PICKER_MINUTE),
                enabled = enabled,
                itemHeight = itemHeight,
                userScrollEnabled = userScrollEnabled,
            ) { offset ->
                state.initialDateTime.minute.plusMinuteOffset(offset + itemTextOffset).toTwoDigitString()
            }
        }
    }
}

private const val ITEM_COUNT = Int.MAX_VALUE
private const val START_INDEX = ITEM_COUNT / 2

@Composable
private fun DateTimePickerSegment(
    height: Dp,
    reset: Boolean,
    onResetFinish: () -> Unit,
    onScrollInProgress: (isScrollInProgress: Boolean) -> Unit,
    onValueChange: (offset: Int) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = false,
    itemHeight: Dp = height,
    userScrollEnabled: Boolean = true,
    text: @Composable (offset: Int) -> String,
) {
    val color = if (enabled) LocalContentColor.current else MaterialTheme.colorScheme.onSurfaceVariant
    val density = LocalDensity.current
    val hapticFeedback = LocalHapticFeedback.current

    val currentOnResetFinish by rememberUpdatedState(onResetFinish)
    val currentOnScrollInProgress by rememberUpdatedState(onScrollInProgress)
    val currentOnValueChange by rememberUpdatedState(onValueChange)

    val listState = rememberLazyListState(initialFirstVisibleItemIndex = START_INDEX)

    // Call the value change event when scrolling
    LaunchedEffect(listState, reset) {
        if (!reset) {
            // Don't call the event while resetting, to skip the new initial value after the reset
            snapshotFlow { listState.firstVisibleItemIndex }
                .drop(1) // Skip initial value
                .collect { firstVisibleItemIndex ->
                    currentOnValueChange(firstVisibleItemIndex - START_INDEX)
                }
        }
    }

    // Reset offset when requested and call the reset finish event
    LaunchedEffect(reset) {
        if (reset) {
            val newItemIndex = START_INDEX
            listState.stopScroll(MutatePriority.UserInput)
            listState.scrollToItem(newItemIndex)
            currentOnResetFinish()
        }
    }

    // Call the scroll in progress event while scrolling
    LaunchedEffect(listState) {
        snapshotFlow { listState.isScrollInProgress }.drop(1) // Skip initial value
            .collect(currentOnScrollInProgress)
    }

    // Snap to the nearest item when scrolling stops
    LaunchedEffect(listState) {
        val itemHeightPx = with(density) { itemHeight.toPx() }
        snapshotFlow { listState.isScrollInProgress }
            .filter { isScrollInProgress -> !isScrollInProgress && listState.firstVisibleItemScrollOffset > 0 }
            .collect {
                val nearestItemIndex =
                    listState.firstVisibleItemIndex + (listState.firstVisibleItemScrollOffset / itemHeightPx).roundToInt()
                try {
                    listState.animateScrollToItem(nearestItemIndex)
                } catch (_: Exception) {
                    // Animation interrupted, do nothing
                }
            }
    }

    // Perform haptic feedback while scrolling and passing an item
    LaunchedEffect(listState) {
        val itemHeightPx = with(density) { itemHeight.toPx() }
        val noFeedbackRange = (itemHeightPx * 0.1).roundToInt()..(itemHeightPx * 0.9).roundToInt()
        snapshotFlow { listState.firstVisibleItemScrollOffset }
            .filter { listState.isScrollInProgress }
            .map { firstVisibleItemScrollOffset -> firstVisibleItemScrollOffset !in noFeedbackRange }
            .distinctUntilChanged()
            .filter { isOutsideNoFeedbackRange -> isOutsideNoFeedbackRange }
            .drop(1) // Don't perform haptic feedback when first scrolling from initial value
            .collect {
                hapticFeedback.performHapticFeedback(HapticFeedbackType.SegmentTick)
            }
    }

    CompositionLocalProvider(LocalContentColor provides color) {
        LazyColumn(
            modifier = Modifier
                .height(height)
                .verticalFade()
                .then(modifier),
            state = listState,
            userScrollEnabled = enabled && userScrollEnabled,
        ) {
            items(ITEM_COUNT, key = { index -> index - START_INDEX }) { index ->
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(itemHeight),
                    contentAlignment = Alignment.Center,
                ) {
                    val offset = index - START_INDEX
                    Text(text(offset), Modifier.testTag(TestTags.DATE_TIME_PICKER_ITEM))
                }
            }
        }
    }
}

// Previews

@Preview(showBackground = true)
@Composable
private fun DefaultPreview() {
    AppTheme {
        Surface {
            val now = LocalClock.current.now()
            DateTimePicker(
                state = rememberDateTimePickerState(now.toEpochMilliseconds(), TimeZone.currentSystemDefault()),
                viewportBounds = null,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun DisabledPreview() {
    AppTheme {
        Surface {
            val now = LocalClock.current.now()
            DateTimePicker(
                state = rememberDateTimePickerState(now.toEpochMilliseconds(), TimeZone.currentSystemDefault()),
                viewportBounds = null,
                enabled = false,
            )
        }
    }
}
