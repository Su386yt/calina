package dev.su386.calina.images

import dev.su386.calina.CalinaConfig
import dev.su386.calina.data.Database
import dev.su386.calina.data.Database.readData
import dev.su386.calina.data.Database.writeData
import dev.su386.calina.images.ImageData.Companion.toImageData
import dev.su386.calina.images.filters.Filter
import dev.su386.calina.utils.FileUtils.safelyDelete
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.flow
import java.io.File
import java.time.ZoneId
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

object ImageManager {
    private const val FILE_PATH = "/image/imagedata.json"
    private val acceptedFileTypes = arrayOf("jpg", "jpeg", "png", "gif", "bmp", "tiff", "webp", "tif"/*, "heic", "mp4", "avi", "mov", "dng","arw"*/)

    val images: MutableMap<String, ImageData> = ConcurrentHashMap()
    private val loadedPaths = mutableSetOf<String>()

    /**
     * Loads all the images that start with the given path sorted by most recent.
     * Goes through folders recursively.
     *
     * @param path - Start directory (note: the method loads images recursively)
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun searchForImages(path: String) {
        runBlocking {
            val count = AtomicInteger(0)
            val totalFiles = AtomicInteger(0)

            File(path).listFiles()
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
                            e.printStackTrace()
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
        writeData(this.FILE_PATH, images.values.toTypedArray())
    }

    /**
     * Removes all images whose paths do not exist
     */
    fun cleanMissingImages(): Int {
        var i = 0
        images.forEach { (_, imageData) ->
            i += imageData.cleanFilePaths()
        }
        return i
    }

    /**
     * Remove imagedata objects that have no file paths attached to them
     *
     * @return the total number of imagedata objects removed
     */
    fun cleanSingleImages(): Int {
        return images.filter { (_, imageData) ->
            imageData.filePaths.isEmpty()
        }.onEach { (key, _) ->
            images.remove(key)
        }.count()
    }

    /**
     * Cleans all icons in the icon folder that do not have an image referencing it
     *
     * @return the numbers of orphans deleted
     */
    fun cleanOrphanedIcons(): Pair<Int, Long> {
        var orphanedIcons = 0
        var orphanedIconSpace = 0L
        val iconPath = File("${Database.PATH}/icons/")
        iconPath.walk()
            .filter { !images.containsKey(it.nameWithoutExtension) || it.length() == 0L}
            .forEach { it.safelyDelete().also { deleted -> if (deleted) orphanedIcons++; orphanedIconSpace += it.length() } }
        return Pair(orphanedIcons, orphanedIconSpace)
    }

    /**
     * Removes all images whose files do not match their hash
     *
     * @param n - Takes the first [n] images
     */
    fun cleanWrongImages(n: Int = images.size) = runBlocking (IO) {
        val jobs = mutableListOf<Deferred<Int>>()
        images.values
            .filter { it.timeSinceLastHashCheck < System.currentTimeMillis() - 1000 * 60 * 60 * 24 * CalinaConfig.get<Double>("performance/imageHashTimeout") }
            .sortedBy { it.timeSinceLastHashCheck }.take(n)
            .forEach {
                jobs.add(
                    async(IO) {
                        it.checkFileHashes()
                    }
                )
            }

        jobs.awaitAll().sum()
    }

    fun getImagesByDate(filter: Filter): List<List<ImageData>> = images
        .values
        .sortedByDescending { it.date }
        .filter { filter.isValidImage(it) }
        .groupBy {
            it.dateTime.toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()
        }
        .values
        .toList()
}
