package dev.su386.calina

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import dev.su386.calina.config.*
import dev.su386.calina.data.Database

object CalinaConfig: Config("Settings", "") {
    private const val PATH = "config.json"

    init {
        this.register(
            path = "gallery/imagePaths",
            option = StringList(
                name = "Image Paths",
                description = "Paths for which to look for images"
            ),
            "imageFolders" // Backwards compatibility from the previous config library
        )
        this["gallery"].name = "Gallery"

        this["performance/imageHashCount"] = Integer(
            name = "Image Hash Count",
            description = "Paths for which to look for images",
            1000
        )
        this["performance/imageHashTimeout"] = Decimal(
            name = "Image Hash Timeout (min)",
            description = "",
            10.0
        )
        this["performance/imageSearchTimeout"] = Decimal(
            name = "Image Search Timeout (min)",
            description = "",
            2.5
        )
        this["performance"].name = "Performance"


    }

    @Deprecated("Use load()", ReplaceWith("load()"))
    override fun loadFromJson(jsonNode: JsonElement) {
        load()
    }

    fun load() {
        val data = Database.readData<JsonElement>(PATH) ?: return
        super.loadFromJson(data)
    }

    @Deprecated("Use save()", ReplaceWith("save()"))
    override fun saveToJson(): JsonElement {
        save()
        return JsonObject()
    }

    fun save() {
        Database.writeData(PATH, super.saveToJson())
    }


}
