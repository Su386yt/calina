package dev.su386.calina.images

import com.drew.imaging.ImageMetadataReader
import com.drew.metadata.exif.ExifIFD0Directory
import com.drew.metadata.exif.ExifSubIFDDirectory
import com.drew.metadata.exif.GpsDirectory

import dev.su386.calina.Calina
import dev.su386.calina.utils.HashingInputStream
import dev.su386.calina.utils.Location
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.IO
import java.awt.Image
import java.io.File
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.file.Files
import java.nio.file.attribute.BasicFileAttributes
import java.security.MessageDigest
import java.util.*
import javax.imageio.ImageIO

class ImageData constructor(
    val location: Location,
    val date: Long,
    val hash: String,
    val cameraInfo: CameraInfo,
    vararg var filePaths: String
) {
    /**
     * Returns an array of all valid images associated with this image data.
     */
    val images: Array<Image> get() {
        return filePaths.mapNotNull { path ->
            val file = File(path)
            val fileHash = runBlocking { file.inputStream().parallelSHA256() }
            if (file.exists() && fileHash == hash) {
                ImageIO.read(file)
            } else {
                null
            }
        }.toTypedArray()
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
            val chunkSize = 1024L * 1024L // 1 MB chunks
            val digest = MessageDigest.getInstance("SHA-256")

            return withContext(Dispatchers.IO) {
                val channel = this@parallelSHA256.channel
                val fileSize = channel.size()

                // Launch coroutines for chunks
                (0 until fileSize step chunkSize).map { start ->
                    async {
                        val size = minOf(chunkSize, fileSize - start).toInt() // Cast to Int
                        val buffer = ByteArray(size)
                        val byteBuffer = ByteBuffer.wrap(buffer)

                        // Reading into ByteBuffer instead of ByteArray
                        channel.position(start).read(byteBuffer)

                        // Updating the digest in a thread-safe way
                        synchronized(digest) {
                            digest.update(buffer)
                        }
                    }
                }.awaitAll()

                // Finalize the hash
                digest.digest().joinToString("") { "%02x".format(it) }
            }
        }

        /**
         * Returns the image data at that path.
         * If no metadata exists in an image, it returns a metadata with default values.
         *
         * Make sure to use Dispatchers.IO
         */
        suspend fun File.toImageData(): ImageData = withContext(Dispatchers.IO) {
            val messageDigest = MessageDigest.getInstance("SHA-256")

            // Open the InputStream and wrap it with HashingInputStream
            this@toImageData.inputStream().buffered(64 * 1024).use { fileInputStream ->
                val hashingInputStream = HashingInputStream(fileInputStream, messageDigest)

                // Read metadata using the hashing input stream
                val metadata = try {
                    ImageMetadataReader.readMetadata(hashingInputStream)
                } catch (e: Exception) {
                    println("Failed to read metadata for file ${this@toImageData.path}: ${e.message}")
                    null
                }

                // Ensure the entire stream is read
                val buffer = ByteArray(64 * 1024)
                while (hashingInputStream.read(buffer) != -1) {
                    // Continue reading to the end to include all bytes in the hash
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
                    this@toImageData.path
                )
            }
        }
    }

    data class CameraInfo  constructor(
        val name: String
    )
}
