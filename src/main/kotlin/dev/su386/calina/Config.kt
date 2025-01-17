package dev.su386.calina

import dev.su386.calina.data.Database.readData
import dev.su386.calina.data.Database.writeData

data class Config(val imageFolders: Array<String> = arrayOf()) {
    companion object {
        private const val CONFIG_PATH: String = "config.json"

        val config = readData<Config>(CONFIG_PATH) ?: Config()

        fun saveConfig() {
            writeData(CONFIG_PATH, config)
        }
    }
}
