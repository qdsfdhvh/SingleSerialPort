package com.seiko.serial.target.msg

import java.util.Queue
import java.util.concurrent.PriorityBlockingQueue
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

import com.seiko.serial.target.MAX_WAIT_RECEIVE_TIME

internal class SerialMsgPostThread : AbsWorkerThread() {

    private val queue: Queue<SerialMsg> = PriorityBlockingQueue()
    private var callback: Callback? = null

    private val lastPostTime = AtomicLong(0)
    private val isWaitReceive = AtomicBoolean(false)

    fun offer(msg: SerialMsg) {
        queue.offer(msg)
    }

    fun setCallback(callback: Callback?) {
        this.callback = callback
    }

    /**
     * 修改等待接收状态
     */
    fun setIsWaitReceive(bool: Boolean) {
        isWaitReceive.lazySet(bool)
    }

    /**
     * 队列长度
     */
    fun size(): Int {
        return queue.size
    }

    /**
     * 清空队列
     */
    fun clearQueue() {
        if (!queue.isEmpty()) {
            queue.clear()
        }
    }

    override fun doRun() {
        // 没有指令发送
        if (queue.isEmpty()) return

        // 等待接收状态 && 发送未超时
        if (isWaitReceive.get() && System.currentTimeMillis() - lastPostTime.get() < MAX_WAIT_RECEIVE_TIME) {
            return
        }

        val msg = queue.poll()
        if (msg != null && callback != null) {
            callback!!.onPostMsg(msg)

            isWaitReceive.lazySet(true)
            lastPostTime.lazySet(System.currentTimeMillis())
        }
    }

    override fun stopThread() {
        super.stopThread()
        queue.clear()
        isWaitReceive.lazySet(false)
    }

    interface Callback {
        fun onPostMsg(msg: SerialMsg)
    }

}
