package app.traced_it.ui.components

import android.text.format.DateUtils
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.traced_it.R
import app.traced_it.ui.theme.AppTheme
import app.traced_it.ui.theme.Spacing
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.filter
import java.util.*
import kotlin.math.roundToInt
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
@Composable
fun TracedTimePicker(
    value: Long,
    onValueChange: (value: Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    // TODO Advance time picker every minute automatically unless user changes the value
    val context = LocalContext.current

    val height = Spacing.inputHeight * 1.25f
    val borderColor = MaterialTheme.colorScheme.outline
    val borderWidth = 1.dp
    val dateWeight = 3f
    val hourWeight = 1f
    val minuteWeight = 1f
    val daysSize = 31 // TODO Replace with infinite lazy list

    val calendar = GregorianCalendar.getInstance(TimeZone.getDefault()).apply { time = Date(value) }
    val initialDate = Triple(
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH) + 1,
        calendar.get(Calendar.DAY_OF_MONTH),
    )
    val initialHour = calendar.get(Calendar.HOUR_OF_DAY).coerceAtMost(23)
    val initialMinute = calendar.get(Calendar.MINUTE).coerceAtMost(59)
    val dateItems = buildList {
        add(initialDate to @Composable { stringResource(R.string.detail_created_at_today) })
        repeat(daysSize - 1) {
            calendar.add(Calendar.DAY_OF_MONTH, -1)
            val value = Triple(
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH) + 1,
                calendar.get(Calendar.DAY_OF_MONTH),
            )
            val text = DateUtils.formatDateTime(
                context,
                calendar.timeInMillis,
                DateUtils.FORMAT_SHOW_YEAR or DateUtils.FORMAT_SHOW_DATE
            )
            add(0, value to @Composable { text })
        }
        calendar.add(Calendar.DAY_OF_MONTH, daysSize - 1)
    }
    val hourItems = (0..23).map { hour ->
        hour to @Composable { hour.toString().padStart(2, '0') }
    }
    val minuteItems = (0..59).map { minute ->
        minute to @Composable { minute.toString().padStart(2, '0') }
    }

    Column(
        modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.windowPadding)
    ) {
        Text(
            stringResource(R.string.detail_created_at_label),
            Modifier.padding(bottom = Spacing.small),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
        )
        Surface(
            color = MaterialTheme.colorScheme.secondaryContainer,
            shape = MaterialTheme.shapes.extraSmall,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            border = BorderStroke(width = borderWidth, color = MaterialTheme.colorScheme.outline),
        ) {
            Row(Modifier.height(height)) {
                TracedTimePickerSegment(
                    items = dateItems,
                    initialValue = initialDate,
                    onValueChange = { (year, month, date) ->
                        calendar.set(year, month, date)
                        onValueChange(calendar.timeInMillis)
                    },
                    borderWidth = borderWidth,
                    borderColor = borderColor,
                    height = height,
                    modifier = Modifier.weight(dateWeight),
                )
                TracedTimePickerSegment(
                    items = hourItems,
                    initialValue = initialHour,
                    onValueChange = { hour ->
                        calendar.set(Calendar.HOUR, hour)
                        onValueChange(calendar.timeInMillis)
                    },
                    borderWidth = borderWidth,
                    borderColor = borderColor,
                    height = height,
                    modifier = Modifier.weight(hourWeight),
                )
                TracedTimePickerSegment(
                    items = minuteItems,
                    initialValue = initialMinute,
                    onValueChange = { minute ->
                        calendar.set(Calendar.MINUTE, minute)
                        onValueChange(calendar.timeInMillis)
                    },
                    borderWidth = borderWidth,
                    borderColor = borderColor,
                    height = height,
                    modifier = Modifier.weight(minuteWeight),
                    last = true,
                )
            }
        }
    }
}

@OptIn(FlowPreview::class)
@Composable
private fun <T : Any> TracedTimePickerSegment(
    items: List<Pair<T, @Composable () -> String>>,
    initialValue: T,
    onValueChange: (value: T) -> Unit,
    borderColor: Color,
    borderWidth: Dp,
    height: Dp,
    modifier: Modifier = Modifier,
    last: Boolean = false,
) {
    val density = LocalDensity.current
    val hapticFeedback = LocalHapticFeedback.current
    val optionHeight = height * 0.333f
    val optionHeightPx = with(density) { optionHeight.toPx() }

    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = items.indexOfFirst { (value) -> value == initialValue }.coerceAtLeast(0)
    )

    // Snap to nearest item when scrolling stops
    LaunchedEffect(listState) {
        snapshotFlow { listState.isScrollInProgress }
            .filter { !it && listState.firstVisibleItemScrollOffset > 0 }
            .collect {
                val nearestItemIndex = listState.firstVisibleItemIndex +
                    (listState.firstVisibleItemScrollOffset / optionHeightPx).roundToInt()
                items.getOrNull(nearestItemIndex)?.let { (value) ->
                    onValueChange(value)
                }
                try {
                    listState.animateScrollToItem(nearestItemIndex)
                } catch (_: Exception) {
                    // Animation interrupted, do nothing
                }
            }
    }

    // Perform haptic feedback when scrolling and passing an item
    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .filter { listState.isScrollInProgress }
            .collect {
                hapticFeedback.performHapticFeedback(HapticFeedbackType.SegmentTick)
            }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxHeight()
            .verticalFade()
            .run {
                if (!last) {
                    rightBorder(borderColor, borderWidth)
                } else {
                    this
                }
            },
        state = listState,
    ) {
        item(-1) {
            // Add spacer as the first and last item, so the selected item is in the middle of the box, not at the top
            Spacer(Modifier.height(optionHeight))
        }
        items(items.size, key = { i -> items[i].first }) { i ->
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(optionHeight),
                contentAlignment = Alignment.Center
            ) {
                Text(items[i].second())
            }
        }
        item(items.size + 1) {
            Spacer(Modifier.height(optionHeight))
        }
    }
}

// Previews

@Preview(showBackground = true)
@Composable
private fun DefaultPreview() {
    AppTheme {
        Surface {
            TracedTimePicker(
                value = System.currentTimeMillis(),
                onValueChange = {},
            )
        }
    }
}
