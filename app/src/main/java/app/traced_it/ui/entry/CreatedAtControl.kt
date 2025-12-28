package app.traced_it.ui.entry

import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.gestures.stopScroll
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.retain.retain
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.LayoutBoundsHolder
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import app.traced_it.R
import app.traced_it.lib.*
import app.traced_it.ui.components.*
import app.traced_it.ui.theme.AppTheme
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import java.util.*
import kotlin.time.ExperimentalTime

private suspend fun <T> reset(
    items: LazyPagingItems<TracedTimePickerItem<T>>,
    listState: LazyListState,
    itemsPerHeight: Int,
) {
    val middleItemIndexAdjustment = itemsPerHeight / 2
    val initialItemIndex = -middleItemIndexAdjustment
    val initialTracedTimePickerItemListIndex =
        items.itemSnapshotList.items.indexOfFirst { it.index == initialItemIndex }
    if (initialTracedTimePickerItemListIndex != -1) {
        listState.stopScroll(MutatePriority.UserInput)
        listState.scrollToItem(initialTracedTimePickerItemListIndex)
    }
}

@OptIn(ExperimentalTime::class)
@Composable
fun CreatedAtControl(
    action: EntryDetailAction,
    onValueChange: (value: Long) -> Unit,
    onChangeInProgress: (changeInProgress: Boolean) -> Unit,
    viewportBounds: LayoutBoundsHolder?,
) {
    val itemsPerHeight = 3
    val daysPageSize = 7
    val hoursPageSize = 12
    val minutesPageSize = 30

    val zone = TimeZone.getDefault()
    val initialValue = when (action) {
        // Reading action here and not in EntryDetailDialog reduces the number of recompositions
        is EntryDetailAction.New -> System.currentTimeMillis()
        is EntryDetailAction.Edit -> action.entry.createdAt
        is EntryDetailAction.Prefill -> System.currentTimeMillis()
    }
    val initialCalendar = gregorianCalendar(zone, initialValue)

    val days = Pager(PagingConfig(pageSize = daysPageSize, enablePlaceholders = true)) {
        DayPagingSource(initialCalendar, daysPageSize, itemsPerHeight)
    }.flow.collectAsLazyPagingItems()
    val hours = Pager(PagingConfig(pageSize = hoursPageSize, enablePlaceholders = true)) {
        RangePagingSource(
            0..23,
            initialCalendar.hour,
            hoursPageSize,
            itemsPerHeight
        )
    }.flow.collectAsLazyPagingItems()
    val minutes = Pager(PagingConfig(pageSize = minutesPageSize, enablePlaceholders = true)) {
        RangePagingSource(
            0..59,
            initialCalendar.minute,
            minutesPageSize,
            itemsPerHeight
        )
    }.flow.collectAsLazyPagingItems()

    CreatedAtControl(
        initialCalendar = initialCalendar,
        days = days,
        hours = hours,
        minutes = minutes,
        onValueChange = onValueChange,
        onChangeInProgress = onChangeInProgress,
        itemsPerHeight = itemsPerHeight,
        viewportBounds = viewportBounds,
    )
}

@OptIn(ExperimentalTime::class)
@Composable
private fun CreatedAtControl(
    initialCalendar: Calendar,
    days: LazyPagingItems<TracedTimePickerItem<Day>>,
    hours: LazyPagingItems<TracedTimePickerItem<Int>>,
    minutes: LazyPagingItems<TracedTimePickerItem<Int>>,
    onValueChange: (value: Long) -> Unit,
    onChangeInProgress: (changeInProgress: Boolean) -> Unit,
    @Suppress("SameParameterValue")
    itemsPerHeight: Int,
    viewportBounds: LayoutBoundsHolder?,
) {
    val middleItemIndexAdjustment = itemsPerHeight / 2

    val coroutineScope = rememberCoroutineScope()
    val hapticFeedback = LocalHapticFeedback.current

    var day by retain {
        mutableStateOf(
            TracedTimePickerItem(
                value = initialCalendar.day,
                index = -middleItemIndexAdjustment
            )
        )
    }
    var hour by retain {
        mutableStateOf(
            TracedTimePickerItem(
                value = initialCalendar.hour,
                index = -middleItemIndexAdjustment
            )
        )
    }
    var minute by retain {
        mutableStateOf(
            TracedTimePickerItem(
                value = initialCalendar.minute,
                index = -middleItemIndexAdjustment
            )
        )
    }
    val dayListState = rememberLazyListState()
    val hourListState = rememberLazyListState()
    val minuteListState = rememberLazyListState()

    TracedControl(
        label = stringResource(R.string.detail_created_at_label),
        labelButton = {
            TextButton(
                onClick = {
                    coroutineScope.launch {
                        // Reset segments in sequence and set state in sequence, because when doing it in parallel,
                        // a segment sometimes ends up showing an incorrect value for some reason.
                        reset(days, dayListState, itemsPerHeight)
                        reset(hours, hourListState, itemsPerHeight)
                        reset(minutes, minuteListState, itemsPerHeight)
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.SegmentTick)
                        day = TracedTimePickerItem(initialCalendar.day, 0)
                        hour = TracedTimePickerItem(initialCalendar.hour, 0)
                        minute = TracedTimePickerItem(initialCalendar.minute, 0)
                        onValueChange(initialCalendar.copy(day.value, hour.value, minute.value).timeInMillis)
                    }
                },
                modifier = Modifier.padding(end = 7.dp), // Align with UnitSelect's dropdown
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            ) {
                Text(
                    stringResource(R.string.detail_created_at_reset),
                    fontWeight = FontWeight.Normal,
                )
            }
        },
    ) {
        TracedTimePicker(
            days = days,
            hours = hours,
            minutes = minutes,
            dayListState = dayListState,
            hourListState = hourListState,
            minuteListState = minuteListState,
            onDayChange = { item ->
                day = item
                onValueChange(initialCalendar.copy(day.value, hour.value, minute.value).timeInMillis)
            },
            onHourChange = { item ->
                hour = item
                onValueChange(initialCalendar.copy(day.value, hour.value, minute.value).timeInMillis)
            },
            onMinuteChange = { item ->
                minute = item
                onValueChange(initialCalendar.copy(day.value, hour.value, minute.value).timeInMillis)
            },
            onChangeInProgress = onChangeInProgress,
            itemsPerHeight = itemsPerHeight,
            viewportBounds = viewportBounds,
            zone = initialCalendar.timeZone,
        )
    }
}

// Previews

@Preview(showBackground = true)
@Composable
private fun DefaultPreview() {
    AppTheme {
        Surface {
            val initialCalendar = gregorianCalendar(TimeZone.getDefault())
            val itemsPerHeight = 3
            val middleItemIndexAdjustment = itemsPerHeight / 2
            val daysPageSize = 7
            val hoursPageSize = 12
            val minutesPageSize = 30
            CreatedAtControl(
                initialCalendar = initialCalendar,
                days = flowOf(
                    PagingData.from(
                        initialCalendar.generateDaysList(
                            -middleItemIndexAdjustment, daysPageSize
                        )
                            .mapIndexed { i, value ->
                                TracedTimePickerItem(
                                    value,
                                    -middleItemIndexAdjustment + i
                                )
                            }
                    )
                ).collectAsLazyPagingItems(),
                hours = flowOf(
                    PagingData.from(
                        (0..23).generateNumbersList(
                            initialCalendar.hour - middleItemIndexAdjustment, hoursPageSize
                        )
                            .mapIndexed { i, value ->
                                TracedTimePickerItem(
                                    value,
                                    -middleItemIndexAdjustment + i
                                )
                            }
                    )
                ).collectAsLazyPagingItems(),
                minutes = flowOf(
                    PagingData.from(
                        (0..59).generateNumbersList(
                            initialCalendar.minute - middleItemIndexAdjustment, minutesPageSize
                        )
                            .mapIndexed { i, value ->
                                TracedTimePickerItem(
                                    value,
                                    -middleItemIndexAdjustment + i
                                )
                            }
                    )
                ).collectAsLazyPagingItems(),
                onValueChange = {},
                onChangeInProgress = {},
                itemsPerHeight = itemsPerHeight,
                viewportBounds = null,
            )
        }
    }
}
