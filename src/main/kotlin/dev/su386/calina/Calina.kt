package dev.su386.calina

import androidx.compose.material.MaterialTheme
import androidx.compose.material.darkColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.window.ApplicationScope
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import dev.su386.calina.Calina.applicationScope
import dev.su386.calina.Calina.exitAppSafely
import dev.su386.calina.app.App
import dev.su386.calina.images.ImageManager.cleanMissingImages
import dev.su386.calina.images.ImageManager.cleanOrphanedIcons
import dev.su386.calina.images.ImageManager.images
import dev.su386.calina.images.ImageManager.loadImageData
import dev.su386.calina.images.ImageManager.readImageData
import dev.su386.calina.images.ImageManager.saveImageData
import dev.su386.calina.images.Tag.Companion.saveTags
import dev.su386.calina.tasks.OnCloseTask
import dev.su386.calina.tasks.OnStartTask
import dev.su386.calina.tasks.RepeatTask
import dev.su386.calina.tasks.TaskManager
import dev.su386.calina.tasks.TaskManager.onStart
import dev.su386.calina.tasks.TaskManager.register
import kotlinx.coroutines.Dispatchers.IO
import java.util.concurrent.atomic.AtomicLong
import javax.imageio.ImageIO


@Composable
fun CalinaTheme(content: @Composable () -> Unit) {
    val colors = remember {
        darkColors(
            primary = Color(0xFF2f65b7),
            primaryVariant = Color(0xFF4e75bf),
            secondary = Color(0xFFABA2EE),
            background = Color(0xFF080019),
            surface = Color(0xFF211e2e),
            error = Color(0xFFC14953),
            onPrimary = Color(0xFF2F2F2F),
            onSecondary = Color(0xFF101935),
            onBackground = Color(0xFFFFFFFF),
            onSurface = Color(0xFFFFFFFF),
            onError = Color(0xFF2F2F2F)
        )
    }

    MaterialTheme( // line 29
        colors = colors,
        content = content
    )
}

fun main() {
    register(OnStartTask("Check ImageIO Plugins") { ImageIO.scanForPlugins() })
    register(OnStartTask("Hello World Task") { println("Hello World!") })
    register(OnStartTask("Load config task", IO) { CalinaConfig.load(); CalinaConfig.save() })
    register(OnStartTask("Load Image Data", IO) {
        loadImageData()
        println("Images loaded: ${images.size}\nBytes loaded: ${Calina.bytesLoaded}\nMB loaded: ${Calina.bytesLoaded.toLong()/1000.0/1000.0}")
        saveImageData()
        saveTags()
        cleanOrphanedIcons().also { println("Orphans Deleted: ${it.first}, Orphan Space Liberated: ${it.second / 1000}KB") }
        images.values.forEach { it.cleanIconPath() }
    })

    register(RepeatTask(
        taskName = "Clean image file paths", taskCooldown = CalinaConfig.get<Long>("performance/imageHashTimeout") * 60L * 1000L,
        startImmediately = false,
        persistentCooldown = true
    ) {
        cleanMissingImages()
        it.duration = CalinaConfig.get<Long>("performance/imageHashTimeout") * 60L * 1000L
        println("Cleaned nonexistent images")
    })
    register(RepeatTask("Look for images", taskCooldown = CalinaConfig.get<Long>("performance/imageSearchTimeout") * 60L * 1000L) {
        println("Searching for images")
        for (string in CalinaConfig.get<List<String>>("gallery/imagePaths")) {
            readImageData(string)
        }
        saveImageData()
        println("Images loaded: ${images.size}\nBytes loaded: ${Calina.bytesLoaded}\nMB loaded: ${Calina.bytesLoaded.toLong()/1000.0/1000.0}")
    })


    register(OnCloseTask("Save config task", IO) { CalinaConfig.save() })
    register(OnCloseTask("Save Image Data", IO) { saveImageData(); saveTags() })

    onStart()
    application {
        applicationScope = this
        Window(onCloseRequest = ::exitAppSafely) {
            CalinaTheme {
                App()
            }
        }
    }
}

object Calina {
    var bytesLoaded = AtomicLong(0)
    var applicationScope: ApplicationScope? = null

    fun exitAppSafely() {
        TaskManager.onClose()
        applicationScope?.exitApplication()
    }
}
