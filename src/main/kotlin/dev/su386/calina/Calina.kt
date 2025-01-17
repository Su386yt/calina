package dev.su386.calina

import dev.su386.calina.Calina.config
import dev.su386.calina.Calina.CONFIG_PATH
import dev.su386.calina.data.Database.getFile
import dev.su386.calina.data.Database.readData
import dev.su386.calina.data.Database.writeData
import dev.su386.calina.images.ImageManager


fun main() {
    println("Hello World!")

    println(getFile(CONFIG_PATH)?.absolutePath)

    for (string in config.imageFolders) {
        ImageManager.loadImages(string)
    }

    ImageManager.writeDatabase("testDatabase.json")
    writeData(CONFIG_PATH, config)
}

object Calina {
    const val CONFIG_PATH: String = "config.json"
    val config: Config = readData<Config>(CONFIG_PATH) ?: Config()

}