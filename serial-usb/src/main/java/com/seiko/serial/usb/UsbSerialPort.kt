package com.seiko.serial.usb


import com.seiko.serial.core.SerialPort
import com.seiko.serial.core.SerialSetting

/**
 * Usb串口
 */
class UsbSerialPort(private val setting: SerialSetting) : SerialPort {

    constructor(baudRate: Int): this(SerialSetting(baudRate))

    private val core: UsbSerialBox = UsbSerialBox.getInstance()

    /**
     * 异步
     */
    override fun open(callback: SerialPort.Callback) {
        core.startBindServiceAndDone { binder -> binder.open(setting, callback) }
    }

    override fun close() {
        core.startBindServiceAndDone { binder -> binder.close() }
    }

    override fun send(bytes: ByteArray) {
        core.startBindServiceAndDone { binder -> binder.send(bytes) }
    }

    override fun setBaudRate(baudRate: Int) {
        core.startBindServiceAndDone { binder -> binder.setBaudRate(baudRate) }
    }

}
