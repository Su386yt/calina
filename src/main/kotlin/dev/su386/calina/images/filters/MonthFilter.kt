package dev.su386.calina.images.filters

import dev.su386.calina.images.ImageData
import java.text.SimpleDateFormat
import java.util.*


private val monthNameMap = mutableMapOf<Int, Set<String>>().apply {
    val localeFormat = SimpleDateFormat("MMMM MM", Locale.getDefault())
    val enFormat = SimpleDateFormat("MMMM MM", Locale.ENGLISH)
    val enUkFormat = SimpleDateFormat("MMMM MM", Locale.Builder().setLanguage("en").setRegion("GB").build())
    val calendar = Calendar.getInstance(Locale.getDefault())

    calendar.set(Calendar.MONTH, 0)
    for (i in 0 until 12) {
        val set = mutableSetOf<String>()
        set.add(localeFormat.format(calendar.time).lowercase())
        set.add(enFormat.format(calendar.time).lowercase())
        set.add(enUkFormat.format(calendar.time).lowercase())
        this[i] = set
        calendar.add(Calendar.MONTH, 1)
    }

}


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
        return monthNameMap[image.calendar.get(Calendar.MONTH)]?.any { keyword.lowercase() in it } == true
    }
}