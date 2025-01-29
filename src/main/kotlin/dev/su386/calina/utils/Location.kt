package dev.su386.calina.utils

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty

data class Location @JsonCreator constructor(
    @JsonProperty("latitude") val latitude: Double,
    @JsonProperty("longitude") val longitude: Double
) {
    private var emptyLocation = false

    companion object {
        val EMPTY = Location(0.0, 0.0).apply { emptyLocation = true }
    }

    @get:JsonIgnore
    val isEmpty: Boolean get() = emptyLocation
}
