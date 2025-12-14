package app.traced_it.ui.components

import android.text.format.DateUtils
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.gestures.stopScroll
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
import app.traced_it.lib.*
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

private data class Item<T>(val value: T, val index: Int)

private class DayPagingSource(
    val middleItemIndexAdjustment: Int,
    val zone: TimeZone,
    val initialValue: Day,
    val batchSize: Int, // Use instead of LoadParams.loadSize to have pages of equal size and prevent key conflicts
) : PagingSource<Int, Item<Day>>() {
    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Item<Day>> {
        (params.key ?: 0).let { pageNumber ->
            val calendar = gregorianCalendar(zone, day = initialValue, hour = 12)
            // Adjust the index, so the first item we render is an item before the initial one, and the initial item is
            // thus in the middle of the scroll container height.
            var index = pageNumber * batchSize - middleItemIndexAdjustment
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
    val middleItemIndexAdjustment: Int,
    val range: IntRange,
    val initialValue: Int,
    val batchSize: Int, // Use instead of LoadParams.loadSize to have pages of equal size and prevent key conflicts
) : PagingSource<Int, Item<Int>>() {
    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Item<Int>> =
        (params.key ?: 0).let { pageNumber ->
            val rangeSize = (range.last - range.first + 1)
            // Adjust the index, so the first item we render is an item before the initial one, and the initial item is
            // thus in the middle of the scroll container height.
            var index = pageNumber * batchSize - middleItemIndexAdjustment
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
    val itemsPerHeight = 3
    val middleItemIndexAdjustment = itemsPerHeight / 2
    val borderColor = MaterialTheme.colorScheme.outline
    val borderWidth = 1.dp
    val dayPageSize = 7
    val hourPageSize = 12
    val minutePageSize = 30

    val zone = TimeZone.getDefault()
    val today = gregorianCalendar(zone).day
    val initialCalendar = gregorianCalendar(zone, initialValue)

    var day by remember { mutableStateOf(Item(value = initialCalendar.day, index = -middleItemIndexAdjustment)) }
    var hour by remember { mutableStateOf(Item(value = initialCalendar.hour, index = -middleItemIndexAdjustment)) }
    var minute by remember { mutableStateOf(Item(value = initialCalendar.minute, index = -middleItemIndexAdjustment)) }

    val days = Pager(PagingConfig(pageSize = dayPageSize, enablePlaceholders = true)) {
        DayPagingSource(middleItemIndexAdjustment, zone, initialCalendar.day, batchSize = dayPageSize)
    }.flow.collectAsLazyPagingItems()
    val hours = Pager(PagingConfig(pageSize = hourPageSize, enablePlaceholders = true)) {
        RangePagingSource(middleItemIndexAdjustment, 0..23, initialCalendar.hour, batchSize = hourPageSize)
    }.flow.collectAsLazyPagingItems()
    val minutes = Pager(PagingConfig(pageSize = minutePageSize, enablePlaceholders = true)) {
        RangePagingSource(middleItemIndexAdjustment, 0..59, initialCalendar.minute, batchSize = minutePageSize)
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
                        days.itemSnapshotList.items.indexOfFirst { it.index == -middleItemIndexAdjustment }
                            .takeIf { it != -1 }
                            ?.let {
                                launch {
                                    dayListState.stopScroll(MutatePriority.UserInput)
                                    if (abs(day.index) > dayPageSize) {
                                        // If the current item is further than one page from the initial item, do a quick
                                        // non-animated scroll, because the animated scroll is not smooth when a page needs to
                                        // be loaded in the process.
                                        dayListState.scrollToItem(it)
                                    } else {
                                        dayListState.animateScrollToItem(it)
                                    }
                                }
                            }
                        hours.itemSnapshotList.items.indexOfFirst { it.index == -middleItemIndexAdjustment }
                            .takeIf { it != -1 }
                            ?.let {
                                launch {
                                    hourListState.stopScroll(MutatePriority.UserInput)
                                    if (abs(hour.index) > hourPageSize) {
                                        hourListState.scrollToItem(it)
                                    } else {
                                        hourListState.animateScrollToItem(it)
                                    }
                                }
                            }
                        minutes.itemSnapshotList.items.indexOfFirst { it.index == -middleItemIndexAdjustment }
                            .takeIf { it != -1 }
                            ?.let {
                                launch {
                                    minuteListState.stopScroll(MutatePriority.UserInput)
                                    if (abs(minute.index) > minutePageSize) {
                                        minuteListState.scrollToItem(it)
                                    } else {
                                        minuteListState.animateScrollToItem(it)
                                    }
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
                    itemsPerHeight = itemsPerHeight,
                    modifier = Modifier
                        .weight(3f)
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
                    itemsPerHeight = itemsPerHeight,
                    modifier = Modifier
                        .weight(1f)
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
                    itemsPerHeight = itemsPerHeight,
                    modifier = Modifier.weight(1f),
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
    @Suppress("SameParameterValue")
    itemsPerHeight: Int,
    modifier: Modifier = Modifier,
    text: @Composable (item: Item<T>) -> String,
) {
    val density = LocalDensity.current
    val hapticFeedback = LocalHapticFeedback.current
    val itemHeight = height / itemsPerHeight.toFloat()

    // Call onValueChange()
    LaunchedEffect(listState) {
        val middleItemIndexAdjustment = itemsPerHeight / 2
        snapshotFlow { listState.firstVisibleItemIndex }
            .drop(1) // Don't call onValueChange() when the composable is first rendered
            .debounce(100) // Prevent too many emissions when resetting the segment
            .collect { firstVisibleItemIndex ->
                items[firstVisibleItemIndex + middleItemIndexAdjustment]?.let { value ->
                    onValueChange(value)
                }
            }
    }

    // Snap to nearest item when scrolling stops
    LaunchedEffect(listState) {
        val itemHeightPx = with(density) { itemHeight.toPx() }
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
