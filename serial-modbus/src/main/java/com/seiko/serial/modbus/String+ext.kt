package com.seiko.serial.modbus

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

fun main() {
    val bytes = byteArrayOf(1, 23, 45, -12, -45)
    println(bytes.toHexString('\u0000'))
}