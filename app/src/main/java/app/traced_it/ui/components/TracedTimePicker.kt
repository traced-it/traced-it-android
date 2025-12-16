package app.traced_it.ui.components

import android.text.format.DateUtils
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.LayoutBoundsHolder
import androidx.compose.ui.layout.onVisibilityChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.paging.PagingData
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
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flowOf
import java.util.*
import kotlin.math.roundToInt

data class TracedTimePickerItem<T>(val value: T, val index: Int)

class DayPagingSource(
    val initialCalendar: Calendar,
    val batchSize: Int, // Use instead of LoadParams.loadSize to have pages of equal size and prevent key conflicts
    val itemsPerHeight: Int,
) : PagingSource<Int, TracedTimePickerItem<Day>>() {
    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, TracedTimePickerItem<Day>> {
        (params.key ?: 0).let { pageNumber ->
            // Adjust the index, so the first item we render is an item before the initial one, and the initial item is
            // thus in the middle of the scroll container height.
            val middleItemIndexAdjustment = itemsPerHeight / 2
            val startOffset = pageNumber * batchSize - middleItemIndexAdjustment
            val data = initialCalendar.generateDaysList(startOffset, batchSize).mapIndexed { i, value ->
                TracedTimePickerItem(value, startOffset + i)
            }
            return LoadResult.Page(
                data = data,
                prevKey = pageNumber - 1,
                nextKey = pageNumber + 1,
            )
        }
    }

    override fun getRefreshKey(state: PagingState<Int, TracedTimePickerItem<Day>>): Int? =
        state.anchorPosition?.let {
            state.closestPageToPosition(it)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(it)?.nextKey?.minus(1)
        }
}

class RangePagingSource(
    val range: IntRange,
    val initialValue: Int,
    val batchSize: Int, // Use instead of LoadParams.loadSize to have pages of equal size and prevent key conflicts
    val itemsPerHeight: Int,
) : PagingSource<Int, TracedTimePickerItem<Int>>() {
    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, TracedTimePickerItem<Int>> =
        (params.key ?: 0).let { pageNumber ->
            // Adjust the index, so the first item we render is an item before the initial one, and the initial item is
            // thus in the middle of the scroll container height.
            val middleItemIndexAdjustment = itemsPerHeight / 2
            val startOffset = pageNumber * batchSize - middleItemIndexAdjustment
            val data = range.generateNumbersList(initialValue + startOffset, batchSize).mapIndexed { i, value ->
                TracedTimePickerItem(value, startOffset + i)
            }
            LoadResult.Page(
                data = data,
                prevKey = pageNumber - 1,
                nextKey = pageNumber + 1,
            )
        }

    override fun getRefreshKey(state: PagingState<Int, TracedTimePickerItem<Int>>): Int? =
        state.anchorPosition?.let {
            state.closestPageToPosition(it)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(it)?.nextKey?.minus(1)
        }
}

@Composable
fun TracedTimePicker(
    days: LazyPagingItems<TracedTimePickerItem<Day>>,
    hours: LazyPagingItems<TracedTimePickerItem<Int>>,
    minutes: LazyPagingItems<TracedTimePickerItem<Int>>,
    dayListState: LazyListState,
    hourListState: LazyListState,
    minuteListState: LazyListState,
    onDayChange: (item: TracedTimePickerItem<Day>) -> Unit,
    onHourChange: (item: TracedTimePickerItem<Int>) -> Unit,
    onMinuteChange: (item: TracedTimePickerItem<Int>) -> Unit,
    onChangeInProgress: (changeInProgress: Boolean) -> Unit,
    itemsPerHeight: Int,
    viewportBounds: LayoutBoundsHolder?,
    zone: TimeZone,
) {
    val height = Spacing.inputHeight * 1.25f
    val borderColor = MaterialTheme.colorScheme.outline
    val borderWidth = 1.dp
    val today = gregorianCalendar(zone).day

    var userScrollEnabled by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.onVisibilityChanged(
            minFractionVisible = 0.5f,
            viewportBounds = viewportBounds,
        ) { visible ->
            userScrollEnabled = visible
        },
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = MaterialTheme.shapes.extraSmall,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        border = BorderStroke(width = borderWidth, color = MaterialTheme.colorScheme.outline),
    ) {
        Row {
            TracedTimePickerSegment(
                listState = dayListState,
                items = days,
                onChangeInProgress = onChangeInProgress,
                onValueChange = onDayChange,
                height = height,
                itemsPerHeight = itemsPerHeight,
                modifier = Modifier
                    .testTag("tracedTimePickerDaySegment")
                    .weight(3f)
                    .rightBorder(borderColor, borderWidth),
                userScrollEnabled = userScrollEnabled,
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
                onChangeInProgress = onChangeInProgress,
                onValueChange = onHourChange,
                height = height,
                itemsPerHeight = itemsPerHeight,
                modifier = Modifier
                    .testTag("tracedTimePickerHourSegment")
                    .weight(1f)
                    .rightBorder(borderColor, borderWidth),
                userScrollEnabled = userScrollEnabled,
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
                onChangeInProgress = onChangeInProgress,
                onValueChange = onMinuteChange,
                height = height,
                itemsPerHeight = itemsPerHeight,
                userScrollEnabled = userScrollEnabled,
                modifier = Modifier
                    .testTag("tracedTimePickerMinuteSegment")
                    .weight(1f),
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

@OptIn(FlowPreview::class)
@Composable
private fun <T : Any> TracedTimePickerSegment(
    listState: LazyListState,
    items: LazyPagingItems<TracedTimePickerItem<T>>,
    onChangeInProgress: (changeInProgress: Boolean) -> Unit,
    onValueChange: (item: TracedTimePickerItem<T>) -> Unit,
    height: Dp,
    itemsPerHeight: Int,
    userScrollEnabled: Boolean,
    modifier: Modifier = Modifier,
    text: @Composable (item: TracedTimePickerItem<T>) -> String,
) {
    val density = LocalDensity.current
    val hapticFeedback = LocalHapticFeedback.current
    val itemHeight = height / itemsPerHeight.toFloat()
    val middleItemIndexAdjustment = itemsPerHeight / 2

    // Call onChangeInProgress()
    LaunchedEffect(listState) {
        snapshotFlow { listState.isScrollInProgress }
            .collect {
                onChangeInProgress(listState.isScrollInProgress)
            }
    }

    // Call onValueChange()
    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .drop(1) // Don't call onValueChange() when the composable is first rendered
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
        userScrollEnabled = userScrollEnabled,
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
            val initialCalendar = gregorianCalendar(TimeZone.getDefault())
            val itemsPerHeight = 3
            val middleItemIndexAdjustment = itemsPerHeight / 2
            val daysPageSize = 7
            val hoursPageSize = 12
            val minutesPageSize = 30
            val dayListState = rememberLazyListState()
            val hourListState = rememberLazyListState()
            val minuteListState = rememberLazyListState()
            TracedTimePicker(
                days = flowOf(
                    PagingData.from(
                        initialCalendar.generateDaysList(
                            -middleItemIndexAdjustment, daysPageSize
                        )
                            .mapIndexed { i, value -> TracedTimePickerItem(value, -middleItemIndexAdjustment + i) }
                    )
                ).collectAsLazyPagingItems(),
                hours = flowOf(
                    PagingData.from(
                        (0..23).generateNumbersList(
                            initialCalendar.hour - middleItemIndexAdjustment, hoursPageSize
                        )
                            .mapIndexed { i, value -> TracedTimePickerItem(value, -middleItemIndexAdjustment + i) }
                    )
                ).collectAsLazyPagingItems(),
                minutes = flowOf(
                    PagingData.from(
                        (0..59).generateNumbersList(
                            initialCalendar.minute - middleItemIndexAdjustment, minutesPageSize
                        )
                            .mapIndexed { i, value -> TracedTimePickerItem(value, -middleItemIndexAdjustment + i) }
                    )
                ).collectAsLazyPagingItems(),
                dayListState = dayListState,
                hourListState = hourListState,
                minuteListState = minuteListState,
                onDayChange = {},
                onHourChange = {},
                onMinuteChange = {},
                onChangeInProgress = {},
                itemsPerHeight = itemsPerHeight,
                viewportBounds = null,
                zone = TimeZone.getDefault(),
            )
        }
    }
}
