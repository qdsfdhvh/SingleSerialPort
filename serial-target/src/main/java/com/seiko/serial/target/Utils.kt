package com.seiko.serial.target

import com.seiko.serial.modbus.addCrc16
import com.seiko.serial.modbus.modBusByteArray

object Utils {

    private val baudRates = listOf(
            0, 50, 75, 110, 134, 150, 200, 300, 600, 1200, 1800, 2400, 4800, 9600,
            19200, 38400, 57600, 115200, 187500, 192000, 230400, 460800, 500000, 576000,
            921600, 1000000, 1152000, 1500000, 2000000, 2500000, 3000000, 3500000, 4000000
    )

    fun isSafeBaudRate(num: Int): Boolean {
        return baudRates.contains(num)
    }

    /**
     * 生成03读取指令
     * @deviceId 从机地址
     * @start 起始地址
     * @end   结束地址
     */
    fun bind03CmdWithDim(deviceId: Byte, start: Int, end: Int): ByteArray {
        return bind03Cmd(deviceId, start, Math.max(start, end) - start + 1)
    }

    /**
     * 生成03读取指令
     * @deviceId 从机地址
     * @address 起始地址
     * @len 读取长度
     */
    fun bind03Cmd(deviceId: Byte, address: Int, len: Int): ByteArray {
        // 从机地址、指令
        val hex1 = byteArrayOf(deviceId, 3)
        // 起始地址
        val hex2 = address.modBusByteArray()
        // 读取长度
        val hex3 = len.modBusByteArray()
        return (hex1 + hex2 + hex3).addCrc16()
    }

    /**
     * 生成06单地址写入指令
     * @deviceId 从机地址
     * @address 地址
     * @num 写入的数据，int型10进制
     */
    fun bind06Cmd(deviceId: Byte, address: Int, num: Int): ByteArray {
        return bind06Cmd(deviceId, address, num.modBusByteArray())
    }

    /**
     * 生成06单地址写入指令
     * @deviceId 从机地址
     * @address 起始地址
     * @bytes 写入的数据，长度为2
     */
    fun bind06Cmd(deviceId: Byte, address: Int, bytes: ByteArray): ByteArray {
        // 从机地址、指令
        val hex1 = byteArrayOf(deviceId, 6)
        // 地址
        val hex2 = address.modBusByteArray()
        return (hex1 + hex2 + bytes).addCrc16()
    }

    /**
     * 生成16写入指令
     * @deviceId 从机地址
     * @address 起始地址
     * @bytes 写入的数据，不限长度
     */
    fun bind16Cmd(deviceId: Byte, address: Int, bytes: ByteArray): ByteArray {
        // 从机地址、指令
        val hex1 = byteArrayOf(deviceId, 16)
        // 起始地址
        val hex2 = address.modBusByteArray()
        // 地址长度
        val hex3 = (bytes.size / 2).modBusByteArray()
        // 数据长度
        val hex4 = (bytes.size).modBusByteArray()
        return (hex1 + hex2 + hex3 + hex4 + bytes).addCrc16()
    }

    /**
     * 生成05写入开关指令
     * @deviceId 从机地址
     * @address 起始地址
     * @bool true合 false分
     */
    fun bind05Cmd(deviceId: Byte, address: Int, bool: Boolean): ByteArray {
        // 从机地址、指令
        val hex1 = byteArrayOf(deviceId, 5)
        // 起始地址
        val hex2 = address.modBusByteArray()
        // 合: FF 00、关：00 00
        val hex3 = if (bool) byteArrayOf(-1, 0) else byteArrayOf(0, 0)
        return (hex1 + hex2 + hex3).addCrc16()
    }

    /**
     * 生成15写入开关指令
     * @deviceId 从机地址
     * @address 起始地址
     * @bools 集合 true合 false分
     */
    fun bind15Cmd(deviceId: Byte, address: Int, bools: BooleanArray): ByteArray {
        // 从机地址、指令
        val hex1 = byteArrayOf(deviceId, 15)
        // 起始地址
        val hex2 = address.modBusByteArray()
        // 读取长度
        val hex3 = bools.size.modBusByteArray()
        // 写入数据 8个一组
        val list = bools.split(8)
        val array = ByteArray(list.size * 2)

        var num: Int
        list.forEachIndexed { i, boolOf8Size ->
            num = 0
            // 0000 0011 2^1 2^0
            boolOf8Size.forEachIndexed { index, bool ->
                if (bool) {
                    num += 1 shl index
                }
            }
            array[2 * i] = (num shr 8 and 0xFF).toByte()
            array[2 * i + 1] = (num and 0xFF).toByte()
        }
        return (hex1 + hex2 + hex3 + array).addCrc16()
    }

    /**
     * 生成01读取指令
     * @deviceId 从机地址
     * @address 起始地址
     * @len 读取长度
     */
    fun bind01Cmd(deviceId: Byte, address: Int, len: Int): ByteArray {
        // 从机地址、指令
        val hex1 = byteArrayOf(deviceId, 1)
        // 起始地址
        val hex2 = address.modBusByteArray()
        // 读取长度
        val hex3 = len.modBusByteArray()
        return (hex1 + hex2 + hex3).addCrc16()
    }

    /**
     * 生成02读取指令
     * @deviceId 从机地址
     * @address 起始地址
     * @len 读取长度
     */
    fun bind02Cmd(deviceId: Byte, address: Int, len: Int): ByteArray {
        // 从机地址、指令
        val hex1 = byteArrayOf(deviceId, 2)
        // 起始地址
        val hex2 = address.modBusByteArray()
        // 读取长度
        val hex3 = len.modBusByteArray()
        return (hex1 + hex2 + hex3).addCrc16()
    }

    /**
     * @param subSize 分割的块大小
     */
    private fun BooleanArray.split(subSize: Int): List<List<Boolean>> {
        val count = if (size % subSize == 0) size / subSize else size / subSize + 1

        val subArrayList = ArrayList<List<Boolean>>(count)

        var index: Int
        for (i in 0 until count) {
            index = i * subSize
            val list = ArrayList<Boolean>(subSize)
            var j = 0
            while (j++ < subSize && index < size) {
                list.add(get(index++))
            }
            subArrayList.add(list)
        }
        return subArrayList
    }
}
