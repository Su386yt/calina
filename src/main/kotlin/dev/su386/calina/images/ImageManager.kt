package dev.su386.calina.images

import dev.su386.calina.data.Database.readData
import dev.su386.calina.data.Database.writeData
import dev.su386.calina.images.ImageData.Companion.toImageData
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.flow
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
                .filter { println(it); it.imageHashes.contains(imageData.hash) }
                .mapNotNull { println(it); it.uuid }
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
        println(imageSet)
        for (image in imageSet){
            println(image)
            registerImage(image)
        }
    }

    /**
     * Write all the images metadata from the JSON file at the given path
     *
     * @see dev.su386.calina.data.Database.writeData
     */
    fun saveImageData(){
        writeData(this.FILE_PATH, images.values.toTypedArray())
    }

    /**
     * Removes all images whose paths do not exist
     */
    fun cleanMissingImages() {
        images.forEach { (_, imageData) ->
            imageData.cleanFilePaths()
        }
    }

    /**
     * Removes all images whose files do not match their hash
     *
     * @param n - Takes the first [n] images
     */
    fun cleanWrongImages(n: Int = images.size) = runBlocking(IO) {
        val jobs = mutableListOf<Deferred<Unit>>()

        images.values.take(n).forEach {
            jobs.add(async(IO) { it.cleanFilePaths(); return@async })
        }

        jobs.awaitAll()
    }
}
