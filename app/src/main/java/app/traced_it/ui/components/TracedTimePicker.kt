package app.traced_it.ui.components

import android.text.format.DateUtils
import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.traced_it.R
import app.traced_it.ui.theme.AppTheme
import app.traced_it.ui.theme.Spacing
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import java.util.*
import kotlin.math.roundToInt
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
@Composable
fun TracedTimePicker(
    initialValue: Long,
    onValueChange: (value: Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val height = Spacing.inputHeight * 1.25f
    val borderColor = MaterialTheme.colorScheme.outline
    val borderWidth = 1.dp
    val dateWeight = 3f
    val hourWeight = 1f
    val minuteWeight = 1f
    val daysSize = 31 // TODO Replace with infinite lazy list

    val calendar = GregorianCalendar.getInstance(TimeZone.getDefault()).apply { time = Date(initialValue) }
    val initialDate = Triple(
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH) + 1,
        calendar.get(Calendar.DAY_OF_MONTH),
    )
    val initialHour = calendar.get(Calendar.HOUR_OF_DAY)
    val initialMinute = calendar.get(Calendar.MINUTE)
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

    val dateListState = rememberLazyListState(
        initialFirstVisibleItemIndex = dateItems.indexOfFirst { it.first == initialDate }.coerceAtLeast(0)
    )
    val hourListState = rememberLazyListState(
        initialFirstVisibleItemIndex = hourItems.indexOfFirst { it.first == initialHour }.coerceAtLeast(0)
    )
    val minuteListState = rememberLazyListState(
        initialFirstVisibleItemIndex = minuteItems.indexOfFirst { it.first == initialMinute }.coerceAtLeast(0)
    )

    Column(
        modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.windowPadding)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                stringResource(R.string.detail_created_at_label),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
            TextButton(
                onClick = {
                    coroutineScope.launch {
                        calendar.time = Date()
                        val currentDate = Triple(
                            calendar.get(Calendar.YEAR),
                            calendar.get(Calendar.MONTH) + 1,
                            calendar.get(Calendar.DAY_OF_MONTH),
                        )
                        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
                        val currentMinute = calendar.get(Calendar.MINUTE)
                        launch {
                            dateListState.animateScrollToItem(
                                dateItems.indexOfFirst { it.first == currentDate }.coerceAtLeast(0)
                            )
                        }
                        launch {
                            hourListState.animateScrollToItem(
                                hourItems.indexOfFirst { it.first == currentHour }.coerceAtLeast(0)
                            )
                        }
                        launch {
                            minuteListState.animateScrollToItem(
                                minuteItems.indexOfFirst { it.first == currentMinute }.coerceAtLeast(0)
                            )
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            ) {
                Text(
                    stringResource(R.string.detail_created_at_reset),
                    fontWeight = FontWeight.Normal,
                )
            }
        }
        Surface(
            color = MaterialTheme.colorScheme.secondaryContainer,
            shape = MaterialTheme.shapes.extraSmall,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            border = BorderStroke(width = borderWidth, color = MaterialTheme.colorScheme.outline),
        ) {
            Row {
                TracedTimePickerSegment(
                    listState = dateListState,
                    items = dateItems,
                    onValueChange = { (year, month, date) ->
                        calendar.set(year, month - 1, date) // FIXME Date is saved wrong
                        onValueChange(calendar.timeInMillis)
                    },
                    height = height,
                    modifier = Modifier
                        .weight(dateWeight)
                        .rightBorder(borderColor, borderWidth),
                )
                TracedTimePickerSegment(
                    listState = hourListState,
                    items = hourItems,
                    onValueChange = { hour ->
                        calendar.set(Calendar.HOUR, hour)
                        onValueChange(calendar.timeInMillis)
                    },
                    height = height,
                    modifier = Modifier
                        .weight(hourWeight)
                        .rightBorder(borderColor, borderWidth),
                )
                TracedTimePickerSegment(
                    listState = minuteListState,
                    items = minuteItems,
                    onValueChange = { minute ->
                        calendar.set(Calendar.MINUTE, minute)
                        onValueChange(calendar.timeInMillis)
                    },
                    height = height,
                    modifier = Modifier.weight(minuteWeight),
                )
            }
        }
    }
}

@OptIn(FlowPreview::class)
@Composable
private fun <T : Any> TracedTimePickerSegment(
    listState: LazyListState,
    items: List<Pair<T, @Composable () -> String>>,
    onValueChange: (value: T) -> Unit,
    height: Dp,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val hapticFeedback = LocalHapticFeedback.current
    val optionHeight = height * 0.333f
    val optionHeightPx = with(density) { optionHeight.toPx() }

    // Call onValueChange()
    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .drop(1) // Don't call onValueChange() when the composable is first rendered
            .collect { firstVisibleItemIndex ->
                items.getOrNull(firstVisibleItemIndex)?.let { (value) ->
                    onValueChange(value)
                }
            }
    }

    // Snap to nearest item when scrolling stops
    LaunchedEffect(listState) {
        snapshotFlow { listState.isScrollInProgress }
            .filter { !it && listState.firstVisibleItemScrollOffset > 0 }
            .collect {
                val nearestItemIndex = listState.firstVisibleItemIndex +
                    (listState.firstVisibleItemScrollOffset / optionHeightPx).roundToInt()
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
        modifier = Modifier
            .height(height)
            .verticalFade()
            .then(modifier),
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
                initialValue = System.currentTimeMillis(),
                onValueChange = {},
            )
        }
    }
}
