package com.seiko.serial.target.filter

import com.seiko.serial.modbus.getCrc16
import java.util.*

/**
 * Crc16校验
 */
class Crc16Filter: IFilter {

    override fun isSafe(bytes: ByteArray): Boolean {
        val len = bytes.size
        if (len < 4) return false

        val sp1 = bytes.copyOfRange(len - 2, len)
        val sp2 = bytes.copyOfRange(0, len - 2).getCrc16()
        return sp1.contentEquals(sp2)
    }

}