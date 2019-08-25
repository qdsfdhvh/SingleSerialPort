package com.seiko.serial

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.seiko.serial.core.SerialPort
import com.seiko.serial.modbus.modBusByteArray
import com.seiko.serial.rs232.RS232SerialPort
import com.seiko.serial.rs232.SerialPortPath
import com.seiko.serial.target.SerialTarget
import com.seiko.serial.target.reactive.data.BoxIntArray
import com.seiko.serial.target.reactive.data.BoxIntValue
import com.seiko.serial.target.reactive.toObservable
import com.seiko.serial.target.reactive.toSingle
import com.seiko.serial.target.toTarget
import com.seiko.serial.usb.UsbSerialPort
import com.seiko.serial.usb.UsbSerialService
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var serial: SerialPort
    private lateinit var target: SerialTarget

    private val disposables = CompositeDisposable()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    /**
     * 打开Rs232串口
     */
    private fun openRs232Serial() {
        serial = RS232SerialPort(SerialPortPath.ttyS3, 9600)
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
     * 对串口通讯的封装
     */
    private fun openTarget() {
        target = serial.toTarget()
        target.start()

        //一段地址连续读取，线圈用MBoxIntArray
        BoxIntArray(address = 123, num = 2, len = 2, sep = 4).toObservable(target)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { intArray -> Log.d(TAG, Arrays.toString(intArray)) }
            .addToDisposables()

        // 单个地址连续读取，线圈用MBoxIntValue
        BoxIntValue(address = 123, len = 2).toObservable(target, postTime = 100)
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
        button.pull(12343, true)
        button.pull(12234, 233.modBusByteArray(2))
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
