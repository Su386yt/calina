package dev.su386.calina.images.tags

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DisabledVisible
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.ui.graphics.vector.ImageVector
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import dev.su386.calina.data.Database
import dev.su386.calina.images.tags.SystemTag.SystemTagManager.systemTags
import java.util.*

/**
 * System tags are tags that permanent and unique. Meaning there will always be exactly one instance of each system tag,
 * System tags have a fixed [uuid].
 *
 * @param name - The display name of the system tag
 * @param uuid - The uuid of the system tag (must be fixed)
 * @param icon - the icon to be displayed
 * @param tagPriority - Tag priority in filter list.
 */
class SystemTag(
    override val name: String,
    uuid: UUID,
    override val icon: ImageVector,
    override val tagPriority: UByte
) : Tag(uuid) {
    init {
        val json = Database.readData<JsonElement>("tags/$uuid.json")
        json?.let {
            loadFromJson(json)
        }
        systemTags.add(this)
    }

    override fun saveToJson(): JsonElement {
        return JsonArray().apply {
            this@SystemTag.imageHashes.forEach {
                add(it)
            }
        }

    }

    override fun loadFromJson(jsonElement: JsonElement) {
        if (!jsonElement.isJsonArray) {
            return
        }

        jsonElement.asJsonArray.forEach {
            imageHashes.add(it.asString)
        }
    }

    object SystemTagManager: TagManager() {
        val systemTags = LinkedList<SystemTag>()
        override fun saveTags() {
            for (systemTag in systemTags) {
                Database.writeData("tags/${systemTag.uuid}.json", systemTag.saveToJson())
            }
        }

        override fun loadTags() {
            throw NotImplementedError("System tags cannot be loaded")
        }
    }

    companion object {
        private val hiddenTagUuid = UUID.fromString("f1a6baf8-48ed-4345-9f07-00c0ad432325")
        val HiddenTag = SystemTag(
            name = "Hidden",
            uuid = hiddenTagUuid,
            icon = Icons.Outlined.DisabledVisible,
            tagPriority = UByte.MAX_VALUE,
        )

        private val likedTagUuid = UUID.fromString("f453ee85-b0c0-44ed-a04b-ea0a2a09c7ca")
        val LikedTag = SystemTag(
            name = "Liked",
            uuid = likedTagUuid,
            icon = Icons.Outlined.FavoriteBorder,
            tagPriority = UByte.MAX_VALUE,
        )
    }
}