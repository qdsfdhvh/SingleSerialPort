package com.seiko.serial.target.reactive.data

import android.util.Log
import com.seiko.serial.modbus.hexString
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
        val bool =  bytes[1].toInt() == 3 && bytes[2].toInt() and 0xFF == bytesLen
        if (isDebug) {
            Log.d(TAG, "Filter(${bytes.hexString()}) = $bool.")
        }
        return bool
    }

    /**
     * @param bytes 完整的modBus指令
     */
    open fun decode(bytes: ByteArray): Boolean {
        for (i in 0 until data.num) {
            array[i] = bytes.copyOfRange(3 + data.sep * 2 * i,
                3 + data.sep * 2 * i + data.len * 2).modBusInt()
        }
        if (isDebug) {
            Log.d(TAG, "Decode Result:${array.contentToString()}.")
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
        if(isDebug) Log.d(TAG, "Update Address：$address.")
    }

    /**
     * 更新数量
     */
    open fun updateNum(num: Int) {
        if (isEqual(num)) return
        data.num = num
        notifyChange()
        bytesLen = data.addressLen() * 2
        if (isDebug) Log.d(TAG, "Update Num: $num.")
    }

    /**
     * 更新发送的指令与放入的Array
     */
    private fun notifyChange() {
        array = bindIntArray()
        post = bindPostCode()
        if (isDebug) Log.d(TAG, "Notify PostArray: ${post.hexString()}.")
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

    companion object {
        private const val TAG = "BoxIntArray"
    }


}