package dev.su386.calina.utils

import java.awt.Graphics2D
import java.awt.geom.AffineTransform
import java.awt.image.BufferedImage
import java.io.File
import kotlin.math.PI

object FileUtils {
    /**
     * Deletes the file without causing an error
     *
     * @return true if a file exists
     */
    fun File.safelyDelete(): Boolean = this.exists().also { this.deleteOnExit(); this.setWritable(true).also{ if (it) this.delete() } }

    /**
     * Applies a composite transform that first flips the image (if needed) and then rotates
     * it by a multiple of 90° (0, 90, 180, or 270) while adjusting for new dimensions.
     */
    fun BufferedImage.applyCompositeTransform(
        flipHorizontally: Boolean,
        flipVertically: Boolean,
        rotateDegrees: Int
    ): BufferedImage {
        // Normalize the rotation angle to [0, 360)
        val effective = ((rotateDegrees % 360) + 360) % 360

        // Build the flip transformation.
        // (If no flipping is needed, this remains the identity transform.)
        val flipTransform = AffineTransform()
        if (flipHorizontally) {
            // Mirror horizontally: (x, y) → (-x + width, y)
            flipTransform.concatenate(
                AffineTransform(-1.0, 0.0, 0.0, 1.0, this.width.toDouble(), 0.0)
            )
        }
        if (flipVertically) {
            // Mirror vertically: (x, y) → (x, -y + height)
            flipTransform.concatenate(
                AffineTransform(1.0, 0.0, 0.0, -1.0, 0.0, this.height.toDouble())
            )
        }

        // Build the rotation transformation.
        // Use the same translation values as in your original rotate method.
        val rotationTransform: AffineTransform = when (effective) {
            0 -> AffineTransform.getTranslateInstance(0.0, 0.0)
            90 ->
                // Clockwise 90°: (x, y) → (y, width - x)
                AffineTransform(0.0, -1.0, 1.0, 0.0, 0.0, this.width.toDouble())
            180 ->
                // 180°: (x, y) → (width - x, height - y)
                AffineTransform(-1.0, 0.0, 0.0, -1.0, this.width.toDouble(), this.height.toDouble())
            270 ->
                // For 270° (which is the same as -90° with our convention)
                // Expected mapping (as in your original rotate):
                // (x, y) → (height - y, x)
                AffineTransform(0.0, 1.0, -1.0, 0.0, this.height.toDouble(), 0.0)
            else -> throw IllegalArgumentException("Angle must be a multiple of 90")
        }

        // Combine the transforms: apply flips first then the rotation.
        // (Note: Concatenation order matters—the matrix on the right is applied first.)
        val compositeTransform = AffineTransform()
        compositeTransform.concatenate(rotationTransform)
        compositeTransform.concatenate(flipTransform)

        // Determine new dimensions after rotation.
        val (newWidth, newHeight) = when (effective) {
            90, 270 -> Pair(this.height, this.width)
            0, 180 -> Pair(this.width, this.height)
            else -> throw IllegalStateException("Unhandled rotation angle")
        }

        // Create the destination image with the corrected dimensions.
        val dest = BufferedImage(newWidth, newHeight, this.type)
        val g2d: Graphics2D = dest.createGraphics()
        g2d.drawImage(this, compositeTransform, null)
        g2d.dispose()
        return dest
    }
}