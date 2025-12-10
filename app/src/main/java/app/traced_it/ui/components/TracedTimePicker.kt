package app.traced_it.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.traced_it.R
import app.traced_it.ui.theme.AppTheme
import app.traced_it.ui.theme.Spacing
import java.util.*
import kotlin.math.min
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
    val borderColor = MaterialTheme.colorScheme.outline
    val borderWidth = 1.dp
    val dateWeight = 3f
    val hourWeight = 1f
    val minuteWeight = 1f

    val valueUtcDate = Date().apply { time = value }
    val valueLocalCalendar = Calendar.getInstance(TimeZone.getDefault()).apply { time = valueUtcDate }

    val initialDate = Triple(
        valueLocalCalendar.get(Calendar.YEAR),
        valueLocalCalendar.get(Calendar.MONTH) + 1,
        valueLocalCalendar.get(Calendar.DAY_OF_MONTH),
    )
    val initialHour = min(valueLocalCalendar.get(Calendar.HOUR_OF_DAY), 23)
    val initialMinute = min(valueLocalCalendar.get(Calendar.MINUTE), 59)

    var date by remember { mutableStateOf(initialDate) }
    var hour by remember { mutableIntStateOf(initialHour) }
    var minute by remember { mutableIntStateOf(initialMinute) }

    val dateOptions = mapOf(initialDate to "today") // TODO
    val minuteOptions = (0..23).associateWith { it.toString().padStart(2, '0') }
    val hourOptions = (0..59).associateWith { it.toString().padStart(2, '0') }

    Column(
        modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.windowPadding)
    ) {
        Row(Modifier.padding(vertical = Spacing.small)) {
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
                    value = date,
                    onValueChange = { date = it },
                    modifier = Modifier.weight(dateWeight),
                    borderWidth = borderWidth,
                    borderColor = borderColor,
                )
                TracedTimePickerSegment(
                    options = hourOptions,
                    value = hour,
                    onValueChange = { hour = it },
                    modifier = Modifier.weight(hourWeight),
                    borderWidth = borderWidth,
                    borderColor = borderColor,
                )
                TracedTimePickerSegment(
                    options = minuteOptions,
                    value = minute,
                    onValueChange = { minute = it },
                    modifier = Modifier.weight(minuteWeight),
                    borderWidth = borderWidth,
                    borderColor = borderColor,
                    last = true,
                )
            }
        }
    }
}

@Composable
private fun <T> TracedTimePickerSegment(
    options: Map<T, String>,
    value: T,
    onValueChange: (value: T) -> Unit,
    modifier: Modifier = Modifier,
    borderColor: Color,
    borderWidth: Dp,
    last: Boolean = false,
) {
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
            }
    ) {
        Column(Modifier.fillMaxWidth().offset(y = Spacing.inputHeight / 3.0f)) {
            options.values.forEach { text ->
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(Spacing.inputHeight / 3.0f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text)
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
