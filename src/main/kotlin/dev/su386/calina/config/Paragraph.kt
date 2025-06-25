package dev.su386.calina.config

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
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
                        color = MaterialTheme.colorScheme.onSurface,
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
                    textStyle = TextStyle(color = MaterialTheme.colorScheme.onSurface),
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }



    override fun loadFromJson(jsonNode: JsonElement) {
        this.value = jsonNode.asString
    }

    override fun saveToJson(): JsonElement {
        return JsonPrimitive(value)
    }
}