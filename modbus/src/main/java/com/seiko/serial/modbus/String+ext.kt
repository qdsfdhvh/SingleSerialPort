package com.seiko.serial.modbus


fun String.modBusByteArray(sep: String = " "): ByteArray {
    val str = this.replace(sep, "")
    return HexUtils.hexStringToBytes(str)
}

fun String.getCrc16(sep: String = " "): String {
    val bytes = modBusByteArray(sep).getCrc16()
    return bytes.hexString(sep)
}

fun String.addCrc16(sep: String = " "): String {
    val bytes = modBusByteArray(sep).addCrc16()
    return bytes.hexString(sep)
}