package dev.su386.calina.images

import com.drew.imaging.ImageMetadataReader
import com.drew.metadata.exif.ExifIFD0Directory
import com.drew.metadata.exif.ExifSubIFDDirectory
import com.drew.metadata.exif.GpsDirectory
import dev.su386.calina.Calina
import dev.su386.calina.data.Database
import dev.su386.calina.utils.HashingInputStream
import dev.su386.calina.utils.Location
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.IO
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.nio.MappedByteBuffer
import java.nio.file.Files
import java.nio.file.attribute.BasicFileAttributes
import java.security.MessageDigest
import java.util.*
import javax.imageio.ImageIO

private const val COMPRESSED_IMAGE_SIZE = 240

class ImageData(
    val location: Location,
    val date: Long,
    val hash: String,
    val cameraInfo: CameraInfo,
    val imageSize: ImageSize,
    vararg var filePaths: String
) {
//    @Transient
//    private var cachedImage: BufferedImage? = null
    private var imageIconPath: String? = null

    /**
     * Returns the image associated with this image data.
     *
     * Caches the image for 30 seconds
     */
    val image: BufferedImage get() {
        try {
//            if (cache != null) {
//                return cache
//            } else {
                for (file in filePaths) {
                    val messageDigest = MessageDigest.getInstance("SHA-256")

                    File(file).inputStream().buffered(64 * 1024).use { inputStream ->
                        val hashingInputStream = HashingInputStream(inputStream, messageDigest)

                        val image = ImageIO.read(hashingInputStream)
                        // Ensure the entire stream is read
                        val buffer = ByteArray(64 * 1024)
                        while (hashingInputStream.read(buffer) != -1) {
                            // Continue reading to the end to include all bytes in the hash
                        }
                        val hash = messageDigest.digest().joinToString("") { "%02x".format(it) }
                        if (this.hash == hash && image != null){
//                            cachedImage = image
                            return image
                        }
                    }
//                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return BufferedImage(32, 32, BufferedImage.TYPE_INT_RGB).also { /* cachedImage = it */ }


    }

    val icon: BufferedImage get() {
        val path = imageIconPath ?: "${Database.PATH}/icons/$hash+$date.png".also { imageIconPath = it }
        val file = File(path).also { it.parentFile.mkdirs() }
        if (file.exists()) {
            return ImageIO.read(file)
        } else {
            val compressedImageSize = imageSize.compressedImageSize
            val original = this.image
            val imageType = if (original.transparency != BufferedImage.OPAQUE)
                BufferedImage.TYPE_INT_ARGB
            else
                BufferedImage.TYPE_INT_RGB

            // Create a new BufferedImage with the scaled dimensions.
            val resizedImage = BufferedImage(compressedImageSize.x, compressedImageSize.y, imageType)

            val graphics: Graphics2D = resizedImage.createGraphics().apply {
                setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
                setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
            }

            // Draw the original image into the resized one.
            graphics.drawImage(original, 0, 0, compressedImageSize.x, compressedImageSize.y, null)
            graphics.dispose()

            file.createNewFile()
            ImageIO.write(resizedImage, "png", file)

            return resizedImage
        }
    }

    val dateTime: Date get() = Date(date)

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

            // Open the InputStream and wrap it with HashingInputStream
            this@toImageData.inputStream().buffered(64 * 1024).use { fileInputStream ->
                val hashingInputStream = HashingInputStream(fileInputStream, messageDigest)

                val image = ImageIO.read(hashingInputStream)
                val imageSize = ImageSize(image?.width ?: 32, image?.height ?: 32)
                // Ensure the entire stream is read
                val buffer = ByteArray(64 * 1024)
                while (hashingInputStream.read(buffer) != -1) {
                    // Continue reading to the end to include all bytes in the hash
                }

                // Read metadata using the hashing input stream
                val metadata = try {
                    ImageMetadataReader.readMetadata(this@toImageData.inputStream())
                } catch (e: Exception) {
                    println("Failed to read metadata for file ${this@toImageData.path}: ${e.message}")
                    null
                }

                // Compute the hash now that we've read all bytes
                val hash = messageDigest.digest().joinToString("") { "%02x".format(it) }

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
