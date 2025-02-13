package dev.su386.calina

import androidx.compose.material.MaterialTheme
import androidx.compose.material.darkColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import dev.su386.calina.app.App
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


@OptIn(DelicateCoroutinesApi::class)
@Composable
fun CalinaTheme(content: @Composable () -> Unit) {
    val colors = remember {
        darkColors(
            primary = Color(0xFF2F97C1),
            primaryVariant = Color(0xFF2F97C1),
            secondary = Color(0xFFABA2EE),
            background = Color(0xFF333745),
            surface = Color(0xFFEBF2FA),
            error = Color(0xFFC14953),
            onPrimary = Color(0xFF2F2F2F),
            onSecondary = Color(0xFF101935),
            onBackground = Color(0xFF92828D),
            onSurface = Color(0xFF2F2F2F),
            onError = Color(0xFF2F2F2F)
        )
    }

    MaterialTheme( // line 29
        colors = colors,
        content = content
    )
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
            CalinaTheme {
                App()
            }
        }
    }
}

object Calina {
    var bytesLoaded = AtomicLong(0)
}
