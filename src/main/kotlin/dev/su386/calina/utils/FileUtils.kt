package dev.su386.calina.utils

import java.io.File

object FileUtils {
    /**
     * Deletes the path the file without causing an error
     *
     * @return true if a file exists and was deleted
     */
    fun File.safelyDelete(): Boolean = this.exists().also { this.deleteOnExit(); this.setWritable(true).also{ if (it) this.delete() } }
}