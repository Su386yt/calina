package dev.su386.calina.utils

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import java.time.LocalTime
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.pow

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



var time by mutableLongStateOf(LocalTime.now().toNanoOfDay() / 1_000_000)
@OptIn(ExperimentalHazeMaterialsApi::class)
@Composable
fun Modifier.sunBackground(
    solarNoon: Long = 12 * 60 * 60 * 1000,
    sunrise: Long = 6 * 60 * 60 * 1000,
    sunset: Long = 19 * 60 * 60 * 1000,
    solarMidnight: Long = 0 * 60 * 60 * 1000,
    sunriseLength: Long = 3 * 60 * 60 * 1000,
    sunsetLength: Long = 3 * 60 * 60 * 1000,
): Modifier {
    /*
     * Color Stops for sun:
     * - Solar Noon - #09608F
     * - SS-2 hours - #00756F
     * - SS-1 hour - #B57D2A
     * - SS-.75 hour - #A14F2F
     * - SS-.5 hour - #85424F
     * - SS-.25 hour - #702F54
     * - Sunset - #692F5A
     * - SS+.25 hour - #6B3A5D
     * - SS+.33 hour - #623866
     * - SS+.5 hour - #3B3C66
     * - SS+.75 hour - #334861
     * - SS+1 hour - #21415C
     * - SS+2 hour - #1D2A6E
     * - Midnight - #171621
     */
    val sunColorStops = arrayOf(
        solarMidnight to Color(0xFF171621),
        (sunrise - sunriseLength) to Color(0xFF1D2A6E),
        (sunrise - sunriseLength * 0.5f).toLong() to Color(0xFF21415C),
        (sunrise - sunriseLength * 0.375f).toLong()  to Color(0xFF334861),
        (sunrise - sunriseLength * 0.25f).toLong() to Color(0xFF3B3C66),
        (sunrise - sunriseLength * 0.125f).toLong() to Color(0xFF6B3A5D),
        sunrise to Color(0xFF692F5A),
        (sunrise + sunriseLength * 0.125f).toLong() to Color(0xFF702F54),
        (sunrise + sunriseLength * 0.25f).toLong() to Color(0xFF85424F),
        (sunrise + sunriseLength * 0.375f).toLong() to Color(0xFFA14F2F),
        (sunrise + sunriseLength * 0.5f).toLong() to Color(0xFFB57D2A),
        (sunrise + sunriseLength) to Color(0xFF00756F),
        solarNoon to Color(0xFF09608F),
        (sunset - sunsetLength) to Color(0xFF00756F),
        (sunset - sunsetLength * 0.5f).toLong() to Color(0xFFB57D2A),
        (sunset - sunsetLength * 0.375f).toLong() to Color(0xFFA14F2F),
        (sunset - sunsetLength * 0.25f).toLong() to Color(0xFF85424F),
        (sunset - sunsetLength * 0.125f).toLong() to Color(0xFF702F54),
        sunset to Color(0xFF692F5A),
        (sunset + sunsetLength * 0.125f).toLong() to Color(0xFF6B3A5D),
        (sunset + sunsetLength * 0.165f).toLong() to Color(0xFF623866),
        (sunset + sunsetLength * 0.25f).toLong() to Color(0xFF3B3C66),
        (sunset + sunsetLength * 0.375f).toLong() to Color(0xFF334861),
        (sunset + sunsetLength * 0.5f).toLong() to Color(0xFF21415C),
        (sunset + sunsetLength) to Color(0xFF1D2A6E),
        solarMidnight + 24 * 60 * 60 * 1000 to Color(0xFF171621),
    )


    /*
     * Color Stops for sky:
     * - Solar Noon - #083854
     * - SS-2 hours - #043633
     * - SS-1 hour - #133147
     * - SS-.75 hour - #0F2E47
     * - SS-.5 hour - #192342
     * - SS-.25 hour - #161D3D
     * - Sunset - #111B3D
     * - SS+.25 hour - #061736
     * - SS+.33 hour - #05102B
     * - SS+.5 hour - #060F2B
     * - SS+.75 hour - #050C30
     * - SS+1 hour - #011126
     * - SS+2 hour - #050D3B
     * - Midnight - #040405
     */
    val skyColorStops = arrayOf(
        solarMidnight to Color(0xFF040405),
        (sunrise - sunriseLength) to Color(0xFF050D3B),
        (sunrise - sunriseLength * 0.5f).toLong() to Color(0xFF011126),
        (sunrise - sunriseLength * 0.375f).toLong()  to Color(0xFF050C30),
        (sunrise - sunriseLength * 0.165f).toLong() to Color(0xFF05102B),
        (sunrise - sunriseLength * 0.25f).toLong() to Color(0xFF060F2B),
        (sunrise - sunriseLength * 0.125f).toLong() to Color(0xFF061736),
        sunrise to Color(0xFF111B3D),
        (sunrise + sunriseLength * 0.125f).toLong() to Color(0xFF161D3D),
        (sunrise + sunriseLength * 0.25f).toLong() to Color(0xFF192342),
        (sunrise + sunriseLength * 0.375f).toLong() to Color(0xFF0F2E47),
        (sunrise + sunriseLength * 0.5f).toLong() to Color(0xFF133147),
        (sunrise + sunriseLength) to Color(0xFF043633),
        solarNoon to Color(0xFF083854),
        (sunset - sunsetLength) to Color(0xFF043633),
        (sunset - sunsetLength * 0.5f).toLong() to Color(0xFF133147),
        (sunset - sunsetLength * 0.375f).toLong() to Color(0xFF0F2E47),
        (sunset - sunsetLength * 0.25f).toLong() to Color(0xFF192342),
        (sunset - sunsetLength * 0.125f).toLong() to Color(0xFF161D3D),
        sunset to Color(0xFF111B3D),
        (sunset + sunsetLength * 0.125f).toLong() to Color(0xFF061736),
        (sunset + sunsetLength * 0.165f).toLong() to Color(0xFF05102B),
        (sunset + sunsetLength * 0.25f).toLong() to Color(0xFF060F2B),
        (sunset + sunsetLength * 0.375f).toLong() to Color(0xFF050C30),
        (sunset + sunsetLength * 0.5f).toLong() to Color(0xFF011126),
        (sunset + sunsetLength) to Color(0xFF050D3B),
        solarMidnight + 24 * 60 * 60 * 1000 to Color(0xFF040405),
    )

    val sunColor = pickColorFromStops(time, sunColorStops)
    val skyColor = pickColorFromStops(time, skyColorStops)
    val nonLinearStops = getSquareRootCurveColorStops(sunColor to skyColor, 1.25)

    return this
        .drawBehind {
            val height = size.height
            val stops = listOf(
                sunrise to 0f,
                solarNoon to .5f,
                sunset to 1f,
                solarMidnight to .5f,
                solarMidnight + 24 * 60 * 60 * 1000 to .5f,
            ).sortedBy { it.first }
            val index = stops.indexOfFirst { it.first > time }
            val (t1, t2) = stops[index - 1] to stops[index]

            val segmentLength = abs(t2.first - t1.first).let { if (it == 0L) 1L else it }

            val elapsed = abs(time - t1.first).coerceIn(0L, segmentLength)
            val fracAlong = elapsed.toFloat() / segmentLength.toFloat()

            val frac = t1.second + (t2.second - t1.second) * fracAlong
            val width = size.width * frac

            val center = Offset(x = width, y = height)
            val brush = Brush.radialGradient(
                colorStops = nonLinearStops,
                center = center,
            )

            drawRect(brush = brush, size = size)
        }
}

fun pickColorFromStops(point: Long, stops: Array<Pair<Long, Color>>): Color = stops.sortedBy { it.first }
    .also { colors ->
        if (colors.last().first < point || colors.first().first > point)
            throw IndexOutOfBoundsException("$point not in range ${colors.first().first}, ${colors.last().first}")
    }
    .let { colors ->
        val color1 = colors.last { it.first < point }
        val color2 = colors.first { it.first > point }

        val range = color2.first - color1.first

        val color1Weight = (color2.first - point) / range.toFloat()
        val color2Weight = (point - color1.first) / range.toFloat()

        return@let Color(
            color1.second.red * color1Weight + color2.second.red * color2Weight,
            color1.second.green * color1Weight + color2.second.green * color2Weight,
            (color1.second.blue * color1Weight + color2.second.blue * color2Weight),
            (color1.second.alpha * color1Weight + color2.second.alpha * color2Weight),
        )
    }

fun getSquareRootCurveColorStops(colorStops: Pair<Color, Color>, power: Double = 2.0): Array<Pair<Float, Color>> {
    return (0..100).map { i ->
        val t = i / 100.0
        val sq = t.pow(power)    // remap t → √t
        sq.toFloat() to lerp(colorStops.first, colorStops.second, t.toFloat())
    }.toTypedArray()
}


