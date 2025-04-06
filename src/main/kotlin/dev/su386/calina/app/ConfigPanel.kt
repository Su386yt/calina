package dev.su386.calina.app

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.su386.calina.CalinaConfig

@Composable
@Preview
fun ConfigPanel() {
    Box(
        modifier = Modifier.fillMaxSize(),

    ) {
        CalinaConfig.getComposable(modifier = Modifier.fillMaxSize())
    }
}