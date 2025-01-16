package dev.su386.calina.data

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import dev.su386.calina.data.ImageData.Companion.toImageData
import java.io.File
import java.io.FileWriter
import java.nio.file.Files

object ImageManager {
    private val images: MutableMap<String, ImageData> = mutableMapOf()
    private val loadedPaths = mutableSetOf<String>()

    /**
     * Loads all the images that start with the given path. Goes through folders recursively
     */
    private val acceptedFileTypes = arrayOf(".jpg", ".jpeg", ".png", ".gif", ".bmp", ".tiff", ".webp")
    fun loadImages(path: String) {
        File(path).walkBottomUp().forEach {
            if (it.isDirectory()){
                return@forEach
            }

            if (loadedPaths.contains(it.path)) {
                return@forEach
            }

            var correctPath = false
            for (extension in acceptedFileTypes) {
                if (it.path.endsWith(extension)) {
                    correctPath = true
                }
            }

            if (!correctPath) {
                return@forEach
            }

            val newImage = it.toImageData()
            registerImage(newImage)
        }
    }


    fun registerImage(imageData: ImageData) {
        images[imageData.id] = imageData
        loadedPaths.add(imageData.cachedPath)
    }
    /**
     * Loads all the images from the JSON file at the given path
     */
    fun loadDatabase(filepath: String) {
        val file = File(filepath)
        file.parentFile.mkdirs()
        file.setReadable(true)

        if (!file.exists()) {
            return
        }

        val imageSet = Gson().fromJson(file.reader(), object : TypeToken<MutableCollection<ImageData>>() {})

        for (image in imageSet){
            registerImage(image)
        }
    }

    fun writeDatabase(path: String){
        val file = File(path)
        file.parentFile.mkdirs()
        file.createNewFile()
        file.setWritable(true)

        val builder = GsonBuilder()
        builder.setPrettyPrinting()
        builder.serializeSpecialFloatingPointValues()
        val gson = builder.create()

        // Converts list to JSON string
        val json = gson.toJson(images.values)

        // Writes string to file
        val writer = FileWriter(file)
        writer.write(json)
        writer.close()
    }
}