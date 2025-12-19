package app.traced_it.ui.components

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp

fun Modifier.verticalFade(): Modifier = this.then(
    Modifier
        .graphicsLayer { alpha = 0.99f }
        .drawWithContent {
            drawContent()
            drawRect(
                brush = Brush.verticalGradient(
                    colorStops = arrayOf(
                        0.0f to Color.Transparent,
                        0.4f to Color.White,
                        0.6f to Color.White,
                        1.0f to Color.Transparent,
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
