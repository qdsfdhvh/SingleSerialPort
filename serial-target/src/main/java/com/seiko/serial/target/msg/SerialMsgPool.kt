package com.seiko.serial.target.msg

import android.util.Log
import com.seiko.serial.target.SerialTarget

internal object SerialMsgPool {

    //缓存池大小
    private const val MAX_POOL_SIZE = 50

    var next: SerialMsg? = null

    private var poolSize = 0

    fun recycle(msg: SerialMsg) {
        synchronized(this) {
            if (poolSize >= MAX_POOL_SIZE) return
            msg.next = next
            next = msg
            poolSize++
//            if (SerialTarget.IS_DEBUG) {
//                Log.d("SerialMsgPool", "回收， 缓存池=$poolSize")
//            }
        }
    }

    fun take(): SerialMsg {
        synchronized(this) {
            next?.let { result ->
                next = result.next
                result.next = null
                poolSize--
//                Log.d("SerialMsgPool", "缓存池取出，缓存池=$poolSize")
                return result
            }
        }
//        Log.d("SerialMsgPool", "创建新的对象，缓冲池 = $poolSize")
        return SerialMsg()
    }

}