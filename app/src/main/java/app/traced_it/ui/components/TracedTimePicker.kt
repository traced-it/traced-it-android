package app.traced_it.ui.components

import android.text.format.DateUtils
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import java.util.*
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.time.ExperimentalTime

private data class Day(val year: Int, val month: Int, val date: Int)

private fun gregorianCalendar(
    zone: TimeZone,
    time: Long? = null,
    day: Day? = null,
    hour: Int? = null,
    minute: Int? = null,
): Calendar =
    GregorianCalendar.getInstance(zone).apply {
        if (time != null) this.time = Date(time)
        if (day != null) this.day = day
        if (hour != null) this.hour = hour
        if (minute != null) this.minute = minute
    }

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

private data class Item<T>(val value: T, val index: Int)

private class DayPagingSource(
    val zone: TimeZone,
    val initialValue: Day,
    val batchSize: Int, // Use instead of LoadParams.loadSize to have pages of equal size and prevent key conflicts
) : PagingSource<Int, Item<Day>>() {
    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Item<Day>> {
        (params.key ?: 0).let { pageNumber ->
            val calendar = gregorianCalendar(zone, day = initialValue, hour = 12)
            // Subtract 1 from index, so the first item we render is an item before the initial one, and the initial
            // item is thus on the second position, i.e. in the middle of the scroll container.
            var index = pageNumber * batchSize - 1
            calendar.addDays(index)
            val data = List(batchSize) { i ->
                if (i != 0) {
                    calendar.addDays(1)
                }
                Item(value = calendar.day, index++)
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

private class RangePagingSource(
    val from: Int,
    val to: Int,
    val initialValue: Int,
    val batchSize: Int, // Use instead of LoadParams.loadSize to have pages of equal size and prevent key conflicts
) : PagingSource<Int, Item<Int>>() {
    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Item<Int>> =
        (params.key ?: 0).let { pageNumber ->
            val rangeSize = (to - from + 1)
            // Subtract 1 from index, so the first item we render is an item before the initial one, and the initial
            // item is thus on the second position, i.e. in the middle of the scroll container.
            var index = pageNumber * batchSize - 1
            val data = List(batchSize) {
                Item(value = (index + initialValue).mod(rangeSize), index++)
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
    val dayPageSize = 7
    val hourPageSize = 12
    val minutePageSize = 30

    val zone = TimeZone.getDefault()
    val today = gregorianCalendar(zone).day
    val initialCalendar = gregorianCalendar(zone, initialValue)

    var day by remember { mutableStateOf(Item(value = initialCalendar.day, index = 0)) }
    var hour by remember { mutableStateOf(Item(value = initialCalendar.hour, index = 0)) }
    var minute by remember { mutableStateOf(Item(value = initialCalendar.minute, index = 0)) }

    val days = Pager(PagingConfig(pageSize = dayPageSize, enablePlaceholders = true)) {
        DayPagingSource(zone,initialCalendar.day, batchSize = dayPageSize)
    }.flow.collectAsLazyPagingItems()
    val hours = Pager(PagingConfig(pageSize = hourPageSize, enablePlaceholders = true)) {
        RangePagingSource(0, 23, initialCalendar.hour, batchSize = hourPageSize)
    }.flow.collectAsLazyPagingItems()
    val minutes = Pager(PagingConfig(pageSize = minutePageSize, enablePlaceholders = true)) {
        RangePagingSource(0, 59, initialCalendar.minute, batchSize = minutePageSize)
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
                        days.itemSnapshotList.items.indexOfFirst { it.index == -1 }.takeIf { it != -1 }?.let {
                            if (abs(day.index) > dayPageSize) {
                                // If the current item is further than one page from the initial item, do a quick
                                // non-animated scroll, because the animated scroll is not smooth when a page needs to
                                // be loaded in the process
                                launch { dayListState.scrollToItem(it) }
                            } else {
                                launch { dayListState.animateScrollToItem(it) }
                            }
                        }
                        hours.itemSnapshotList.items.indexOfFirst { it.index == -1 }.takeIf { it != -1 }?.let {
                            if (abs(hour.index) > hourPageSize) {
                                launch { hourListState.scrollToItem(it) }
                            } else {
                                launch { hourListState.animateScrollToItem(it) }
                            }
                        }
                        minutes.itemSnapshotList.items.indexOfFirst { it.index == -1 }.takeIf { it != -1 }?.let {
                            if (abs(minute.index) > minutePageSize) {
                                launch { minuteListState.scrollToItem(it) }
                            } else {
                                launch { minuteListState.animateScrollToItem(it) }
                            }
                        }
                        day = Item(initialCalendar.day, 0)
                        hour = Item(initialCalendar.hour, 0)
                        minute = Item(initialCalendar.minute, 0)
                        onValueChange(
                            gregorianCalendar(zone, initialValue, day.value, hour.value, minute.value).timeInMillis
                        )
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
                        day = item
                        onValueChange(
                            gregorianCalendar(zone, initialValue, day.value, hour.value, minute.value).timeInMillis
                        )
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
                            gregorianCalendar(zone, day = item.value).timeInMillis,
                            DateUtils.FORMAT_SHOW_YEAR or DateUtils.FORMAT_SHOW_DATE,
                        )
                    }
                }
                TracedTimePickerSegment(
                    listState = hourListState,
                    items = hours,
                    onValueChange = { item ->
                        hour = item
                        onValueChange(
                            gregorianCalendar(zone, initialValue, day.value, hour.value, minute.value).timeInMillis
                        )
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
                        minute = item
                        onValueChange(
                            gregorianCalendar(zone, initialValue, day.value, hour.value, minute.value).timeInMillis
                        )
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
    val itemHeight = height * 0.333f
    val itemHeightPx = with(density) { itemHeight.toPx() }

    // Call onValueChange()
    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .drop(1) // Don't call onValueChange() when the composable is first rendered
            .debounce(100) // Prevent too many emissions when resetting the segment
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
                    (listState.firstVisibleItemScrollOffset / itemHeightPx).roundToInt()
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
            key = items.itemKey { it.index }
        ) { index ->
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(itemHeight),
                contentAlignment = Alignment.Center,
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
