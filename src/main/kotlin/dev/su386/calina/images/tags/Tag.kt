package dev.su386.calina.images.tags

import androidx.compose.ui.graphics.vector.ImageVector
import com.google.gson.JsonElement
import java.util.*

/**
 * Tags can be added to images
 *
 * @param uuid - unique identifier for each tag
 */
abstract class Tag(
    val uuid: UUID
){
    init {
        tags[uuid] = this
    }

    /**
     * Display name of the tag
     */
    abstract val name: String

    /**
     * The order in which to display each tag in a list. Tags with the same priority will be sorted by image count
     */
    abstract val tagPriority: UByte

    /**
     * Image hashes that are tagged with this tag
     */
    val imageHashes: MutableSet<String> = mutableSetOf()

    /*
     * Tag priority which includes image count
     */
    val calculatedTagPriority: UInt
        get() {
            return (tagPriority.toUInt() shl (UInt.SIZE_BITS - UByte.SIZE_BITS)) + tags.size.toUInt()
        }

    /**
     * Icon associated with each tag
     */
    open val icon: ImageVector? = null

    /**
     * This method is not called anywhere else, and must be called by the tag's associated [TagManager]
     *
     * @return - a JsonElement which can be passed into [loadFromJson], by which [imageHashes] can be populated
     */
    abstract fun saveToJson(): JsonElement

    /**
     * Populates [imageHashes] with a jsonElement returned from [saveToJson]
     *
     * @param jsonElement - JsonElement returned from [saveToJson]
     */
    abstract fun loadFromJson(jsonElement: JsonElement)

    /**
     * TagManager manages the bulk loading and saving of tags
     */
    abstract class TagManager {
        abstract fun saveTags()
        abstract fun loadTags()
    }

    companion object {
        val tags = mutableMapOf<UUID, Tag>()
    }
}
