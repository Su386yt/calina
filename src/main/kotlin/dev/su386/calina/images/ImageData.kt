package dev.su386.calina.images

import com.drew.imaging.ImageMetadataReader
import com.drew.metadata.exif.ExifImageDirectory
import com.drew.metadata.exif.ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL
import com.drew.metadata.exif.GpsDirectory
import com.google.gson.annotations.Expose
import dev.su386.calina.Calina
import dev.su386.calina.utils.Location
import java.awt.Image
import java.io.File
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
                if (file.exists() && file.readBytes().hashSHA() == hash) {
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
        fun ByteArray.hashSHA() : String{
            val md = MessageDigest.getInstance("SHA-256")
            val bytes =  md.digest(this)
            return Base64.getUrlEncoder().encodeToString(bytes)
        }

        /**
         * Returns the image data at that path.
         * If no metadata exists in an image, it returns a metadata with default values.
         */
        fun File.toImageData(): ImageData {
            val bytes = this.readBytes()
            val metadata = try {
                ImageMetadataReader.readMetadata(bytes.inputStream())
            } catch (e: Exception) {
                null
            }

            val gpsDirectory = metadata?.getFirstDirectoryOfType(GpsDirectory::class.java)
            val location = gpsDirectory?.geoLocation?.let {
                Location(it.latitude, it.longitude)
            } ?: Location.EMPTY

            val exifData = metadata?.getFirstDirectoryOfType(ExifImageDirectory::class.java)
            val time = exifData
                ?.getDate(TAG_DATETIME_ORIGINAL)
                ?.time
                ?: Files
                    .readAttributes(this.toPath(), BasicFileAttributes::class.java)
                    ?.creationTime()
                    ?.toMillis()
                        ?: System.currentTimeMillis()

            Calina.bytesLoaded.addAndGet(bytes.size.toLong())

            return ImageData(
                location,
                time,
                bytes.hashSHA(),
                CameraInfo(""),
                this.path
            )
        }
    }

    data class CameraInfo(
        val name: String
    )
}
