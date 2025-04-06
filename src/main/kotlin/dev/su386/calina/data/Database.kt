package dev.su386.calina.data

import com.google.gson.ExclusionStrategy
import com.google.gson.FieldAttributes
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import java.io.File
import java.lang.reflect.TypeVariable
import java.nio.file.Files

/**
 * An object to manage and access the database
 */
object Database {
    /**
     * @link https://docs.oracle.com/javase/tutorial/essential/environment/sysprop.html
     */
    val PATH = "${System.getProperty("user.home")}/.calina"
    val JSON = GsonBuilder()
        .setExclusionStrategies(TransientExclusionStrategy())
        .setPrettyPrinting()
        .create()
    /**
     * Access data from the database
     *
     * @param T - The specified class to load as
     * @param path - The path the data is stored as
     * @return data at the database location of the instance T
     */
    inline fun <reified T> readData(
        path: String,
    ): T? {
        val file = File("$PATH/${path.trim('/', '.')}")
        if (!file.exists()) return null
        val json: T = JSON.fromJson(JSON.newJsonReader(file.bufferedReader()), object : TypeToken<T>() {}.type)
        return if (json == null) {
            return null
        } else {
            json
        }
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

        try {
            file.outputStream().bufferedWriter().use { writer ->
                JSON.toJson(data, writer)
            }
        } catch (e: Exception) {
            println("Error writing data: ${e.message}")
        }
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

    private class TransientExclusionStrategy : ExclusionStrategy {
        override fun shouldSkipField(f: FieldAttributes): Boolean {
            return f.getAnnotation(Transient::class.java) != null
        }

        override fun shouldSkipClass(clazz: Class<*>?): Boolean {
            return false
        }
    }
}
