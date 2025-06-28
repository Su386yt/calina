package dev.su386.calina.images.filters

import dev.su386.calina.images.ImageData
/**
 * Creates a junction of filters
 *
 * @param filters - Filters to add in junction
 * @param junctionType - Whether to treat this junction as a Conjunction ([JunctionType.CONJUNCTION]) or a Disjunction ([JunctionType.DISJUNCTION])
 */
class FilterJunction(
    private val junctionType: JunctionType, private vararg val filters: Filter

): Filter() {
    constructor(junctionType: JunctionType, filters: List<Filter>): this(junctionType, *filters.toTypedArray())

    override fun isValidImage(image: ImageData): Boolean {
        if (filters.isEmpty()) {
            return true
        }
        return if (junctionType == JunctionType.CONJUNCTION) {
            !filters.any { filter -> !filter.isValidImage(image) }
        } else {
            filters.any { filter -> filter.isValidImage(image) }
        }
    }

    companion object {
        fun List<Filter>.toConjunction(): FilterJunction = FilterJunction(JunctionType.CONJUNCTION, this)
        fun List<Filter>.toDisJunction(): FilterJunction = FilterJunction(JunctionType.DISJUNCTION, this)

    }
}

enum class JunctionType {
    DISJUNCTION,
    CONJUNCTION
}