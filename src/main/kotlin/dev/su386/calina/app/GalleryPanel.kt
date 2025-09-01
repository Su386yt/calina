package dev.su386.calina.app

import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Checkbox
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme.colors
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.runtime.*
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.toPainter
import androidx.compose.ui.input.pointer.changedToDown
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.rememberPopupPositionProviderAtPosition
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.materials.CupertinoMaterials
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import dev.su386.calina.Calina.shiftPressed
import dev.su386.calina.app.App.closePopup
import dev.su386.calina.app.App.openPopup
import dev.su386.calina.app.App.searchBarContent
import dev.su386.calina.images.ImageData
import dev.su386.calina.images.ImageManager.getImagesByDate
import dev.su386.calina.images.ImageManager.images
import dev.su386.calina.images.filters.*
import dev.su386.calina.images.filters.FilterJunction.Companion.toConjunction
import dev.su386.calina.images.filters.FilterJunction.Companion.toDisJunction
import dev.su386.calina.images.tags.SystemTag.Companion.HiddenTag
import dev.su386.calina.images.tags.SystemTag.Companion.LikedTag
import dev.su386.calina.images.tags.Tag.Companion.tags
import dev.su386.calina.utils.AutoResizeText
import dev.su386.calina.utils.fillMaxHeightToMax
import dev.su386.calina.utils.fillMaxWidthToMax
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.concurrent.Semaphore
import kotlin.collections.listOf
import kotlin.io.path.Path
import kotlin.io.path.name
import kotlin.math.roundToInt


private var lazyListState: LazyListState? = null
private val semaphore = Semaphore(20)
private val selectedHashes = mutableStateListOf<String>()
private var lastClickedImageHash by mutableStateOf("")
private val selectedTagFilters = mutableStateListOf<UUID>()
private var imagesDisplayed by mutableStateOf(listOf<DayData>())
private var imagesDisplayedList by mutableStateOf(listOf<ImageData>())
private var imagesDisplayedCount by mutableIntStateOf(0)
private var scrollbarStops by mutableStateOf(listOf<ScrollBarStop>())

@Composable
fun GalleryPanel() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopEnd) {
        GalleryWaterfall(Modifier.fillMaxWidth()
            .padding(start = 5.dp,end = 10.dp))
        Column(
            Modifier.fillMaxSize()
        ) {
            FilterBar(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            )
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {

                ScrollBar()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class, ExperimentalHazeMaterialsApi::class)
@Composable
fun ScrollBar() {
    var boxSize by remember { mutableStateOf(IntSize.Zero) }
    var percent by remember { mutableDoubleStateOf(Double.NaN) }
    var isClicked by remember { mutableStateOf(false) }
    var isHovered by remember { mutableStateOf(false) }
    val tooltipState = rememberTooltipState()
    val scope = rememberCoroutineScope()

    fun updateOffset(raw: Offset) {
        if (boxSize.width > 0 && boxSize.height > 0) {
            percent = (raw.y.toDouble() / boxSize.height).coerceIn(0.0, 1.0)
            isHovered = (raw.x.toDouble() / boxSize.width) >= 0
        }
    }

    Box(
        modifier =  Modifier
            .fillMaxHeight()
            .hazeSource(LocalHazeState.current,5f)
            .then(if (isHovered) {
                Modifier.width(40.dp)
                    .hazeEffect(LocalHazeState.current, CupertinoMaterials.thick())
            } else {
                Modifier.width(20.dp)
                    .hazeEffect(LocalHazeState.current, CupertinoMaterials.thin())
            })
            .onSizeChanged { boxSize = it }
            .pointerInput(boxSize) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        event.changes.forEach { change ->
                            if (change.changedToDown()) isClicked = true
                            if (change.changedToUp()) isClicked = false
                            updateOffset(change.position)
                        }
                    }
                }
            }
    ) {
        if (isHovered) {
            scope.launch { tooltipState.show() }
            val dayIndex = if (percent.isFinite()) {
                val imageNumber = (imagesDisplayedList.size * percent).roundToInt()
                    .coerceIn(0, imagesDisplayedList.size - 1)
                imagesDisplayed.indexOfFirst { it.imagesBeforeStart < imageNumber && imageNumber <= it.imagesBeforeEnd }
                    .coerceIn(0, imagesDisplayed.size - 1)
            } else {
                0
            }

            LaunchedEffect(isClicked, dayIndex) {
                if (isClicked) {
                    lazyListState?.scrollToItem(dayIndex)
                }
            }

            TooltipBox(
                positionProvider = rememberPopupPositionProviderAtPosition(
                    Offset(0.toFloat(), (boxSize.height * percent).toFloat()),
                    alignment = Alignment.CenterEnd
                ),
                tooltip = {
                    val day = imagesDisplayed[dayIndex].images.first()
                    val format = SimpleDateFormat("MMMM YYYY", Locale.getDefault())
                    PlainTooltip { Text(format.format(day.dateTime)) }
                },
                state = tooltipState,
                content = { /* Your content that triggers the tooltip */ },
                enableUserInput = false
            )
        }

        val textMeasurer = rememberTextMeasurer()
        Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            val step = size.height.toDouble() / imagesDisplayedList.size
            val radius = 2.dp.toPx()
            for (stop in scrollbarStops) {
                if (stop.stopType == ScrollBarStop.StopType.YEAR) {
                    if (isHovered) {
                        drawText(
                            textMeasurer = textMeasurer,
                            text = stop.year.toString(),
                            style = TextStyle(
                                color = Color.Gray,
                                textAlign = TextAlign.Center,
                                fontSize = 10.sp
                            ),
                            size = Size(size.width, size.height),
                            topLeft = Offset(0f, (stop.pos * step).toFloat()),
                        )
                    } else {
                        drawRoundRect(
                            color = Color.Gray,
                            size = Size(radius * 3f, radius * 1.5f),
                            topLeft = Offset((size.width - radius * 3f) / 2f , (stop.pos * step).toFloat()),
                        )
                    }
                } else {
                    drawCircle(
                        color = Color.DarkGray,
                        radius = radius,
                        center = Offset(size.width / 2f, (stop.pos * step).toFloat())
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalHazeMaterialsApi::class)
@Composable
fun FilterBar(modifier: Modifier) {
    Row(
        modifier
            .clip(RoundedCornerShape(17))
            .hazeSource(LocalHazeState.current,4f)
            .hazeEffect(LocalHazeState.current, CupertinoMaterials.thin()),
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
                if (selectedHashes.any { LikedTag.imageHashes.contains(it) }) {
                    Icon(
                        Icons.Outlined.Favorite,
                        contentDescription = "Unlike ${selectedHashes.size} images",
                        modifier = Modifier
                            .padding(10.dp)
                            .fillMaxHeight()
                            .aspectRatio(1f)
                            .clickable {
                                selectedHashes.forEach {
                                    images[it]?.removeTag(LikedTag)
                                }
                                deselectAll()
                                updateImages()
                            },
                        tint = colorScheme.onBackground,
                    )
                } else {
                    Icon(
                        Icons.Outlined.FavoriteBorder,
                        contentDescription = "Like ${selectedHashes.size} images",
                        modifier = Modifier
                            .padding(10.dp)
                            .fillMaxHeight()
                            .aspectRatio(1f)
                            .clickable {
                                selectedHashes.forEach {
                                    images[it]?.addTag(LikedTag)
                                }
                                deselectAll()
                                updateImages()
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
                    label = {  Text("Images: $imagesDisplayedCount", maxLines = 1) },
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
    lazyListState = rememberLazyListState()

    updateImages()
    lazyListState?.let { listState ->
        LazyColumn(
            state = listState,
            modifier = modifier
        ) {
            item {
                Spacer(Modifier.height(55.dp))
            }
            items(imagesDisplayed, key = { it.key }) { imageData ->
                Day(
                    modifier = Modifier
                        .padding(5.dp)
                        .fillMaxWidth(),
                    date = imageData.images.first().dateTime,
                    images = imageData.images.toTypedArray(),
                )
            }
        }
    }
}

@OptIn(ExperimentalHazeMaterialsApi::class)
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
            .hazeSource(state = LocalHazeState.current, 2f)
            .hazeEffect(LocalHazeState.current, CupertinoMaterials.ultraThin())
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
                        layoutResult.size.width.let {if (it != 0) parentWidth = it}
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
                } else {
                    openPopup { ImageDisplay(
                        Modifier.fillMaxSize(),
                        image,
                        painter
                    ) }
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
                .fillMaxHeightToMax(.25f, 25.dp)
        ) {
            Row(
                Modifier.weight(1f),
                horizontalArrangement = Arrangement.Start
            ) {
                if (LikedTag.imageHashes.contains(image.hash)) {
                    Icon(
                        Icons.Outlined.Favorite,
                        "Liked",
                        Modifier
                            .padding(horizontal = 7.dp)
                            .aspectRatio(1f),
                        tint = colors.primary
                    )
                }
                if (HiddenTag.imageHashes.contains(image.hash)) {
                    Icon(
                        Icons.Outlined.VisibilityOff,
                        "Hidden",
                        Modifier
                            .padding(horizontal = 7.dp)
                            .aspectRatio(1f),
                        tint = Color.White
                    )
                }
            }
            Row(
                Modifier.weight(1f),
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
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ImageDisplay(
    modifier: Modifier = Modifier,
    imageData: ImageData,
    iconPainter: Painter = ColorPainter(Color.Black),
) {
    val painter by rememberAsyncImage(imageData, false, iconPainter)
    var info by remember { mutableStateOf(false) }
    Column(
        modifier
            .background(Color.Black)
            .onClick {},
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {


        Row(
            Modifier.fillMaxWidth()
                .height(35.dp),

        ) {

            Row(
                Modifier.fillMaxHeight()
                    .weight(1f),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {

            }

            Row(
                Modifier.fillMaxHeight()
                    .weight(1f),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val date = imageData
                    .dateTime
                    .toInstant()
                    .atOffset(
                        ZoneOffset.ofTotalSeconds(TimeZone.getDefault().rawOffset / 1000)
                    )
                    .toLocalDateTime()
                    .format(DateTimeFormatter.ofPattern("HH:mm dd MMMM yyyy"))

                AutoResizeText(
                    date,
                    modifier = Modifier
                        .padding(5.dp)
                        .fillMaxHeight()
                        .weight(1f),
                    color = Color.White
                )
            }
            Row(
                Modifier.fillMaxHeight()
                    .weight(1f),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Outlined.Info,
                    contentDescription = "Info",
                    Modifier
                        .padding(5.dp)
                        .fillMaxHeight()
                        .aspectRatio(1f)
                        .clickable {
                            info = !info
                        },
                    tint = Color.White
                )
                Icon(
                    Icons.Outlined.Close,
                    contentDescription = "Clear",
                    Modifier
                        .padding(5.dp)
                        .fillMaxHeight()
                        .aspectRatio(1f)
                        .clickable {
                            closePopup()
                        },
                    tint = Color.White
                )
            }
        }
        Row(
            Modifier
                .weight(1f)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painter,
                    contentDescription = "",
                    modifier = Modifier
                        .aspectRatio(imageData.imageSize.ratio)
                        .fillMaxSize()
                        .background(color = colorScheme.onBackground)
                )
            }
            if (info) {
                Column(
                    Modifier
                        .fillMaxWidthToMax(.33f, 350.dp)
                        .fillMaxHeight()
                ) {
                    AutoResizeText(
                        Path(imageData.filePaths.first()).name,
                        modifier = Modifier
                            .padding(4.dp)
                            .height(30.dp)
                            .fillMaxWidth(),
                        color = Color.White,
                    )
                    Row(
                        Modifier
                            .padding(5.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val date = imageData
                            .dateTime
                            .toInstant()
                            .atOffset(
                                ZoneOffset.ofTotalSeconds(TimeZone.getDefault().rawOffset / 1000)
                            )
                            .toLocalDateTime()
                            .format(DateTimeFormatter.ofPattern("HH:mm\ndd MMMM yyyy"))

                        Icon(
                            Icons.Outlined.Event,
                            "Date",
                            Modifier
                                .padding(10.dp)
                                .height(25.dp)
                                .aspectRatio(1f),
                            tint = Color.White
                        )
                        Text(
                            date,
                            color = Color.White,
                            fontSize = 18.sp
                        )
                    }
                    Row(
                        Modifier
                            .padding(5.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Outlined.Height,
                            "Image Size",
                            Modifier
                                .padding(10.dp)
                                .height(25.dp)
                                .aspectRatio(1f),
                            tint = Color.White
                        )
                        Row {
                            Text(
                                "${imageData.imageSize.x}x${imageData.imageSize.y}",
                                color = Color.White,
                                fontSize = 18.sp,
                                modifier = Modifier.padding(horizontal = 10.dp)
                            )

                            Text(
                                "${imageData.byteSize / 1024L} KiB",
                                color = Color.White,
                                fontSize = 18.sp,
                                modifier = Modifier.padding(horizontal = 10.dp)
                            )
                        }
                    }
                    Row(
                        Modifier
                            .padding(5.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val allNull = imageData.cameraInfo.name == null &&
                                imageData.cameraInfo.iso == null &&
                                imageData.cameraInfo.flash == null &&
                                imageData.cameraInfo.fNumber == null &&
                                imageData.cameraInfo.apertureValue == null &&
                                imageData.cameraInfo.colorSpace == null &&
                                imageData.cameraInfo.exposureTime == null &&
                                imageData.cameraInfo.exposureCompensation == null &&
                                imageData.cameraInfo.focalLength == null
                        val cameraInfoIcon = if (allNull) {
                            Icons.Outlined.NoPhotography
                        } else {
                            Icons.Outlined.Camera
                        }
                        Icon(
                            cameraInfoIcon,
                            "Camera Info",
                            Modifier
                                .padding(10.dp)
                                .height(25.dp)
                                .aspectRatio(1f),
                            tint = Color.White
                        )
                        Column {
                            Row {
                                imageData.cameraInfo.name?.let {
                                    Text(
                                        it,
                                        color = Color.White,
                                        fontSize = 18.sp,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                    )
                                }
                            }

                            Row {
                                imageData.cameraInfo.fNumber?.let {
                                    Text(
                                        "f/$it",
                                        color = Color.White,
                                        fontSize = 18.sp,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                    )
                                }
                                imageData.cameraInfo.focalLength?.let {
                                    Text(
                                        "$it mm" ,
                                        color = Color.White,
                                        fontSize = 18.sp,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                    )
                                }
                            }

                            Row {
                                imageData.cameraInfo.iso?.let {
                                    Text(
                                        "$it ISO" ,
                                        color = Color.White,
                                        fontSize = 18.sp,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                    )
                                }

                                imageData.cameraInfo.exposureTime?.let {
                                    Text(
                                        if (it == "1" || it == "1.0") "$it sec" else "$it secs",
                                        color = Color.White,
                                        fontSize = 18.sp,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                    )
                                }
                            }
                            Row {
                                imageData.cameraInfo.flash?.let {
                                    Text(
                                        it,
                                        color = Color.White,
                                        fontSize = 18.sp,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.weight(1f))

                    Row(
                        Modifier
                            .padding(5.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val filePaths = StringBuilder().apply {
                            imageData.filePaths.forEach {
                                this.append("$it\n")
                            }
                        }.toString()
                        Icon(
                            Icons.Outlined.Folder,
                            "File paths",
                            Modifier
                                .padding(10.dp)
                                .height(25.dp)
                                .aspectRatio(1f),
                            tint = Color.White
                        )
                        Text(
                            filePaths,
                            modifier = Modifier
                                .padding(4.dp)
                                .fillMaxWidth(),
                            color = Color.White,
                            fontSize = 12.sp
                        )
                    }

                    Row(
                        Modifier
                            .padding(5.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Outlined.Numbers,
                            "Hash",
                            Modifier
                                .padding(10.dp)
                                .height(25.dp)
                                .aspectRatio(1f),
                            tint = Color.DarkGray
                        )
                        Text(
                            imageData.hash,
                            modifier = Modifier
                                .padding(4.dp)
                                .fillMaxWidth(),
                            color = Color.DarkGray,
                            fontStyle = FontStyle.Italic,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
        ImageCarousel(
            Modifier.height(40.dp)
                .fillMaxWidth(),
            imageData.hash
        )
    }
}

@Composable
private fun ImageCarousel(
    modifier: Modifier = Modifier,
    selectedImageHash: String,
) {
    var parentWidth by remember { mutableStateOf(0) }
    var parentHeight by remember { mutableStateOf(0) }
    val selectedImageIndex = imagesDisplayedList.indexOfFirst { it.hash == selectedImageHash }
    Row(
        modifier
            .onGloballyPositioned { layoutResult ->
                parentWidth = layoutResult.size.width
            },
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.Bottom
    ) {
        val imagesInCarousel = getImagesForCarousel(
            height = parentHeight,
            parentWidth = parentWidth,
            padding = 2.dp,
            startIndex = selectedImageIndex
        )
        val ratio = 4f / 3f

        Row (
            Modifier.weight(1f)
                .fillMaxHeight(0.9f)
                .wrapContentWidth(Alignment.End, unbounded = true)
                .onGloballyPositioned { layoutResult ->
                    parentHeight = layoutResult.size.height
                },
            horizontalArrangement = Arrangement.End
        ) {
            for (image in imagesInCarousel.first) {
                val painter by rememberAsyncImage(image)

                Box(
                    Modifier
                        .padding(horizontal = 2.dp)
                        .aspectRatio(ratio)
                        .fillMaxSize()
                        .clip(RoundedCornerShape(10))
                        .background(color = colorScheme.onBackground)
                        .clickable {
                            closePopup()
                            openPopup {
                                ImageDisplay(
                                    Modifier.fillMaxSize(),
                                    image,
                                    painter
                                )
                            }
                        }
                ) {
                    Image(
                        painter = painter,
                        contentDescription = "",
                        modifier = Modifier
                            .fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }

            }
        }

        Image(
            painter = rememberAsyncImage(imagesDisplayedList[selectedImageIndex]).value,
            contentDescription = "",
            modifier = Modifier
                .padding(horizontal = 2.5.dp)
                .aspectRatio(ratio)
                .fillMaxWidth()
                .clip(RoundedCornerShape(15))
                .border(1.dp, colors.primary, RoundedCornerShape(15))
                .background(color = Color.Black)
        )

        Row (
            Modifier.weight(1f)
                .fillMaxHeight(0.9f)
                .wrapContentWidth(Alignment.Start, unbounded = true),
            horizontalArrangement = Arrangement.Start,
        ) {
            for (image in imagesInCarousel.second) {
                val painter by rememberAsyncImage(image)
                Box(
                    Modifier
                        .padding(horizontal = 2.dp)
                        .aspectRatio(ratio)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(10))
                        .background(color = Color.Black)
                        .clickable {
                            closePopup()
                            openPopup {
                                ImageDisplay(
                                    Modifier.fillMaxSize(),
                                    image,
                                    painter
                                )
                            }
                        }
                ) {
                    Image(
                        painter = painter,
                        contentDescription = "",
                        modifier = Modifier
                            .fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }
        }
    }
}

private fun getImagesForCarousel(
    height: Int,
    parentWidth: Int,
    padding: Dp,
    startIndex: Int
): Pair<List<ImageData>, List<ImageData>> {
    val left = mutableListOf<ImageData>()
    val right = mutableListOf<ImageData>()
    var leftWidth = 0f
    var rightWidth = 0f
    var leftIndex = startIndex - 1
    var rightIndex = startIndex + 1
    val ratio = imagesDisplayedList[startIndex].imageSize.ratio
    val imageWidth =  height * ratio

    while (
        parentWidth > 0 && height > 0 &&
        leftWidth < parentWidth/2 &&
        rightWidth < parentWidth/2 &&
        (leftIndex >= 0 || rightIndex < imagesDisplayedCount)
    ) {
        if (rightIndex < imagesDisplayedCount) {
            right.add(imagesDisplayedList[rightIndex])
            rightWidth += imageWidth + padding.value
            rightIndex++
        }
        if (leftIndex >= 0) {
            left.add(imagesDisplayedList[leftIndex])
            leftWidth += imageWidth + padding.value
            leftIndex--
        }
    }

    return Pair(left.reversed(), right)
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
        DayOfWeekFilter(searchBarContent.toString()),
        DayOfMonthFilter(searchBarContent.toString()),
        MonthFilter(searchBarContent.toString()),
        YearFilter(searchBarContent.toString()),
        TagNameFilter(searchBarContent.toString()),
        conjunction.toConjunction()
    ).toDisJunction()

    val tagFilterConjunction = mutableListOf<TagFilter>()

    Snapshot.withMutableSnapshot {
        selectedTagFilters
            .forEach { uuid ->
                if (uuid == HiddenTag.uuid) {
                    return@forEach
                }

                tags[uuid]?.let { tag ->
                    tagFilterConjunction.add(TagFilter(tag))
                }
            }

        if (!selectedTagFilters.contains(HiddenTag.uuid)) {
            tagFilterConjunction.add(HiddenItemsFilter())
        }
    }

    imagesDisplayed = mutableListOf()
    imagesDisplayed = getImagesByDate(
        FilterJunction(JunctionType.CONJUNCTION, searchFilterDisjunction, tagFilterConjunction.toConjunction())
    ).also { imagesDisplayed ->
        imagesDisplayedList = mutableListOf<ImageData>().apply {
            imagesDisplayed.forEach { addAll(it.images) }
        }.toList().also { imagesDisplayedCount = it.size }
    }

    updateScrollBarData()
}

private fun updateScrollBarData() {
    val stops = mutableListOf<ScrollBarStop>()
    var prevYear: Int? = null
    var prevMonth: Int? = null
    for (i in imagesDisplayedList.indices) {
        val im = imagesDisplayedList[i]
        if (im.calendar.get(Calendar.YEAR) != prevYear) {
            prevYear = im.calendar.get(Calendar.YEAR)
            prevMonth = im.calendar.get(Calendar.MONTH)

            stops.add(
                ScrollBarStop(
                    pos = i,
                    stopType = ScrollBarStop.StopType.YEAR,
                    month = prevMonth,
                    year = prevYear
                )
            )
        } else if (im.calendar.get(Calendar.MONTH) != prevMonth) {
            prevMonth = im.calendar.get(Calendar.MONTH)

            stops.add(
                ScrollBarStop(
                    pos = i,
                    stopType = ScrollBarStop.StopType.MONTH,
                    month = prevMonth,
                    year = prevYear
                )
            )
        }
    }

    scrollbarStops = stops
}

private fun selectImage(image: ImageData) {
    val selected = selectedHashes.contains(image.hash)
    val lastImageData = images[lastClickedImageHash]
    if (shiftPressed && lastImageData != null) {
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
fun rememberAsyncImage(
    image: ImageData,
    icon: Boolean = true,
    placeholder: Painter = ColorPainter(Color.Gray)
): State<Painter> {
    val scope = rememberCoroutineScope()
    val key = remember(image.hash, icon) { "${image.hash}-${if (icon) "icon" else "full"}" }

    return rememberSaveable(key) {
        mutableStateOf(placeholder)
    }.apply {
        if (value == placeholder) {
            LaunchedEffect(image, icon) {
                scope.launch(Dispatchers.IO) {
                    semaphore.acquire()
                    try {
                        val im = if (icon) {
                            image.icon
                        } else {
                            image.image
                        }
                        val loadedPainter = im.toPainter()
                        withContext(Dispatchers.Unconfined) {
                            value = loadedPainter
                        }
                        im.flush()
                    } finally {
                        semaphore.release()
                    }
                }
            }
        }
    }
}

data class DayData(
    val year: Int,
    val month: Byte,
    val day: Byte,
    val images: List<ImageData>,
    val imagesBeforeStart: Int,
) {
    val count: Int = images.size
    val imagesBeforeEnd = imagesBeforeStart + count
    val key: String
        get() = "$year$month$day"
}

private data class ScrollBarStop(val pos: Int, val stopType: StopType, val month: Int, val year: Int) {
    enum class StopType {
        MONTH,
        YEAR
    }
}