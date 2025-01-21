package dev.su386.calina.images

import com.drew.imaging.ImageMetadataReader
import com.drew.metadata.exif.ExifImageDirectory
import com.drew.metadata.exif.ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL
import com.drew.metadata.exif.GpsDirectory
import com.google.gson.annotations.Expose
import dev.su386.calina.Calina
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

class ImageData(
    @Expose
    val location: Location,
    @Expose
    val date: Long,
    @Expose
    val hash: String,
    @Expose
    val cameraInfo: CameraInfo,
    vararg paths: String
) {
    var filePaths = paths as Array<String>

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

    @Expose(serialize = false, deserialize = false)
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
        suspend fun File.toImageData(): ImageData {
            // Open the InputStream and read metadata in a background IO context
            val inputStream = this.inputStream()

            val metadata = withContext(Dispatchers.IO) {
                try {
                    ImageMetadataReader.readMetadata(inputStream)
                } catch (e: Exception) {
                    null
                }
            }

            // Extract GPS and EXIF data (using the metadata)
            val gpsDirectory = metadata?.getFirstDirectoryOfType(GpsDirectory::class.java)
            val location = gpsDirectory?.geoLocation?.let {
                Location(it.latitude, it.longitude)
            } ?: Location.EMPTY

            val exifData = metadata?.getFirstDirectoryOfType(ExifImageDirectory::class.java)
            val time = withContext(Dispatchers.IO) {
                exifData?.getDate(TAG_DATETIME_ORIGINAL)
                    ?.time
                    ?: Files
                        .readAttributes(this@toImageData.toPath(), BasicFileAttributes::class.java)
                        ?.creationTime()
                        ?.toMillis()
                            ?: System.currentTimeMillis()
            }


            // Update bytesLoaded in a thread-safe manner
            Calina.bytesLoaded.addAndGet(this.length())

            // Call the parallelSHA256 suspending function to compute the hash (ensure it's called within a coroutine)
            val hash = withContext(Dispatchers.IO) {
                inputStream.use { stream ->
                    stream.parallelSHA256()
                }
            }

            // Return the ImageData object with all the necessary information
            return ImageData(
                location,
                time,
                hash,
                CameraInfo(""),  // Placeholder for CameraInfo, you might want to extract this from metadata
                this.path
            )
        }
    }

    data class CameraInfo(
        val name: String
    )
}
