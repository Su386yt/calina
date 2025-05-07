package dev.su386.calina.app

import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Checkbox
import androidx.compose.material.Icon
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
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
import dev.su386.calina.Calina.shiftPressed
import dev.su386.calina.app.App.closePopup
import dev.su386.calina.app.App.openPopup
import dev.su386.calina.app.App.searchBarContent
import dev.su386.calina.images.ImageData
import dev.su386.calina.images.ImageManager.getImagesByDate
import dev.su386.calina.images.ImageManager.images
import dev.su386.calina.images.tags.Tag.Companion.tags
import dev.su386.calina.images.filters.*
import dev.su386.calina.images.filters.FilterJunction.Companion.toConjunction
import dev.su386.calina.images.filters.FilterJunction.Companion.toDisJunction
import dev.su386.calina.images.tags.HiddenTag
import dev.su386.calina.utils.AutoResizeText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Semaphore


private val semaphore = Semaphore(20)
private val selectedHashes = mutableStateListOf<String>()
private var lastClickedImageHash by mutableStateOf("")
private val selectedTagFilters = mutableStateListOf<UUID>()
private var imagesDisplayed by mutableStateOf(listOf<List<ImageData>>())
private var imagesDisplayedSize by mutableStateOf(0)

@Composable
fun GalleryPanel() {
    Column(Modifier.fillMaxSize()) {
        FilterBar(
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        )
        GalleryWaterfall(Modifier.fillMaxWidth())
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FilterBar(modifier: Modifier) {
    Row(
        modifier
            .clip(RoundedCornerShape(17))
            .background(colorScheme.surface)
            .border(1.dp, colorScheme.background),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        if (selectedHashes.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.End
            ) {
                Icon(
                    Icons.Outlined.Delete,
                    contentDescription = "Delete ${selectedHashes.size} images",
                    modifier = Modifier
                        .padding(10.dp)
                        .fillMaxHeight()
                        .aspectRatio(1f)
                        .clickable {
                            openPopup {
                                AlertDialog(
                                    icon = {
                                        Icon(Icons.Outlined.Delete, contentDescription = "Delete")
                                    },
                                    title = {
                                        Text(text = "Delete ${selectedHashes.size} image${if (selectedHashes.size != 1) "s" else ""}")
                                    },
                                    text = {
                                        Text(text = "Are you sure you would like to delete ${selectedHashes.size} image${if (selectedHashes.size != 1) "s" else ""}?")
                                    },
                                    onDismissRequest = {
                                        closePopup()
                                    },
                                    confirmButton = {
                                        TextButton(
                                            onClick = {
                                                closePopup()
                                            },
                                            content = { Text("Delete ${selectedHashes.size} images") },
                                        )
                                    },
                                    dismissButton = {
                                        TextButton(
                                            onClick = {
                                                closePopup()
                                            },
                                            content = { Text("Return") },
                                        )
                                    }
                                )
                            }
                        },
                    tint = colorScheme.onBackground,
                )
                if (selectedHashes.any { HiddenTag.imageHashes.contains(it) }) {
                    Icon(
                        Icons.Outlined.Visibility,
                        contentDescription = "Unhide ${selectedHashes.size} images",
                        modifier = Modifier
                            .padding(10.dp)
                            .fillMaxHeight()
                            .aspectRatio(1f)
                            .clickable {
                                openPopup {
                                    AlertDialog(
                                        icon = {
                                            Icon(Icons.Outlined.Visibility, contentDescription = "Unhide")
                                        },
                                        title = {
                                            Text(text = "Unhide ${selectedHashes.size} image${if (selectedHashes.size != 1) "s" else ""}")
                                        },
                                        text = {
                                            Text(text = "Are you sure you would like to Unhide ${selectedHashes.size} image${if (selectedHashes.size != 1) "s" else ""}?")
                                        },
                                        onDismissRequest = {
                                            closePopup()
                                        },
                                        confirmButton = {
                                            TextButton(
                                                onClick = {
                                                    closePopup()
                                                    selectedHashes.forEach {
                                                        images[it]?.removeTag(HiddenTag)
                                                    }
                                                    deselectAll()
                                                    updateImages()
                                                },
                                                content = { Text("Unhide ${selectedHashes.size} images") },
                                            )
                                        },
                                        dismissButton = {
                                            TextButton(
                                                onClick = {
                                                    closePopup()
                                                },
                                                content = { Text("Return") },
                                            )
                                        }
                                    )
                                }
                            },
                        tint = colorScheme.onBackground,
                    )
                } else {
                    Icon(
                        Icons.Outlined.VisibilityOff,
                        contentDescription = "Hide ${selectedHashes.size} images",
                        modifier = Modifier
                            .padding(10.dp)
                            .fillMaxHeight()
                            .aspectRatio(1f)
                            .clickable {
                                openPopup {
                                    AlertDialog(
                                        icon = {
                                            Icon(Icons.Outlined.VisibilityOff, contentDescription = "Hide")
                                        },
                                        title = {
                                            Text(text = "Hide ${selectedHashes.size} image${if (selectedHashes.size != 1) "s" else ""}")
                                        },
                                        text = {
                                            Text(text = "Are you sure you would like to hide ${selectedHashes.size} image${if (selectedHashes.size != 1) "s" else ""}?")
                                        },
                                        onDismissRequest = {
                                            closePopup()
                                        },
                                        confirmButton = {
                                            TextButton(
                                                onClick = {
                                                    closePopup()
                                                    selectedHashes.forEach {
                                                        images[it]?.addTag(HiddenTag)
                                                    }
                                                    deselectAll()
                                                    updateImages()
                                                },
                                                content = { Text("Hide ${selectedHashes.size} images") },
                                            )
                                        },
                                        dismissButton = {
                                            TextButton(
                                                onClick = {
                                                    closePopup()
                                                },
                                                content = { Text("Return") },
                                            )
                                        }
                                    )
                                }
                            },
                        tint = colorScheme.onBackground,
                    )
                }
                InputChip(
                    modifier = Modifier.padding(horizontal = 15.dp, vertical = 10.dp),
                    onClick = {
                        deselectAll()
                    },
                    label = { Text("Selected Images: ${selectedHashes.size}") },
                    selected = true,
                    avatar = {
                        Icon(
                            Icons.Outlined.Image,
                            contentDescription = "",
                            Modifier.size(InputChipDefaults.AvatarSize),
                        )
                    },
                    trailingIcon = {
                        Icon(
                            Icons.Outlined.Close,
                            contentDescription = "Clear",
                            Modifier.size(InputChipDefaults.AvatarSize)
                        )
                    },
                )
            }
        } else {
            LazyRow(
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                item {
                    Icon(
                        if (selectedTagFilters.isEmpty()) {
                            Icons.Outlined.FilterList
                        } else {
                            Icons.Outlined.FilterListOff
                        },
                        contentDescription = "Filters",
                        modifier = Modifier
                            .fillMaxHeight()
                            .aspectRatio(1f)
                            .padding(vertical = 5.dp, horizontal = 15.dp)
                            .onClick {
                                selectedTagFilters.clear()
                            },
                        tint = colorScheme.onSurface,
                    )
                }
                tags.values
                    .sortedByDescending { it.calculatedTagPriority }
                    .forEach { tag ->
                    item {
                        FilterChip(
                            leadingIcon = {
                                tag.icon?.let {
                                    Icon(
                                        it,
                                        tag.name,
                                        tint = colorScheme.onSurface
                                    )
                                }
                            },
                            selected = selectedTagFilters.contains(tag.uuid),
                            onClick = {
                                if (selectedTagFilters.contains(tag.uuid)) {
                                    selectedTagFilters.remove(tag.uuid)
                                } else {
                                    selectedTagFilters.add(tag.uuid)
                                }
                            },
                            label = { Text(tag.name) },
                            colors = FilterChipDefaults.filterChipColors(labelColor = colorScheme.onSurface)
                        )
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxHeight(),
                horizontalArrangement = Arrangement.End
            ) {
                InputChip(
                    modifier = Modifier.padding(horizontal = 15.dp, vertical = 10.dp),
                    onClick = {
                        updateImages()
                    },
                    label = { Text("Images: $imagesDisplayedSize", maxLines = 1) },
                    selected = true,
                    avatar = {
                        Icon(
                            Icons.Outlined.Image,
                            contentDescription = "",
                            Modifier.size(InputChipDefaults.AvatarSize)
                        )
                    },
                    trailingIcon = {
                        Icon(
                            Icons.Outlined.Refresh,
                            contentDescription = "Refresh",
                            Modifier.size(InputChipDefaults.AvatarSize)
                        )
                    },
                )
            }
        }
    }
}

@Composable
fun GalleryWaterfall(modifier: Modifier) {
    val listState = rememberLazyListState()

    updateImages()
    LazyColumn(
        state = listState,
        modifier = modifier
    ) {
        items(imagesDisplayed, key = { it.first().dateTime }) { imageGroup ->
            Day(
                modifier = Modifier
                    .padding(5.dp)
                    .fillMaxWidth(),
                date = imageGroup.first().dateTime,
                images = imageGroup.toTypedArray(),
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
            .background(color = colorScheme.surface)
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
                color = colorScheme.onBackground,
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
            ImageCard(
                image = image,
                modifier = Modifier
                    .height(height) // Make sure all images use the exact same height
                    .aspectRatio(image.imageSize.ratio)
                    .padding(horizontal = padding),
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ImageCard(
    modifier: Modifier,
    image: ImageData,
) {
    val painter by rememberAsyncImage(image)
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    Box(
        modifier = modifier
            .hoverable(interactionSource)
            .clip(RoundedCornerShape(4.dp))
            .onClick(interactionSource = interactionSource) {
                if (selectedHashes.isNotEmpty()) {
                    selectImage(image)
                }
            },

        contentAlignment = Alignment.TopCenter
    ) {
        Image(
            painter = painter,
            contentDescription = "",
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(4.dp))
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(25.dp),
            horizontalArrangement = Arrangement.End
        ) {
            if (isHovered || selectedHashes.contains(image.hash)) {
                Checkbox(
                    selectedHashes.contains(image.hash),
                    modifier = Modifier,
                    onCheckedChange = {
                        selectImage(image)
                    }
                )
            }
        }
    }
}

fun deselectAll() {
    selectedHashes.clear()
    lastClickedImageHash = ""
}

fun updateImages() {
    val searchBarContent = if (searchBarContent.text == "Search...") { "" } else { searchBarContent.text }
    val tokens = searchBarContent.split(" ", "/", "-")

    val conjunction = mutableListOf<Filter>()
    tokens.forEach {
        conjunction.add(
            listOf(
                DayOfWeekFilter(it),
                DayOfMonthFilter(it),
                MonthFilter(it),
                YearFilter(it),
                TagNameFilter(it)
            ).toDisJunction()
        )
    }
    val searchFilterDisjunction = mutableListOf(
        DayOfWeekFilter(searchBarContent),
        DayOfMonthFilter(searchBarContent),
        MonthFilter(searchBarContent),
        YearFilter(searchBarContent),
        TagNameFilter(searchBarContent),
        conjunction.toConjunction()
    ).toDisJunction()
    val tagFilterConjunction = mutableListOf<TagFilter>()
    selectedTagFilters
        .toList()
        .forEach { uuid ->
            tags[uuid]?.let { tag ->
                if (tag.calculatedTagPriority == 0u) {
                    tagFilterConjunction.add(TagFilter(tag))
                }
            }
        }

    if (!selectedTagFilters.contains(HiddenTag.uuid)) {
        tagFilterConjunction.add(HiddenItemsFilter())
    }

    imagesDisplayed = getImagesByDate(
        FilterJunction(JunctionType.CONJUNCTION, searchFilterDisjunction, tagFilterConjunction.toConjunction())
    ).also { imagesDisplayedSize = it.first }.second
}

private fun selectImage(image: ImageData) {
    val selected = selectedHashes.contains(image.hash)
    val lastImageData = images[lastClickedImageHash]
    if (shiftPressed && lastImageData != null) {
        val imagesDisplayedList = mutableListOf<ImageData>().apply {
            imagesDisplayed.forEach {
                addAll(it)
            }
        }

        imagesDisplayedList
            .subList(imagesDisplayedList.indexOf(lastImageData), imagesDisplayedList.indexOf(image) + 1)
            .forEach {
                if (selected) {
                    selectedHashes.remove(it.hash)
                } else {
                    if (!selectedHashes.contains(it.hash)) {
                        selectedHashes.add(it.hash)
                    }
                }
            }
    } else {
        if (selected) {
            selectedHashes.remove(image.hash)
        } else {
            if (!selectedHashes.contains(image.hash)) {
                selectedHashes.add(image.hash)
            }
        }
    }
    lastClickedImageHash = image.hash
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
