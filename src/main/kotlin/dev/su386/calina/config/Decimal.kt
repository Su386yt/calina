package dev.su386.calina.config

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
import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import dev.su386.calina.utils.AutoResizeText

class Decimal(
    name: String,
    description: String,
    defaultValue: Double = 0.0
) : ConfigOption(
    name,
    description,
    size = 1.5f
) {
    override var value: Double = defaultValue

    @Composable
    override fun getComposable() {
        var text by remember{ mutableStateOf("$value") }
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
                            .padding(4.dp)
                            .fillMaxHeight(.5f),
                        color = MaterialTheme.colors.onSurface,
                        align = Alignment.CenterStart,
                    )
                }

                TextField(
                    value = text,  // Use state directly here
                    onValueChange = { newText ->
                        try {
                            text = newText
                            this@Decimal.value = newText.toDoubleOrNull() ?: this@Decimal.value
                            isError = newText.toDoubleOrNull() == null
                        } catch (e: Exception) {
                            isError = true
                            e.printStackTrace()
                        }
                    },
                    maxLines = 1,
                    isError = isError,
                    textStyle = TextStyle(color = MaterialTheme.colors.onSurface),
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }



    override fun loadFromJson(jsonNode: JsonElement) {
        this.value = jsonNode.asDouble
    }

    override fun saveToJson(): JsonElement {
        return JsonPrimitive(value)
    }
}