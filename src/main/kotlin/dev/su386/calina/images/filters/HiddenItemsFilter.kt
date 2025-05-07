package dev.su386.calina.images.filters

import dev.su386.calina.images.ImageData
import dev.su386.calina.images.tags.HiddenTag

class HiddenItemsFilter: TagFilter(HiddenTag) {
    override fun isValidImage(image: ImageData): Boolean {
        return !super.isValidImage(image)
    }
}