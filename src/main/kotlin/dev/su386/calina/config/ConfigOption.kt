package dev.su386.calina.config

import androidx.compose.runtime.Composable
import com.google.gson.JsonElement

/**
 * @param name - The display name for the config option
 * @param description - The description for the config option
 */
abstract class ConfigOption(
    var name: String,
    var description: String,
    val size: Float = 1f
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

    @Composable
    abstract fun getComposable()

    /**
     * @param jsonNode
     */
    abstract fun loadFromJson(jsonNode: JsonElement)


    /**
     * @return
     */
    abstract fun saveToJson(): JsonElement
}
