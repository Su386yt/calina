package dev.su386.calina.config

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import dev.su386.calina.utils.AutoResizeText

class Toggle(
    name: String,
    description: String,
    private val defaultState: Boolean,
): ConfigOption(name, description) {
    override var value: Boolean = defaultState

    @Composable
    override fun getComposable() {
        AutoResizeText(
            text = "Toggle",
            modifier = Modifier.fillMaxSize(),
            align = Alignment.CenterStart
        )
    }


    override fun loadFromJson(jsonNode: JsonNode) {
        value = jsonNode["state"]?.asBoolean() ?: defaultState
    }

    override fun saveToJson(): JsonNode {
        return JsonNodeFactory.instance.objectNode().put("state", value)
    }

}