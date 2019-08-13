package com.seiko.serial

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.seiko.serial.core.SerialPort
import com.seiko.serial.modbus.modBusByteArray
import com.seiko.serial.rs232.Rs232SerialPort
import com.seiko.serial.rs232.SerialPath
import com.seiko.serial.target.reactive.data.BoxIntArray
import com.seiko.serial.target.reactive.data.BoxIntValue
import com.seiko.serial.target.reactive.observable
import com.seiko.serial.target.target
import com.seiko.serial.usb.UsbSerialPort
import com.seiko.serial.usb.UsbSerialService
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import java.util.*

class MainActivity : AppCompatActivity() {

    private val target = Rs232SerialPort(SerialPath.ttyS2, 115200).target(debug = true)

    private val disposables = CompositeDisposable()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        target.start()

        BoxIntArray(123123, 2).observable(target)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ array ->
                Log.d("MainActivity", Arrays.toString(array))
            }, { error ->
                Log.d("MainActivity", "Warn.", error)
            })
            .addToDisposables()

        BoxIntValue(23451, len = 2).observable(target, postTime = 100)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ value ->
                Log.d("MainActivity", value.toString())
            }, { error ->
                Log.d("MainActivity", "Warn.", error)
            })
            .addToDisposables()

        val button = ButtonModule()
        target.addSerialModule(button)

        button.pull(12343, true)
        button.pull(12234, 233.modBusByteArray(2))
    }

    private fun useRs232() {
        val serial = Rs232SerialPort(SerialPath.ttyS3, 9600)
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

    private fun useUsb() {
        UsbSerialService.addFilterDevice(11,  22)
        val serial = UsbSerialPort(9600)
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

    override fun onDestroy() {
        disposables.clear()
        target.close()
        super.onDestroy()
    }

    private fun Disposable.addToDisposables() {
        disposables.add(this)
    }
}
