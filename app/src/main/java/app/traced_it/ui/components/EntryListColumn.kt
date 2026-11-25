package app.traced_it.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.PagingData
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import app.traced_it.data.database.Entry
import app.traced_it.ui.ScrollTarget
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Duration.Companion.seconds

@Composable
fun EntryListColumn(
    entries: Flow<PagingData<Entry>>,
    highlightedUid: StateFlow<Int?>,
    readOnly: Boolean,
    scrollTarget: StateFlow<ScrollTarget?>,
    selectedUid: State<Int?>,
    modifier: Modifier = Modifier,
    animationsEnabled: Boolean = true,
    onDelete: (uid: Int) -> Unit,
    onFinishHighlight: () -> Unit,
    onRepeat: (uid: Int) -> Unit,
    onFinishScrollToTarget: () -> Unit,
    onSelect: (uid: Int?) -> Unit,
    onUpdate: (uid: Int) -> Unit,
) {
    val clock = LocalClock.current
    val locale = LocalLocale.current.platformLocale
    val listState = rememberLazyListState()
    val timeZone = rememberSystemTimeZone()

    val entries = entries.collectAsLazyPagingItems()
    val scrollTarget by scrollTarget.collectAsStateWithLifecycle()
    val currentOnFinishScrollToTarget by rememberUpdatedState(onFinishScrollToTarget)

    val focusedUid = rememberSaveable { mutableStateOf<Int?>(null) }
    var now by remember { mutableStateOf(clock.now()) }
    val today by remember { derivedStateOf { now.toLocalDateTime(timeZone).date } }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1.seconds)
            now = clock.now()
        }
    }

    // Scroll to item
    LaunchedEffect(scrollTarget) {
        scrollTarget?.let { scrollTarget ->
            try {
                when (scrollTarget) {
                    is ScrollTarget.Top -> listState.scrollToItem(0)
                    is ScrollTarget.Item -> withTimeoutOrNull(3.seconds) {
                        snapshotFlow {
                            // Emit every time the list refreshes
                            entries.itemCount to entries.itemSnapshotList.getOrNull(scrollTarget.index)?.uid
                        }.first { (itemCount, uidAtIndex) ->
                            when {
                                itemCount <= scrollTarget.index -> {
                                    // Do nothing, because the list is still loading.
                                    false
                                }

                                uidAtIndex == null -> {
                                    // Trigger page load, because the item is a placeholder.
                                    entries[scrollTarget.index]
                                    false
                                }

                                uidAtIndex != scrollTarget.uid -> {
                                    // Do nothing, because the item is an old item at the same index.
                                    false
                                }

                                else -> {
                                    // Scroll to an item by index and stop flow collection.
                                    if (!listState.isFullyVisible(scrollTarget.uid, scrollTarget.index)) {
                                        listState.scrollToItem(scrollTarget.index)
                                    }
                                    true
                                }
                            }
                        }
                    }
                }
            } finally {
                currentOnFinishScrollToTarget()
            }
        }
    }

    LazyColumn(
        modifier = modifier
            .selectableGroup()
            .testTag(TestTags.ENTRY_LIST_COLUMN),
        state = listState,
    ) {
        items(
            entries.itemCount,
            key = entries.itemKey { it.uid },
            contentType = { index -> entries.getEntryListItem(index, today, timeZone).contentType },
        ) { index ->
            val modifier = if (animationsEnabled) {
                // Disable fade-in and placement animations, because they interfere with scrolling to highlighted item
                Modifier.animateItem(fadeInSpec = null, placementSpec = null)
            } else {
                Modifier
            }
            when (val item = entries.getEntryListItem(index, today, timeZone)) {
                is EntryListItem.Placeholder -> {
                    PlaceholderEntryListItem()
                }

                is EntryListItem.Default -> {
                    val entry = item.entry
                    EntryListItem(
                        entry = entry,
                        isFocused = { focusedUid.value == entry.uid },
                        isHighlighted = { highlightedUid.value == entry.uid },
                        isSelected = { selectedUid.value == entry.uid },
                        locale = locale,
                        now = now,
                        odd = index % 2 != 0,
                        readOnly = readOnly,
                        timeZone = timeZone,
                        modifier = modifier,
                        onDelete = { onDelete(entry.uid) },
                        onFinishHighlight = onFinishHighlight,
                        onFocus = { focusedUid.value = entry.uid },
                        onRepeat = { onRepeat(entry.uid) },
                        onToggle = { selected -> onSelect(if (selected) entry.uid else null) },
                        onUpdate = { onUpdate(entry.uid) },
                    )
                }

                is EntryListItem.WithHeading -> {
                    val entry = item.entry
                    Column(modifier) {
                        EntryHeading(
                            entry = entry,
                            locale = locale,
                            timeZone = timeZone,
                        )
                        EntryListItem(
                            entry = entry,
                            isFocused = { focusedUid.value == entry.uid },
                            isHighlighted = { highlightedUid.value == entry.uid },
                            isSelected = { selectedUid.value == entry.uid },
                            locale = locale,
                            now = now,
                            odd = index % 2 != 0,
                            readOnly = readOnly,
                            timeZone = timeZone,
                            onDelete = { onDelete(entry.uid) },
                            onFinishHighlight = onFinishHighlight,
                            onFocus = { focusedUid.value = entry.uid },
                            onRepeat = { onRepeat(entry.uid) },
                            onToggle = { selected -> onSelect(if (selected) entry.uid else null) },
                            onUpdate = { onUpdate(entry.uid) },
                        )
                    }
                }
            }
        }
    }
}

private fun LazyListState.isFullyVisible(key: Any, index: Int): Boolean {
    val info = layoutInfo
    val item = info.visibleItemsInfo.firstOrNull { it.key == key } ?: return false
    if (item.index != index) return false
    val start = info.viewportStartOffset + info.beforeContentPadding
    val end = info.viewportEndOffset - info.afterContentPadding
    return item.offset >= start && item.offset + item.size <= end
}
