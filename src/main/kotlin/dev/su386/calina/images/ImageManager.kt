package dev.su386.calina.images

import dev.su386.calina.data.Database.readData
import dev.su386.calina.data.Database.writeData
import dev.su386.calina.images.ImageData.Companion.toImageData
import java.io.File

object ImageManager {
    private const val FILE_PATH = "/image/imagedata.json"
    private val acceptedFileTypes = arrayOf("jpg", "jpeg", "png", "gif", "bmp", "tiff", "webp")

    val images: MutableMap<String, ImageData> = mutableMapOf()
    private val loadedPaths = mutableSetOf<String>()

    /**
     * Loads all the images that start with the given path sorted by most recent.
     * Goes through folders recursively.
     *
     * @param path - Start directory (note: the method loads images recursively)
     */
    fun readImageData(path: String) {
        File(path).walkBottomUp()
            .filter { !it.isDirectory && it.extension in acceptedFileTypes  && it.path !in loadedPaths  }
            .forEach {
                val newImage = it.toImageData()
                registerImage(newImage)
            }
    }

    /**
     * Load an image into the RAM cache, either from persistent storage, or
     * an unseen image
     *
     * @param imageData - Image to be loaded into
     */
    private fun registerImage(imageData: ImageData) {
        images[imageData.id] = imageData

        Tag.tags.values
            .filter { imageData.hash in it.imageHashes }
            .forEach { imageData.tags.add(it.uuid) }

        loadedPaths.add(imageData.cachedPath)
    }

    /**
     * Loads all the images from the JSON file at the given path
     *
     * @see dev.su386.calina.data.Database.readData
     */
    fun loadImageData() {
        val imageSet = readData<MutableSet<ImageData>>(this.FILE_PATH) ?: return

        for (image in imageSet){
            registerImage(image)
        }
    }

    /**
     * Write all the images metadata from the JSON file at the given path
     *
     * @see dev.su386.calina.data.Database.writeData
     */
    fun saveImageData(){
        writeData(this.FILE_PATH, images.values)
    }
}
