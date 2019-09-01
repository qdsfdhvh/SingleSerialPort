package com.seiko.serial.target

interface SerialModule {

    /**
     * 绑定设备
     */
    fun attach(target: Target?)

    /**
     * 接收字节
     */
    fun accept(bytes: ByteArray): Boolean

    /**
     * 优先级
     */
    fun getPriority(): Int

    /**
     * 标记
     */
    fun getTag(): String

    /**
     * 串口设备
     */
    interface Target {

        /**
         * 发送指令
         */
        fun send(device: SerialModule, bytes: ByteArray)

        /**
         * 开始
         */
        fun start()

        /**
         * 停止
         */
        fun close()

        /**
         * 添加Module
         */
        fun addSerialModule(module: SerialModule): Boolean

        /**
         * 删除Module
         */
        fun delSerialModule(module: SerialModule): Boolean

    }

}