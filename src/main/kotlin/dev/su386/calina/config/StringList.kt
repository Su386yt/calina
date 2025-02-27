package dev.su386.calina.config

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.JsonNodeFactory

class StringList(
    name: String,
    description: String,
    defaultValue: MutableList<String> = mutableListOf(),
    vararg defaultValues: String
) : ConfigOption(
    name,
    description
) {
    override val value: MutableList<String> = defaultValue

    init {
        for (value in defaultValues) {
            this.value.add(value)
        }
    }


    override fun loadFromJson(jsonNode: JsonNode) {
        val array = jsonNode as ArrayNode
        for (item in array) {
            value.add(item.asText().toString())
        }
    }

    override fun saveToJson(): JsonNode {
        val obj = JsonNodeFactory.instance.arrayNode()

        for (en in value) {
            obj.add(en)
        }

        return obj
    }
}