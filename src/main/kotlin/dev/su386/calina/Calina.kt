package dev.su386.calina

import dev.su386.calina.Calina.config
import dev.su386.calina.Config.Companion.loadConfig
import dev.su386.calina.Calina.configPath
import dev.su386.calina.data.ImageManager


fun main() {
    println("Hello World!")
    loadConfig(configPath)

    for (string in config.imageFolders) {
        ImageManager.loadImages(string)
    }

    ImageManager.writeDatabase("./testDatabase.json")
}

object Calina {
    const val configPath: String = "./config.json"
    val config: Config = loadConfig(configPath)
}