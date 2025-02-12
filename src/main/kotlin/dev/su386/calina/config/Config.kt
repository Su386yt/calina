package dev.su386.calina.config

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
    private val previousPathMap: MutableMap<String, Array<String>> = mutableMapOf()

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

        for (en in previousPathMap.entries) {
            for (previousPath in en.value) {
                val trimmedPath = previousPath.trim('/')
                if ("/" in trimmedPath){
                    val split = trimmedPath.split("/")
                    var newObject = obj
                    for (i in 0..<(split.size - 1)) {
                        if (newObject.has(split[i]) && newObject[split[i]] is ObjectNode) {
                            newObject = newObject[split[i]] as ObjectNode
                        }
                    }

                    this[en.key.trim('/')].loadFromJson(newObject[split.last()])
                } else {
                    if (obj.has(previousPath)) {
                        this[en.key.trim('/')].loadFromJson(obj[previousPath])
                    }
                }
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

    fun register(path: String, option: ConfigOption, vararg previousPaths: String) {
        if(previousPaths.isNotEmpty()) {
            previousPathMap[path] = previousPaths as Array<String>
        }

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
        val trimmedPath = path.trim('/')
        if ("/" in trimmedPath) {
            val subconfig = map[trimmedPath.substringBefore("/")]
            if (subconfig !is Config) {
                throw IllegalArgumentException("Invalid path $trimmedPath. No subconfig found at ${trimmedPath.substringBefore("/")}")
            }
            return subconfig[trimmedPath.substringAfter("/")]
        } else {
            return map[trimmedPath] ?: throw IllegalArgumentException("Invalid path $trimmedPath. No option found at $trimmedPath.")
        }
    }

    fun <T> get(path: String): T {
        return get(path).value()
    }
}