package com.seiko.serial.tcp

import android.os.SystemClock
import com.seiko.serial.core.SerialPort
import com.seiko.serial.thread.AbsWorkerThread
import com.seiko.serial.thread.SerialBuffer
import okio.*
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.Socket

class TcpSerialPort(private val host: String, private val port: Int): SerialPort {

    private var socket: Socket? = null
    private var readThread: ReadThread? = null
    private var sendThread: SendThread? = null
    private val serialBuffer = SerialBuffer()

    override fun open(callback: SerialPort.Callback) {
        Thread {
            try {
                val socket = Socket(host, port)
                readThread = ReadThread(socket.getInputStream(), callback).apply { start() }
                sendThread = SendThread(socket.getOutputStream()).apply { start() }
                this.socket = socket

                callback.onSuccess()
            } catch (e: Exception) {
                callback.onError(e)
            }
        }.start()
    }

    override fun close() {
        if (socket != null) {
            try {
                readThread?.stopThread()
                readThread = null
                sendThread?.stopThread()
                sendThread = null
                socket!!.close()
                socket = null
            } catch (ignored: Exception) {
            }
        }
    }

    override fun send(bytes: ByteArray) {
        serialBuffer.putWriteBuffer(bytes)
    }

    override fun setBaudRate(baudRate: Int) {

    }

    private inner class SendThread(outputStream: OutputStream) : AbsWorkerThread() {

        private val sink: BufferedSink = outputStream.sink().buffer()

        override fun doRun() {
            val data = serialBuffer.writeBuffer
            if (data.isNotEmpty()) {
                try {
                    sink.write(data)
                    sink.flush()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            } else {
                SystemClock.sleep(1)
            }
        }

        override fun stopThread() {
            try {
                sink.close()
            } catch (e: IOException) {
                e.printStackTrace()
            } finally {
                super.stopThread()
            }
        }
    }

    private inner class ReadThread(inputStream: InputStream,
                                   private val mCallback: SerialPort.Callback) : AbsWorkerThread() {

        private val source: BufferedSource = inputStream.source().buffer()

        override fun doRun() {
            val size: Long
            try {
                size = source.read(serialBuffer.readBuffer, SerialBuffer.DEFAULT_READ_BUFFER_SIZE)
                if (size > 0) {
                    val data = serialBuffer.dataReceived
                    serialBuffer.clearReadBuffer()
                    mCallback.onResult(data)
                } else {
                    // 暂停一点时间，免得一直循环造成CPU占用率过高
                    SystemClock.sleep(1)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

        }

        override fun stopThread() {
            try {
                source.close()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                super.stopThread()
            }
        }
    }


}