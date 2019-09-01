package com.seiko.serial.target.data

import android.util.Log
import com.seiko.serial.modbus.toHexString
import com.seiko.serial.modbus.toModBusInt
import com.seiko.serial.target.Utils

/**
 * ModBus单个地址读取
 */
private const val TAG = "BoxIntValue"

open class BoxIntValue(protected val data: BoxIntParam) {

    constructor(address: Int, len: Int = 1): this(BoxIntParam(address, 1, len, len))

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
        val bool = bytes[1].toInt() == 3 && bytes[2].toInt() and 0xFF == bytesLen
        if (debug) {
            Log.d(TAG, "Filter(${bytes.toHexString()}) = $bool.")
        }
        return bool
    }

    /**
     * @param bytes 完整的modBus指令
     */
    open fun decode(bytes: ByteArray): Boolean {
        value = bytes.copyOfRange(3, 3 + data.len * 2).toModBusInt()
        if (debug) {
            Log.d(TAG, "Decode Result:$value.")
        }
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
        if(debug) Log.d(TAG, "Update Address：$address.")
    }

    /**
     * 更新发送的指令与放入的Array
     */
    private fun notifyChange() {
        value = 0
        post = bindPostCode()
        if (debug) Log.d(TAG, "Notify PostArray: ${post.toHexString()}.")
    }

    protected open fun bindPostCode(): ByteArray {
        return Utils.bind03Cmd(deviceId, data.address, data.addressLen())
    }

    /**
     * 扩展参数
     */
    var debug = false            // 是否调试
    var priority = 99            // 优先级

}