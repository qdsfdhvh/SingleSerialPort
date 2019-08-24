package com.seiko.serial.rs232

import android.util.Log
import com.seiko.serial.core.SerialPort
import com.seiko.serial.core.SerialSetting
import java.io.File
import java.util.*

class RS232SerialPort(private val path: String,
                      private val setting: SerialSetting): AbsSerialPort(), SerialPort {

    constructor(path: String, baudRate: Int): this(path, SerialSetting(baudRate))

    private var readThread: SerialReadThread? = null
    private var sendThread: SerialSendThread? = null

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
            readThread = SerialReadThread(inputStream, callback).apply { start() }
            sendThread = SerialSendThread(outputStream).apply { start() }

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
            readThread?.close()
            readThread = null
            sendThread?.close()
            sendThread = null
            mFd = null
        }
    }

    override fun send(bytes: ByteArray) {
        sendThread?.send(bytes)
    }

    override fun setBaudRate(baudRate: Int) {
        if (mFd != null) {
            deviceBaudRate(baudRate)
        }
    }


    companion object {
        private const val TAG = "RS232SerialPort"
    }
}