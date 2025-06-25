package dev.su386.calina.utils

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.ScaleFactor
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlin.math.min

@Composable
fun AutoResizeText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    align: Alignment = Alignment.Center
) {
    BoxWithConstraints(
        modifier = modifier,
        contentAlignment = align
    ) {
        val density = LocalDensity.current
        val textMeasurer = rememberTextMeasurer()

        // Calculate the maximum font size
        val maxFontSize = remember(maxWidth, maxHeight, density, text) {
            with(density) {
                var textSize = maxHeight * 0.7f // starting point based on container height
                var measuredWidth: Int

                do {
                    textSize -= 1f.dp
                    val textLayoutResult = textMeasurer.measure(
                        text = text,
                        style = TextStyle(fontSize = textSize.toSp())
                    )
                    measuredWidth = textLayoutResult.size.width
                } while (measuredWidth > maxWidth.roundToPx() && textSize > 1.dp)

                textSize.toSp()
            }
        }

        Text(
            text = text,
            modifier = Modifier,
            fontSize = maxFontSize,
            color = color,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = TextStyle(lineHeight = maxFontSize)
        )
    }
}

fun Modifier.fillMaxWidthToMax(
    fraction: Float = 1f,
    maxWidthDp: Dp
): Modifier = this.then(
    Modifier.layout { measurable, constraints ->
        // `this` is a MeasureScope: Dp.toPx() is in scope here
        val maxPx    = constraints.maxWidth
        val clampPx  = maxWidthDp.toPx().toInt()
        val targetPx = min((maxPx * fraction).toInt(), clampPx)

        // force measurement to exactly `targetPx`
        val placeable = measurable.measure(
            constraints.copy(minWidth = targetPx, maxWidth = targetPx)
        )
        layout(placeable.width, placeable.height) {
            placeable.placeRelative(0, 0)
        }
    }
)

fun Modifier.fillMaxHeightToMax(
    fraction: Float = 1f,
    maxHeightDp: Dp
): Modifier = this.then(
    Modifier.layout { measurable, constraints ->
        // `this` is a MeasureScope: Dp.toPx() is in scope here
        val maxPx = constraints.maxHeight
        val clampPx = maxHeightDp.toPx().toInt()
        val targetPx = min((maxPx * fraction).toInt(), clampPx)

        // force measurement to exactly `targetPx`
        val placeable = measurable.measure(
            constraints.copy(minHeight = targetPx, maxHeight = targetPx)
        )
        layout(placeable.width, placeable.height) {
            placeable.placeRelative(0, 0)
        }
    }
)

