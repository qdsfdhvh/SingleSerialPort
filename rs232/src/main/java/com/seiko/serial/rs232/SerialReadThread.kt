package com.seiko.serial.rs232

import com.seiko.serial.core.SerialPort
import okio.Buffer
import okio.buffer
import okio.source

import java.io.IOException
import java.io.InputStream

class SerialReadThread internal constructor(
    inputStream: InputStream,
    private val callback: SerialPort.Callback
) : Thread() {

    private val source = inputStream.source().buffer()

    override fun run() {
        super.run()

        val buffer = Buffer()
        var size: Long
        try {
            while (!interrupted()) {
                size = source.read(buffer, MAX_BUFFER_SIZE.toLong())
                if (size > 0) {
                    callback.onResult(buffer.readByteArray())
                    buffer.clear()
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    @Synchronized
    override fun start() {
        super.start()
    }

    fun close() {
        try {
            source.close()
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            super.interrupt()
        }
    }

    companion object {
        private const val MAX_BUFFER_SIZE = 16 * 4096
    }

}
