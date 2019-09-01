package com.seiko.serial.target.msg

import com.seiko.serial.target.SerialModule

data class SerialMsg(
    var module: SerialModule = EMPTY_MODULE,
    var bytes: ByteArray = ByteArray(0)
): Comparable<SerialMsg> {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SerialMsg

        if (module != other.module) return false
        if (!bytes.contentEquals(other.bytes)) return false
        return true
    }

    override fun hashCode(): Int {
        var result = module.hashCode()
        result = 31 * result + bytes.contentHashCode()
        return result
    }

    /**
     * 优先级大-1, 优先级小1
     */
    override fun compareTo(other: SerialMsg): Int {
        return if (this.module.getPriority() < other.module.getPriority()) -1 else 1
    }

    /*******************************************
     *                 缓存池                   *
     *******************************************/

    var next: SerialMsg? = null

    fun recycle() {
        SerialMsgPool.recycle(this)
    }

    companion object {

        fun obtain(module: SerialModule, bytes: ByteArray): SerialMsg {
            return SerialMsgPool.take().apply {
                this.module = module
                this.bytes = bytes
            }
        }

        private val EMPTY_MODULE = object : SerialModule {
            override fun attach(target: SerialModule.Target?) {}

            override fun accept(bytes: ByteArray): Boolean = true

            override fun getPriority(): Int = 99

            override fun getTag(): String = "EMPTY_MODULE"
        }
    }
}