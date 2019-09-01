package com.seiko.serial.target.decode

interface IDecode {

    fun check(bytes: ByteArray): ByteArray

    fun bytesOfSend(bytes: ByteArray)

}