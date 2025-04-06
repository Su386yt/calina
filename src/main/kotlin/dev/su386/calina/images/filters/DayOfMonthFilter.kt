package dev.su386.calina.images.filters

import dev.su386.calina.images.ImageData
import java.util.*

/**
 * Filters for images that were taken on a day of the month
 *
 * @param keyword - Keyword to look for in the day
 */
class DayOfMonthFilter(
    private val keyword: String,
): Filter() {
    override fun isValidImage(image: ImageData): Boolean {
        return image.calendar.get(Calendar.DAY_OF_MONTH) == keyword.toIntOrNull()
    }
}