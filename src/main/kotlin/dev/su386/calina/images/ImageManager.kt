package dev.su386.calina.images

import dev.su386.calina.data.Database.readData
import dev.su386.calina.data.Database.writeData
import dev.su386.calina.images.ImageData.Companion.toImageData
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Semaphore
import kotlin.collections.plus
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

object ImageManager {
    private const val FILE_PATH = "/image/imagedata.json"
    private val acceptedFileTypes = arrayOf("jpg", "jpeg", "png", "gif", "bmp", "tiff", "webp", "tif", "heic", "mp4", "avi", "mov", "dng","arw")

    val images: MutableMap<String, ImageData> = ConcurrentHashMap()
    private val loadedPaths = mutableSetOf<String>()

    /**
     * Loads all the images that start with the given path sorted by most recent.
     * Goes through folders recursively.
     *
     * @param path - Start directory (note: the method loads images recursively)
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun readImageData(path: String) {
        runBlocking {
            val count = AtomicInteger(0)
            val totalFiles = AtomicInteger(0)

            File(path).walkTopDown()
                .filter { file ->
                    file.extension.lowercase() in acceptedFileTypes && file.path !in loadedPaths
                }
                .onEach { totalFiles.incrementAndGet() } // Count files on-the-fly
                .asFlow()
                .flatMapMerge(concurrency = Runtime.getRuntime().availableProcessors()) { file ->
                    flow {
                        try {
                            val newImage = withContext(IO) { file.toImageData() }
                            registerImage(newImage)
                            val processed = count.incrementAndGet()

                            // Update progress less frequently to reduce I/O overhead
                            if (processed % 100 == 0 || processed == totalFiles.get()) {
                                println("Image: $processed/${totalFiles.get()} (${processed * 100 / totalFiles.get()}%)")
                            }
                            emit(Unit)
                        } catch (e: Exception) {
                            println("Error processing file ${file.path}: ${e.message}")
                        }
                    }
                }
                .collect()
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
