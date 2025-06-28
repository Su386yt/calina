package dev.su386.calina

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.window.ApplicationScope
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import dev.su386.calina.Calina.applicationScope
import dev.su386.calina.Calina.exitAppSafely
import dev.su386.calina.Calina.shiftPressed
import dev.su386.calina.app.App
import dev.su386.calina.app.theme.CalinaTheme
import dev.su386.calina.app.updateImages
import dev.su386.calina.images.ImageManager.cleanMissingImages
import dev.su386.calina.images.ImageManager.cleanOrphanedIcons
import dev.su386.calina.images.ImageManager.cleanSingleImages
import dev.su386.calina.images.ImageManager.cleanWrongImages
import dev.su386.calina.images.ImageManager.images
import dev.su386.calina.images.ImageManager.loadImageData
import dev.su386.calina.images.ImageManager.saveImageData
import dev.su386.calina.images.ImageManager.searchForImages
import dev.su386.calina.images.tags.SystemTag
import dev.su386.calina.tasks.OnCloseTask
import dev.su386.calina.tasks.OnStartTask
import dev.su386.calina.tasks.RepeatTask
import dev.su386.calina.tasks.TaskManager
import dev.su386.calina.tasks.TaskManager.onStart
import dev.su386.calina.tasks.TaskManager.register
import dev.su386.calina.utils.hour
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicLong
import javax.imageio.ImageIO

@OptIn(ExperimentalHazeMaterialsApi::class)
fun main() = runBlocking {
    register(OnStartTask("Check ImageIO Plugins") { ImageIO.scanForPlugins() })
    register(OnStartTask("Hello World Task") { println("Hello World!") })
    register(OnStartTask("Load config task", IO) { CalinaConfig.load(); CalinaConfig.save() })
    register(OnStartTask("Load Image Data", IO) {
        try {
            loadImageData()
            println("Images loaded: ${images.size}\nBytes loaded: ${Calina.bytesLoaded}\nMB loaded: ${Calina.bytesLoaded.toLong()/1000.0/1000.0}")
            saveImageData()
            cleanOrphanedIcons().also { println("Orphans Deleted: ${it.first}, Orphan Space Liberated: ${it.second / 1000}KB") }
            withContext(Dispatchers.Unconfined) {
                updateImages()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        images.values.forEach { it.cleanIconPath() }
    })

    register(RepeatTask(
        taskName = "Clean image file paths", taskCooldown = 8 * 60L * 1000L,
        startImmediately = true,
    ) {
        cleanMissingImages().also { it2 -> println("Missing image data deleted: $it2") }
        cleanSingleImages().also { it2 -> println("Single image data deleted: $it2") }
    })
    register(RepeatTask("Look for images", taskCooldown = CalinaConfig.get<Long>("performance/imageSearchTimeout") * 60L * 1000L) {
        println("Searching for images")
        for (string in CalinaConfig.get<List<String>>("gallery/imagePaths")) {
            searchForImages(string)
        }
        saveImageData()
        println("Images loaded: ${images.size}\nBytes loaded: ${Calina.bytesLoaded}\nMB loaded: ${Calina.bytesLoaded.toLong()/1000.0/1000.0}")
    })
    register(RepeatTask("UpdateHour", taskCooldown = 200) {
        hour += 0.075f
    })
    register(RepeatTask("Clean Wrong Images", taskCooldown = 5 * 60L * 1000L) {
        cleanWrongImages().also { it2 -> println("Cleaned $it2 wrong images") }
        it.duration = CalinaConfig.get<Long>("performance/imageHashTimeout") * 60L * 1000L
    })

    register(OnCloseTask("Save config task", IO) { CalinaConfig.save() })
    register(OnCloseTask("Save Image Data", IO) {
        try {
            saveImageData()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        try {
            SystemTag.SystemTagManager.saveTags()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    })

    launch {
        onStart()
    }

    application {
        applicationScope = this
        Window(
            onCloseRequest = ::exitAppSafely,
            onKeyEvent = {
                if (it.key == Key.ShiftLeft && it.type == KeyEventType.KeyDown) {
                    shiftPressed = true
                    true
                } else if (it.key == Key.ShiftLeft && it.type == KeyEventType.KeyUp) {
                    shiftPressed = false
                    true
                } else {
                    false
                }
            }
        ) {
            CalinaTheme {
                App()
            }
        }
    }
}

object Calina {
    var bytesLoaded = AtomicLong(0)
    var applicationScope: ApplicationScope? = null
    var shiftPressed by mutableStateOf(false)

    fun exitAppSafely() {
        TaskManager.onClose()
        applicationScope?.exitApplication()
    }
}
