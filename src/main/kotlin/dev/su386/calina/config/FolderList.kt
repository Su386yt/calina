package dev.su386.calina.config

import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import dev.su386.calina.Calina.shiftPressed
import dev.su386.calina.utils.AutoResizeText
import java.awt.Desktop
import java.io.File
import javax.swing.JFileChooser
import javax.swing.UIManager
import javax.swing.UnsupportedLookAndFeelException
import javax.swing.filechooser.FileSystemView
import kotlin.io.path.Path

class FolderList(
    name: String,
    description: String,
    defaultValue: SnapshotStateList<String> = mutableStateListOf<String>(),
) : ConfigOption(
    name,
    description,
    size = 10f
) {
    override val value: SnapshotStateList<String> = defaultValue
    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    @Composable
    override fun getComposable() {
        Column {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .fillMaxWidth()
                    .wrapContentHeight(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                AutoResizeText(
                    text = "$name:",
                    modifier = Modifier
                        .padding(12.dp)
                        .height(30.dp),
                    color = MaterialTheme.colorScheme.onSurface,
                    align = Alignment.CenterStart,
                )
                FilledTonalButton(
                    modifier = Modifier.height(56.dp),
                    onClick = {
                        try {
                            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())

                        } catch (_: UnsupportedLookAndFeelException) {
                            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName())
                        }
                        val chooser = JFileChooser(
                            FileSystemView.getFileSystemView().defaultDirectory,
                            FileSystemView.getFileSystemView()
                        ).apply {
                            dialogTitle = "Select a Folder"
                            fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
                            isAcceptAllFileFilterUsed = false
                        }

                        val result = chooser.showOpenDialog(null)

                        if (result == JFileChooser.APPROVE_OPTION) {
                            chooser.selectedFile
                                ?.walk()
                                ?.filter { it.isDirectory }
                                ?.take(500)
                                ?.forEach { folder ->
                                    if (!value.contains(folder.name)) {
                                        value.add(folder.absolutePath)
                                    }
                                    val seen = mutableSetOf<String>()
                                    value.retainAll { seen.add(it) }
                                }
                        }
                    }
                ) {
                    Icon(
                        Icons.Filled.Folder,
                        contentDescription = "Add Folders",
                        Modifier.height(24.dp)
                            .aspectRatio(1f)
                    )
                    Text(
                        "Add Folders",
                        maxLines = 1
                    )
                }
            }

            FlowRow(
                Modifier.wrapContentHeight()
                    .fillMaxWidth(.85f)
                    .padding(12.dp),
            ) {
                var hoveredValue by remember { mutableStateOf("")}

                value.sorted()
                    .forEach { folder ->
                    val path = Path(folder).toAbsolutePath()
                    val label = when (val count = path.nameCount) {
                        in 1..3 -> path.toString()
                        else -> "...${File.separator}${path.subpath(count - 3, count)}${File.separator}"
                    }
                    val interactionSource = remember { MutableInteractionSource() }
                    val isHovered by interactionSource.collectIsHoveredAsState()
                    if (isHovered) {
                        hoveredValue = path.toString()
                    } else if (hoveredValue == path.toString()) {
                        hoveredValue = ""
                    }
                    InputChip(
                        modifier = Modifier.height(32.dp),
                        selected = if (shiftPressed && hoveredValue.isNotEmpty() && path.toString().startsWith(hoveredValue) ) {
                            true
                        } else if (isHovered) {
                            true
                        }else {
                            false
                        },
                        onClick = {
                            if (Desktop.isDesktopSupported()) {
                                Desktop.getDesktop().open(path.toFile())
                            }
                        },
                        label = { Text(
                            label,
                            textDecoration = if (shiftPressed && hoveredValue.isNotEmpty()  && path.toString().startsWith(hoveredValue) ) {
                                TextDecoration.LineThrough
                            } else if (isHovered) {
                                TextDecoration.LineThrough
                            } else {
                                TextDecoration.None
                            }
                        ) },
                        trailingIcon = {
                            Icon(
                                Icons.Outlined.Close,
                                "Delete",
                                Modifier.clickable {
                                    if (shiftPressed) {
                                        value.removeAll { it.startsWith(folder) }
                                    } else {
                                        value.remove(folder)
                                    }
                                }
                                .hoverable(interactionSource),
                            )
                        }
                    )
                }
            }
        }
    }

    override fun loadFromJson(jsonNode: JsonElement) {
        jsonNode.asJsonArray
            .forEach { value.add(it.asString) }
    }

    override fun saveToJson(): JsonElement = JsonArray().apply { value.forEach { add(it) } }
}