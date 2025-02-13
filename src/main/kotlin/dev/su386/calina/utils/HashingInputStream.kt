package dev.su386.calina.utils

import java.io.InputStream
import java.security.MessageDigest
import kotlin.math.min

class HashingInputStream(
    private val inputStream: InputStream,
    private val messageDigest: MessageDigest
) : InputStream() {

    private var streamPosition: Long = 0

    override fun read(): Int {
        val byteRead = inputStream.read()
        if (byteRead != -1) {
            messageDigest.update(byteRead.toByte())
            streamPosition++
        }
        return byteRead
    }

    override fun read(b: ByteArray, off: Int, len: Int): Int {
        val bytesRead = inputStream.read(b, off, len)
        if (bytesRead > 0) {
            messageDigest.update(b, off, bytesRead)
            streamPosition += bytesRead
        }
        return bytesRead
    }

    override fun skip(n: Long): Long {
        // We need to read the skipped bytes and update the digest.
        val bufferSize = 64 * 1024 // 64KB buffer
        val buffer = ByteArray(bufferSize)

        var bytesToSkip = n
        var totalBytesSkipped = 0L

        while (bytesToSkip > 0) {
            val bytesRead = inputStream.read(buffer, 0, min(bufferSize.toLong(), bytesToSkip).toInt())
            if (bytesRead == -1) {
                break
            }
            messageDigest.update(buffer, 0, bytesRead)
            bytesToSkip -= bytesRead
            totalBytesSkipped += bytesRead
            streamPosition += bytesRead
        }

        return totalBytesSkipped
    }

    override fun available(): Int {
        return inputStream.available()
    }

    override fun close() {
        inputStream.close()
    }

    override fun mark(readlimit: Int) {
        inputStream.mark(readlimit)
    }

    override fun reset() {
        inputStream.reset()
    }

    override fun markSupported(): Boolean {
        return inputStream.markSupported()
    }
}
