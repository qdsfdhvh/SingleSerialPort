package com.seiko.serial.modbus

/**
 * 将字节数组装维相应的int
 */
fun ByteArray.modBusInt(): Int {
    return when(size) {
        4 -> ((this[2].toInt() and 0xFF shl 24)
                or (this[3].toInt() and 0xFF shl 16)
                or (this[0].toInt() and 0xFF shl 8)
                or (this[1].toInt() and 0xFF))
        2 -> ((this[0].toInt() and 0xFF shl 8)
                or (this[1].toInt() and 0xFF))
        1 -> this[0].toInt()
        else -> 0
    }
}

/**
 * 获得此数组的crc16数组
 * @return crc16
 */
fun ByteArray.getCrc16(): ByteArray {
    return Crc16Utils.crc16ByteArray(this)
}

/**
 * 为此数组的尾部添加crc16数组
 * @return bytes + crc16
 */
fun ByteArray.addCrc16(): ByteArray {
    return this + getCrc16()
}

/**
 * 将此数组转为Hex
 * @param sep 间隔符
 * @return Hex
 */
fun ByteArray.hexString(sep: String = " "): String {
    return HexUtils.bytesToHexString(this, sep)
}

/**
 * @param target  所需寻找的数组
 * @return 需要寻找的数组所在的位置，没有返回-1
 */
fun ByteArray.indexOfArray(target: ByteArray): Int {
    var start = 0
    when {
        target.isEmpty() -> return -1
        target.size > this.size -> return -1
        else -> while (true) {
            while (start < this.size && this[start] != target[0]) {
                ++start
            }

            if (start == this.size || target.size + start > this.size) {
                return -1
            }

            var o1 = start

            var o2 = 0
            while (o1 < this.size && o2 < target.size && this[o1] == target[o2]) {
                ++o1
                ++o2
            }

            if (o2 == target.size) {
                return start
            }

            ++start
        }
    }
}