package dev.su386.calina.config

import com.fasterxml.jackson.databind.JsonNode

/**
 * @param name - The display name for the config option
 * @param description - The description for the config option
 */
abstract class ConfigOption(
    var name: String,
    var description: String
) {
    /**
     * The state of the config option
     *
     * The state can be of type any, but should be overloaded to a specific type
     */
    abstract val value: Any

    /**
     * @return [ConfigOption.value] as a specified type [T]
     * @param T - The type to case [ConfigOption.value]
     */
    fun <T> value(): T {
        return value as T
    }

    /**
     * @param jsonNode
     */
    abstract fun loadFromJson(jsonNode: JsonNode)


    /**
     * @return
     */
    abstract fun saveToJson(): JsonNode
}
