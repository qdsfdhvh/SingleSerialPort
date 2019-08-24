package com.seiko.serial.rs232

import android.os.SystemClock
import com.seiko.serial.core.SerialPort
import okio.Buffer
import okio.ByteString
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
        val buffer = Buffer()
        var size: Long

        while(true) {

            if (Thread.currentThread().isInterrupted) {
                break
            }

            try {
                size = source.read(buffer, MAX_BUFFER_SIZE)
                if (size > 0) {
                    callback.onResult(buffer.readByteArray())
                    buffer.clear()
                } else {
                    // 暂停一点时间，免得一直循环造成CPU占用率过高
                    SystemClock.sleep(1)
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }

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
        private const val MAX_BUFFER_SIZE = 2048L
    }

}
