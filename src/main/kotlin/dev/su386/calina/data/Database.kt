package dev.su386.calina.data

import com.google.gson.GsonBuilder
import java.io.File
import java.nio.file.Files

/**
 * An object to manage and access the database
 */
object Database {
    /**
     * @link https://docs.oracle.com/javase/tutorial/essential/environment/sysprop.html
     */
    val PATH = "${System.getProperty("user.home")}/calina"
    val gson = GsonBuilder().setPrettyPrinting().create()
    val cache = mutableMapOf<String, String>()

    /**
     * Access data from the database
     *
     * @param T - The specified class to load as
     * @param path - The path the data is stored as
     * @return data at the database location of the instance T
     */
    inline fun <reified T> readData(path: String): T? {
        val content = cache.getOrPut(path) {
            val file = File("$PATH/${path.trim('/', '.')}")
            if (!file.exists()) return null
            file.bufferedReader().use { it.readText() }
        }

        return gson.fromJson(content, T::class.java)
    }

    /**
     * Store data in the database at a specified path
     *
     * @param path - Local path within database
     * @param data - Data to be stored in the database
     */
    fun writeData(path: String, data: Any) {
        val file = File("$PATH/${path.trim('/', '.')}")
        Files.createDirectories(file.toPath().parent)
        file.createNewFile()
        file.setWritable(true)
        val string = gson.toJson(data)
        file.bufferedWriter().use { it.write(string) }

        cache[path] = string
    }

    /**
     *
     * @param path - Local path within the database
     * @return true if data exists at the specified path within the database
     */
    fun existsData(path: String): Boolean {
        return File("$PATH/$path").exists()
    }

    /**
     * Access the File instance from the database
     *
     * @param path - Local path within database
     * @return An instance of the File object of the database at the specified path, or null if the file doesn't exist
     */
    fun getFile(path: String): File? {
        val file = File("$PATH/${path.trim('/', '.')}")
        return if (file.exists()) file else null
    }
}