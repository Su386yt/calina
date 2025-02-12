package dev.su386.calina.config

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.JsonNode

abstract class ConfigOption(
    var name: String,
    var description: String
) {
    abstract val value: Any

    fun <T> value(): T {
        return value as T
    }

    abstract fun loadFromJson(jsonNode: JsonNode)

    abstract fun saveToJson(): JsonNode
}
