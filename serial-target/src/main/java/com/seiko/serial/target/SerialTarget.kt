package com.seiko.serial.target

import android.util.Log
import com.seiko.serial.core.SerialPort
import com.seiko.serial.modbus.hexString
import com.seiko.serial.target.decode.BreakageDecode
import com.seiko.serial.target.decode.IDecode
import com.seiko.serial.target.filter.Crc16Filter
import com.seiko.serial.target.filter.IFilter
import io.reactivex.BackpressureStrategy
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import io.reactivex.internal.operators.flowable.FlowableCreate
import io.reactivex.schedulers.Schedulers
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.PriorityBlockingQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference
import kotlin.collections.ArrayList

class SerialTarget(private val serial: SerialPort,
                   private val debug: Boolean): SerialModule.Target {

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
    private val isWaitReceive = AtomicBoolean(false)
    private val lastPostTime = AtomicLong(0)
    private val lastReceiveTime = AtomicLong(0)

    /*******************************************
     *                   Fun                   *
     *******************************************/

    /**
     * 启动串口
     */
    override fun start() {
        serialStart()
    }

    /**
     * 关闭串口
     */
    override fun close() {
        serialStop()
        // 删除目前存在的devices
        for (module in ArrayList(modules)) {
            delSerialModule(module)
        }
    }

    /**
     * 发送指令
     */
    override fun send(device: SerialModule, bytes: ByteArray) {
        SerialMsg.obtain(device, bytes).offer()
    }

    /**
     * 添加串口Module
     */
    override fun addSerialModule(module: SerialModule): Boolean {
        if (modules.contains(module)) return true

        val bool = modules.add(module)
        if (bool) {
            module.attach(this)
            if (isOpen.get()) module.attach(this)

            if (debug) Log.v(TAG, "添加Module：${module.javaClass.simpleName}。")
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
            module.attach(null)

            if (debug) Log.v(TAG, "删除Module：${module.javaClass.simpleName}。")
        }
        return bool
    }

    /*******************************************
     *               POST / OFFER              *
     *******************************************/

    /**
     * 待发送字节的队列
     */
//    private val queue = ConcurrentLinkedQueue<SerialMsg>()
    private val queue = PriorityBlockingQueue<SerialMsg>()

    /**
     * 队列发送事件
     */
    private var postQueue: Disposable? = null

    /**
     * 开启发送队列
     */
    private fun startPostQueue() {
        stopPostQueue()
        postQueue = Observable.interval(0, DEFAULT_POST_TIME, TimeUnit.MILLISECONDS)
            .filter {
                when {
                    queue.isEmpty() -> false
                    !isWaitReceive.get() -> true
                    else -> System.currentTimeMillis().minus(lastPostTime.get()) > MAX_WAIT_RECEIVE_TIME
                }
            }
            .subscribe { queue.poll().post() }
    }

    /**
     * 关闭发送队列
     */
    private fun stopPostQueue() {
        if (null != postQueue) {
            postQueue!!.dispose()
            postQueue = null
            queue.clear()
        }
    }

    /**
     * 放入队列
     */
    private fun SerialMsg.offer() {
        queue.offer(this)
    }
    /**
     * 发送字节数组
     */
    private fun SerialMsg?.post() {
        if (this == null) return

        if (debug) {
            val msg = "%s -> POST：%s".format(module.getTag(), bytes.hexString())
            Log.v(TAG, msg)
        }

        currentModule.set(module)
        serial.send(bytes)
        iDecode.bytesOfSend(bytes)

        recycle() //注销

        if (lastPostTime.get() != 0L
            && System.currentTimeMillis().minus(lastReceiveTime.get()) > MAX_CONNECT_TIME) {
            if (queue.isNotEmpty()) {
                if (debug) Log.d(TAG, "尝试清空队列：${queue.isEmpty()}")
                queue.clear()
            }
        }

        /*
         * 修改状态 -> 正在等待接收 / 最后发送时间
         */
        isWaitReceive.set(true)
        lastPostTime.set(System.currentTimeMillis())
    }



    /*******************************************
     *             onStart / onPause            *
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
     *           Serial start / close          *
     *******************************************/

    private var serialDisposable: Disposable? = null

    private fun serialStart() {
        serialStop()

        serialDisposable = FlowableCreate<ByteArray>({ emitter ->
                emitter.setCancellable {
                    onPause()
                    serial.close()
                }

                serial.open(object : SerialPort.Callback {
                    override fun onSuccess() {
                        isOpen.set(true)
                        onStart()
                    }

                    override fun onResult(bytes: ByteArray) {
                        if (debug) {
                            val msg = "RAW：%s".format(bytes.hexString())
                            Log.v(TAG, msg)
                        }

                        var bak = iDecode.check(bytes)
                        while (bak.isNotEmpty()) {
                            emitter.onNext(bak)
                            bak = iDecode.check(ByteArray(0))
                        }
                    }

                    override fun onError(e: Throwable) {
                        Log.e(TAG, "出现异常。", e)
                        isOpen.set(false)
                        onPause()
                    }
                })
            }, BackpressureStrategy.BUFFER)
            .subscribeOn(Schedulers.computation())
            .filter { bytes ->
                val bool = iFilter.isSafe(bytes)
                if (!bool && debug) {
                    Log.d(TAG, "过滤异常字节：${bytes.contentToString()}")
                }
                return@filter bool
            }
            .doOnCancel { isOpen.set(false) }
            .subscribe({ bytes ->
                currentModule.get()?.let { module ->
                    module.accept(bytes)

                    if (debug) {
                        val msg = "%s -> READ：%s".format(module.getTag(), bytes.hexString())
                        Log.v(TAG, msg)
                    }
                }
            }, { error ->
                Log.e(TAG, "Warn.", error)
            })
    }

    private fun serialStop() {
        if (serialDisposable != null) {
            serialDisposable!!.dispose()
            serialDisposable = null
        }
    }

    /*******************************************
     *                COMPANION                *
     *******************************************/

    companion object {
        private const val TAG = "SerialTarget"

        /**
         * 默认底层指令队列发送间隔，因为PLC有3ms的发送延时，所以设为4ms。
         */
        private const val DEFAULT_POST_TIME = 4L

        /**
         * 最多等待*ms
         */
        private const val MAX_WAIT_RECEIVE_TIME = 60L

        /**
         * 超过*ms时，认为连接断开，开始清理队列，防止阻塞。
         */
        private const val MAX_CONNECT_TIME = 1200L

    }
}