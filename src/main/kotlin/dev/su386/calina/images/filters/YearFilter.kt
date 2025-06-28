package dev.su386.calina.images.filters

import dev.su386.calina.images.ImageData
import java.util.*

/**
 * Filters for images that were taken in a certain year.
 *
 * @param keyword - Keyword to look for in year
 */
class YearFilter(
    private val keyword: String,
): Filter() {
    override fun isValidImage(image: ImageData): Boolean {
        val int = keyword.trim().toIntOrNull() ?: return false
        return image.calendar.get(Calendar.YEAR) == int || image.calendar.get(Calendar.YEAR) % 100 == int
    }
}