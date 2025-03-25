package dev.su386.calina.images.filters

import dev.su386.calina.images.ImageData
import java.text.SimpleDateFormat
import java.util.*

/**
 * Filters for images that were taken in a certain month.
 * Uses [Locale.getDefault] as well as English UK and US.
 *
 * @param keyword - Keyword to look for in month (checks both integer and name of the month)
 */
class MonthFilter(
    private val keyword: String,
): Filter() {
    override fun isValidImage(image: ImageData): Boolean {
        val date = image.dateTime
        val localeFormat = SimpleDateFormat("MMMM MM", Locale.getDefault())
        val enFormat = SimpleDateFormat("MMMM MM", Locale.ENGLISH)
        val enUkFormat = SimpleDateFormat("MMMM MM", Locale.Builder().setLanguage("en").setRegion("GB").build())

        return keyword.lowercase() in localeFormat.format(date).lowercase() ||
            keyword.lowercase() in enFormat.format(date).lowercase() ||
            keyword.lowercase() in enUkFormat.format(date).lowercase()
    }
}