package com.seiko.serial.usb


import com.seiko.serial.core.SerialPort
import com.seiko.serial.core.SerialSetting
import com.seiko.serial.usb.UsbSerialService.DEFAULT_CLIENTS

/**
 * Usb串口
 */
class UsbSerialPort(private val setting: UsbSerialSetting) : SerialPort {

    // DEFAULT_CLIENTS 会自动搜索可用的第一个USB串口
    constructor(setting: SerialSetting, vid: Int = - 1, pid: Int = -1) : this(UsbSerialSetting(
        setting, vid, pid,
        if (vid == -1 || pid == -1) DEFAULT_CLIENTS else "$vid-$pid"))

    constructor(baudRate: Int, vid: Int = -1, pid: Int = -1): this(SerialSetting(baudRate), vid, pid)

    private val core: UsbSerialBox = UsbSerialBox.getInstance()

    override fun open(callback: SerialPort.Callback) {
        core.startBindServiceAndDone { binder -> binder.open(setting, callback) }
    }

    override fun close() {
        core.startBindServiceAndDone { binder -> binder.close(setting.key) }
    }

    override fun send(bytes: ByteArray) {
        core.startBindServiceAndDone { binder -> binder.send(setting.key, bytes) }
    }

    override fun setBaudRate(baudRate: Int) {
        core.startBindServiceAndDone { binder -> binder.setBaudRate(setting.key, baudRate) }
    }

}
