package dev.su386.calina.images.filters

import dev.su386.calina.images.ImageData
import dev.su386.calina.images.Tag

/**
 * Filters images that have a specific tag
 *
 * @param tag - The tag to filter for
 */
class TagFilter(
    private val tag: Tag,
): Filter() {
    override fun isValidImage(image: ImageData): Boolean {
        return image.tags.contains(tag.uuid)
    }
}