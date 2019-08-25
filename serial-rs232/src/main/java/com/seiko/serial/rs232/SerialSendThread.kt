package com.seiko.serial.rs232
//
//import android.os.Handler
//import android.os.HandlerThread
//import android.os.Looper
//import android.os.Message
//
//import java.io.IOException
//import java.io.OutputStream
//
//import okio.BufferedSink
//import okio.buffer
//import okio.sink
//
//
//class SerialSendThread(outputStream: OutputStream): HandlerThread("mSendingHandlerThread") {
//
//    private var handler: SerialSendHandler? = null
//    private val sink: BufferedSink = outputStream.sink().buffer()
//
//    @Synchronized
//    override fun start() {
//        super.start()
//        handler = SerialSendHandler(looper)
//    }
//
//    fun close() {
//        try {
//            sink.close()
//        } catch (e: IOException) {
//            e.printStackTrace()
//        } finally {
//            interrupt()
//            quit()
//        }
//    }
//
//    fun send(bytes: ByteArray) {
//        handler?.obtainMessage(0, bytes)?.sendToTarget()
//    }
//
//    private inner class SerialSendHandler internal constructor(looper: Looper) : Handler(looper) {
//        override fun handleMessage(msg: Message) {
//            try {
//                if (msg.obj is ByteArray) {
//                    val data = msg.obj as ByteArray
//                    sink.write(data)
//                    sink.flush()
//                }
//            } catch (e: IOException) {
//                e.printStackTrace()
//            }
//        }
//    }
//}
