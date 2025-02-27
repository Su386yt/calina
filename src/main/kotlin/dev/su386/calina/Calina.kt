package dev.su386.calina

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.fasterxml.jackson.databind.JsonNode
import dev.su386.calina.data.Database
import dev.su386.calina.images.ImageManager
import dev.su386.calina.images.ImageManager.loadImageData
import dev.su386.calina.images.ImageManager.readImageData
import dev.su386.calina.images.ImageManager.saveImageData
import dev.su386.calina.images.Tag.Companion.saveTags
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
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


@OptIn(DelicateCoroutinesApi::class)
fun main() {
    // Launch background tasks in a non-blocking coroutine
    GlobalScope.launch(IO) {
        println("Hello World!")
        loadImageData()
        CalinaConfig.load()
        println("Images loaded: ${ImageManager.images.size}\nBytes loaded: ${Calina.bytesLoaded}\nMB loaded: ${Calina.bytesLoaded.toLong()/1000.0/1000.0}")

        println("Read all data")
        for (string in CalinaConfig.get<List<String>>("gallery/imagePaths")) {
            readImageData(string)
        }
        println("Read all images")

        saveImageData()
        CalinaConfig.save()
        saveTags()
        println("Images loaded: ${ImageManager.images.size}\nBytes loaded: ${Calina.bytesLoaded}\nMB loaded: ${Calina.bytesLoaded.toLong()/1000.0/1000.0}")
    }

    // Now start the UI without blocking the background tasks
    application {
        Window(onCloseRequest = ::exitApplication) {
            MaterialTheme {
                App()
            }
        }
    }
}

object Calina {
    var bytesLoaded = AtomicLong(0)
}
