package app.traced_it.ui.components

import androidx.compose.animation.Animatable
import androidx.compose.animation.core.EaseOutQuint
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.AnchoredDraggableDefaults
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.gestures.snapTo
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.paging.compose.LazyPagingItems
import app.traced_it.R
import app.traced_it.data.database.Entry
import app.traced_it.data.di.defaultFakeEntries
import app.traced_it.ui.theme.AppTheme
import app.traced_it.ui.theme.Spacing
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import java.util.Locale
import kotlin.math.roundToInt
import kotlin.time.Instant

sealed interface EntryListItem {
    enum class ContentType { Default, Placeholder, WithHeading }

    val contentType: ContentType

    data object Placeholder : EntryListItem {
        override val contentType = ContentType.Placeholder
    }

    data class Default(val entry: Entry) : EntryListItem {
        override val contentType = ContentType.Default
    }

    data class WithHeading(val entry: Entry) : EntryListItem {
        override val contentType = ContentType.WithHeading
    }
}

fun LazyPagingItems<Entry>.getEntryListItem(index: Int, today: LocalDate, timeZone: TimeZone): EntryListItem =
    when (val entry = get(index)) {
        null -> EntryListItem.Placeholder
        else -> {
            val prevEntry = if (index > 0) get(index - 1) else null
            val hasHeading = when (prevEntry) {
                null -> !entry.dateEquals(today, timeZone)
                else -> !entry.dateEquals(prevEntry, timeZone)
            }
            if (hasHeading) {
                EntryListItem.WithHeading(entry)
            } else {
                EntryListItem.Default(entry)
            }
        }
    }

private enum class DragValue { Start, Center, End }

@Composable
fun EntryListItem(
    entry: Entry,
    isFocused: () -> Boolean,
    isHighlighted: () -> Boolean,
    isSelected: () -> Boolean,
    locale: Locale,
    now: Instant,
    odd: Boolean,
    readOnly: Boolean,
    timeZone: TimeZone,
    modifier: Modifier = Modifier,
    onDelete: () -> Unit,
    onFinishHighlight: () -> Unit,
    resistance: Float = 2f,
    onFocus: () -> Unit,
    onRepeat: () -> Unit,
    onToggle: (selected: Boolean) -> Unit,
    onUpdate: () -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current
    val resources = LocalResources.current

    val focused = isFocused()
    val highlighted = isHighlighted()
    val selected = isSelected()

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

    val actionWidthPx = with(density) { Spacing.swipeActionWidth.toPx().roundToInt() }
    val draggableState = remember {
        AnchoredDraggableState(
            initialValue = DragValue.Center,
            anchors = DraggableAnchors {
                DragValue.Start at -actionWidthPx * resistance
                DragValue.Center at 0f
                DragValue.End at actionWidthPx * resistance
            },
        )
    }

    // Mark the item as focused while its being dragged
    LaunchedEffect(draggableState) {
        snapshotFlow { draggableState.currentValue }
            .filter { it != DragValue.Center }
            .distinctUntilChanged()
            .collect {
                onFocus()
            }
    }

    // Snap the item to the center position when another item gets focused
    LaunchedEffect(focused) {
        if (!focused && draggableState.currentValue != DragValue.Center) {
            draggableState.snapTo(DragValue.Center)
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
            onFinishHighlight()
        }
    }

    Box(
        modifier
            .background(containerColor)
            .clipToBounds(),
    ) {
        Row(
            Modifier
                .drawBehind {
                    drawRect(animatedBackground.value)
                }
                .selectable(
                    selected,
                    enabled = !readOnly,
                    onClick = {
                        onFocus()
                        onToggle(!selected)
                    }
                )
                .fillMaxWidth()
                .padding(horizontal = Spacing.windowPadding, vertical = Spacing.medium)
                .offset {
                    IntOffset(x = (draggableState.requireOffset() / resistance).roundToInt(), y = 0)
                }
                .anchoredDraggable(
                    draggableState,
                    orientation = Orientation.Horizontal,
                    flingBehavior = AnchoredDraggableDefaults.flingBehavior(
                        draggableState,
                        positionalThreshold = { distance -> distance * 0.5f },
                    ),
                )
                .testTag(TestTags.ENTRY_LIST_ITEM),
            horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                entry.formatContentWithAmount(resources),
                Modifier
                    .weight(1f)
                    .testTag(TestTags.ENTRY_LIST_ITEM_CONTENT),
                color = contentColor,
            )
            Text(
                if (selected) {
                    entry.formatExactTime(resources, locale, timeZone)
                } else {
                    entry.formatRelativeTime(resources, now, locale, timeZone)
                },
                Modifier.testTag(TestTags.ENTRY_LIST_ITEM_TIME),
                color = contentColor,
            )
            Button(
                onClick = onRepeat,
                modifier = Modifier
                    .size(Spacing.listButtonSize)
                    .testTag(TestTags.ENTRY_LIST_ITEM_REPEAT),
                enabled = !readOnly,
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
                contentPadding = PaddingValues.Zero,
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
                        draggableState.snapTo(DragValue.Center)
                    }
                    onUpdate()
                },
                Modifier
                    .align(Alignment.CenterStart)
                    .width(Spacing.swipeActionWidth)
                    .fillMaxHeight()
                    .offset {
                        IntOffset(x = (draggableState.requireOffset() / resistance).roundToInt() - actionWidthPx, y = 0)
                    }
                    .background(MaterialTheme.colorScheme.tertiary)
                    .testTag(TestTags.ENTRY_LIST_ITEM_UPDATE),
                enabled = !readOnly,
                colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.onTertiary),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Edit,
                    contentDescription = stringResource(R.string.list_item_update),
                )
            }
            IconButton(
                {
                    coroutineScope.launch {
                        draggableState.snapTo(DragValue.Center)
                    }
                    onDelete()
                },
                Modifier
                    .align(Alignment.CenterEnd)
                    .width(Spacing.swipeActionWidth)
                    .fillMaxHeight()
                    .offset {
                        IntOffset(x = (draggableState.requireOffset() / resistance).roundToInt() + actionWidthPx, y = 0)
                    }
                    .background(MaterialTheme.colorScheme.error)
                    .testTag(TestTags.ENTRY_LIST_ITEM_DELETE),
                enabled = !readOnly,
                colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.onError),
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
    Spacer(Modifier.height(50.dp))
}

@Composable
fun EntryHeading(entry: Entry, locale: Locale, timeZone: TimeZone) {
    Text(
        entry.formatDate(locale, timeZone),
        Modifier
            .padding(horizontal = Spacing.windowPadding, vertical = Spacing.small)
            .testTag(TestTags.ENTRY_LIST_HEADING)
            .semantics { heading() },
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.bodyMedium,
    )
}

@Preview(showBackground = true)
@Composable
private fun DefaultPreview() {
    AppTheme {
        Surface {
            Column {
                val locale = LocalLocale.current.platformLocale
                val now = LocalClock.current.now()
                val timeZone = TimeZone.currentSystemDefault()
                EntryListItem(
                    entry = defaultFakeEntries[0],
                    isFocused = { false },
                    isHighlighted = { false },
                    isSelected = { false },
                    locale = locale,
                    now = now,
                    odd = true,
                    readOnly = false,
                    timeZone = timeZone,
                    onDelete = {},
                    onFinishHighlight = {},
                    onFocus = {},
                    onRepeat = {},
                    onToggle = {},
                    onUpdate = {},
                )
                EntryListItem(
                    entry = defaultFakeEntries[1],
                    isFocused = { false },
                    isHighlighted = { true },
                    isSelected = { false },
                    locale = locale,
                    now = now,
                    odd = false,
                    readOnly = false,
                    timeZone = timeZone,
                    onDelete = {},
                    onFinishHighlight = {},
                    onFocus = {},
                    onRepeat = {},
                    onToggle = {},
                    onUpdate = {},
                )
                EntryListItem(
                    entry = defaultFakeEntries[2],
                    isFocused = { false },
                    isHighlighted = { false },
                    isSelected = { false },
                    locale = locale,
                    now = now,
                    odd = true,
                    readOnly = false,
                    timeZone = timeZone,
                    onDelete = {},
                    onFinishHighlight = {},
                    onFocus = {},
                    onRepeat = {},
                    onToggle = {},
                    onUpdate = {},
                )
                EntryHeading(
                    entry = defaultFakeEntries[3],
                    locale = locale,
                    timeZone = timeZone,
                )
                EntryListItem(
                    entry = defaultFakeEntries[3],
                    isFocused = { false },
                    isHighlighted = { false },
                    isSelected = { true },
                    locale = locale,
                    now = now,
                    odd = false,
                    readOnly = false,
                    timeZone = timeZone,
                    onDelete = {},
                    onFinishHighlight = {},
                    onFocus = {},
                    onRepeat = {},
                    onToggle = {},
                    onUpdate = {},
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ReadOnlyPreview() {
    AppTheme {
        Surface {
            Column {
                val locale = LocalLocale.current.platformLocale
                val now = LocalClock.current.now()
                val timeZone = TimeZone.currentSystemDefault()
                EntryListItem(
                    entry = defaultFakeEntries[0],
                    isFocused = { false },
                    isHighlighted = { false },
                    isSelected = { false },
                    locale = locale,
                    now = now,
                    odd = true,
                    readOnly = true,
                    timeZone = timeZone,
                    onDelete = {},
                    onFinishHighlight = {},
                    onFocus = {},
                    onRepeat = {},
                    onToggle = {},
                    onUpdate = {},
                )
                EntryListItem(
                    entry = defaultFakeEntries[1],
                    isFocused = { false },
                    isHighlighted = { true },
                    isSelected = { false },
                    locale = locale,
                    now = now,
                    odd = false,
                    readOnly = true,
                    timeZone = timeZone,
                    onDelete = {},
                    onFinishHighlight = {},
                    onFocus = {},
                    onRepeat = {},
                    onToggle = {},
                    onUpdate = {},
                )
                EntryListItem(
                    entry = defaultFakeEntries[2],
                    isFocused = { false },
                    isHighlighted = { false },
                    isSelected = { false },
                    locale = locale,
                    now = now,
                    odd = true,
                    readOnly = true,
                    timeZone = timeZone,
                    onDelete = {},
                    onFinishHighlight = {},
                    onFocus = {},
                    onRepeat = {},
                    onToggle = {},
                    onUpdate = {},
                )
                EntryHeading(
                    entry = defaultFakeEntries[3],
                    locale = locale,
                    timeZone = timeZone,
                )
                EntryListItem(
                    entry = defaultFakeEntries[3],
                    isFocused = { false },
                    isHighlighted = { false },
                    isSelected = { true },
                    locale = locale,
                    now = now,
                    odd = false,
                    readOnly = true,
                    timeZone = timeZone,
                    onDelete = {},
                    onFinishHighlight = {},
                    onFocus = {},
                    onRepeat = {},
                    onToggle = {},
                    onUpdate = {},
                )
            }
        }
    }
}
