package dev.su386.calina.images

import dev.su386.calina.data.Database.readData
import dev.su386.calina.data.Database.writeData
import dev.su386.calina.images.ImageData.Companion.toImageData
import java.io.File
import java.nio.file.Files
import java.nio.file.attribute.BasicFileAttributes

object ImageManager {
    private val images: MutableMap<String, ImageData> = mutableMapOf()
    private val loadedPaths = mutableSetOf<String>()



    private val acceptedFileTypes = arrayOf(".jpg", ".jpeg", ".png", ".gif", ".bmp", ".tiff", ".webp")

    /**
     * Loads all the images that start with the given path sorted by most recent.
     * Goes through folders recursively.
     *
     * @param path - Start directory (note: the method loads images recursively)
     */
    fun loadImages(path: String) {
        File(path).walkBottomUp()
            .filter { it.isFile && it.extension in acceptedFileTypes && it.path !in loadedPaths }
            .sortedBy { Files.readAttributes(it.toPath(), BasicFileAttributes::class.java).lastModifiedTime().toMillis() }
            .forEach {
                val newImage = it.toImageData()
                registerImage(newImage)
            }
    }


    private fun registerImage(imageData: ImageData) {
        images[imageData.id] = imageData
        loadedPaths.add(imageData.cachedPath)
    }

    /**
     * Loads all the images from the JSON file at the given path
     *
     * @param filepath - The local path within the database
     * @see dev.su386.calina.data.Database.readData
     */
    fun loadDatabase(filepath: String) {
        val imageSet = readData<MutableSet<ImageData>>(filepath) ?: return

        for (image in imageSet){
            registerImage(image)
        }
    }

    /**
     * Write all the images metadata from the JSON file at the given path
     *
     * @param filepath - The local path within the database
     * @see dev.su386.calina.data.Database.writeData
     */
    fun writeDatabase(path: String){
        writeData(path, images.values)
    }
}