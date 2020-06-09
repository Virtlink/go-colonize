package com.virtlink.gocolonize

import java.io.OutputStream

/**
 * Input stream that cannot be closed.
 */
class UncloseableOutputStream(
    val wrappedStream: OutputStream
) : OutputStream() {

    override fun close() {
        // Close ignored
    }

    override fun equals(other: Any?): Boolean = wrappedStream.equals(other)

    override fun hashCode(): Int = wrappedStream.hashCode()

    override fun toString(): String = wrappedStream.toString()

    override fun write(b: Int) = wrappedStream.write(b)

    override fun write(b: ByteArray) = wrappedStream.write(b)

    override fun write(b: ByteArray, off: Int, len: Int) = wrappedStream.write(b, off, len)

    override fun flush() = wrappedStream.flush()
}