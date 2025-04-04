package dev.su386.calina.config

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import dev.su386.calina.utils.AutoResizeText

class Paragraph(
    name: String,
    description: String,
    defaultValue: String = ""
) : ConfigOption(
    name,
    description,
    size = 2f
) {
    override var value: String = defaultValue

    @Composable
    override fun getComposable() {
        var state by remember { mutableStateOf(this.value) }
        var isError by remember { mutableStateOf(false) }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .fillMaxSize(),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize(),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.Center,
                ) {
                    AutoResizeText(
                        text = "$name:",
                        modifier = Modifier
                            .padding(12.dp)
                            .fillMaxHeight(.5f),
                        color = MaterialTheme.colors.onSurface,
                        align = Alignment.CenterStart,
                    )
                }

                TextField(
                    value = state,  // Use state directly here
                    onValueChange = { newText ->
                        try {
                            state = newText  // Update state directly
                            isError = false
                        } catch (e: Exception) {
                            isError = true
                        }
                    },
                    maxLines = 2,
                    isError = isError,
                    textStyle = TextStyle(color = MaterialTheme.colors.onSurface),
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }



    override fun loadFromJson(jsonNode: JsonNode) {

        this.value = jsonNode.asText("")

    }

    override fun saveToJson(): JsonNode {
        return JsonNodeFactory.instance.textNode(value)
    }
}