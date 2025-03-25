package dev.su386.calina.app

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.toPainter
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.su386.calina.app.App.searchBarContent
import dev.su386.calina.images.ImageData
import dev.su386.calina.images.ImageManager.getImagesByDate
import dev.su386.calina.images.filters.DayFilter
import dev.su386.calina.images.filters.MonthFilter
import dev.su386.calina.images.filters.TagNameFilter
import dev.su386.calina.images.filters.YearFilter
import dev.su386.calina.utils.AutoResizeText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Semaphore


private val semaphore = Semaphore(20)

@Composable
fun GalleryPanel() {
    GalleryWaterfall(Modifier.fillMaxWidth())
}

@Composable
fun GalleryWaterfall(modifier: Modifier) {
    val listState = rememberLazyListState()
    val searchBarContent = if (searchBarContent.text == "Search...") { "" } else { searchBarContent.text }
    val filters = listOf(
        DayFilter(searchBarContent),
        MonthFilter(searchBarContent),
        YearFilter(searchBarContent),
        TagNameFilter(searchBarContent)
    )
    val images = getImagesByDate(filters)

    LazyColumn(
        state = listState,
        modifier = modifier
    ) {
        items(images, key = { it.first().dateTime }) { imageGroup ->
            Day(
                modifier = Modifier
                    .padding(5.dp)
                    .fillMaxWidth(),
                date = imageGroup.first().dateTime,
                images = imageGroup.toTypedArray()
            )
        }
    }
}

@Composable
private fun Day(modifier: Modifier = Modifier, date: Date, images: Array<ImageData>) {
    var parentWidth by remember { mutableStateOf(0) }
    val parentWidthDp = with(LocalDensity.current) { parentWidth.toDp() }

    val groupedImages = groupImagesIntoRows(
        images,
        200.dp,
        5.dp,
        parentWidthDp,
    )

    Box(
        modifier = modifier
            .height(IntrinsicSize.Min)
            .clip(RoundedCornerShape(10.dp))
            .background(color = MaterialTheme.colors.surface)
    ) {
        Column(modifier = Modifier.padding(5.dp).fillMaxWidth()) {
            val timezone = TimeZone.getDefault()
            val dateFormatter = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.getDefault()).apply {
                timeZone = timezone // Apply the timezone to the formatter
            }

            AutoResizeText(
                text = dateFormatter.format(date),
                modifier = Modifier
                    .padding(horizontal = 10.dp, vertical = 5.dp)
                    .fillMaxWidth()
                    .height(40.dp),
                color = MaterialTheme.colors.onBackground,
                align = Alignment.CenterStart,
            )

            Column(
                Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min)
                    .onGloballyPositioned { layoutResult ->
                        parentWidth = layoutResult.size.width
                    }
            ) {
                for (i in 0..<groupedImages.size - 1 ) {
                    val group = groupedImages[i]
                    GalleryRow(
                        group.second,
                        group.first,
                        Modifier
                            .fillMaxWidth()
                            .padding(2.dp),
                    )
                }

                if (groupedImages.isNotEmpty()) {
                    val group = groupedImages.last()
                    GalleryRow(
                        group.second,
                        group.first,
                        Modifier
                            .fillMaxWidth()
                            .padding(2.dp),
                        horizontalArrangement = Arrangement.Start
                    )
                }
            }
        }
    }


}

@Composable
private fun GalleryRow(
    images: List<ImageData>,
    height: Dp,
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.SpaceBetween
) {
    val padding = if (horizontalArrangement == Arrangement.Start) {
        2.dp
    } else {
        0.dp
    }
    Row(
        horizontalArrangement = horizontalArrangement,
        modifier = modifier
            .height(height) // Ensures the row height is set
    ) {

        for (image in images) {
            val painter by rememberAsyncImage(image)
            Image(
                painter = painter,
                contentDescription = "",
                modifier = Modifier
                    .height(height) // Make sure all images use the exact same height
                    .aspectRatio(image.imageSize.ratio)
                    .padding(horizontal = padding)
                    .clip(RoundedCornerShape(4.dp))
            )
        }
    }
}

private fun groupImagesIntoRows(
    images: Array<ImageData>,
    targetRowHeight: Dp,
    padding: Dp,
    parentWidth: Dp
): List<Pair<Dp, List<ImageData>>> {
    val rows = mutableListOf<Pair<Dp, MutableList<ImageData>>>()
    var currentRow = mutableListOf<ImageData>()
    var currentWidth = 0f

    for (image in images) {
        val imageWidth =  targetRowHeight.value * image.imageSize.ratio
        currentRow.add(image)
        currentWidth += imageWidth + padding.value

        if (parentWidth.value > 0 && currentWidth > parentWidth.value && currentRow.isNotEmpty()) {
            rows.add(Pair(targetRowHeight * ((parentWidth.value)/ (currentWidth)), currentRow))

            currentRow = mutableListOf()
            currentWidth = 0f
        }
    }

    if (currentRow.isNotEmpty()) {
        rows.add(Pair(targetRowHeight, currentRow))
    }

    return rows
}

@Composable
fun rememberAsyncImage(image: ImageData): State<Painter> {
    val placeholder: Painter = ColorPainter(Color.Gray) // Placeholder while loading
    return produceState(placeholder, image) {
        value = withContext(Dispatchers.IO) {
            semaphore.acquire()
            try {
                val icon = image.icon
                icon.toPainter().also { icon.flush() }
            } finally {
                semaphore.release()
            }
        }
    }
}
