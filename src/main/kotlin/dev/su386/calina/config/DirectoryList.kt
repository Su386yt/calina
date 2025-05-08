package dev.su386.calina.config

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.InputChip
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import dev.su386.calina.utils.AutoResizeText


class DirectoryList (
    name: String,
    description: String,
    defaultDirectories: MutableList<String> = mutableListOf(),
    vararg defaultDirectoriesExtra: String
) : ConfigOption(
    name,
    description,
    size = 2f
){
    override val value = defaultDirectories

    init {
        for (directory in defaultDirectoriesExtra){
            this.value.add(directory)
        }
    }

    @Preview
    @Composable
    override fun getComposable() {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .fillMaxSize(),
        ){
            Row(
                modifier = Modifier
                    .fillMaxSize(),
            ){
                AutoResizeText(
                    text = "$name:",
                    modifier = Modifier
                        .padding(7.dp)
                        .height(24.dp),
                    color = MaterialTheme.colors.onSurface,
                    align = Alignment.CenterStart,
                )
                // The box containing the directory paths
                InputChip(
                    text = this@DirectoryList.value[0]
                )
                Box(
                    modifier = Modifier
                        .border(1.dp, MaterialTheme.colors.onSurface, RoundedCornerShape(10.dp))
                        .padding(2.dp)
                ){
                    Text(
                        text = this@DirectoryList.value[0],
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colors.onSurface,
                        modifier = Modifier
                            .padding(5.dp)
                    )
                }
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