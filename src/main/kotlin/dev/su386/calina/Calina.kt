package dev.su386.calina

import dev.su386.calina.Config.Companion.config
import dev.su386.calina.Config.Companion.saveConfig
import dev.su386.calina.data.Database
import dev.su386.calina.images.ImageManager
import dev.su386.calina.images.ImageManager.loadImageData
import dev.su386.calina.images.ImageManager.readImageData
import dev.su386.calina.images.ImageManager.saveImageData
import dev.su386.calina.images.Tag
import dev.su386.calina.images.Tag.Companion.loadTags
import dev.su386.calina.images.Tag.Companion.saveTags
import java.util.*
import java.util.concurrent.atomic.AtomicLong


fun main() {
    println("Hello World!")
    loadTags()
    loadImageData()

    for (string in config.imageFolders) {
        readImageData(string)
    }

    Tag.tags[UUID.fromString("41b27d08-a801-42d0-a866-fd6b77b2aeca")]?.let { tag ->
        ImageManager.images.values.forEach {
            it.addTag(tag)
        }
    }

    saveImageData()
    saveConfig()
    saveTags()

    println("Images loaded: ${ImageManager.images.size}\nBytes loaded: ${Calina.bytesLoaded}\nMB loaded: ${Calina.bytesLoaded.toLong()/1000.0/1000.0}")
}

object Calina {
    var bytesLoaded = AtomicLong(0)
}
