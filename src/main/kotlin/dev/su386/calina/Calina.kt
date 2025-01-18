package dev.su386.calina

import dev.su386.calina.Config.Companion.config
import dev.su386.calina.Config.Companion.saveConfig
import dev.su386.calina.images.ImageManager
import dev.su386.calina.images.ImageManager.saveImageData
import dev.su386.calina.images.Tag
import dev.su386.calina.images.Tag.Companion.loadTags
import dev.su386.calina.images.Tag.Companion.saveTags
import java.util.*


fun main() {
    println("Hello World!")
    loadTags()

    val startTime = System.currentTimeMillis()
    ImageManager.loadImageData()
    val loadImageData = System.currentTimeMillis()

    for (string in config.imageFolders) {
        ImageManager.readImageData(string)
    }

    val imageLoadTime = (System.currentTimeMillis())

    Tag.tags[UUID.fromString("41b27d08-a801-42d0-a866-fd6b77b2aeca")]?.let { tag ->
        ImageManager.images.values.forEach {
            it.addTag(tag)
        }
    }

    val imageTagTime = (System.currentTimeMillis())
    saveImageData()

    val saveImageTime = System.currentTimeMillis()

    saveConfig()
    saveTags()

    println("Images loaded: ${ImageManager.images.size}\nTime to load database: ${loadImageData - startTime}ms\nTime to load images: ${imageLoadTime - loadImageData}ms\nTime to tag images: ${imageTagTime - imageLoadTime}ms\nTime to save database: ${saveImageTime - imageTagTime}ms")
}
