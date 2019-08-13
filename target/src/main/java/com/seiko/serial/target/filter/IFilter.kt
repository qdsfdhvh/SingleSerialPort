package com.seiko.serial.target.filter

interface IFilter {

    fun isSafe(bytes: ByteArray): Boolean

}