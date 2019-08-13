package com.seiko.serial.target.reactive.data

import com.seiko.serial.target.Utils

/**
 * M地址的连续读取
 * PS: M地址不同于ModBus的地址，不存在一个数据分成多个字节。
 */
open class MBoxIntValue(address: Int) : BoxIntValue(
    BoxIntParam(
        address,
        1,
        1
    )
) {

//    private var bytesLen: Int = if (data.addressLen() % 8 == 0) data.addressLen() / 8 else data.addressLen() / 8 + 1


    override fun filter(bytes: ByteArray): Boolean {
//        if (isDebug) Log.d("Filter:${Arrays.toString(bytes)}")
        return bytes[1].toInt() == 1 && bytes[2].toInt() and 0xFF == 1
    }

    /**
     * @param bytes 完整的modBus指令
     */
    // [1, 1, 1, 48, 81, -100] num = [48], 00110000
    // [1, 1, 1, 0, 81, -120] num = [0]
    override fun decode(bytes: ByteArray): Boolean {
//        if (isDebug) Timber.d("Decode-Bytes:${Arrays.toString(bytes)}")
        val char = bytes[3].toInt().toBinary().last()
        value = if (char == '0') 0 else 1
        return true
    }

    override fun bindPostCode(): ByteArray {
        return Utils.bind01Cmd(deviceId, data.address, 1)
    }

    private fun Int.toBinary(): String {
        return Integer.toBinaryString((this and 0xFF) + 0x100).substring(1)
    }

}