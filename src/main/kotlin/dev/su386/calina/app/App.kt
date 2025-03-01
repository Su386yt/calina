package dev.su386.calina.app

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.su386.calina.app.App.activeIndex
import dev.su386.calina.app.App.panels

@Composable
@Preview
fun App() {
    Row(
        modifier = Modifier
            .fillMaxSize(1f)
            .background(MaterialTheme.colors.background),
    ) {
        // Nav Rail
        Column {
            // Nav Bar Box
            Box(modifier = Modifier
                .fillMaxHeight()
                .width(75.dp)
            ) {
                NavRail(
                    modifier = Modifier
                        .padding(start = 2.dp, end = 6.dp, top = 3.dp, bottom = 3.dp),
                    iconsData = panels.toTypedArray(),
                )

            }
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
}

@Composable
fun NavigationWindow(modifier: Modifier = Modifier) {
    Box(modifier = modifier) {
        val panels = remember { panels }

        panels[activeIndex].panel()
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
}