package com.seiko.serial

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.seiko.serial.modbus.modBusByteArray
import com.seiko.serial.rs232.Rs232SerialPort
import com.seiko.serial.rs232.SerialPath
import com.seiko.serial.target.reactive.data.BoxIntArray
import com.seiko.serial.target.reactive.data.BoxIntValue
import com.seiko.serial.target.reactive.observable
import com.seiko.serial.target.target
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import java.util.*

class MainActivity : AppCompatActivity() {

    private val target = Rs232SerialPort(SerialPath.ttyS2, 115200).target(debug = true)
    private val button = ButtonModule()

    private val disposables = CompositeDisposable()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        target.addSerialModule(button)
        target.start()

        BoxIntArray(123123, 2).observable(target)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ array ->
                Log.d("MainActivity", Arrays.toString(array))
            }, { error ->
                Log.d("MainActivity", "Warn.", error)
            })
            .addToDisposables()

        BoxIntValue(23451).observable(target, postTime = 100)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ value ->
                Log.d("MainActivity", value.toString())
            }, { error ->
                Log.d("MainActivity", "Warn.", error)
            })
            .addToDisposables()

        button.pull(12343, true)
        button.pull(12234, 233.modBusByteArray(2))
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
