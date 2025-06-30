package dev.su386.calina.images.filters

import dev.su386.calina.images.ImageData
import dev.su386.calina.images.tags.Tag

/**
 * Filters images that have a specific tag
 *
 * @param tag - The tag to filter for
 */
open class TagFilter(
    private val tag: Tag?,
): Filter() {
    override fun isValidImage(image: ImageData): Boolean {
        if (tag == null) {
            return true
        }
        return image.tags.contains(tag.uuid)
    }
}