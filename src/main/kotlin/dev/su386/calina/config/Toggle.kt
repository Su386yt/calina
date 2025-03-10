package dev.su386.calina.config

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import dev.su386.calina.utils.AutoResizeText

class Toggle(
    name: String,
    description: String,
    defaultState: Boolean,
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

    override fun loadFromJson(jsonNode: JsonElement) {
        value = jsonNode.asBoolean
    }

    override fun saveToJson(): JsonElement {
        return JsonPrimitive(value)
    }

}