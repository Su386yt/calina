package dev.su386.calina.images.filters

import dev.su386.calina.images.ImageData
import java.text.SimpleDateFormat
import java.util.*


/**
 * Filters for images that were taken in a certain year.
 * Uses [Locale.getDefault] as well as English UK and US.
 *
 * @param keyword - Keyword to look for in year
 */
class YearFilter(
    private val keyword: String,
): Filter() {
    override fun isValidImage(image: ImageData): Boolean {
        val date = image.dateTime
        val localeFormat = SimpleDateFormat("YY", Locale.getDefault())
        val enFormat = SimpleDateFormat("YY", Locale.ENGLISH)
        val enUkFormat = SimpleDateFormat("YY", Locale.Builder().setLanguage("en").setRegion("GB").build())
        val strippedKeyword = if (keyword.startsWith("20") && keyword.length != 2) { keyword.substring(2) } else { keyword }.trimStart('0')

        return keyword.lowercase() in localeFormat.format(date).lowercase() ||
                keyword.lowercase() in enFormat.format(date).lowercase() ||
                keyword.lowercase() in enUkFormat.format(date).lowercase() ||
                strippedKeyword.lowercase() in localeFormat.format(date).lowercase() ||
                strippedKeyword.lowercase() in enFormat.format(date).lowercase() ||
                strippedKeyword.lowercase() in enUkFormat.format(date).lowercase()
    }
}