package dev.su386.calina.images

import com.drew.imaging.ImageMetadataReader
import com.drew.metadata.exif.ExifIFD0Directory
import com.drew.metadata.exif.ExifSubIFDDirectory
import com.drew.metadata.exif.GpsDirectory
import dev.su386.calina.Calina
import dev.su386.calina.CalinaConfig
import dev.su386.calina.data.Database
import dev.su386.calina.images.ImageData.CameraInfo.Companion.flashFromIfdValue
import dev.su386.calina.images.ImageData.ImageSize.Orientation.Companion.fromIfd0Value
import dev.su386.calina.images.tags.Tag
import dev.su386.calina.utils.FileUtils.applyCompositeTransform
import dev.su386.calina.utils.HashingInputStream
import dev.su386.calina.utils.Location
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.withContext
import org.apache.commons.imaging.Imaging
import java.awt.image.BufferedImage
import java.io.File
import java.io.FileInputStream
import java.nio.file.Files
import java.nio.file.attribute.BasicFileAttributes
import java.security.MessageDigest
import java.util.*
import javax.imageio.ImageIO
import javax.imageio.ImageReadParam

private const val COMPRESSED_IMAGE_SIZE = 240

class ImageData(
    val location: Location,
    val date: Long,
    val offset: Int?,
    val hash: String,
    val byteSize: Long,
    val cameraInfo: CameraInfo,
    private var cachedImageSize: ImageSize?,
    vararg var filePaths: String
) {
    private var imageIconPath: String? = null
    var timeSinceLastHashCheck: Long = 0L
    val imageSize: ImageSize get() {
        val cache = cachedImageSize
        if (cache != null) {
            return cache
        } else {
            var imageSize: ImageSize? = null
            try {
                val im = Imaging.getImageSize(File(this.filePaths.firstOrNull()))
                imageSize = ImageSize(im.width, im.height, null)
            } catch (_: Exception) {

            }

            return imageSize.also { cachedImageSize = it } ?: ImageSize(32, 32, null)
        }
    }
    /**
     * @return the BufferedImage associated with this image data.
     */
    val image: BufferedImage
        get() {
            try {
                for (filePath in filePaths) {
                    val file = File(filePath)
                    val digest = MessageDigest.getInstance("SHA-256")
                    // Use the 'use' construct for automatic closing.
                    val imageBytes = FileInputStream(file).use { fis ->
                        HashingInputStream(fis, digest).use { hashingStream ->
                            hashingStream.readAllBytes()
                        }
                    }
                    val calcedHash = digest.digest().joinToString("") { "%02x".format(it) }
                    if (calcedHash != hash) continue

                    // Load the image and apply a composite transform.
                    var loadedImage = ImageIO.read(imageBytes.inputStream())
                    loadedImage = loadedImage.applyCompositeTransform(
                        flipHorizontally = this.imageSize.orientation?.flipHorizontally == true,
                        flipVertically = this.imageSize.orientation?.flipVertically == true,
                        rotateDegrees = this.imageSize.orientation?.rotateAngle ?: 0
                    )
                    return loadedImage
                }
            } catch (e: Exception) {
                println("Error loading image for ${filePaths.firstOrNull()}: ${e.message}")
            }
            println("No image found")
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
                    image = reader.read(0, param).applyCompositeTransform(
                        flipHorizontally = this.imageSize.orientation?.flipHorizontally == true,
                        flipVertically = this.imageSize.orientation?.flipVertically == true,
                        rotateDegrees = this.imageSize.orientation?.rotateAngle ?: 0
                    )
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


    val tags: Set<UUID> get() = Tag.tags.filter { it.value.imageHashes.contains(hash) }.keys.toSet()

    /**
     * Adds a new tag to this image
     *
     * @param tag - tag to add
     */
    fun addTag(tag: Tag) {
        tag.imageHashes.add(this.hash)
    }

    /**
     * Remove a tag from this image
     *
     * @param tag - tag to remove
     */
    fun removeTag(tag: Tag) {
        tag.imageHashes.remove(this.hash)
    }


    /**
     * Checks whether the file in [filePaths] exists within the allowed paths, and removes them if they do not
     *
     * @return The number of file paths removed
     */
    fun cleanFilePaths(): Int {
        val oldLength = filePaths.size
        this.filePaths = this.filePaths.filter { path ->
            File(path).exists() && CalinaConfig.get<List<String>>("gallery/folderPaths").any {
                folder ->  File(path).parentFile.absolutePath == File(folder).absolutePath
            }
        }.toTypedArray()
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
    fun checkFileHashes(): Int {
        val validPaths = mutableSetOf<String>()
        this.filePaths
            .forEach { path ->
                if (File(path).lastModified() < this.timeSinceLastHashCheck) {
                    validPaths.add(path)
                    return@forEach
                }

                val digest = MessageDigest.getInstance("SHA-256")
                val inputStream = HashingInputStream(FileInputStream(path), digest)
                inputStream.readAllBytes()
                inputStream.close()
                val hash = digest.digest().joinToString("") { "%02x".format(it) }
                if (hash == this.hash) {
                    validPaths.add(path)
                }

                this.timeSinceLastHashCheck = System.currentTimeMillis()
            }

        return (this.filePaths.size - validPaths.size).also{ this.filePaths = validPaths.toTypedArray() }
    }

    override fun toString(): String {
        return "ImageData(location=$location, date=$date, hash='$hash', cameraInfo=$cameraInfo, filePaths=${filePaths.contentToString()}, tags=$tags)"
    }

    companion object {
        /**
         * @return the image data at that path. If no metadata exists in an image, it returns a metadata with default values.
         *
         * Make sure to use Dispatchers.IO
         */
        suspend fun File.toImageData(): ImageData = withContext(IO) {
            val messageDigest = MessageDigest.getInstance("SHA-256")

            val hashingInputStream = HashingInputStream(this@toImageData.inputStream().buffered(), messageDigest)
            var imageSize: ImageSize? = null


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
                val byteSize = this@toImageData.length()
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
                val offset = exifDirectory?.dateOriginal?.timezoneOffset

                val ifd0Directory = metadata?.getFirstDirectoryOfType(ExifIFD0Directory::class.java)
                val cameraMake = ifd0Directory?.getString(ExifIFD0Directory.TAG_MAKE)
                val cameraModel = ifd0Directory?.getString(ExifIFD0Directory.TAG_MODEL)
                val orientation = fromIfd0Value(ifd0Directory?.getString(ExifIFD0Directory.TAG_ORIENTATION))

                try {
                    val im = Imaging.getImageSize(this@toImageData)
                    imageSize = if (orientation.rotateAngle % 180 == 0) {
                        ImageSize(im.width, im.height, orientation)
                    } else {
                        ImageSize(im.height, im.width, orientation)
                    }
                } catch (_: Exception) {

                }

                val ifdDirectory = metadata?.getFirstDirectoryOfType(ExifSubIFDDirectory::class.java)
                val iso = ifdDirectory?.getString(ExifSubIFDDirectory.TAG_ISO_SPEED)
                val flash = flashFromIfdValue(ifdDirectory?.getString(ExifSubIFDDirectory.TAG_FLASH))
                val focalLength = ifdDirectory?.getString(ExifSubIFDDirectory.TAG_FOCAL_LENGTH)
                val exposureTime = ifdDirectory?.getString(ExifSubIFDDirectory.TAG_EXPOSURE_TIME)
                val fNumber = ifdDirectory?.getString(ExifSubIFDDirectory.TAG_FNUMBER)
                val apertureValue = ifdDirectory?.getString(ExifSubIFDDirectory.TAG_APERTURE)
                val exposureCompensation = ifdDirectory?.getString(ExifSubIFDDirectory.TAG_EXPOSURE_INDEX)
                val colorSpace = ifdDirectory?.getString(ExifSubIFDDirectory.TAG_COLOR_SPACE)


                // Update bytesLoaded atomically
                Calina.bytesLoaded.addAndGet(this@toImageData.length())

                // Return the ImageData object with all the necessary information
                ImageData(
                    location = location,
                    date = time,
                    offset = offset,
                    hash = hash,
                    byteSize = byteSize,
                    cameraInfo = CameraInfo(
                        cameraMake?.let { make  -> cameraModel?.let { "$make $it" } },
                        iso,
                        flash,
                        focalLength,
                        exposureTime,
                        fNumber,
                        apertureValue,
                        exposureCompensation,
                        colorSpace,
                    ),
                    imageSize,
                    this@toImageData.path,
                ).apply { this.timeSinceLastHashCheck = System.currentTimeMillis() }
                    .also { input.close() }
            }
        }
    }

    data class CameraInfo(
        val name: String?,
        val iso: String?,
        val flash: String?,
        val focalLength: String?,
        val exposureTime: String?,
        val fNumber: String?,
        val apertureValue: String?,
        val exposureCompensation: String?,
        val colorSpace: String?,
    ) {
        companion object {
            fun flashFromIfdValue(ifdValue: String?): String =
//                0x0	= No Flash
                //0x1	= Fired
                //0x5	= Fired, Return not detected
                //0x7	= Fired, Return detected
                //0x8	= On, Did not fire
                //0x9	= On, Fired
                //0xd	= On, Return not detected
                //0xf	= On, Return detected
                //0x10	= Off, Did not fire
                //0x14	= Off, Did not fire, Return not detected
                //0x18	= Auto, Did not fire
                //0x19	= Auto, Fired
                //0x1d	= Auto, Fired, Return not detected
                //0x1f	= Auto, Fired, Return detected
                //0x20	= No flash function
                //0x30	= Off, No flash function
                //0x41	= Fired, Red-eye reduction
                //0x45	= Fired, Red-eye reduction, Return not detected
                //0x47	= Fired, Red-eye reduction, Return detected
                //0x49	= On, Red-eye reduction
                //0x4d	= On, Red-eye reduction, Return not detected
                //0x4f	= On, Red-eye reduction, Return detected
                //0x50	= Off, Red-eye reduction
                //0x58	= Auto, Did not fire, Red-eye reduction
                //0x59	= Auto, Fired, Red-eye reduction
                //0x5d	= Auto, Fired, Red-eye reduction, Return not detected
                //0x5f	= Auto, Fired, Red-eye reduction, Return
                when (ifdValue) {
                    0x0.toString() -> "No Flash"
                    0x1.toString() -> "Fired"
                    0x5.toString() -> "Flash Fired, Return not detected"
                    0x7.toString() -> "Flash Fired, Return detected"
                    0x8.toString() -> "Flash On, Did not fire"
                    0x9.toString() -> "Flash On, Fired"
                    0xd.toString() -> "Flash On, Return not detected"
                    0xf.toString() -> "Flash On, Return detected"
                    0x10.toString() -> "Flash Off, Did not fire"
                    0x14.toString() -> "Flash Off, Did not fire, Return not detected"
                    0x18.toString() -> "Flash Auto, Did not fire"
                    0x19.toString() -> "Flash Auto, Fired"
                    0x1d.toString() -> "Flash Auto, Fired, Return not detected"
                    0x1f.toString() -> "Flash Auto, Fired, Return detected"
                    0x20.toString() -> "No flash function"
                    0x30.toString() -> "Off, No flash function"
                    0x41.toString() -> "Flash Fired, Red-eye reduction"
                    0x45.toString() -> "Flash Fired, Red-eye reduction, Return not detected"
                    0x47.toString() -> "Flash Fired, Red-eye reduction, Return detected"
                    0x49.toString() -> "Flash On, Red-eye reduction"
                    0x4d.toString() -> "Flash On, Red-eye reduction, Return not detected"
                    0x4f.toString() -> "Flash On, Red-eye reduction, Return detected"
                    0x50.toString() -> "Flash Off, Red-eye reduction"
                    0x58.toString() -> "Flash Auto, Did not fire, Red-eye reduction"
                    0x59.toString() -> "Flash Auto, Fired, Red-eye reduction"
                    0x5d.toString() -> "Flash Auto, Fired, Red-eye reduction, Return not detected"
                    0x5f.toString() -> "Flash Auto, Fired, Red-eye reduction, Return detected"
                    else -> ""
                }

        }
    }

    data class ImageSize(val x: Int, val y: Int, val orientation: Orientation?) {
        val ratio get() = x.toFloat() / y
        val compressedImageSize: ImageSize get() = if (x < y) {
                ImageSize(COMPRESSED_IMAGE_SIZE, (COMPRESSED_IMAGE_SIZE / ratio).toInt(), orientation)
            } else {
                ImageSize((COMPRESSED_IMAGE_SIZE * ratio).toInt(), COMPRESSED_IMAGE_SIZE, orientation)
            }
        enum class Orientation(val rotateAngle: Int, val flipVertically: Boolean, val flipHorizontally: Boolean, vararg val idf0Value: String?) {
            NORMAL(0, false, false, "1", null),
            MIRROR_HORIZONTAL(0, false, true,"2"),
            ROTATE_180(180, false, false, "3"),
            MIRROR_VERTICAL(0, true, false, "4"),
            MIRROR_HORIZONTAL_ROTATE_270_CW(-270, false, true, "5"),
            ROTATE_90_CW(-90, false, false,"6"),
            MIRROR_HORIZONTAL_ROTATE_90_CW(-90, false, true, "7"),
            ROTATE_270_CW(-270, false, false, "8");

            companion object {
                fun fromIfd0Value(ifd0Value: String?): Orientation {
                    return Orientation.entries.firstOrNull { it.idf0Value.contains(ifd0Value) } ?: NORMAL
                }
            }
        }
    }

}

