package app.traced_it.ui.components

import android.text.format.DateUtils
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
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingSource
import androidx.paging.PagingState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
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

private data class Day(val year: Int, val month: Int, val date: Int)

private var Calendar.day
    get() = Day(this.get(Calendar.YEAR), this.get(Calendar.MONTH), this.get(Calendar.DATE))
    set(day) {
        this.set(day.year, day.month, day.date)
    }

private var Calendar.hour
    get() = this.get(Calendar.HOUR_OF_DAY)
    set(hour) {
        this.set(Calendar.HOUR_OF_DAY, hour)
    }

private var Calendar.minute
    get() = this.get(Calendar.MINUTE)
    set(minute) {
        this.set(Calendar.MINUTE, minute)
    }

private fun Calendar.addDays(days: Int) {
    this.add(Calendar.DATE, days)
}

private data class Item<T>(val index: Int, val value: T)

private class DayPagingSource(val initialValue: Day) : PagingSource<Int, Item<Day>>() {
    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Item<Day>> {
        (params.key ?: 0).let { pageNumber ->
            val calendar = GregorianCalendar.getInstance(TimeZone.getDefault()).apply { day = initialValue }
            // Subtract 1 from index, so the first item we render is an item before the initial one, and the initial
            // item is thus on the second position, i.e. in the middle of the scroll container.
            var index = pageNumber * params.loadSize - 1
            calendar.addDays(index)
            val data = List(params.loadSize) { i ->
                if (i != 0) {
                    calendar.addDays(1)
                }
                Item(value = calendar.day, index = index++)
            }
            return LoadResult.Page(
                data = data,
                prevKey = pageNumber - 1,
                nextKey = pageNumber + 1,
            )
        }
    }

    override fun getRefreshKey(state: PagingState<Int, Item<Day>>): Int? =
        state.anchorPosition?.let {
            state.closestPageToPosition(it)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(it)?.nextKey?.minus(1)
        }
}

private class RangePagingSource(val from: Int, val to: Int, val initialValue: Int) : PagingSource<Int, Item<Int>>() {
    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Item<Int>> =
        (params.key ?: 0).let { pageNumber ->
            val rangeSize = (to - from + 1)
            // Subtract 1 from index, so the first item we render is an item before the initial one, and the initial
            // item is thus on the second position, i.e. in the middle of the scroll container.
            var index = pageNumber * params.loadSize - 1
            val data = List(params.loadSize) {
                Item(value = (index + initialValue).mod(rangeSize), index = index++)
            }
            LoadResult.Page(
                data = data,
                prevKey = pageNumber - 1,
                nextKey = pageNumber + 1,
            )
        }

    override fun getRefreshKey(state: PagingState<Int, Item<Int>>): Int? =
        state.anchorPosition?.let {
            state.closestPageToPosition(it)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(it)?.nextKey?.minus(1)
        }
}

@OptIn(ExperimentalTime::class)
@Composable
fun TracedTimePicker(
    initialValue: Long,
    onValueChange: (value: Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val coroutineScope = rememberCoroutineScope()

    val height = Spacing.inputHeight * 1.25f
    val borderColor = MaterialTheme.colorScheme.outline
    val borderWidth = 1.dp
    val dayWeight = 3f
    val hourWeight = 1f
    val minuteWeight = 1f

    val today = GregorianCalendar.getInstance(TimeZone.getDefault()).apply { time = Date() }.day
    val initialCalendar = GregorianCalendar.getInstance(TimeZone.getDefault()).apply { time = Date(initialValue) }
    val calendar = GregorianCalendar.getInstance(TimeZone.getDefault()).apply { time = Date(initialValue) }
    val days = Pager(PagingConfig(pageSize = 20, enablePlaceholders = true)) {
        DayPagingSource(initialCalendar.day)
    }.flow.collectAsLazyPagingItems()
    val hours = Pager(PagingConfig(pageSize = 20, enablePlaceholders = true)) {
        RangePagingSource(0, 23, initialCalendar.hour)
    }.flow.collectAsLazyPagingItems()
    val minutes = Pager(PagingConfig(pageSize = 20, enablePlaceholders = true)) {
        RangePagingSource(0, 59, initialCalendar.minute)
    }.flow.collectAsLazyPagingItems()
    val dayListState = rememberLazyListState()
    val hourListState = rememberLazyListState()
    val minuteListState = rememberLazyListState()

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
                        calendar.day = initialCalendar.day
                        calendar.hour = initialCalendar.hour
                        calendar.minute = initialCalendar.minute
                        days.itemSnapshotList.items.indexOfFirst { it.index == -1 }.takeIf { it != -1 }?.let {
                            launch { dayListState.animateScrollToItem(it) }
                        }
                        hours.itemSnapshotList.items.indexOfFirst { it.index == -1 }.takeIf { it != -1 }?.let {
                            launch { hourListState.animateScrollToItem(it) }
                        }
                        minutes.itemSnapshotList.items.indexOfFirst { it.index == -1 }.takeIf { it != -1 }?.let {
                            launch { minuteListState.animateScrollToItem(it) }
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
                    listState = dayListState,
                    items = days,
                    onValueChange = { item ->
                        calendar.day = item.value
                        onValueChange(calendar.timeInMillis)
                    },
                    height = height,
                    modifier = Modifier
                        .weight(dayWeight)
                        .rightBorder(borderColor, borderWidth),
                ) { item ->
                    if (item.value == today) {
                        stringResource(R.string.detail_created_at_today)
                    } else {
                        DateUtils.formatDateTime(
                            LocalContext.current,
                            GregorianCalendar.getInstance(TimeZone.getDefault()).apply { this.day = item.value }
                                .timeInMillis,
                            DateUtils.FORMAT_SHOW_YEAR or DateUtils.FORMAT_SHOW_DATE,
                        )
                    }
                }
                TracedTimePickerSegment(
                    listState = hourListState,
                    items = hours,
                    onValueChange = { item ->
                        calendar.hour = item.value
                        onValueChange(calendar.timeInMillis)
                    },
                    height = height,
                    modifier = Modifier
                        .weight(hourWeight)
                        .rightBorder(borderColor, borderWidth),
                ) { item ->
                    if (item.value <= 9) {
                        "0${item.value}"
                    } else {
                        item.value.toString()
                    }
                }
                TracedTimePickerSegment(
                    listState = minuteListState,
                    items = minutes,
                    onValueChange = { item ->
                        calendar.minute = item.value
                        onValueChange(calendar.timeInMillis)
                    },
                    height = height,
                    modifier = Modifier.weight(minuteWeight),
                ) { item ->
                    if (item.value <= 9) {
                        "0${item.value}"
                    } else {
                        item.value.toString()
                    }
                }
            }
        }
    }
}

@OptIn(FlowPreview::class)
@Composable
private fun <T : Any> TracedTimePickerSegment(
    listState: LazyListState,
    items: LazyPagingItems<Item<T>>,
    onValueChange: (item: Item<T>) -> Unit,
    height: Dp,
    modifier: Modifier = Modifier,
    text: @Composable (item: Item<T>) -> String,
) {
    val density = LocalDensity.current
    val hapticFeedback = LocalHapticFeedback.current
    val optionHeight = height * 0.333f
    val optionHeightPx = with(density) { optionHeight.toPx() }

    // Call the value-change callback
    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .drop(1) // Don't call onValueChange() when the composable is first rendered
            .collect { firstVisibleItemIndex ->
                items[firstVisibleItemIndex]?.let { value ->
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
        items(
            items.itemCount,
            key = items.itemKey { it.index } // FIXME Crash key already used
        ) { index ->
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(optionHeight),
                contentAlignment = Alignment.Center
            ) {
                items[index]?.let { item ->
                    Text(text(item))
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
            TracedTimePicker(
                initialValue = System.currentTimeMillis(),
                onValueChange = {},
            )
        }
    }
}
