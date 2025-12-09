package app.traced_it.ui.entry

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.Animatable
import androidx.compose.animation.core.EaseOutQuint
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
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
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

enum class DragValue { Start, Center, End }

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun EntryListItem(
    entry: Entry,
    modifier: Modifier = Modifier,
    prevEntry: Entry? = null,
    now: Long = System.currentTimeMillis(),
    highlighted: Boolean = false,
    odd: Boolean = false,
    selected: Boolean = false,
    onToggle: () -> Unit = {},
    onUpdate: () -> Unit = {},
    onDelete: () -> Unit = {},
    onAddWithSameText: () -> Unit = {},
    onHighlightingFinished: () -> Unit = {},
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

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
    val state = remember {
        AnchoredDraggableState(
            initialValue = DragValue.Center,
            anchors = DraggableAnchors {
                DragValue.Start at with(density) { -Spacing.swipeActionWidth.toPx() * resistance }
                DragValue.Center at 0f
                DragValue.End at with(density) { Spacing.swipeActionWidth.toPx() * resistance }
            },
        )
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
            onHighlightingFinished()
        }
    }

    entry.getHeader(context, prevEntry)?.let { header ->
        Text(
            header,
            Modifier.padding(
                horizontal = Spacing.windowPadding,
                vertical = Spacing.small,
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
        )
    }

    Box(
        modifier
            .background(containerColor)
            .clip(RectangleShape)
    ) {
        Row(
            Modifier
                .testTag("entryListItem")
                .background(animatedBackground.value)
                .clickable { onToggle() }
                .fillMaxWidth()
                .padding(
                    horizontal = Spacing.windowPadding,
                    vertical = Spacing.medium,
                )
                .offset {
                    IntOffset(
                        x = (state.requireOffset() / resistance).roundToInt(),
                        y = 0,
                    )
                }
                .anchoredDraggable(
                    state,
                    orientation = Orientation.Horizontal,
                    flingBehavior = AnchoredDraggableDefaults.flingBehavior(
                        state,
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
                            entry.amountUnit.formatHtml(context, entry.amount)
                                ?: entry.amountUnit.format(context, entry.amount)
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
                onClick = onAddWithSameText,
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
                    scope.launch {
                        state.animateTo(DragValue.Center)
                    }
                    onUpdate()
                },
                modifier = Modifier
                    .testTag("entryListItemEditButton")
                    .align(Alignment.CenterStart)
                    .width(Spacing.swipeActionWidth + leftActionWidthAdjustment)
                    .fillMaxHeight()
                    .offset {
                        IntOffset(
                            x = (state.requireOffset() / resistance - Spacing.swipeActionWidth.toPx()).roundToInt(),
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
                onClick = {
                    scope.launch {
                        state.animateTo(DragValue.Center)
                    }
                    onDelete()
                },
                modifier = Modifier
                    .testTag("entryListItemDeleteButton")
                    .align(Alignment.CenterEnd)
                    .width(Spacing.swipeActionWidth)
                    .fillMaxHeight()
                    .offset {
                        IntOffset(
                            x = (state.requireOffset() / resistance + Spacing.swipeActionWidth.toPx()).roundToInt(),
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

@RequiresApi(Build.VERSION_CODES.O)
@Preview(showBackground = true)
@Composable
private fun DefaultPreview() {
    AppTheme {
        Column(Modifier.background(MaterialTheme.colorScheme.surface)) {
            EntryListItem(
                entry = defaultFakeEntries[0],
            )
            EntryListItem(
                entry = defaultFakeEntries[1],
                prevEntry = defaultFakeEntries[0],
                highlighted = true,
            )
            EntryListItem(
                entry = defaultFakeEntries[2],
                prevEntry = defaultFakeEntries[1],
                odd = true,
            )
            EntryListItem(
                entry = defaultFakeEntries[3],
                prevEntry = defaultFakeEntries[2],
                selected = true,
            )
        }
    }
}
