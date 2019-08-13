package com.seiko.serial.core

interface SerialPort {

    /**
     * 开启串口
     * @param callback 回调接口
     */
    fun open(callback: Callback)

    /**
     * 关闭串口
     */
    fun close()

    /**
     * 发送字节数组
     * @param bytes 字节数组
     */
    fun send(bytes: ByteArray)

    /**
     * 修改波特率
     * @param baudRate 波特率
     */
    fun setBaudRate(baudRate: Int)

    /**
     * 回调
     */
    interface Callback {
        /**
         * 打开成功
         */
        fun onSuccess()

        /**
         * 接收的字节数组
         * @param bytes 字节数组
         */
        fun onResult(bytes: ByteArray)

        /**
         * 打开失败
         */
        fun onError(e: Throwable)
    }

}