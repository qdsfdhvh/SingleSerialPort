package com.seiko.serial

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.seiko.serial.core.SerialPort
import com.seiko.serial.rs232.RS232SerialPort
import com.seiko.serial.rs232.SerialPortPath
import com.seiko.serial.target.SerialTarget
import com.seiko.serial.target.data.BoxIntArray
import com.seiko.serial.target.data.BoxIntValue
import com.seiko.serial.target.toObservable
import com.seiko.serial.target.toSingle
import com.seiko.serial.target.toTarget
import com.seiko.serial.tcp.TcpSerialPort
import com.seiko.serial.usb.UsbSerialPort
import com.seiko.serial.usb.UsbSerialService
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.disposables.Disposable
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var serial: SerialPort
    private lateinit var target: SerialTarget

    private val disposables = CompositeDisposable()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        SerialTarget.IS_DEBUG = true
        openTarget()

    }

    /**
     * 打开Rs232串口
     */
    private fun openRs232Serial() {
        serial = RS232SerialPort(SerialPortPath.ttyS2, 115200)
        serial.open(object : SerialPort.Callback {
            override fun onSuccess() {

            }

            override fun onResult(bytes: ByteArray) {
                // in work thread
            }

            override fun onError(e: Throwable) {

            }
        })
        serial.send(byteArrayOf(1, 2, 3))
    }

    /**
     * 尝试开启可用的usb串口设备
     */
    private fun openUsbSerialAuto() {
        // 过滤特殊的usb设备
        UsbSerialService.addFilterDevice(1000,  2000)

        serial = UsbSerialPort(9600)
        serial.open(object : SerialPort.Callback {
            override fun onSuccess() {

            }

            override fun onResult(bytes: ByteArray) {
                // in work thread
            }

            override fun onError(e: Throwable) {

            }
        })
        serial.send(byteArrayOf(1, 2, 3))
    }

    /**
     * 打开指定的usb串口设备
     */
    private fun openUsbSerial() {
        serial = UsbSerialPort(9600, vid = 1000, pid = 2000)
        serial.open(object : SerialPort.Callback {
            override fun onSuccess() {

            }

            override fun onResult(bytes: ByteArray) {
                // in work thread
            }

            override fun onError(e: Throwable) {

            }
        })
        serial.send(byteArrayOf(1, 2, 3))
    }

    /**
     * 打开Rs232串口
     */
    private fun openTcpSerial() {
        serial = TcpSerialPort("192.168.1.1", 8080)
        serial.open(object : SerialPort.Callback {
            override fun onSuccess() {

            }

            override fun onResult(bytes: ByteArray) {
                // in work thread
            }

            override fun onError(e: Throwable) {

            }
        })
        serial.send(byteArrayOf(1, 2, 3))
    }

    /**
     * 对串口通讯的封装
     */
    private fun openTarget() {
        serial = RS232SerialPort(SerialPortPath.ttyS2, 115200)
        target = serial.toTarget()
        target.start()

        //一段地址连续读取，线圈用MBoxIntArray
        BoxIntArray(address = 1602, num = 6, len = 2, sep = 2)
            .toObservable(target, postTime = 500, debug = false)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { intArray -> Log.d(TAG, Arrays.toString(intArray)) }
            .addToDisposables()

        // 单个地址连续读取，线圈用MBoxIntValue
        BoxIntValue(address = 1632, len = 2)
            .toObservable(target, postTime = 1000, debug = false)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { intValue -> Log.d(TAG, intValue.toString()) }
            .addToDisposables()

        // 一段地址单次读取
        BoxIntArray(address = 132, num = 2, len = 2, sep = 4).toSingle(target)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { intArray -> Log.d(TAG, Arrays.toString(intArray)) }
            .addToDisposables()

        // 单个地址单次读取
        BoxIntValue(address = 123, len = 2).toSingle(target)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { intValue -> Log.d(TAG, intValue.toString())  }
            .addToDisposables()

        // 写入地址
        val button = ButtonModule()
        target.addSerialModule(button)
//        button.pull(12343, true)
        //[1, 16, -93, -95, 0, 5, 10, 0, -56, 0, -56, 0, 100, 0, 120, 0, 0, -51, 56]
        //[1, 16, -93, -95, 0, 5, 0, 10, 0, -56, 0, -56, 0, 100, 0, 120, 0, 0, -42, 41]
        button.pull(byteArrayOf(1, 16, -93, -95, 0, 5, 10, 0, -56, 0, -56, 0, 100, 0, 120, 0, 0, -51, 56))
//        button.pull(12234, 233.toModBusByteArray(2))
        // 01 10 a3 a1 00 05 00 0a 00 c8 00 c8 00 64 00 78 00 00 d6 29
        // 01 90 03 0c 01
    }

    override fun onDestroy() {
        disposables.clear()
        target.close()
        serial.close()
        super.onDestroy()
    }

    private fun Disposable.addToDisposables() {
        disposables.add(this)
    }

    companion object {
        private const val TAG = "MainActivity"
    }
}
