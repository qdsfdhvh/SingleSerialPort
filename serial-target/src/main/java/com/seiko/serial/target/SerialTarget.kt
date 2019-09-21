package com.seiko.serial.target

import android.util.Log
import com.seiko.serial.core.SerialPort
import com.seiko.serial.modbus.toHexString
import com.seiko.serial.target.decode.BreakageDecode
import com.seiko.serial.target.decode.IDecode
import com.seiko.serial.target.filter.Crc16Filter
import com.seiko.serial.target.filter.IFilter
import com.seiko.serial.target.msg.SerialMsg
import com.seiko.serial.target.msg.SerialMsgPostThread
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.PriorityBlockingQueue
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference
import kotlin.collections.ArrayList

class SerialTarget(private val serial: SerialPort): SerialModule.Target, SerialMsgPostThread.Callback {

    /**
     * 字节处理，处理断包
     */
    var iDecode: IDecode = BreakageDecode()

    /**
     * 字节过滤，默认：Crc16校验
     */
    var iFilter: IFilter = Crc16Filter()

    /**
     * 设备集合
     */
    private val modules by lazy { CopyOnWriteArrayList<SerialModule>() }
    private val currentModule by lazy { AtomicReference<SerialModule>() }

    /**
     * 是否启动
     */
    private val isOpen = AtomicBoolean(false)

    /**
     * 安全锁
     */
    private val lastReceiveTime = AtomicLong(System.currentTimeMillis())
    private var sendThread: SerialMsgPostThread? = null

    /*******************************************
     *                   Fun                   *
     *******************************************/

    /**
     * 启动串口
     */
    override fun start() {
        if (isOpen.get()) return
        isOpen.lazySet(true)

        var result: ByteArray
        serial.open(object : SerialPort.Callback {
            override fun onSuccess() {
                onStart()
            }

            override fun onError(e: Throwable) {
                onPause()
                Log.e(TAG, "WARN.", e)
            }

            override fun onResult(bytes: ByteArray) {
                if (IS_DEBUG) {
                    Log.d(TAG, "RAW：${bytes.toHexString()}")
                }

                result = iDecode.check(bytes)

                while (result.isNotEmpty()) {
                    if (iFilter.isSafe(result)) {
                        decodeBytes(result)
                    } else if (IS_DEBUG) {
                        Log.d(TAG, "过滤异常字节：${bytes.toHexString()}")
                    }
                    result = iDecode.check(ByteArray(0))
                }
            }
        })
    }

    /**
     * 关闭串口
     */
    override fun close() {
        if (!isOpen.get()) return
        isOpen.lazySet(false)

        // 删除目前存在的devices
        for (module in ArrayList(modules)) {
            delSerialModule(module)
        }
        serial.close()
    }

    /**
     * 发送指令
     */
    override fun send(device: SerialModule, bytes: ByteArray) {
        sendThread?.offer(SerialMsg.obtain(device, bytes))
    }

    /**
     * 添加串口Module
     */
    override fun addSerialModule(module: SerialModule): Boolean {
        if (modules.contains(module)) return true

        val bool = modules.add(module)
        if (bool) {
            if (isOpen.get()) module.attach(this)

            if (IS_DEBUG) {
                Log.v(TAG, "添加Module：${module.javaClass.simpleName}。")
            }
        }
        return bool
    }

    /**
     * 删除串口Module
     */
    override fun delSerialModule(module: SerialModule): Boolean {
        val bool = modules.remove(module)
        if (bool) {
            if (isOpen.get()) module.attach(null)

            if (IS_DEBUG) {
                Log.v(TAG, "删除Module：${module.javaClass.simpleName}。")
            }
        }
        return bool
    }

    /*******************************************
     *               POST / OFFER              *
     *******************************************/

    /**
     * 开启发送队列
     */
    private fun startPostQueue() {
        stopPostQueue()

        val bak = SerialMsgPostThread()
        bak.setCallback(this)
        bak.start()
        sendThread = bak
    }

    /**
     * 关闭发送队列
     */
    private fun stopPostQueue() {
        val bak = sendThread
        if (bak != null) {
            bak.setCallback(null)
            bak.stopThread()
            sendThread = null
        }
    }

    /*******************************************
     *            onStart / onPause            *
     *******************************************/

    private fun onStart() {
        startPostQueue()
        modules.forEach { it.attach(this) }
    }

    private fun onPause() {
        modules.forEach { it.attach(null) }
        stopPostQueue()
    }

    /*******************************************
     *           Bytes Post / Decode           *
     *******************************************/

    override fun onPostMsg(msg: SerialMsg) {
        val module = msg.module
        val bytes = msg.bytes

        currentModule.set(module)
        serial.send(bytes)
        iDecode.bytesOfSend(bytes)

        if (IS_DEBUG) {
            Log.v(TAG,  "%s -> POST：%s".format(module.getTag(), bytes.toHexString()))
        }

        msg.recycle() // 回收Msg

        if (System.currentTimeMillis().minus(lastReceiveTime.get()) > MAX_CONNECT_TIME) {
            if (IS_DEBUG) {
                Log.d(TAG, "尝试清空队列：${sendThread?.size()}")
            }
            sendThread?.clearQueue()
        }
    }

    private fun decodeBytes(bytes: ByteArray) {
        currentModule.get()?.let { module ->
            if (module.accept(bytes)) {
                sendThread?.setIsWaitReceive(false)
            }

            if (IS_DEBUG) {
                Log.v(TAG,  "%s -> READ：%s".format(module.getTag(), bytes.toHexString()))
            }
        }

        lastReceiveTime.lazySet(System.currentTimeMillis())
    }

    /*******************************************
     *                COMPANION                *
     *******************************************/

    companion object {
        /**
         * 是否调试
         */
        var IS_DEBUG = false

        private const val TAG = "SerialTarget"
    }
}