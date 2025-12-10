package app.traced_it.ui.components

import android.text.format.DateUtils
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.times
import app.traced_it.R
import app.traced_it.ui.theme.AppTheme
import app.traced_it.ui.theme.Spacing
import java.util.*
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.time.ExperimentalTime

fun Modifier.verticalFade(): Modifier = this.then(
    Modifier
        .graphicsLayer { alpha = 0.99f }
        .drawWithContent {
            drawContent()
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color.White,
                        Color.Transparent,
                    ),
                    startY = 0f,
                    endY = size.height,
                ),
                blendMode = BlendMode.DstIn,
            )
        }
)

fun Modifier.rightBorder(
    color: Color,
    width: Dp,
) = this.then(
    Modifier.drawBehind {
        drawLine(
            color = color,
            start = Offset(x = size.width, y = 0f),
            end = Offset(x = size.width, y = size.height),
            strokeWidth = width.toPx(),
        )
    }
)

@OptIn(ExperimentalTime::class)
@Composable
fun TracedTimePicker(
    value: Long,
    onValueChange: (value: Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    val borderColor = MaterialTheme.colorScheme.outline
    val borderWidth = 1.dp
    val optionSize = Spacing.inputHeight / 3
    val dateWeight = 3f
    val hourWeight = 1f
    val minuteWeight = 1f
    val daysSize = 31 // TODO Replace with infinite lazy list

    val calendar = GregorianCalendar.getInstance(TimeZone.getDefault()).apply { time = Date(value) }
    val initialDate = Triple(
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH) + 1,
        calendar.get(Calendar.DAY_OF_MONTH),
    )
    val initialHour = min(calendar.get(Calendar.HOUR_OF_DAY), 23)
    val initialMinute = min(calendar.get(Calendar.MINUTE), 59)
    val dateOptions = buildList {
        add(initialDate to @Composable { stringResource(R.string.detail_created_at_today) })
        repeat(daysSize - 1) {
            calendar.add(Calendar.DAY_OF_MONTH, -1)
            val value = Triple(
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH) + 1,
                calendar.get(Calendar.DAY_OF_MONTH),
            )
            val text = DateUtils.formatDateTime(context, calendar.timeInMillis, DateUtils.FORMAT_SHOW_YEAR or DateUtils.FORMAT_SHOW_DATE)
            add(0, value to @Composable { text })
        }
        calendar.add(Calendar.DAY_OF_MONTH, daysSize - 1)
    }
    val hourOptions = (0..23).map { hour ->
        hour to @Composable { hour.toString().padStart(2, '0') }
    }
    val minuteOptions = (0..59).map { minute ->
        minute to @Composable { minute.toString().padStart(2, '0') }
    }

    Column(
        modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.windowPadding)
    ) {
        Row(Modifier.padding(bottom = Spacing.small)) {
            Text(
                stringResource(R.string.detail_created_at_label_date),
                Modifier.weight(dateWeight),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                stringResource(R.string.detail_created_at_label_hour),
                Modifier.weight(hourWeight),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                stringResource(R.string.detail_created_at_label_minute),
                Modifier.weight(minuteWeight),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        Surface(
            color = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            border = BorderStroke(width = borderWidth, color = MaterialTheme.colorScheme.outline),
        ) {
            Row(Modifier.height(Spacing.inputHeight)) {
                TracedTimePickerSegment(
                    options = dateOptions,
                    initialValue = initialDate,
                    onValueChange = { (year, month, date) ->
                        calendar.set(year, month, date)
                        onValueChange(calendar.timeInMillis)
                    },
                    modifier = Modifier.weight(dateWeight),
                    borderWidth = borderWidth,
                    borderColor = borderColor,
                    optionSize = optionSize,
                )
                TracedTimePickerSegment(
                    options = hourOptions,
                    initialValue = initialHour,
                    onValueChange = { hour ->
                        calendar.set(Calendar.HOUR, hour)
                        onValueChange(calendar.timeInMillis)
                    },
                    modifier = Modifier.weight(hourWeight),
                    borderWidth = borderWidth,
                    borderColor = borderColor,
                    optionSize = optionSize,
                )
                TracedTimePickerSegment(
                    options = minuteOptions,
                    initialValue = initialMinute,
                    onValueChange = { minute ->
                        calendar.set(Calendar.MINUTE, minute)
                        onValueChange(calendar.timeInMillis)
                    },
                    modifier = Modifier.weight(minuteWeight),
                    borderWidth = borderWidth,
                    borderColor = borderColor,
                    optionSize = optionSize,
                    last = true,
                )
            }
        }
    }
}

@Composable
private fun <T : Any> TracedTimePickerSegment(
    options: List<Pair<T, @Composable () -> String>>,
    initialValue: T,
    onValueChange: (value: T) -> Unit,
    modifier: Modifier = Modifier,
    borderColor: Color,
    borderWidth: Dp,
    optionSize: Dp,
    last: Boolean = false,
) {
    val density = LocalDensity.current
    val draggableState = remember {
        AnchoredDraggableState(
            initialValue = initialValue,
            anchors = DraggableAnchors {
                options.forEachIndexed { i, (value) ->
                    value at i * (with(density) { -optionSize.toPx() })
                }
            },
        )
    }

    val contentHeight = options.size * optionSize

    LaunchedEffect(draggableState) {
        snapshotFlow { draggableState.settledValue }
            .collect { onValueChange(it) }
    }

    Box(
        modifier
            .fillMaxHeight()
            .verticalFade()
            .run {
                if (!last) {
                    rightBorder(borderColor, borderWidth)
                } else {
                    this
                }
            },
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .requiredHeight(contentHeight)
                .offset(
                    // The column is vertically centered inside the box by default, which we need to compensate here
                    y = (contentHeight - optionSize) / 2
                )
                .offset {
                    IntOffset(
                        x = 0,
                        y = draggableState.requireOffset().roundToInt(),
                    )
                }
                .anchoredDraggable(
                    draggableState,
                    orientation = Orientation.Vertical,
                    flingBehavior = AnchoredDraggableDefaults.flingBehavior(
                        draggableState,
                        positionalThreshold = { distance -> distance * 0.5f },
                    ),
                ),
        ) {
            options.forEach { (_, text) ->
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(optionSize),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text())
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
                value = System.currentTimeMillis(),
                onValueChange = {},
            )
        }
    }
}
