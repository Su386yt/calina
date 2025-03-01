package dev.su386.calina

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import dev.su386.calina.config.Config
import dev.su386.calina.config.Paragraph
import dev.su386.calina.config.StringList
import dev.su386.calina.data.Database
import dev.su386.calina.tasks.OnCloseTask
import dev.su386.calina.tasks.OnStartTask
import dev.su386.calina.tasks.TaskManager
import kotlinx.coroutines.Dispatchers.IO

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
    }

    @Deprecated("Use load()", ReplaceWith("load()"))
    override fun loadFromJson(jsonNode: JsonNode) {
        load()
    }

    fun load() {
        val data = Database.readData<JsonNode>(PATH) ?: return
        super.loadFromJson(data)
    }

    @Deprecated("Use save()", ReplaceWith("save()"))
    override fun saveToJson(): JsonNode {
        save()
        return JsonNodeFactory.instance.objectNode()
    }

    fun save() {
        Database.writeData(PATH, super.saveToJson())
    }


}
