package app.traced_it.ui.entry

import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.gestures.stopScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.LayoutBoundsHolder
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.paging.*
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import app.traced_it.R
import app.traced_it.lib.*
import app.traced_it.ui.entry.EntryDetailAction
import app.traced_it.ui.theme.AppTheme
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import java.util.*
import kotlin.collections.indexOfFirst
import kotlin.time.ExperimentalTime

private suspend fun <T> reset(
    items: LazyPagingItems<app.traced_it.ui.components.TracedTimePickerItem<T>>,
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
        _root_ide_package_.app.traced_it.ui.components.DayPagingSource(initialCalendar, daysPageSize, itemsPerHeight)
    }.flow.collectAsLazyPagingItems()
    val hours = Pager(PagingConfig(pageSize = hoursPageSize, enablePlaceholders = true)) {
        _root_ide_package_.app.traced_it.ui.components.RangePagingSource(
            0..23,
            initialCalendar.hour,
            hoursPageSize,
            itemsPerHeight
        )
    }.flow.collectAsLazyPagingItems()
    val minutes = Pager(PagingConfig(pageSize = minutesPageSize, enablePlaceholders = true)) {
        _root_ide_package_.app.traced_it.ui.components.RangePagingSource(
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
        itemsPerHeight = itemsPerHeight,
        viewportBounds = viewportBounds,
    )
}

@OptIn(ExperimentalTime::class)
@Composable
private fun CreatedAtControl(
    initialCalendar: Calendar,
    days: LazyPagingItems<app.traced_it.ui.components.TracedTimePickerItem<Day>>,
    hours: LazyPagingItems<app.traced_it.ui.components.TracedTimePickerItem<Int>>,
    minutes: LazyPagingItems<app.traced_it.ui.components.TracedTimePickerItem<Int>>,
    onValueChange: (value: Long) -> Unit,
    @Suppress("SameParameterValue")
    itemsPerHeight: Int,
    viewportBounds: LayoutBoundsHolder?,
) {
    val middleItemIndexAdjustment = itemsPerHeight / 2

    val coroutineScope = rememberCoroutineScope()
    val hapticFeedback = LocalHapticFeedback.current

    var day by remember {
        mutableStateOf(
            _root_ide_package_.app.traced_it.ui.components.TracedTimePickerItem(
                value = initialCalendar.day,
                index = -middleItemIndexAdjustment
            )
        )
    }
    var hour by remember {
        mutableStateOf(
            _root_ide_package_.app.traced_it.ui.components.TracedTimePickerItem(
                value = initialCalendar.hour,
                index = -middleItemIndexAdjustment
            )
        )
    }
    var minute by remember {
        mutableStateOf(
            _root_ide_package_.app.traced_it.ui.components.TracedTimePickerItem(
                value = initialCalendar.minute,
                index = -middleItemIndexAdjustment
            )
        )
    }
    val daysListState = rememberLazyListState()
    val hoursListState = rememberLazyListState()
    val minutesListState = rememberLazyListState()

    _root_ide_package_.app.traced_it.ui.components.TracedControl(
        label = stringResource(R.string.detail_created_at_label),
        labelButton = {
            TextButton(
                onClick = {
                    coroutineScope.launch {
                        // Reset segments in sequence and set state in sequence, because when doing it in parallel,
                        // a segment sometimes ends up showing an incorrect value for some reason.
                        reset(days, daysListState, itemsPerHeight)
                        reset(hours, hoursListState, itemsPerHeight)
                        reset(minutes, minutesListState, itemsPerHeight)
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.SegmentTick)
                        day =
                            _root_ide_package_.app.traced_it.ui.components.TracedTimePickerItem(initialCalendar.day, 0)
                        hour =
                            _root_ide_package_.app.traced_it.ui.components.TracedTimePickerItem(initialCalendar.hour, 0)
                        minute = _root_ide_package_.app.traced_it.ui.components.TracedTimePickerItem(
                            initialCalendar.minute,
                            0
                        )
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
        _root_ide_package_.app.traced_it.ui.components.TracedTimePicker(
            days = days,
            hours = hours,
            minutes = minutes,
            daysListState = daysListState,
            hoursListState = hoursListState,
            minutesListState = minutesListState,
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
                                _root_ide_package_.app.traced_it.ui.components.TracedTimePickerItem(
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
                                _root_ide_package_.app.traced_it.ui.components.TracedTimePickerItem(
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
                                _root_ide_package_.app.traced_it.ui.components.TracedTimePickerItem(
                                    value,
                                    -middleItemIndexAdjustment + i
                                )
                            }
                    )
                ).collectAsLazyPagingItems(),
                onValueChange = {},
                itemsPerHeight = itemsPerHeight,
                viewportBounds = null,
            )
        }
    }
}
