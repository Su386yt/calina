package dev.su386.calina.utils

import java.io.File

object FileUtils {
    /**
     * Deletes the file without causing an error
     *
     * @return true if a file exists
     */
    fun File.safelyDelete(): Boolean = this.exists().also { this.deleteOnExit(); this.setWritable(true).also{ if (it) this.delete() } }
}