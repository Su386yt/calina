package dev.su386.calina.images.filters

import dev.su386.calina.images.ImageData
import java.text.SimpleDateFormat
import java.util.*

/**
 * Filters for images that were taken in a certain day or day of week
 * Uses [Locale.getDefault] as well as English UK and US
 *
 * @param keyword - Keyword to look for in the day
 */
class DayFilter(
    private val keyword: String,
): Filter() {
    override fun isValidImage(image: ImageData): Boolean {
        val date = image.dateTime
        val localeFormat = SimpleDateFormat("EEEE dd", Locale.getDefault())
        val enFormat = SimpleDateFormat("EEEE dd", Locale.ENGLISH)
        val enUkFormat = SimpleDateFormat("EEEE dd", Locale.Builder().setLanguage("en").setRegion("GB").build())

        return keyword.lowercase() in localeFormat.format(date).lowercase() ||
                keyword.lowercase() in enFormat.format(date).lowercase() ||
                keyword.lowercase() in enUkFormat.format(date).lowercase()
    }
}