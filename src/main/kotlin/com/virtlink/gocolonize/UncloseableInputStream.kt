package com.virtlink.gocolonize

import java.io.InputStream

/**
 * Input stream that cannot be closed.
 */
class UncloseableInputStream(
    val wrappedStream: InputStream
) : InputStream() {

    override fun close() {
        // Close ignored
    }

    override fun equals(other: Any?): Boolean = wrappedStream.equals(other)

    override fun hashCode(): Int = wrappedStream.hashCode()

    override fun toString(): String = wrappedStream.toString()

    override fun skip(n: Long): Long = wrappedStream.skip(n)

    override fun available(): Int = wrappedStream.available()

    override fun reset() = wrappedStream.reset()

    override fun mark(readlimit: Int) = wrappedStream.mark(readlimit)

    override fun markSupported(): Boolean = wrappedStream.markSupported()

    override fun read(): Int = wrappedStream.read()

    override fun read(b: ByteArray): Int = wrappedStream.read(b)

    override fun read(b: ByteArray, off: Int, len: Int): Int = wrappedStream.read(b, off, len)
}