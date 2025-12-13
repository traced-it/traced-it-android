package app.traced_it.ui.components

import android.text.format.DateUtils
import android.util.Log
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
            calendar.addDays(pageNumber * params.loadSize)
            val data = (0..<params.loadSize).map { i ->
                if (i != 0) {
                    calendar.addDays(1)
                }
                Item(index = pageNumber * params.loadSize + i, value = calendar.day)
            }
            Log.i(
                null,
                "DayPagingSource(initialValue=$initialValue).load(pageNumber=$pageNumber, loadSize=${params.loadSize}) -> $data"
            )
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
            val firstNumber = initialValue + pageNumber * params.loadSize
            val data = (0..<params.loadSize).map { i ->
                Item(index = firstNumber + i, value = (firstNumber + i).mod(rangeSize))
            }
            Log.i(
                null,
                "RangePagingSource(from=$from, to=$to, initialValue=$initialValue).load(pageNumber=$pageNumber, loadSize=${params.loadSize}) -> $data"
            )
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

    val initialCalendar = GregorianCalendar.getInstance(TimeZone.getDefault()).apply { time = Date(initialValue) }
    var day by remember { mutableStateOf(initialCalendar.day) }
    var hour by remember { mutableIntStateOf(initialCalendar.hour) }
    var minute by remember { mutableIntStateOf(initialCalendar.minute) }
    val calendar = GregorianCalendar.getInstance(TimeZone.getDefault()).apply {
        this.time = Date(initialValue)
        this.day = day
        this.hour = hour
        this.minute = minute
    }

    val days = Pager(PagingConfig(pageSize = 20, enablePlaceholders = true)) {
        DayPagingSource(day)
    }.flow.collectAsLazyPagingItems()
    val hours = Pager(PagingConfig(pageSize = 20, enablePlaceholders = true)) {
        RangePagingSource(0, 23, hour)
    }.flow.collectAsLazyPagingItems()
    val minutes = Pager(PagingConfig(pageSize = 20, enablePlaceholders = true)) {
        RangePagingSource(0, 59, minute)
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
                        day = initialCalendar.day
                        hour = initialCalendar.hour
                        minute = initialCalendar.minute
                        // TODO
                        // launch { dayListState.animateScrollToItem(0) }
                        // launch { hourListState.animateScrollToItem(0) }
                        // launch { minuteListState.animateScrollToItem(0) }
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
                        // TODO
                        // day = item.value
                        // onValueChange(calendar.timeInMillis)
                    },
                    height = height,
                    modifier = Modifier
                        .weight(dayWeight)
                        .rightBorder(borderColor, borderWidth),
                ) { item ->
                    if (item.index == 0) {
                        stringResource(R.string.detail_created_at_today)
                    } else {
                        val calendar = GregorianCalendar.getInstance(TimeZone.getDefault()).apply {
                            this.day = item.value
                        }
                        DateUtils.formatDateTime(
                            LocalContext.current,
                            calendar.timeInMillis,
                            DateUtils.FORMAT_SHOW_YEAR or DateUtils.FORMAT_SHOW_DATE,
                        )
                    }
                }
                TracedTimePickerSegment(
                    listState = hourListState,
                    items = hours,
                    onValueChange = { item ->
                        // TODO
                        // hour = item.value
                        // onValueChange(calendar.timeInMillis)
                    },
                    height = height,
                    modifier = Modifier
                        .weight(hourWeight)
                        .rightBorder(borderColor, borderWidth),
                ) { item ->
                    item.value.toString().padStart(2, '0')
                }
                TracedTimePickerSegment(
                    listState = minuteListState,
                    items = minutes,
                    onValueChange = { item ->
                        // TODO
                        // minute = item.value
                        // onValueChange(calendar.timeInMillis)
                    },
                    height = height,
                    modifier = Modifier.weight(minuteWeight),
                ) { item ->
                    item.value.toString().padStart(2, '0')
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

    // Call onValueChange()
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
        // FIXME Fix infinite loading from paging source backwards
        items(
            items.itemCount,
            key = items.itemKey { it.index }
        ) { index ->
            items[index]?.let { item ->
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(optionHeight),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text(item))
                }
            } ?: Box {}
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
