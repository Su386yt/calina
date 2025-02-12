package dev.su386.calina

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import dev.su386.calina.config.Config
import dev.su386.calina.config.StringList
import dev.su386.calina.data.Database

object CalinaConfig: Config("Calina", "") {
    private const val path = "config.json"

    init {
        this["gallery/imagePaths"] = StringList("Image Paths", "Paths for which to look for images")
        this["gallery"].name = "Gallery"
    }

    @Deprecated("Use load()", ReplaceWith("load()"))
    override fun loadFromJson(jsonNode: JsonNode) {
        load()
    }

    fun load() {
        val data = Database.readData<JsonNode>(path) ?: return
        super.loadFromJson(data)
    }

    @Deprecated("Use save()", ReplaceWith("save()"))
    override fun saveToJson(): JsonNode {
        save()
        return JsonNodeFactory.instance.objectNode()
    }

    fun save() {
        Database.writeData("test", super.saveToJson())
    }


}
