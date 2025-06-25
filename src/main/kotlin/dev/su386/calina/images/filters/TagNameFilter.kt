package dev.su386.calina.images.filters

import dev.su386.calina.images.ImageData
import dev.su386.calina.images.tags.Tag

/**
 * Filters images that have the tag with a specific name.
 *
 * @param keyword - Keyword to look for
 */
class TagNameFilter(
    private val keyword: String,
): Filter() {
    override fun isValidImage(image: ImageData): Boolean {
        return Tag.tags.values.any { it.name.lowercase().contains(keyword.lowercase()) && image.tags.contains(it.uuid) }
    }
}