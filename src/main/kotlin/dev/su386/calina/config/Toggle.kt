package dev.su386.calina.config

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.JsonNodeFactory

class Toggle(
    name: String,
    description: String,
    private val defaultState: Boolean,
): ConfigOption(name, description) {
    override var value: Boolean = defaultState

    override fun loadFromJson(jsonNode: JsonNode) {
        value = jsonNode["state"]?.asBoolean() ?: defaultState
    }

    override fun saveToJson(): JsonNode {
        return JsonNodeFactory.instance.objectNode().put("state", value)
    }

}