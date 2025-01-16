package dev.su386.calina

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonParser
import java.io.File
import java.io.FileWriter
import kotlin.reflect.javaType
import kotlin.reflect.typeOf

class Config(
    val imageFolders: Array<String> = arrayOf(),
) {
    companion object {
        @OptIn(ExperimentalStdlibApi::class)
        fun loadConfig(path: String): Config {
            val file = File(path)
            file.parentFile.mkdirs()
            file.setWritable(true)
            file.setReadable(true)

            // If it is the first time creating a file, we insert the default values in
            if (file.createNewFile()) {
                saveConfig(Config(), path)
            }

            return Gson().fromJson(file.reader(), typeOf<Config>().javaType)
        }

        fun saveConfig(config: Config, path: String) {
            val file = File(path)
            file.parentFile.mkdirs()
            file.createNewFile()
            file.setWritable(true)

            val builder = GsonBuilder()
            builder.setPrettyPrinting()
            builder.serializeSpecialFloatingPointValues()
            val gson = builder.create()

            // Converts list to JSON string
            val json = gson.toJson(config)

            // Writes string to file
            val writer = FileWriter(file)
            writer.write(json)
            writer.close()
        }
    }
}