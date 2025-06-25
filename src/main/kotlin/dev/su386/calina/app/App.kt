package dev.su386.calina.app

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.onClick
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.MaterialTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import dev.su386.calina.app.App.activeIndex
import dev.su386.calina.app.App.navigationStack
import dev.su386.calina.app.App.panels
import dev.su386.calina.app.App.searchBarContent
import dev.su386.calina.utils.AutoResizeText

@OptIn(ExperimentalFoundationApi::class)
@Composable
@Preview
fun App() {
    val focusManager = LocalFocusManager.current
    val stack = remember { navigationStack }
    Box(
        modifier = Modifier
            .fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize(1f)
                .background(colorScheme.background)
                .focusable(true)
                .onClick {
                    focusManager.clearFocus()
                },
        ) {
            Box(
                Modifier
                .fillMaxHeight()
                .width(75.dp)
            ) {
                NavRail(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth()
                        .padding(start = 2.dp, end = 6.dp, top = 3.dp, bottom = 3.dp),
                    iconsData = panels.toTypedArray(),
                )
            }

            // Display
            Column(
                horizontalAlignment = Alignment.Start,
            ) {
                Box(modifier = Modifier
                    .padding(5.dp)
                    .fillMaxHeight()
                    .fillMaxWidth()
                ) {
                    NavigationWindow(modifier = Modifier.fillMaxSize())
                }
            }
        }
        println("Recomposing app")
        println(stack.size)
        for (popup in stack) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                println("a")
                popup()
            }
        }
    }

}

@Composable
fun NavigationWindow(modifier: Modifier = Modifier) {
    Column(modifier = modifier.focusable(true)) {
        Header(
            heading = panels[activeIndex].name,
            modifier = Modifier.fillMaxWidth()
                .fillMaxHeight(.075f)
                .heightIn(max = 50.dp)
                .padding(4.dp)
        )
        val panels = remember { panels }

        panels[activeIndex].panel()
    }
}

@Composable
fun Header(heading: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        AutoResizeText(
            text = heading,
            modifier = Modifier
                .fillMaxHeight()
                .weight(.1f),
            color = colorScheme.onBackground,
        )
        Box(
            Modifier.weight(.2f)
        )
        SearchBar(
            modifier = Modifier
                .fillMaxWidth(.5f)
                .fillMaxHeight()
        )
        Box(
            Modifier.weight(.2f)
        )
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .weight(.1f)
                .clip(RoundedCornerShape(25))
        )
    }
}

@Composable
fun SearchBar(modifier: Modifier) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(40))
                .background(color = colorScheme.surface)
        ) {
            BasicTextField(
                value = searchBarContent,
                onValueChange = { newText ->
                    searchBarContent = newText.copy(
                        selection = if (
                            newText.text == "Search..." ||
                            (newText.text == searchBarContent.text &&
                            searchBarContent.selection == TextRange(0, searchBarContent.text.length) &&
                            newText.selection.end == searchBarContent.text.length)
                            ) {
                            searchBarContent.selection
                        } else {
                            newText.selection
                        }
                    )
                    activeIndex = 0
                },
                maxLines = 1,
                textStyle = TextStyle(
                    color = colorScheme.onSurface,
                ),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
                    .align(Alignment.CenterStart)
                    .padding(horizontal = 10.dp)
                    .onFocusChanged { focusState ->
                        searchBarContent = searchBarContent.copy(
                            selection = if (focusState.isFocused) {
                                TextRange(0, searchBarContent.text.length)
                            } else {
                                TextRange.Zero
                            }
                        )

                    },
                cursorBrush = SolidColor(colorScheme.onSurface)
            )
        }
    }
}

object App {
    val panels = listOf(
        NavRailIconData(
            "Gallery",
            Icons.Default.Add,
            { GalleryPanel() },
            { println("Home button clicked") }
        ),
        NavRailIconData(
            "Settings",
            Icons.Default.Settings,
            { ConfigPanel() },
            { println("Settings button clicked") }
        ),
    )
    var activeIndex by mutableStateOf(0)
    var searchBarContent by mutableStateOf(TextFieldValue("Search..."))
    val navigationStack = mutableStateListOf<@Composable () -> Unit>()

    fun closePopup() {
        if (navigationStack.isNotEmpty()) {
            navigationStack.removeLast()
        } else {
            activeIndex = 0
        }
    }

    fun closeAllPopups() {
        navigationStack.clear()
        activeIndex = 0
    }

    fun openPopup(popupComposable: @Composable () -> Unit) {
        navigationStack.add(popupComposable)
    }
}