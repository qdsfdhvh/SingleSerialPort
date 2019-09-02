package com.seiko.serial.rs232

import android.os.SystemClock
import android.util.Log
import com.seiko.serial.core.SerialPort
import com.seiko.serial.core.SerialSetting
import okio.*
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.*

class RS232SerialPort(private val path: String,
                      private val setting: SerialSetting): AbsSerialPort(), SerialPort {

    constructor(path: String, baudRate: Int): this(path, SerialSetting(baudRate))

    private var readThread: ReadThread? = null
    private var sendThread: SendThread? = null
    private val serialBuffer = SerialBuffer()

    override fun open(callback: SerialPort.Callback) {
        val file = File(path)
        if (!chmod777(file)) {
            callback.onError(FileSystemException(file))
            return
        }

        try {
            val mfd = deviceOpen(path, setting.baudRate, setting.data, setting.stop, setting.parity)
            if (mfd == null) {
                callback.onError(Exception("Cant open FileDescriptor."))
                return
            }

            mFd = mfd
            readThread = ReadThread(inputStream, callback).apply { start() }
            sendThread = SendThread(outputStream).apply { start() }

            val msg = String.format(
                Locale.CHINA, "成功开启Serial(%s)，参数(%s, %s, %s, %s)。", path,
                setting.baudRate, setting.data, setting.stop, setting.parity)
            Log.d(TAG, msg)

            callback.onSuccess()
        } catch (e: Exception) {
            callback.onError(e)
        }
    }

    override fun close() {
        if (mFd != null) {
            deviceClose()
            readThread?.stopThread()
            readThread = null
            sendThread?.stopThread()
            sendThread = null
            mFd = null
        }
    }

    override fun send(bytes: ByteArray) {
        serialBuffer.putWriteBuffer(bytes)
    }

    override fun setBaudRate(baudRate: Int) {
        if (mFd != null) {
            deviceBaudRate(baudRate)
        }
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
            } catch (e: IOException) {
                e.printStackTrace()
            }

        }

        override fun stopThread() {
            try {
                source.close()
            } catch (e: IOException) {
                e.printStackTrace()
            } finally {
                super.stopThread()
            }
        }
    }

    companion object {
        private const val TAG = "RS232SerialPort"
    }
}