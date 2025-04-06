package dev.su386.calina.images.filters

import dev.su386.calina.images.ImageData

/**
 * A class to filter images
 */
abstract class Filter {
    /**
     * @param image - Image to check against filter.
     * @return true if [image] is a valid image for the filter type, false otherwise.
     */
    abstract fun isValidImage(image: ImageData): Boolean
}