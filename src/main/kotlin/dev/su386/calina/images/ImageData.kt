package dev.su386.calina.images

import com.drew.imaging.ImageMetadataReader
import com.drew.metadata.exif.ExifImageDirectory
import com.drew.metadata.exif.ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL
import com.drew.metadata.exif.GpsDirectory
import dev.su386.calina.utils.Location
import java.awt.Image
import java.io.File
import java.nio.file.Files
import java.nio.file.attribute.BasicFileAttributes
import java.security.MessageDigest
import java.util.*
import javax.imageio.ImageIO

class ImageData(
    val location: Location,
    val date: Long,
    val hash: String,
    val cameraInfo: CameraInfo,
    val cachedPath: String
) {
    /**
     * Attempts to load the image at cachedPath.
     * If the hash of image matches hash, returns the image
     * Otherwise, null
     */
    val image: Image? get() {
        val file = File(cachedPath)
        val bytes = file.readBytes()

        return if (file.exists() && bytes.hashSHA() == hash) {
            try {
                ImageIO.read(bytes.inputStream())
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        } else {
            null
        }
    }

    val id: String get() = "$hash+$cachedPath"
    val dateTime: Date get() = Date(date)
    @Transient
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
            val metadata = ImageMetadataReader.readMetadata(this)

            val gpsDirectory = metadata.getFirstDirectoryOfType(GpsDirectory::class.java)
            val location = gpsDirectory?.geoLocation?.let {
                Location(it.latitude, it.longitude)
            } ?: Location.EMPTY

            val exifData = metadata.getFirstDirectoryOfType(ExifImageDirectory::class.java)
            val time = exifData
                ?.getDate(TAG_DATETIME_ORIGINAL)
                ?.time
                ?: Files
                    .readAttributes(this.toPath(), BasicFileAttributes::class.java)
                    ?.creationTime()
                    ?.toMillis()
                        ?: System.currentTimeMillis()
            
            return ImageData(
                location,
                time,
                this.readBytes().hashSHA(),
                CameraInfo(""),
                this.path
            )
        }
    }

    data class CameraInfo(
        val name: String
    )
}
