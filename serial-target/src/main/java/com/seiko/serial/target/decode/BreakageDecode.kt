package com.seiko.serial.target.decode

import com.seiko.serial.modbus.indexOfArray
import com.seiko.serial.modbus.toModBusInt
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

/**
 * 尝试处理断包
 */
class BreakageDecode : IDecode {

    private val currentHead = AtomicReference<ByteArray>()
    private val currentSize = AtomicInteger()
    private var queueRemain = EMPTY_BYTES

    private val lastPostBytes = AtomicReference<ByteArray>()

    override fun check(bytes: ByteArray): ByteArray {
        var bak = bytes
        if (queueRemain.isNotEmpty()) {
            if (queueRemain.size < 1024 * 10) {
                bak = queueRemain + bytes
            }
            queueRemain = EMPTY_BYTES
        }
        return decodeBytes(bak)
    }

    private fun decodeBytes(bytes: ByteArray): ByteArray {
        // 没有头字节直接返回EMPTY_BYTES
        val head = currentHead.get() ?: return EMPTY_BYTES

        // 如果在现有缓存池中没有找到头字节，全部废弃。
        val index = bytes.indexOfArray(head)
        if (index == -1) {
//            queueRemain.offer(bytes)
            queueRemain += bytes
            return EMPTY_BYTES
        }

        // 当前缓存池只有一个头，返回null等下次操作。
        if (index + head.size >= bytes.size) {
//            queueRemain.offer(bytes)
            queueRemain += bytes
            return EMPTY_BYTES
        }

        val len = currentSize.get()
        if (len <= 0) return EMPTY_BYTES

        // 数据位置 + 数据长度 与 缓存池数据长度 作比较。
        // IF 数据不够长，放回缓存池, 返回null等下次处理。
        if (index + len > bytes.size) {
//            Log.d("RxDecode", "长度${index + len} > ${bytes.size}, 存入缓存池等下次操作。");
//            queueRemain.offer(bytes)
            queueRemain += bytes
            return EMPTY_BYTES
        }
        // IF 数据过多，将多余的数据放入缓存池。
        else if (index + len < bytes.size) {
//            Log.d("RxDecode", "长度${index + len} < ${bytes.size}, 过于数据存入缓存池。");
//            queueRemain.offer(bytes.copyOfRange(index + len , bytes.size))
            queueRemain += bytes.copyOfRange(index + len , bytes.size)
        }


        // 获得一帧有效数组
        val data = bytes.copyOfRange(index, index + len)
//        Log.d("RxDecode", "获得有效数据:" + Arrays.toString(data))
        return data
    }

    /**
     * 从发送的字节中获取一些参数，用于解析、拼接读取到的数组
     */
    override fun bytesOfSend(bytes: ByteArray) {
        if (bytes.size < 4) return

        val lastBytes = lastPostBytes.get() ?: return
        if (bytes.contentEquals(lastBytes)) return
        lastPostBytes.set(bytes)

        when (bytes[1].toInt()) {
            3 -> {
                val num = bytes.copyOfRange(4, 6).toModBusInt() * 2
                // 获得数据长度，数据长度 + 头长度 + 1(数据长度) + 校验位长度 = 一帧有效数组的长度
                val len = num + 2 + 1 + 2
                currentSize.set(len)
                setHead(byteArrayOf(bytes[0], 3, num.toByte()))
            }
            1 -> {
                // [1, 1, 1, 0, 81, -120]
                val bak = bytes.copyOfRange(4, 6).toModBusInt()
                val num = if (bak % 8 == 0) bak / 8 else bak / 8 + 1
                val len = num + 2 + 1 + 2
                currentSize.set(len)
                setHead(byteArrayOf(bytes[0], 1 ,num.toByte()))
            }
            else -> {
                currentSize.set(bytes.size)
                setHead(bytes.copyOfRange(0, bytes.size - 2))
            }
        }
    }

    private fun setHead(bytes: ByteArray) {
        currentHead.set(bytes)
//        Log.d(TAG, "替换字节头：${Arrays.toString(bytes)}")
    }

    companion object {
//        private const val TAG = "BreakageDecode"
        private val EMPTY_BYTES = ByteArray(0)
    }
}
