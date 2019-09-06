package com.seiko.serial.modbus

/**
 * 将字节数组装维相应的int
 */
fun ByteArray.toModBusInt(): Int {
    return when(size) {
        4 -> ((this[2].toPositiveInt() shl 24)
                or (this[3].toPositiveInt() shl 16)
                or (this[0].toPositiveInt() shl 8)
                or (this[1].toPositiveInt()))
        2 -> ((this[0].toPositiveInt() shl 8)
                or (this[1].toPositiveInt()))
        1 -> this[0].toInt()
        else -> 0
    }
}

/**
 * 获得此数组的crc16数值
 * @return crc16数值
 */
fun ByteArray.getCrc16Short(): Short {
    var crcReg = 0xFFFF
    for (d in this) {
        crcReg = (crcReg shr 8) xor (CRC16_TABLE[(crcReg xor d.toInt()) and 0xFF])
    }
    return crcReg.toShort()
}

/**
 * 获得此数组的crc16数组
 * @return crc16数组
 */
fun ByteArray.getCrc16(): ByteArray {
    val crcReg = getCrc16Short()
    return byteArrayOf(
        (crcReg.toPositiveInt().toByte()),
        (crcReg.toPositiveInt() shr 8).toByte())
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
 * @param toLowerCase 是否小写
 * @return Hex
 */
fun ByteArray.toHexString(sep: Char = ' ', toLowerCase: Boolean = true): String {
    return encodeHex(this, sep, if (toLowerCase) HEX_DIGIT_LOWER_CHARS else HEX_DIGIT_UPPER_CHARS)
}

/**
 * 将数组转为Hex
 * @param bytes 需要转换的字节
 * @param sep 分隔符
 * @param toDigits HEX字典
 * @return HEX
 */
private fun encodeHex(bytes: ByteArray, sep: Char, toDigits: CharArray): String {
    val result: CharArray
    if (sep == '\u0000') { //空字符
        result = CharArray(bytes.size * 2)
        var c = 0
        for (b in bytes) {
            result[c++] = toDigits[b shr 4 and 0xf]
            result[c++] = toDigits[b       and 0xf]
        }
    } else {
        val resultSize = bytes.size * 3 - 1
        result = CharArray( resultSize)
        var c = 0
        for (b in bytes) {
            result[c++] = toDigits[b shr 4 and 0xf]
            result[c++] = toDigits[b       and 0xf]
            if (c == resultSize) {
                break
            } else {
                result[c++] = sep
            }
        }
    }
    return String(result)
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