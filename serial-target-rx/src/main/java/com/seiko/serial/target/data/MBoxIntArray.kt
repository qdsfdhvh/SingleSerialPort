package com.seiko.serial.target.data

import android.util.Log
import com.seiko.serial.modbus.toHexString
import com.seiko.serial.target.Utils
import kotlin.math.min

/**
 * M地址的连续读取
 */
open class MBoxIntArray(data: BoxIntParam): BoxIntArray(data) {

    constructor(address: Int, num: Int, len: Int = 2, sep: Int = len):
            this(BoxIntParam(address, num, len, sep))

    private var bytesLen: Int = if (data.addressLen() % 8 == 0)
        data.addressLen() / 8 else data.addressLen() / 8 + 1

    override fun filter(bytes: ByteArray): Boolean {
        val bool = bytes[1].toInt() == 1 && bytes[2].toInt() and 0xFF == bytesLen
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
        val bak = bytes.copyOfRange(3, 3 + bytesLen)
        val size = bak.size
        var i = 0
        while (i < size) {
            val str = bak[i].toInt().toBinary()
//            if (debug) Timber.d("Decode-${i}_Binary:$str")

            val len = str.length
            for (j in 0 until min(len, array.size - i * len)) {
                val char = str[len - 1 - j]  // 8 - 1 - j
                array[i * len + j] = if (char == '0') 0 else 1
//                if (debug) Timber.d("i = $i, j = $j, i * len + j = ${(i * len + j)}")
            }
            i++
        }

        if (debug) {
            Log.d(TAG, "Decode Result:${array.contentToString()}.")
        }
        return true
    }

    /**
     * 更新数量
     */
    override fun updateNum(num: Int) {
        if (isEqual(num)) return
        super.updateNum(num)
        bytesLen = (num * data.sep) / 8 + 1
    }

    override fun bindPostCode(): ByteArray {
        return Utils.bind01Cmd(deviceId, data.address, data.addressLen())
    }

    private fun Int.toBinary(): String {
        return Integer.toBinaryString((this and 0xFF) + 0x100).substring(1)
    }

    companion object {
        private const val TAG = "MBoxIntArray"
    }
}