package com.seiko.serial.target.data

import android.util.Log
import com.seiko.serial.modbus.toBinary
import com.seiko.serial.modbus.toHexString
import com.seiko.serial.target.Utils

/**
 * M地址的连续读取
 * PS: M地址不同于ModBus的地址，不存在一个数据分成多个字节。
 */
private const val TAG = "MBoxIntValue"

open class MBoxIntValue(address: Int, len: Int = 1) : BoxIntValue(BoxIntParam(address, 1, len)) {

    override fun filter(bytes: ByteArray): Boolean {
        val bool = bytes[1].toInt() == 1 && bytes[2].toInt() and 0xFF == 1
        if (debug) {
            Log.d(TAG, "Filter(${bytes.toHexString()}) = $bool.")
        }
        return bool
    }

    /**
     * @param bytes 完整的modBus指令
     */
    // [1, 1, 1, 48, 81, -100] num = [48], 00110000
    // [1, 1, 1, 0, 81, -120] num = [0]
    override fun decode(bytes: ByteArray): Boolean {
        val char = bytes[3].toInt().toBinary().last()
        value = if (char == '0') 0 else 1
        if (debug) {
            Log.d(TAG, "Decode Result:$value.")
        }
        return true
    }

    override fun bindPostCode(): ByteArray {
        return Utils.bind01Cmd(deviceId, data.address, 1)
    }

}