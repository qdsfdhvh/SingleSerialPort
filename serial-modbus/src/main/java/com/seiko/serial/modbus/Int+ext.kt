package com.seiko.serial.modbus

/**
 * 将十进制数字转为相应的ModBus数组
 * @param size 需要转换成多长的数组
 * @return ModBus数组
 */
fun Int.toModBusByteArray(size: Int = 2): ByteArray {
    val bytes = ByteArray(size)
    when (size) {
        1 -> bytes[0] = this.toByte()
        2 -> {
            bytes[0] = (this shr 8 and 0xFF).toByte()
            bytes[1] = (this and 0xFF).toByte()
        }
        4 -> {
            bytes[2] = (this shr 24 and 0xFF).toByte()
            bytes[3] = (this shr 16 and 0xFF).toByte()
            bytes[0] = (this shr 8 and 0xFF).toByte()
            bytes[1] = (this and 0xFF).toByte()
        }
        else -> bytes[0] = 0
    }
    return bytes
}

/**
 * 将10进制数字转为二进制字符串
 * @return 二进制字符串
 */
@Suppress("NOTHING_TO_INLINE") // Syntactic sugar.
inline fun Int.toBinary(): String {
    return Integer.toBinaryString((this and 0xFF) + 0x100).substring(1)
}