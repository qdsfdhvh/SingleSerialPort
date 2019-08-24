package com.seiko.serial.target

data class SerialMsg(
    var module: SerialModule,
    var bytes: ByteArray
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

    private var next: SerialMsg? = null

    private var use = false

    fun recycle() {
        if (use) return
        recycleUnchecked()
    }

    private fun recycleUnchecked() {
        use = false

        // 重置所有属性

        synchronized(sPoolSync) {
            if (sPoolSize < MAX_POOL_SIZE) {
                next = sPool
                sPool = this
                sPoolSize++
//                Log.d("SerialMsg", "回收， 缓存池=$sPoolSize")
            }
        }
    }

    companion object {

        private val sPoolSync = Any()
        private var sPool: SerialMsg? = null
        private var sPoolSize = 0

        private const val MAX_POOL_SIZE = 50   // 缓存池大小

        fun obtain(module: SerialModule, bytes: ByteArray): SerialMsg {
            synchronized(sPoolSync) {
                if (sPool != null) {
                    val m = sPool!!
                    sPool = m.next
                    m.next = null
                    m.use = false
                    sPoolSize--
//                    Log.d("SerialMsg", "缓存池取出，缓存池=$sPoolSize")
                    m.module = module
                    m.bytes = bytes
                    return m
                }
            }
//            Log.d("SerialMsg", "创建新的对象，缓冲池 = $sPoolSize")
            return SerialMsg(module, bytes)
        }
    }
}