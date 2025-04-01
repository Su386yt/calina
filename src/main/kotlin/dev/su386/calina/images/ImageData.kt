package dev.su386.calina.images

import com.drew.imaging.ImageMetadataReader
import com.drew.metadata.exif.ExifIFD0Directory
import com.drew.metadata.exif.ExifSubIFDDirectory
import com.drew.metadata.exif.GpsDirectory
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import dev.su386.calina.Calina
import dev.su386.calina.utils.HashingInputStream
import dev.su386.calina.utils.Location
import kotlinx.coroutines.*
import java.awt.Image
import java.io.File
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.file.Files
import java.nio.file.attribute.BasicFileAttributes
import java.security.MessageDigest
import java.util.*
import javax.imageio.ImageIO

class ImageData @JsonCreator constructor(
    @JsonProperty("location")
    val location: Location,
    @JsonProperty("date")
    val date: Long,
    @JsonProperty("hash")
    val hash: String,
    @JsonProperty("cameraInfo")
    val cameraInfo: CameraInfo,
    @JsonProperty("filePaths")
    vararg var filePaths: String
) {
    /**
     * Returns an array of all valid images associated with this image data.
     */
    @get:JsonIgnore
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

    @get:JsonIgnore
    val dateTime: Date get() = Date(date)

    @JsonIgnore
    val tags: MutableSet<UUID> = mutableSetOf()

    /**
     * Adds a new tag to this image
     *
     * @param tag - tag to add
     */
    fun addTag(tag: Tag) {
        tags.add(tag.uuid)
        tag.imageHashes.add(this.hash)
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

    data class CameraInfo @JsonCreator constructor(
        @JsonProperty("name") val name: String
    )
}
