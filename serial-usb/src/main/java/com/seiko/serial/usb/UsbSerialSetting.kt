package com.seiko.serial.usb

import com.seiko.serial.core.SerialSetting

data class UsbSerialSetting(
    val setting: SerialSetting,
    val vid: Int,
    val pid: Int,
    val key: String)