package dev.su386.calina

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import dev.su386.calina.data.Database.readData
import dev.su386.calina.data.Database.writeData

data class Config @JsonCreator constructor(
    @JsonProperty("imageFolders")
    val imageFolders: Array<String> = arrayOf()
) {
    companion object {
        private const val CONFIG_PATH: String = "config.json"

        val config = readData<Config>(CONFIG_PATH) ?: Config()

        fun saveConfig() {
            writeData(CONFIG_PATH, config)
        }
    }
}
