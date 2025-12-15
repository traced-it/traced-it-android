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
import androidx.compose.ui.layout.LayoutBoundsHolder
import androidx.compose.ui.layout.onVisibilityChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.paging.*
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import app.traced_it.R
import app.traced_it.lib.*
import app.traced_it.ui.entry.EntryDetailAction
import app.traced_it.ui.theme.AppTheme
import app.traced_it.ui.theme.Spacing
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import java.util.*
import kotlin.math.roundToInt
import kotlin.time.ExperimentalTime

private data class Item<T>(val value: T, val index: Int)

private const val ITEMS_PER_HEIGHT = 3
private const val MIDDLE_ITEM_INDEX_ADJUSTMENT = ITEMS_PER_HEIGHT / 2
private const val DAYS_PAGE_SIZE = 7
private const val HOURS_PAGE_SIZE = 12
private const val MINUTES_PAGE_SIZE = 30

private class DayPagingSource(
    val initialCalendar: Calendar,
    val batchSize: Int, // Use instead of LoadParams.loadSize to have pages of equal size and prevent key conflicts
) : PagingSource<Int, Item<Day>>() {
    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Item<Day>> {
        (params.key ?: 0).let { pageNumber ->
            // Adjust the index, so the first item we render is an item before the initial one, and the initial item is
            // thus in the middle of the scroll container height.
            val startOffset = pageNumber * batchSize - MIDDLE_ITEM_INDEX_ADJUSTMENT
            val data = initialCalendar.generateDaysList(startOffset, batchSize).mapIndexed { i, value ->
                Item(value, startOffset + i)
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
    val range: IntRange,
    val initialValue: Int,
    val batchSize: Int, // Use instead of LoadParams.loadSize to have pages of equal size and prevent key conflicts
) : PagingSource<Int, Item<Int>>() {
    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Item<Int>> =
        (params.key ?: 0).let { pageNumber ->
            // Adjust the index, so the first item we render is an item before the initial one, and the initial item is
            // thus in the middle of the scroll container height.
            val startOffset = pageNumber * batchSize - MIDDLE_ITEM_INDEX_ADJUSTMENT
            val data = range.generateNumbersList(initialValue + startOffset, batchSize).mapIndexed { i, value ->
                Item(value, startOffset + i)
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
    action: EntryDetailAction,
    onValueChange: (value: Long) -> Unit,
    viewportBounds: LayoutBoundsHolder?,
    modifier: Modifier = Modifier,
) {
    val zone = TimeZone.getDefault()
    val initialValue = when (action) {
        // Reading action here and not in EntryDetailDialog reduces the number of recompositions
        is EntryDetailAction.New -> System.currentTimeMillis()
        is EntryDetailAction.Edit -> action.entry.createdAt
        is EntryDetailAction.Prefill -> System.currentTimeMillis()
    }
    val initialCalendar = gregorianCalendar(zone, initialValue)

    val days = Pager(PagingConfig(pageSize = DAYS_PAGE_SIZE, enablePlaceholders = true)) {
        DayPagingSource(initialCalendar, batchSize = DAYS_PAGE_SIZE)
    }.flow.collectAsLazyPagingItems()
    val hours = Pager(PagingConfig(pageSize = HOURS_PAGE_SIZE, enablePlaceholders = true)) {
        RangePagingSource(0..23, initialCalendar.hour, batchSize = HOURS_PAGE_SIZE)
    }.flow.collectAsLazyPagingItems()
    val minutes = Pager(PagingConfig(pageSize = MINUTES_PAGE_SIZE, enablePlaceholders = true)) {
        RangePagingSource(0..59, initialCalendar.minute, batchSize = MINUTES_PAGE_SIZE)
    }.flow.collectAsLazyPagingItems()

    TracedTimePicker(
        initialCalendar = initialCalendar,
        days = days,
        hours = hours,
        minutes = minutes,
        itemsPerHeight = ITEMS_PER_HEIGHT,
        onValueChange = onValueChange,
        viewportBounds = viewportBounds,
        modifier = modifier,
    )
}

private suspend fun <T> reset(items: LazyPagingItems<Item<T>>, listState: LazyListState) {
    val initialItemListIndex =
        items.itemSnapshotList.items.indexOfFirst { it.index == -MIDDLE_ITEM_INDEX_ADJUSTMENT }
    if (initialItemListIndex != -1) {
        listState.stopScroll(MutatePriority.UserInput)
        listState.scrollToItem(initialItemListIndex)
    }
}

@OptIn(ExperimentalTime::class)
@Composable
private fun TracedTimePicker(
    initialCalendar: Calendar,
    days: LazyPagingItems<Item<Day>>,
    hours: LazyPagingItems<Item<Int>>,
    minutes: LazyPagingItems<Item<Int>>,
    @Suppress("SameParameterValue")
    itemsPerHeight: Int,
    viewportBounds: LayoutBoundsHolder?,
    onValueChange: (value: Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val coroutineScope = rememberCoroutineScope()
    val hapticFeedback = LocalHapticFeedback.current

    val height = Spacing.inputHeight * 1.25f
    val borderColor = MaterialTheme.colorScheme.outline
    val borderWidth = 1.dp

    val zone = initialCalendar.timeZone
    val today = gregorianCalendar(zone).day
    var day by remember {
        mutableStateOf(Item(value = initialCalendar.day, index = -MIDDLE_ITEM_INDEX_ADJUSTMENT))
    }
    var hour by remember {
        mutableStateOf(Item(value = initialCalendar.hour, index = -MIDDLE_ITEM_INDEX_ADJUSTMENT))
    }
    var minute by remember {
        mutableStateOf(Item(value = initialCalendar.minute, index = -MIDDLE_ITEM_INDEX_ADJUSTMENT))
    }
    val dayListState = rememberLazyListState()
    val hourListState = rememberLazyListState()
    val minuteListState = rememberLazyListState()
    var userScrollEnabled by remember { mutableStateOf(false) }

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
                        // Reset segments in sequence and set state in sequence, because when doing it in parallel,
                        // a segment sometimes ends up showing an incorrect value for some reason.
                        reset(days, dayListState)
                        reset(hours, hourListState)
                        reset(minutes, minuteListState)
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.SegmentTick)
                        day = Item(initialCalendar.day, 0)
                        hour = Item(initialCalendar.hour, 0)
                        minute = Item(initialCalendar.minute, 0)
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
        }
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
                    onValueChange = { item ->
                        day = item
                        onValueChange(initialCalendar.copy(day.value, hour.value, minute.value).timeInMillis)
                    },
                    height = height,
                    itemsPerHeight = itemsPerHeight,
                    modifier = Modifier
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
                    onValueChange = { item ->
                        hour = item
                        onValueChange(initialCalendar.copy(day.value, hour.value, minute.value).timeInMillis)
                    },
                    height = height,
                    itemsPerHeight = itemsPerHeight,
                    modifier = Modifier
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
                    onValueChange = { item ->
                        minute = item
                        onValueChange(initialCalendar.copy(day.value, hour.value, minute.value).timeInMillis)
                    },
                    height = height,
                    itemsPerHeight = itemsPerHeight,
                    userScrollEnabled = userScrollEnabled,
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
    userScrollEnabled: Boolean,
    modifier: Modifier = Modifier,
    text: @Composable (item: Item<T>) -> String,
) {
    val density = LocalDensity.current
    val hapticFeedback = LocalHapticFeedback.current
    val itemHeight = height / itemsPerHeight.toFloat()

    // Call onValueChange()
    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .drop(1) // Don't call onValueChange() when the composable is first rendered
            .debounce(100) // Prevent too many emissions when resetting the segment
            .collect { firstVisibleItemIndex ->
                items[firstVisibleItemIndex + MIDDLE_ITEM_INDEX_ADJUSTMENT]?.let { value ->
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
            TracedTimePicker(
                initialCalendar = initialCalendar,
                days = flowOf(
                    PagingData.from(
                        initialCalendar.generateDaysList(
                            -MIDDLE_ITEM_INDEX_ADJUSTMENT, DAYS_PAGE_SIZE
                        )
                            .mapIndexed { i, value -> Item(value, -MIDDLE_ITEM_INDEX_ADJUSTMENT + i) }
                    )
                ).collectAsLazyPagingItems(),
                hours = flowOf(
                    PagingData.from(
                        (0..23).generateNumbersList(
                            initialCalendar.hour - MIDDLE_ITEM_INDEX_ADJUSTMENT, HOURS_PAGE_SIZE
                        )
                            .mapIndexed { i, value -> Item(value, -MIDDLE_ITEM_INDEX_ADJUSTMENT + i) }
                    )
                ).collectAsLazyPagingItems(),
                minutes = flowOf(
                    PagingData.from(
                        (0..59).generateNumbersList(
                            initialCalendar.minute - MIDDLE_ITEM_INDEX_ADJUSTMENT, MINUTES_PAGE_SIZE
                        )
                            .mapIndexed { i, value -> Item(value, -MIDDLE_ITEM_INDEX_ADJUSTMENT + i) }
                    )
                ).collectAsLazyPagingItems(),
                itemsPerHeight = ITEMS_PER_HEIGHT,
                viewportBounds = null,
                onValueChange = {},
            )
        }
    }
}
