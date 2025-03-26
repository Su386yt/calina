package dev.su386.calina.images.filters

import dev.su386.calina.images.ImageData
import java.text.SimpleDateFormat
import java.util.*

private val daysOfWeekLocale = mutableMapOf<Int, Set<String>>().apply {
    val localeFormat = SimpleDateFormat("EEEE", Locale.getDefault())
    val enFormat = SimpleDateFormat("EEEE", Locale.ENGLISH)
    val enUkFormat = SimpleDateFormat("EEEE", Locale.Builder().setLanguage("en").setRegion("GB").build())
    val calendar = Calendar.getInstance()

    calendar.set(Calendar.DAY_OF_WEEK, 1)
    for (i in 1 until 8) {
        val set = mutableSetOf<String>()
        set.add(localeFormat.format(calendar.time).lowercase())
        set.add(enFormat.format(calendar.time).lowercase())
        set.add(enUkFormat.format(calendar.time).lowercase())
        this[i] = set
        calendar.add(Calendar.DAY_OF_WEEK, 1)
    }
}

/**
 * Filters for images that were taken on a day of week
 * Uses [Locale.getDefault] as well as English UK and US
 *
 * @param keyword - Keyword to look for in the day
 */
class DayOfWeekFilter(
    private val keyword: String,
): Filter() {
    override fun isValidImage(image: ImageData): Boolean {
        return daysOfWeekLocale[image.calendar.get(Calendar.DAY_OF_WEEK)]?.any { keyword.lowercase() in it } == true
    }
}