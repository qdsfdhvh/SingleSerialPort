package com.seiko.serial.target.reactive.data

import com.seiko.serial.modbus.modBusInt
import com.seiko.serial.target.Utils

/**
 * ModBus单个地址读取
 */
open class BoxIntValue(protected val data: BoxIntParam) {

    constructor(address: Int, len: Int = 1): this(
        BoxIntParam(
            address,
            1,
            len,
            len
        )
    )

    /**
     * 设备Id，充绒机的plc地址基本都是1，暂时不用适配
     */
    var deviceId: Byte = 1

    /**
     * 数据集合
     */
    protected var value: Int = 0
    private lateinit var post: ByteArray

    private var bytesLen: Int = data.addressLen() * 2

    init {
        notifyChange()
    }

    /**
     * @param bytes 过滤的指令
     */
    open fun filter(bytes: ByteArray): Boolean {
        return bytes[1].toInt() == 3 && bytes[2].toInt() and 0xFF == bytesLen
    }

    /**
     * @param bytes 完整的modBus指令
     */
    open fun decode(bytes: ByteArray): Boolean {
//        if (isDebug) Log.d(Arrays.toString(bytes))
        value = bytes.copyOfRange(3, 3 + data.len * 2).modBusInt()
        return true
    }

    /**
     * 读取字节所发送的指令
     */
    open fun readByte() = post

    /**
     * 获取当前数据
     */
    open fun getDecodeValue() = value

    /**
     * 获取当前起始地址
     */
    open fun getStartAddress() = data.address

    /**
     * 是否相同
     */
    open fun isEqual(num: Int) = data.num == num

    /**
     * 更新起始地址
     */
    open fun updateAddress(address: Int) {
        if (data.address == address) return
        data.address = address
        post = Utils.bind03Cmd(deviceId, address, data.addressLen())

//        if(isDebug) Timber.d("更新地址：$address")
    }

//    /**
//     * 更新数量
//     */
//    open fun updateNum(num: Int) {
//        if (isEqual(num)) return
//        data.num = num
//        notifyChange()
//        bytesLen = data.addressLen() * 2
//    }

    /**
     * 更新发送的指令与放入的Array
     */
    private fun notifyChange() {
        value = 0
        post = bindPostCode()
    }

    protected open fun bindPostCode(): ByteArray {
        return Utils.bind03Cmd(deviceId, data.address, data.addressLen())
    }

    /**
     * 扩展参数
     */
    var isDebug = false            // 是否调试


}