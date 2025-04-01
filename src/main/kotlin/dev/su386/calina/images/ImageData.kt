package dev.su386.calina.images

import com.drew.imaging.ImageMetadataReader
import com.drew.metadata.exif.ExifIFD0Directory
import com.drew.metadata.exif.ExifSubIFDDirectory
import com.drew.metadata.exif.GpsDirectory
import dev.su386.calina.Calina
import dev.su386.calina.data.Database
import dev.su386.calina.images.ImageData.Companion.toImageData
import dev.su386.calina.utils.FileUtils.safelyDelete
import dev.su386.calina.utils.HashingImageInputStream
import dev.su386.calina.utils.HashingInputStream
import dev.su386.calina.utils.Location
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.IO
import org.apache.commons.imaging.Imaging
import java.awt.image.BufferedImage
import java.io.File
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.file.Files
import java.nio.file.attribute.BasicFileAttributes
import java.security.MessageDigest
import java.util.*
import javax.imageio.ImageIO
import javax.imageio.ImageReadParam
import javax.imageio.stream.ImageInputStream

private const val COMPRESSED_IMAGE_SIZE = 240

class ImageData(
    val location: Location,
    val date: Long,
    val hash: String,
    val cameraInfo: CameraInfo,
    private var cachedImageSize: ImageSize?,
    vararg var filePaths: String
) {
    private var imageIconPath: String? = null
    val imageSize: ImageSize get() {
        val cache = cachedImageSize
        if (cache != null) {
            return cache
        } else {
            var imageSize: ImageSize? = null
            try {
                val im = Imaging.getImageSize(File(this.filePaths.firstOrNull()))
                imageSize = ImageSize(im.width, im.height)
            } catch (_: Exception) {

            }

            return imageSize.also { cachedImageSize = it } ?: ImageSize(32, 32)
        }
    }
    /**
     * Returns the image associated with this image data.
     *
     * Caches the image for 30 seconds
     */
    val image: BufferedImage get() {
        try {
            for (file in filePaths) {
                val messageDigest = MessageDigest.getInstance("SHA-256")

                val imageInputStream = ImageIO.createImageInputStream(File(file))
                val hashingInputStream = HashingImageInputStream(imageInputStream, messageDigest)
                var image: BufferedImage? = null

                try {
                    image = ImageIO.read(hashingInputStream)
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                // Ensure the entire stream is read
                val buffer = ByteArray(64 * 1024)
                while (hashingInputStream.read(buffer) != -1) {
                    // Continue reading to the end to include all bytes in the hash
                }
                val hash = messageDigest.digest().joinToString("") { "%02x".format(it) }
                // Compute the hash now that we've read all bytes
                hashingInputStream.close()

                if (this.hash == hash && image != null){
//                    totalLoaded++
                    return image
                }
            }
        } catch (e: Exception) {
            println("Error loading icon for ${this.filePaths.firstOrNull()}: ${e.message}")
        }

        return BufferedImage(32, 32, BufferedImage.TYPE_INT_RGB)
    }

    val icon: BufferedImage get() {
        val path = imageIconPath ?: "${Database.PATH}/icons/${hash}.png".also { imageIconPath = it }
        val iconFile = File(path).also { it.parentFile.mkdirs() }
        iconFile.setWritable(true)
        if (iconFile.exists()) {
            try {
                return ImageIO.read(iconFile)
            } catch (e: Exception) {
                println("Error loading icon for ${this.filePaths.firstOrNull()} (hash: $hash): ${e.message}")

                imageIconPath = null
                if (iconFile.exists()) {
                    iconFile.delete()
                    iconFile.deleteOnExit()
                }
                return this.icon
            }
        } else {
            var image: BufferedImage? = null
            try {
                for (file in filePaths){
                    // Create an input stream for the image file.
                    val imageInputStream = ImageIO.createImageInputStream(File(file)) ?: continue
                    val readers = ImageIO.getImageReaders(imageInputStream)
                    if (!readers.hasNext()) continue

                    val reader = readers.next()
                    reader.input = imageInputStream

                    // Calculate a subsampling factor (an integer >= 1).
                    var subsampling = 1
                    while ((imageSize.x / subsampling) > this.imageSize.compressedImageSize.x && (imageSize.y / subsampling) > this.imageSize.compressedImageSize.y) {
                        subsampling++
                    }
                    // If subsampling overshoots, step back one.
                    if (subsampling > 1) subsampling--

                    // Set the subsampling factor.
                    val param: ImageReadParam = reader.defaultReadParam
                    param.setSourceSubsampling(subsampling, subsampling, 0, 0)

                    // Read the image with subsampling applied.
                    image = reader.read(0, param)
                    reader.dispose()
                    imageInputStream.close()
                }

                iconFile.createNewFile()
                ImageIO.write(image, "png", iconFile)

            } catch (e: Exception) {
                println("Error creating icon for ${this.filePaths.firstOrNull()}: ${e.message}")
            }

            return image ?: BufferedImage(imageSize.compressedImageSize.x, imageSize.compressedImageSize.y, BufferedImage.TYPE_INT_RGB)
        }
    }

    val dateTime: Date get() = Date(date)
    val calendar: Calendar get() = Calendar.getInstance().apply { timeInMillis = date }

    @Transient
    private var _tags: MutableSet<UUID>? = null

    val tags: MutableSet<UUID> get() = _tags ?: mutableSetOf<UUID>().also{ this._tags = it }

    /**
     * Adds a new tag to this image
     *
     * @param tag - tag to add
     */
    fun addTag(tag: Tag) {
        tags.add(tag.uuid)
        tag.imageHashes.add(this.hash)
    }

    /**
     * Checks whether the file in [filePaths] exists, and removes them if they do not
     *
     * @return The number of file paths removed
     */
    fun cleanFilePaths(): Int {
        val oldLength = filePaths.size
        this.filePaths = this.filePaths.filter { path -> File(path).exists() }.toTypedArray()
        return oldLength - filePaths.size
    }

    /**
     * Checks whether the file in [imageIconPath] exists, and removes them if they do not
     *
     * @return true if a file was removed, else otherwise
     */
    fun cleanIconPath(): Boolean {
        val file = File(imageIconPath ?: return false)
        return file.exists().also { if (!it) this.imageIconPath = null }
    }


    /**
     * Checks whether the file in [filePaths] refers to the same file, and removes them if they do not
     *
     * @return The number of file paths removed
     */
    suspend fun checkFileHashes(): Int = runBlocking<Int> {
        val jobs = mutableListOf<Deferred<Unit>>()
        val cleanedPaths = mutableListOf<String>()
        val oldLength = this@ImageData.filePaths.size

        this@ImageData.filePaths.forEach { path ->
            jobs.add(async(IO) {
                if (File(path).inputStream().parallelSHA256() == this@ImageData.hash) {
                    cleanedPaths.add(path)
                }
            })
        }

        jobs.awaitAll()

        this@ImageData.filePaths = cleanedPaths.toTypedArray()

        return@runBlocking oldLength - this@ImageData.filePaths.size
    }

    override fun toString(): String {
        return "ImageData(location=$location, date=$date, hash='$hash', cameraInfo=$cameraInfo, filePaths=${filePaths.contentToString()}, tags=$tags)"
    }

    companion object {
        /**
         * Returns an SHA-256 hash of the byte array
         */
        suspend fun FileInputStream.parallelSHA256(): String {
            val chunkSize = 4L * 1024L * 1024L // 4MB chunks
            val digest = MessageDigest.getInstance("SHA-256")

            return withContext(IO) {
                val channel = this@parallelSHA256.channel
                val fileSize = channel.size()

                val results = (0 until fileSize step chunkSize).map { start ->
                    async {
                        val size = minOf(chunkSize, fileSize - start)
                        val mappedBuffer: MappedByteBuffer = channel.map(
                            java.nio.channels.FileChannel.MapMode.READ_ONLY,
                            start,
                            size
                        )

                        val buffer = ByteArray(mappedBuffer.remaining())
                        mappedBuffer.get(buffer)
                        buffer //Return the buffer.
                    }
                }.awaitAll()

                results.forEach{buffer ->
                    digest.update(buffer)
                }

                digest.digest().joinToString("") { "%02x".format(it) }
            }
        }

        /**
         * Returns the image data at that path.
         * If no metadata exists in an image, it returns a metadata with default values.
         *
         * Make sure to use Dispatchers.IO
         */
        suspend fun File.toImageData(): ImageData = withContext(IO) {
            val messageDigest = MessageDigest.getInstance("SHA-256")

            val hashingInputStream = HashingInputStream(this@toImageData.inputStream().buffered(), messageDigest)
            var imageSize: ImageSize? = null
            try {
                val im = Imaging.getImageSize(this@toImageData)
                imageSize = ImageSize(im.width, im.height)
            } catch (_: Exception) {

            }

            // Ensure the entire stream is read
            val buffer = ByteArray(64 * 1024)
            while (hashingInputStream.read(buffer) != -1) {
                // Continue reading to the end to include all bytes in the hash
            }
            // Compute the hash now that we've read all bytes
            val hash = messageDigest.digest().joinToString("") { "%02x".format(it) }
            hashingInputStream.close()

            // Open the InputStream and wrap it with HashingInputStream
            this@toImageData.inputStream().buffered().use { input ->
                // Read metadata using the hashing input stream
                val metadata = try {
                    ImageMetadataReader.readMetadata(input)
                } catch (e: Exception) {
                    println("Failed to read metadata for file ${this@toImageData.path}: ${e.message}")
                    null
                }

                // Extract metadata as before
                val gpsDirectory = metadata?.getFirstDirectoryOfType(GpsDirectory::class.java)
                val location = gpsDirectory?.geoLocation?.let {
                    Location(it.latitude, it.longitude)
                } ?: Location.EMPTY

                val exifDirectory = metadata?.getFirstDirectoryOfType(ExifSubIFDDirectory::class.java)
                val time = try {
                    exifDirectory?.dateOriginal?.time
                        ?: exifDirectory?.getDate(ExifSubIFDDirectory.TAG_DATETIME_DIGITIZED)?.time
                        ?: Files.readAttributes(this@toImageData.toPath(), BasicFileAttributes::class.java)
                            .creationTime()
                            .toMillis()
                } catch (e: Exception) {
                    println("Failed to get creation time for file ${this@toImageData.path}: ${e.message}")
                    System.currentTimeMillis()
                }

                val ifd0Directory = metadata?.getFirstDirectoryOfType(ExifIFD0Directory::class.java)
                val cameraModel = ifd0Directory?.getString(ExifIFD0Directory.TAG_MODEL) ?: ""

                // Update bytesLoaded atomically
                Calina.bytesLoaded.addAndGet(this@toImageData.length())

                // Return the ImageData object with all the necessary information
                ImageData(
                    location = location,
                    date = time,
                    hash = hash,
                    cameraInfo = CameraInfo(cameraModel),
                    imageSize,
                    this@toImageData.path,
                )
            }
        }
    }

    data class CameraInfo(
        val name: String
    )

    data class ImageSize(val x: Int, val y: Int) {
        val ratio get() = x.toFloat() / y
        val compressedImageSize: ImageSize get() = if (x < y) {
                ImageSize(COMPRESSED_IMAGE_SIZE, (COMPRESSED_IMAGE_SIZE / ratio).toInt())
            } else {
                ImageSize((COMPRESSED_IMAGE_SIZE * ratio).toInt(), COMPRESSED_IMAGE_SIZE)
            }
        }

}
