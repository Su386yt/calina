package dev.su386.calina.images

import dev.su386.calina.data.Database.readData
import dev.su386.calina.data.Database.writeData
import dev.su386.calina.images.ImageData.Companion.toImageData
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.IO
import kotlin.collections.plus
import java.io.File

object ImageManager {
    private const val FILE_PATH = "/image/imagedata.json"
    private val acceptedFileTypes = arrayOf("jpg", "jpeg", "png", "gif", "bmp", "tiff", "webp", "tif", "heic", "mp4", "avi", "mov", "dng","arw")

    val images: MutableMap<String, ImageData> = mutableMapOf()
    private val loadedPaths = mutableSetOf<String>()

    /**
     * Loads all the images that start with the given path sorted by most recent.
     * Goes through folders recursively.
     *
     * @param path - Start directory (note: the method loads images recursively)
     */
    fun readImageData(path: String) {
        runBlocking {
            val coroutines = mutableSetOf<Deferred<Unit>>()
            val walk = File(path).walkTopDown()
                .filter { it.extension.lowercase() in acceptedFileTypes && it.path !in loadedPaths  }
            var i = 0
            val count = walk.count()
            walk.forEach {
                    coroutines.add(
                        async(IO) {
                            try {
                                val newImage = it.toImageData()
                                registerImage(newImage)
                                i++
                                println("Image: $i/$count (${i*100/count})")
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }

                        }
                    )
                }

            coroutines.awaitAll()
        }

    }

    /**
     * Load an image into the RAM cache, either from persistent storage, or
     * an unseen image
     *
     * @param imageData - Image to be loaded into
     */
    private fun registerImage(imageData: ImageData?) {
        imageData ?: return

        if (imageData.hash in images) {
            images[imageData.hash]?.let { it.filePaths = it.filePaths as Array<String> + imageData.filePaths }
            return
        }

        images[imageData.hash] = imageData
        imageData.tags.addAll(
            Tag.tags.values
                .filter { it.imageHashes.contains(imageData.hash) }
                .mapNotNull { it.uuid }
        )

        loadedPaths.addAll(imageData.filePaths)
    }

    /**
     * Loads all the images from the JSON file at the given path
     *
     * @see dev.su386.calina.data.Database.readData
     */
    fun loadImageData() {
        val imageSet = readData<MutableSet<ImageData>>(this.FILE_PATH) ?: mutableSetOf()

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
        writeData(this.FILE_PATH, images.values.toList())
    }
}
