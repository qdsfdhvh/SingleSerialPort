package com.seiko.serial.target.reactive.data

import com.seiko.serial.modbus.modBusInt
import com.seiko.serial.target.Utils

/**
 * ModBus地址连续读取
 */
open class BoxIntArray(protected val data: BoxIntParam) {

    constructor(address: Int, num: Int, len: Int = 2, sep: Int = len): this(BoxIntParam(address, num, len, sep))

    /**
     * 设备Id，充绒机的plc地址基本都是1，暂时不用适配
     */
    protected var deviceId: Byte = 1

    /**
     * 数据集合
     */
    protected lateinit var array: IntArray
    protected lateinit var post: ByteArray

    private var bytesLen: Int = data.addressLen() * 2

    init {
        notifyChange()
    }

    /**
     * @param bytes 过滤的指令
     */
    open fun filter(bytes: ByteArray): Boolean {
        return bytes[1].toInt() == 3
                && bytes[2].toInt() and 0xFF == bytesLen
    }

    /**
     * @param bytes 完整的modBus指令
     */
    open fun decode(bytes: ByteArray): Boolean {
        for (i in 0 until data.num) {
            array[i] = bytes.copyOfRange(3 + data.sep * 2 * i,
                3 + data.sep * 2 * i + data.len * 2).modBusInt()
        }
        return true
    }

    /**
     * 读取字节所发送的指令
     */
    fun readByte() = post

    /**
     * 获取当前数据
     */
    fun getDecodeArray() = array

    /**
     * 获取当前起始地址
     */
    open fun getStartAddress() = data.address

//    /**
//     * 字节长度
//     */
//    open fun getLen() = data.addressLen() * 2

    /**
     * 是否相同
     */
    open fun isEqual(num: Int) = data.num == num

//    /**
//     * 指令
//     */
//    open fun postCode() = 3

    /**
     * 更新起始地址
     */
    open fun updateAddress(address: Int) {
        if (data.address == address) return
        data.address = address
        post = Utils.bind03Cmd(deviceId, address, data.addressLen())

//        if(isDebug) Timber.d("更新地址：$address")
    }

    /**
     * 更新数量
     */
    open fun updateNum(num: Int) {
        if (isEqual(num)) return
        data.num = num
        notifyChange()
        bytesLen = data.addressLen() * 2
    }

    /**
     * 更新发送的指令与放入的Array
     */
    private fun notifyChange() {
        array = bindIntArray()
        post = bindPostCode()
    }

    protected open fun bindIntArray(): IntArray {
        return IntArray(data.num)
    }

    protected open fun bindPostCode(): ByteArray {
        return Utils.bind03Cmd(deviceId, data.address, data.addressLen())
    }


    /**
     * 扩展参数
     */
    var isDebug = false            // 是否调试


}