[![Build Status](https://travis-ci.org/YoKeyword/Fragmentation.svg?branch=master)](https://travis-ci.org/YoKeyword/Fragmentation)
[![Download](https://api.bintray.com/packages/qdsfdhvh/maven/SingleSerialPort/images/download.svg)](https://bintray.com/qdsfdhvh/maven/SingleSerialPort/_latestVersion)
[![Hex.pm](https://img.shields.io/hexpm/l/plug.svg)](https://www.apache.org/licenses/LICENSE-2.0)

# 如何使用

**1. 项目下app的build.gradle中依赖：**
````gradle
// 使用RS232串口
implementation 'com.seiko.serial:serial-rs232:x.y.z'
// 使用USB串口
implementation 'com.seiko.serial:serial-usb:x.y.z'
// 使用Tcp
implementation 'com.seiko.serial:serial-tcp:x.y.z'
// 串口使用Rx封装
implementation 'com.seiko.serial:serial-target-rx:x.y.z'
// 字节处理
implementation 'com.seiko.serial:serial-modbus:x.y.z'
````

**2.实现方式**
````kotlin
var serial: SerialPort

/** RS232串口 **/
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
serial.close()

/** USB串口1：尝试开启可用的USB设备 **/
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
serial.close()

/** USB串口2：开启指定USB设备 **/
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
serial.close()

/** RS232串口 **/
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
serial.close()

/** Target使用 **/
target = serial.toTarget()
target.start()

//一段地址连续读取，线圈用MBoxIntArray
BoxIntArray(address = 123, num = 2, len = 2, sep = 4).toObservable(target)
    .observeOn(AndroidSchedulers.mainThread())
    .subscribe { intArray -> Log.d(TAG, Arrays.toString(intArray)) }

// 单个地址连续读取，线圈用MBoxIntValue
BoxIntValue(address = 123, len = 2).toObservable(target, postTime = 100)
    .observeOn(AndroidSchedulers.mainThread())
    .subscribe { intValue -> Log.d(TAG, intValue.toString()) }

// 一段地址单次读取
BoxIntArray(address = 132, num = 2, len = 2, sep = 4).toSingle(target)
    .observeOn(AndroidSchedulers.mainThread())
    .subscribe { intArray -> Log.d(TAG, Arrays.toString(intArray)) }

// 单个地址单次读取
BoxIntValue(address = 123, len = 2).toSingle(target)
    .observeOn(AndroidSchedulers.mainThread())
    .subscribe { intValue -> Log.d(TAG, intValue.toString())  }

// 写入地址
val button = ButtonModule()
target.addSerialModule(button)
button.pull(12343, true)
button.pull(12234, byteArrayOf(1, 2, 3))

target.close()

````

## LICENSE
````
Copyright 2019 Seiko

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
````
