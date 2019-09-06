package com.seiko.serial.modbus

fun String.toModBusByteArray(sep: Char = ' ', isLowerCase: Boolean = true): ByteArray {
    return encodeBytes(this, sep, if (isLowerCase) HEX_DIGIT_LOWER_CHARS else HEX_DIGIT_UPPER_CHARS)
}

private fun encodeBytes(hex: String, sep: Char, toDigits: CharArray): ByteArray {
    if (hex.isEmpty()) return ByteArray(0)

    val encoded = hex.replace(sep.toString(), "")
    val result = ByteArray(encoded.length / 2)
    for (i in encoded.indices step 2) {
        val firstIndex = toDigits.indexOf(encoded[i])
        val secondIndex = toDigits.indexOf(encoded[i + 1])
        val octet = firstIndex shl 4 or secondIndex
        result[i shr 1] = octet.toByte()
    }
    return result
}

fun String.getCrc16(sep: Char = ' ', isLowerCase: Boolean = true): String {
    val bytes = toModBusByteArray(sep, isLowerCase).getCrc16()
    return bytes.toHexString(sep, isLowerCase)
}

fun String.addCrc16(sep: Char = ' ', isLowerCase: Boolean = true): String {
    val bytes = toModBusByteArray(sep, isLowerCase).addCrc16()
    return bytes.toHexString(sep, isLowerCase)
}