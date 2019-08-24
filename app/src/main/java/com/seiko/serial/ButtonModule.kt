package com.seiko.serial

import android.util.Log
import com.seiko.serial.target.SerialModule
import com.seiko.serial.target.Utils
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import kotlin.collections.ArrayList
import kotlin.math.roundToInt

/**
 * 此管理同于管理M地址，对M地址写入1，修改后改回0。
 * 已实现功能：
 * 1.开始/停止充绒。
 * 2.去皮。
 *
 *
 * PS: 1.目前只有去皮是PLC中转去改传感器才真正需要通过状态来确认是否修改成功，
 *       其他大多是直接修改PLC，除了通讯断开外基本都会成功。
 */
class ButtonModule: SerialModule {

    private val deviceId: Byte = 1

    /**
     * 安全锁
     */
    private val isWaitReceive = AtomicBoolean(false)
    private val lastPostTime = AtomicLong(0)

    /**
     * 指令集合
     */
    private val vector = Collections.synchronizedList(ArrayList<Model>())

    private var target: SerialModule.Target? = null
    private var disposable: Disposable? = null

    /**
     * 放入待处理的地址, 触发一次ON，修改后改回OFF
     */
    fun pull(address: Int) {
        vector.add(AlwaysOff(address, true))
    }

    /**
     * 翻入待处理的地址，修改当前地址的状态
     */
    fun pull(address: Int, bool: Boolean) {
        vector.add(ChangeState(address, bool))
    }

    /**
     * 简单的地址写入
     */
    fun pull(address: Int, bytes: ByteArray) {
        vector.add(Single(address, bytes))
    }

    override fun attach(target: SerialModule.Target?) {
        if (target != null) {
            this.target = target
            onStart()
        } else {
            onStop()
        }
    }

    /**
     * 开个线程用于读取M地址的当前状态
     */
    private fun onStart() {
        onStop()

        disposable = Observable.interval(10, TimeUnit.MILLISECONDS)
            .subscribe({
                // 没有指令，不作处理
                if (vector.isEmpty()) return@subscribe

                // 如果发送时间超过阈值，第一个指令废弃，并重置isWaitReceive
                if (isWaitReceive.get()) {
                    val last = lastPostTime.get()
                    if (last > 0 && System.currentTimeMillis().minus(last) > MAX_WAIT_RECEIVE_TIME) {
                        isWaitReceive.set(false)
                        vector.removeAt(0)
                        if (vector.isEmpty()) return@subscribe
                    }
                }

                if (!isWaitReceive.compareAndSet(false, true)) return@subscribe

                val data = try {
                     vector[0]
                } catch (e: Exception) {
                    e.printStackTrace()
                    return@subscribe
                }

                if (data is Single) {
                    write(data.address, data.bytes)?.singlePost()
                } else {
                    readM(data.address).singlePost()
                }
            }, { error ->
                Log.e("ButtonModel", "Warn.", error)
            })
    }

    private fun onStop() {
        if (disposable != null) {
            disposable!!.dispose()
            disposable = null

            // 重新连接时不处理旧指令
            vector.clear()
        }
    }

    /**
     * 开始处理M地址
     *
     * list不为空时，onStart的线程会尝试读取list中第一个地址的状态。
     *
     * CHANGE_STATE流程：
     * 读取状态如果与item中待修改的bool状态相同，将此地址从list中删除，否则会一直尝试发送MAX_COUNT次。
     * ALWAYS_OFF流程：
     * 读取状态如果为OFF，写入ON，在此期间onStart线程会继续读取第一个地址的状态，在此期间只要读到OFF，就会一直发，直到成功。
     * 修改ON成功后，会改变存于item中的bool状态，同上，一会读到ON都会发，直到成功。
     * 修改OFF成功后，因为存于item的bool状态已经变化，会此地址从list中删除，至此M地址修改完成，尝试下一个地址修改。
     */
    override fun accept(bytes: ByteArray) {
        if (vector.isEmpty()) return

        when(bytes[1].toInt()) {
            1 -> {
                val item = vector[0]
                // 1：ON  0：OFF
                val off = bytes[3].toInt() == 1

                when(item) {
                    is ChangeState -> {
                        if (off == item.bool || item.count > MAX_POST_COUNT) {
                            vector.remove(item)
                            isWaitReceive.set(false)
                        } else {
                            writeM(item.address, item.bool).singlePost()
                            item.count++
                        }
                    }
                    is AlwaysOff -> {
                        if (off) {
                            if (item.count <= MAX_POST_COUNT) {
                                writeM(item.address, false).singlePost()
                                item.bool = false
                                item.count++
                            } else {
                                vector.remove(item)
                                isWaitReceive.set(false)
                            }
                        } else {
                            if (item.bool && item.count <= MAX_POST_COUNT) {
                                writeM(item.address, true).singlePost()
                                item.count++
                            } else {
                                vector.remove(item)
                                isWaitReceive.set(false)
                            }
                        }
                    }
                }
            }
            5 -> {
                when(val item = vector[0]) {
                    is ChangeState, is AlwaysOff -> {
                        readM(item.address).singlePost()
                    }
                }
            }
            6 -> {
                vector.removeAt(0)
                isWaitReceive.set(false)
            }
        }
    }

    private fun ByteArray?.singlePost() {
        if (this == null) return
        target?.send(this@ButtonModule, this)
        lastPostTime.set(System.currentTimeMillis())
    }

    override fun getPriority(): Int {
        return 0
    }

    override fun getTag(): String {
        return "ButtonModule"
    }

    /*******************************************
     *                 Utils                   *
     *******************************************/

    private fun write(address: Int, bytes: ByteArray): ByteArray? {
        return when {
            bytes.isEmpty() -> null
            // 一个地址2个字节
            bytes.size == 2 -> Utils.bind06Cmd(deviceId, address, bytes)
            else -> Utils.bind16Cmd(deviceId, address, bytes)
        }
    }

    private fun writeM(address: Int, bool: Boolean): ByteArray {
        return Utils.bind05Cmd(deviceId, address, bool)
    }

//    private fun read(address: Int, len: Int = 1): ByteArray {
//        return Utils.bind03Cmd(deviceId, address, len)
//    }

    private fun readM(address: Int, len: Int = 1): ByteArray {
        return Utils.bind01Cmd(deviceId, address, len)
    }

    /*******************************************
     *                 Model                   *
     *******************************************/

    private data class ChangeState(
            override val address: Int,
            var bool: Boolean,
            var count: Int = 0
    ): Model

    private data class AlwaysOff(
            override val address: Int,
            var bool: Boolean,
            var count: Int = 0
    ): Model

    private data class Single(
        override val address: Int,
        val bytes: ByteArray
    ): Model {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Single

            if (address != other.address) return false
            if (!bytes.contentEquals(other.bytes)) return false

            return true
        }

        override fun hashCode(): Int {
            var result = address
            result = 31 * result + bytes.contentHashCode()
            return result
        }

    }

    private interface Model {
        val address: Int
    }

    /*******************************************
     *                Companion                *
     *******************************************/

    companion object {
        /**
         * 最大尝试数量
         */
        private const val MAX_POST_COUNT = 3

        /**
         * 最多等待*ms
         */
        private const val MAX_WAIT_RECEIVE_TIME = 50L

        /**
         * 当前PLC的最大工步数量
         */
        private const val MAX_STEP_COUNT = 100
    }

}