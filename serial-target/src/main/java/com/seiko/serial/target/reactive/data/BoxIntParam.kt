package com.seiko.serial.target.reactive.data

/**
 * PS:这里的数据都是以地址长度为准，而不是字节长度， 一个地址 = 2个字节
 *
 * @param address 读取的起始地址
 * @param num 预期要读取多少数据
 * @param len 每个数据的地址长度  一般为 1、2、4
 * @param sep 每个数据的位置间隔  sep 必须大于 len
 */
data class BoxIntParam(
        var address: Int,
        var num: Int,
        val len: Int,
        val sep: Int = len) {

    /**
     * 所读取的地址长度
     */
    fun addressLen() = num * sep
}