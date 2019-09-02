package com.seiko.serial.target.decode

import com.seiko.serial.modbus.indexOfArray
import com.seiko.serial.modbus.toModBusInt
import java.nio.ByteBuffer
import java.util.*
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

/**
 * 尝试处理断包
 */
class BreakageDecode : IDecode {

    private val currentHead = AtomicReference<ByteArray>()
    private val currentSize = AtomicInteger()
    //    private val queueRemain = LinkedList<Byte>()
//    private var queueRemain = EMPTY_BYTES
    private var queueRemain: ByteBuffer = ByteBuffer.allocate(4086)

    private val lastPostBytes = AtomicReference<ByteArray>()

    override fun check(bytes: ByteArray): ByteArray {
//        var bak = bytes
//        if (queueRemain.isNotEmpty()) {
//            if (queueRemain.size < 1024 * 10) {
//                bak = queueRemain + bytes
//            }
//            queueRemain = EMPTY_BYTES
//        }

        queueRemain.put(bytes)
        val size = queueRemain.position()
        val newBytes = if (size > 0) {
            val bak = queueRemain.getDataReceived()
            queueRemain.clear()
            bak
        } else {
            EMPTY_BYTES
        }
        return decodeBytes(newBytes)
    }

    private fun decodeBytes(bytes: ByteArray): ByteArray {
        // 没有头字节直接返回EMPTY_BYTES
        val head = currentHead.get() ?: return EMPTY_BYTES

        // 如果在现有缓存池中没有找到头字节，全部废弃。
        val index = bytes.indexOfArray(head)
        if (index == -1) {
//            queueRemain.offer(bytes)
//            queueRemain += bytes
            queueRemain.put(bytes)
            return EMPTY_BYTES
        }

        // 当前缓存池只有一个头，返回null等下次操作。
        if (index + head.size >= bytes.size) {
//            queueRemain.offer(bytes)
//            queueRemain += bytes
            queueRemain.put(bytes)
            return EMPTY_BYTES
        }

        val len = currentSize.get()
        if (len <= 0) return EMPTY_BYTES

        // 数据位置 + 数据长度 与 缓存池数据长度 作比较。
        // IF 数据不够长，放回缓存池, 返回null等下次处理。
        if (index + len > bytes.size) {
//            Log.d("RxDecode", "长度${index + len} > ${bytes.size}, 存入缓存池等下次操作。");
//            queueRemain.offer(bytes)
//            queueRemain += bytes
            queueRemain.put(bytes)
            return EMPTY_BYTES
        }
        // IF 数据过多，将多余的数据放入缓存池。
        else if (index + len < bytes.size) {
//            Log.d("RxDecode", "长度${index + len} < ${bytes.size}, 过于数据存入缓存池。");
//            queueRemain.offer(bytes.copyOfRange(index + len , bytes.size))
//            queueRemain += bytes.copyOfRange(index + len , bytes.size)
            queueRemain.put(bytes.copyOfRange(index + len , bytes.size))
        }


        // 获得一帧有效数组
        return bytes.copyOfRange(index, index + len)
    }

    /**
     * 从发送的字节中获取一些参数，用于解析、拼接读取到的数组
     */
    override fun bytesOfSend(bytes: ByteArray) {
        if (bytes.size < 4) return

        if (Arrays.equals(lastPostBytes.get(), bytes)) return
        lastPostBytes.lazySet(bytes)

        when (bytes[1].toInt()) {
            3 -> {
                val num = bytes.copyOfRange(4, 6).toModBusInt() * 2
                // 获得数据长度，数据长度 + 头长度 + 1(数据长度) + 校验位长度 = 一帧有效数组的长度
                val len = num + 2 + 1 + 2
                currentSize.lazySet(len)
                setHead(byteArrayOf(bytes[0], 3, num.toByte()))
            }
            1 -> {
                // [1, 1, 1, 0, 81, -120]
                val bak = bytes.copyOfRange(4, 6).toModBusInt()
                val num = if (bak % 8 == 0) bak / 8 else bak / 8 + 1
                val len = num + 2 + 1 + 2
                currentSize.lazySet(len)
                setHead(byteArrayOf(bytes[0], 1 ,num.toByte()))
            }
            16 -> {
                currentSize.lazySet(8)
                setHead(bytes.copyOfRange(0, 6))
            }
            else -> {
                currentSize.lazySet(bytes.size)
                setHead(bytes.copyOfRange(0, bytes.size - 2))
            }
        }
    }

    private fun setHead(bytes: ByteArray) {
        currentHead.lazySet(bytes)
//        Log.d(TAG, "替换字节头：${Arrays.toString(bytes)}")
    }

}

fun Collection<Int>.toModBusBytes(byteSize: Int = 2): ByteArray {
    val bytes = ByteArray(this.size * byteSize)
    forEachIndexed { index, weight ->
        when(byteSize) {
            2 -> {
                bytes[index * 2 + 0] = (weight shr 8 and 0xFF).toByte()
                bytes[index * 2 + 1] = (weight and 0xFF).toByte()
            }
            4 -> {
                bytes[index * 4 + 2] = (weight shr 24 and 0xFF).toByte()
                bytes[index * 4 + 3] = (weight shr 16 and 0xFF).toByte()
                bytes[index * 4 + 0] = (weight shr 8 and 0xFF).toByte()
                bytes[index * 4 + 1] = (weight and 0xFF).toByte()
            }
            else -> {
                bytes[index * 1 + 0] = (weight and 0xFF).toByte()
            }
        }
    }
    return bytes
}

private val EMPTY_BYTES = ByteArray(0)

private fun ByteBuffer.getDataReceived(): ByteArray {
    val dst = ByteArray(position())
    position(0)
    get(dst, 0, dst.size)
    return dst
}
