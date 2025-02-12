package dev.su386.calina.config

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import com.fasterxml.jackson.databind.node.ObjectNode

open class Config(
    name: String,
    description: String
): ConfigOption(
    name,
    description,
) {

    final override val value: Config = this

    override fun loadFromJson(jsonNode: JsonNode) {
        val obj = if (jsonNode.isObject) {
            jsonNode as ObjectNode
        } else {
            throw IllegalStateException("Json Node is not a config. $jsonNode")
        }

        for (key in obj.fieldNames()) {
            if (obj.has(key)) {
                map[key]?.loadFromJson(obj[key]) ?: continue
            }
        }
    }

    override fun saveToJson(): JsonNode {
        val obj = JsonNodeFactory.instance.objectNode()

        for (en in map.entries) {
            obj.set<JsonNode>(en.key, en.value.saveToJson())
        }

        return obj
    }

    private val map: MutableMap<String, ConfigOption> = mutableMapOf()

    fun register(path: String, option: ConfigOption) {
        val trimmedPath = path.trim('/')
        if ("/" in trimmedPath) {
            var subconfig = map[trimmedPath.substringBefore("/")]
            if (subconfig == null) {
                subconfig = Config("", "")
                map[trimmedPath.substringBefore("/")] = subconfig
            }

            if (subconfig !is Config){
                throw IllegalArgumentException("Invalid path $path. Item already located at ${path.substringBefore("/")}")
            }
            subconfig.register(trimmedPath.substringAfter("/"), option)
        } else {
            map[trimmedPath] = option
        }
    }

    operator fun set(path: String, option: ConfigOption) {
        register(path, option)
    }

    operator fun get(path: String): ConfigOption {
        if ("/" in path) {
            val subconfig = map[path.substringBefore("/")]
            if (subconfig !is Config) {
                throw IllegalArgumentException("Invalid path $path. No subconfig found at ${path.substringBefore("/")}")
            }
            return subconfig[path.substringAfter("/")]
        } else {
            return map[path] ?: throw IllegalArgumentException("Invalid path $path. No option found at $path.")
        }
    }

    fun <T> get(path: String): T {
        return get(path).value()
    }
}