package dev.su386.calina.app

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.su386.calina.Calina
import dev.su386.calina.app.App.panels

@Composable
@Preview
fun App() {

    Row(
        modifier = Modifier
            .fillMaxSize(1f)
            .background(MaterialTheme.colors.background),
    ) {
        var activeNavIndex = remember { mutableStateOf(0) }

        // Nav Rail
        Column {
            // Nav Bar Box
            Box(modifier = Modifier
                .fillMaxHeight()
                .width(75.dp)
            ) {
                NavRail(activeNavIndex, *panels.toTypedArray())
            }
        }

        // Display
        Column {
            Box(modifier = Modifier
                .fillMaxHeight()
                .background(Color.Red)
                .fillMaxWidth()
            ) {
            }
        }
    }
}

object App {
    val panels = listOf(
        NavRailIconData("Settings", Icons.Default.Settings, false, { println("Settings button clicked") })
    )
}