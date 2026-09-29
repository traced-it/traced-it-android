package app.traced_it.ui.entry

import androidx.compose.animation.Animatable
import androidx.compose.animation.core.EaseOutQuint
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import app.traced_it.R
import app.traced_it.data.di.defaultFakeEntries
import app.traced_it.data.local.database.Entry
import app.traced_it.data.local.database.noneUnit
import app.traced_it.ui.theme.AppTheme
import app.traced_it.ui.theme.Spacing
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private enum class DragValue { Start, Center, End }

@OptIn(ExperimentalFoundationApi::class, FlowPreview::class)
@Composable
fun LazyItemScope.EntryListItem(
    entry: Entry?,
    prevEntry: Entry?,
    focusedEntry: Entry?,
    highlightedEntryUid: Int?,
    index: Int,
    now: Long = System.currentTimeMillis(),
    selectedEntry: Entry?,
    animationsEnabled: Boolean = true,
    onAddWithSameText: (entry: Entry) -> Unit,
    onDelete: (entry: Entry) -> Unit,
    onFocus: (entry: Entry) -> Unit,
    onSelect: (entry: Entry?) -> Unit,
    onUnsetHighlightedEntry: () -> Unit,
    onUpdate: (entry: Entry) -> Unit,
) {
    if (entry == null) {
        PlaceholderEntryListItem()
    } else if (entry.hasHeader(LocalContext.current, prevEntry)) {
        Column {
            HeaderEntryListItem(entry)
            StandardEntryListItem(
                entry = entry,
                focused = focusedEntry == entry,
                highlighted = highlightedEntryUid == entry.uid,
                now = now,
                odd = index % 2 != 0,
                selected = selectedEntry == entry,
                animationsEnabled = animationsEnabled,
                onAddWithSameText = onAddWithSameText,
                onDelete = onDelete,
                onFocus = onFocus,
                onSelect = onSelect,
                onUnsetHighlightedEntry = onUnsetHighlightedEntry,
                onUpdate = onUpdate,
            )
        }
    } else {
        StandardEntryListItem(
            entry = entry,
            focused = focusedEntry == entry,
            highlighted = highlightedEntryUid == entry.uid,
            now = now,
            odd = index % 2 != 0,
            selected = selectedEntry == entry,
            animationsEnabled = animationsEnabled,
            onAddWithSameText = onAddWithSameText,
            onDelete = onDelete,
            onFocus = onFocus,
            onSelect = onSelect,
            onUnsetHighlightedEntry = onUnsetHighlightedEntry,
            onUpdate = onUpdate,
        )
    }
}

@Composable
private fun LazyItemScope.StandardEntryListItem(
    entry: Entry,
    now: Long = System.currentTimeMillis(),
    highlighted: Boolean = false,
    odd: Boolean = false,
    selected: Boolean = false,
    focused: Boolean = false,
    animationsEnabled: Boolean = true,
    onAddWithSameText: (entry: Entry) -> Unit,
    onDelete: (entry: Entry) -> Unit,
    onFocus: (entry: Entry) -> Unit,
    onSelect: (entry: Entry?) -> Unit,
    onUnsetHighlightedEntry: () -> Unit,
    onUpdate: (entry: Entry) -> Unit,
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val resources = LocalResources.current

    val containerColor = if (selected) {
        MaterialTheme.colorScheme.tertiaryContainer
    } else if (odd) {
        MaterialTheme.colorScheme.surfaceContainerHigh
    } else {
        Color.Transparent
    }
    val contentColor = if (selected) {
        MaterialTheme.colorScheme.onTertiaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    val highlightedContainerColor = MaterialTheme.colorScheme.tertiaryContainer
    val animatedBackground = remember { Animatable(Color.Transparent) }

    val density = LocalDensity.current
    // Reduce left action width to make sure no part of it is visible in the initial position due to rounding.
    val leftActionWidthAdjustment = -(1).dp
    val resistance = 2
    val draggableState = remember {
        AnchoredDraggableState(
            initialValue = DragValue.Center,
            anchors = DraggableAnchors {
                DragValue.Start at with(density) { -Spacing.swipeActionWidth.toPx() * resistance }
                DragValue.Center at 0f
                DragValue.End at with(density) { Spacing.swipeActionWidth.toPx() * resistance }
            },
        )
    }

    // Mark the item as focused, when dragged
    LaunchedEffect(draggableState) {
        snapshotFlow { draggableState.currentValue }
            .filter { it != DragValue.Center }
            .collect {
                onFocus(entry)
            }
    }

    // Reset the item to the center position, when another item gets focused
    LaunchedEffect(focused) {
        if (!focused && draggableState.currentValue != DragValue.Center) {
            draggableState.animateTo(DragValue.Center)
        }
    }

    LaunchedEffect(highlighted) {
        if (highlighted) {
            animatedBackground.animateTo(
                highlightedContainerColor,
                tween(500, easing = EaseOutQuint),
            )
            animatedBackground.animateTo(
                Color.Transparent,
                tween(1000, easing = LinearEasing),
            )
            onUnsetHighlightedEntry()
        }
    }

    Box(
        Modifier
            .run {
                if (animationsEnabled) {
                    animateItem()
                } else {
                    this
                }
            }
            .background(containerColor)
            .clip(RectangleShape),
    ) {
        Row(
            Modifier
                .testTag("entryListItem")
                .drawBehind {
                    drawRect(animatedBackground.value)
                }
                .clickable {
                    onFocus(entry)
                    onSelect(if (selected) null else entry)
                }
                .fillMaxWidth()
                .padding(
                    horizontal = Spacing.windowPadding,
                    vertical = Spacing.medium,
                )
                .offset {
                    IntOffset(
                        x = (draggableState.requireOffset() / resistance).roundToInt(),
                        y = 0,
                    )
                }
                .anchoredDraggable(
                    draggableState,
                    orientation = Orientation.Horizontal,
                    flingBehavior = AnchoredDraggableDefaults.flingBehavior(
                        draggableState,
                        positionalThreshold = { distance -> distance * 0.5f },
                    ),
                ),
            horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                buildAnnotatedString {
                    append(entry.content)
                    if (entry.amountUnit != noneUnit) {
                        append(" (")
                        append(
                            entry.amountUnit.formatHtml(resources, entry.amount)
                                ?: entry.amountUnit.format(resources, entry.amount)
                        )
                        append(")")
                    }
                },
                Modifier.weight(1f),
                color = contentColor,
            )
            Text(
                if (selected) {
                    entry.formatExactTime(context)
                } else {
                    entry.formatTime(context, now)
                },
                color = contentColor,
            )
            Button(
                onClick = { onAddWithSameText(entry) },
                modifier = Modifier
                    .testTag("entryListItemAddButton")
                    .size(Spacing.listButtonSize),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                contentPadding = PaddingValues(0.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(R.string.list_item_add),
                )
            }
        }
        Box(Modifier.matchParentSize()) {
            IconButton(
                {
                    coroutineScope.launch {
                        draggableState.animateTo(DragValue.Center)
                    }
                    onUpdate(entry)
                },
                Modifier
                    .testTag("entryListItemEditButton")
                    .align(Alignment.CenterStart)
                    .width(Spacing.swipeActionWidth + leftActionWidthAdjustment)
                    .fillMaxHeight()
                    .offset {
                        IntOffset(
                            x = (draggableState.requireOffset() / resistance - Spacing.swipeActionWidth.toPx()).roundToInt(),
                            y = 0,
                        )
                    }
                    .background(MaterialTheme.colorScheme.tertiary),
                colors = IconButtonDefaults.iconButtonColors(
                    contentColor = MaterialTheme.colorScheme.onTertiary,
                ),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Edit,
                    contentDescription = stringResource(R.string.list_item_update),
                )
            }
            IconButton(
                {
                    coroutineScope.launch {
                        draggableState.animateTo(DragValue.Center)
                    }
                    onDelete(entry)
                },
                Modifier
                    .testTag("entryListItemDeleteButton")
                    .align(Alignment.CenterEnd)
                    .width(Spacing.swipeActionWidth)
                    .fillMaxHeight()
                    .offset {
                        IntOffset(
                            x = (draggableState.requireOffset() / resistance + Spacing.swipeActionWidth.toPx()).roundToInt(),
                            y = 0,
                        )
                    }
                    .background(MaterialTheme.colorScheme.error),
                colors = IconButtonDefaults.iconButtonColors(
                    contentColor = MaterialTheme.colorScheme.onError
                ),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = stringResource(R.string.list_item_delete),
                )
            }
        }
    }
}

@Composable
fun PlaceholderEntryListItem() {
    Spacer(Modifier.height(10.dp))
}

@Composable
fun HeaderEntryListItem(entry: Entry) {
    Text(
        entry.formatDate(LocalContext.current),
        Modifier.padding(
            horizontal = Spacing.windowPadding,
            vertical = Spacing.small,
        ),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.bodyMedium,
    )
}

@Preview(showBackground = true)
@Composable
private fun DefaultPreview() {
    AppTheme {
        Surface {
            LazyColumn {
                item {
                    EntryListItem(
                        entry = defaultFakeEntries[0],
                        prevEntry = null,
                        index = 0,
                        focusedEntry = null,
                        highlightedEntryUid = null,
                        selectedEntry = null,
                        animationsEnabled = false,
                        onAddWithSameText = {},
                        onDelete = {},
                        onUnsetHighlightedEntry = {},
                        onFocus = {},
                        onSelect = {},
                        onUpdate = {},
                    )
                }
                item {
                    EntryListItem(
                        entry = defaultFakeEntries[1],
                        prevEntry = defaultFakeEntries[0],
                        index = 1,
                        focusedEntry = null,
                        highlightedEntryUid = defaultFakeEntries[1].uid,
                        selectedEntry = null,
                        animationsEnabled = false,
                        onAddWithSameText = {},
                        onDelete = {},
                        onUnsetHighlightedEntry = {},
                        onFocus = {},
                        onSelect = {},
                        onUpdate = {},
                    )
                }
                item {
                    EntryListItem(
                        entry = defaultFakeEntries[2],
                        prevEntry = defaultFakeEntries[1],
                        index = 2,
                        focusedEntry = null,
                        highlightedEntryUid = null,
                        selectedEntry = null,
                        animationsEnabled = false,
                        onAddWithSameText = {},
                        onDelete = {},
                        onUnsetHighlightedEntry = {},
                        onFocus = {},
                        onSelect = {},
                        onUpdate = {},
                    )
                }
                item {
                    EntryListItem(
                        entry = defaultFakeEntries[3],
                        prevEntry = defaultFakeEntries[2],
                        index = 3,
                        focusedEntry = null,
                        highlightedEntryUid = null,
                        selectedEntry = defaultFakeEntries[3],
                        animationsEnabled = false,
                        onAddWithSameText = {},
                        onDelete = {},
                        onUnsetHighlightedEntry = {},
                        onFocus = {},
                        onSelect = {},
                        onUpdate = {},
                    )
                }
            }
        }
    }
}
