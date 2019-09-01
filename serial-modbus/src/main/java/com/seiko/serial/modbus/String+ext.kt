package com.seiko.serial.modbus

import java.util.*

//fun String.toModBusByteArray(sep: Char = ' '): ByteArray {
//    if (this == "") return ByteArray(0)
//
//    val hexString = this.replace(sep.toString(), "").toUpperCase(Locale.US)
//    val length = hexString.length / 2
//    val hexChars = hexString.toCharArray()
//    val d = ByteArray(length)
//    for (i in 0 until length) {
//        val pos = i * 2
//        d[i] = (charToByte(hexChars[pos]) shl 4 or charToByte(hexChars[pos + 1])).toByte()
//    }
//    return d
//}

fun String.toModBusByteArray(sep: Char = ' '): ByteArray {
    if (this == "") return ByteArray(0)

    val encoded = this.replace(sep.toString(), "")
    val result = ByteArray(encoded.length / 2)
    for (i in encoded.indices step 2) {
        val firstIndex = HEX_DIGIT_CHARS.indexOf(encoded[i])
        val secondIndex = HEX_DIGIT_CHARS.indexOf(encoded[i + 1])
        val octet = firstIndex shl 4 or secondIndex
        result[i shr 1] = octet.toByte()
    }
    return result
}

fun String.getCrc16(sep: Char = ' '): String {
    val bytes = toModBusByteArray(sep).getCrc16()
    return bytes.toHexString(sep)
}

fun String.addCrc16(sep: Char = ' '): String {
    val bytes = toModBusByteArray(sep).addCrc16()
    return bytes.toHexString(sep)
}