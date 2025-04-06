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
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import dev.su386.calina.utils.AutoResizeText

class StringList(
    name: String,
    description: String,
    defaultValue: MutableList<String> = mutableListOf(),
    vararg defaultValues: String
) : ConfigOption(
    name,
    description,
    size = 2f
) {
    override val value: MutableList<String> = defaultValue

    init {
        for (value in defaultValues) {
            this.value.add(value)
        }
    }

    @Preview
    @Composable
    override fun getComposable() {
        val list by remember { mutableStateOf(this.value) }
        var selection by remember { mutableStateOf(TextRange(0)) }
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
                val text = mutableStateOf(StringBuilder().apply {
                    for (s in list) {
                        append("\"$s\", ")
                    }
                }.toString())
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
                    value = TextFieldValue(text = text.value, selection = selection),
                    onValueChange = { it ->
                        val previousSize = value.size
                        text.value = it.text
                        selection = it.selection
                        try {
                            val split = mutableListOf(*it.text.split(", ").toTypedArray())
                            for (i in split.indices) {
                                split[i] = split[i].trim().trim('"', ',')
                            }

                            value.clear()
                            for (i in split.indices) {
                                if (split[i].isEmpty()) {
                                    continue
                                }
                                value.add(split[i])
                            }

                            if (value.size > previousSize) {
                                selection = TextRange(selection.start + 1)
                            }

                            isError = false
                        } catch (e:Exception){
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


    override fun loadFromJson(jsonNode: JsonElement) {
        val array = jsonNode.asJsonArray
        for (item in array) {
            value.add(item.asString)
        }
    }

    override fun saveToJson(): JsonElement {
        val obj = JsonArray()

        for (en in value) {
            obj.add(en)
        }

        return obj
    }
}