package dev.su386.calina.utils

data class Location(val latitude: Double, val longitude: Double) {
    private var emptyLocation = false

    companion object {
        val EMPTY = Location(0.0, 0.0).apply { emptyLocation = true }
    }

    val isEmpty: Boolean get() = emptyLocation
}
