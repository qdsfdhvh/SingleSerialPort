package com.seiko.serial.target.decode

import io.reactivex.Observable

interface IDecode {

    fun check(bytes: ByteArray): ByteArray

    fun bytesOfSend(bytes: ByteArray)

}