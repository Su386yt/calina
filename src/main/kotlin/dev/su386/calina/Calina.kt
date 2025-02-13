package dev.su386.calina

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import dev.su386.calina.Config.Companion.config
import dev.su386.calina.Config.Companion.saveConfig
import dev.su386.calina.images.ImageManager
import dev.su386.calina.images.ImageManager.loadImageData
import dev.su386.calina.images.ImageManager.readImageData
import dev.su386.calina.images.ImageManager.saveImageData
import dev.su386.calina.images.Tag.Companion.saveTags
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import java.util.concurrent.atomic.AtomicLong


@Composable
@Preview
fun App() {
    var text by remember { mutableStateOf("Hello, World!") }

    MaterialTheme {
        Button(onClick = {
            text = "Hello, Desktop!"
        }) {
            Text(text)
        }
    }
}


fun main() {
    println("Hello World!")
    loadImageData()
    println("Images loaded: ${ImageManager.images.size}\nBytes loaded: ${Calina.bytesLoaded}\nMB loaded: ${Calina.bytesLoaded.toLong()/1000.0/1000.0}")
    println("Read all data")

    for (string in config.imageFolders) {
        readImageData(string)
    }
    println("Read all images")

    saveImageData()
    println("Saving data")
    saveConfig()
    saveTags()
    println("Images loaded: ${ImageManager.images.size}\nBytes loaded: ${Calina.bytesLoaded}\nMB loaded: ${Calina.bytesLoaded.toLong()/1000.0/1000.0}")

    return application {
        Window(onCloseRequest = ::exitApplication) {
            App()
        }
    }
}

object Calina {
    var bytesLoaded = AtomicLong(0)
}
