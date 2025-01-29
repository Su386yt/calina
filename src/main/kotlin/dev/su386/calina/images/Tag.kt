package dev.su386.calina.images

import com.fasterxml.jackson.annotation.JsonIgnore
import dev.su386.calina.data.Database.readData
import dev.su386.calina.data.Database.writeData
import java.util.*

class Tag(
    val name: String,
) {
    val uuid = UUID.randomUUID()
    val imageHashes: MutableSet<String> = mutableSetOf()

    init {
        tags[uuid] = this
    }

    companion object {
        @JsonIgnore
        val tags = mutableMapOf<UUID, Tag>()

        /**
         * Save tags to persistent storage
         *
         * @see dev.su386.calina.data.Database.writeData
         */
        fun saveTags() {
            writeData("tags/tags.json", tags.values)
        }

        /**
         * Load tags from persistent storage
         *
         * @see dev.su386.calina.data.Database.readData
         */
        fun loadTags() {
            readData<MutableCollection<Tag>>("tags/tags.json")
                ?.associateBy { it.uuid }
                ?.let { tags.putAll(it) }
        }
    }
}
