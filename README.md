Under Construction...

**USE RS232**
````kotlin
val serial = Rs232SerialPort(SerialPath.ttyS3, 9600)
serial.open(object : SerialPort.Callback {
    override fun onSuccess() {
        
    }

    override fun onResult(bytes: ByteArray) {
        
    }

    override fun onError(e: Throwable) {
        
    }
})
serial.send(byteArrayOf(1, 2, 3))
````
**USB USB**
````kotlin
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
````

**USE Target**
````kotlin
val target = Rs232SerialPort(SerialPath.ttyS2, 115200).target()
target.start()

val address = 12345

BoxIntArray(address, num = 3, len = 2).observable(target)
           .observeOn(AndroidSchedulers.mainThread())
           .subscribe { array ->
            //[123, 345, 45]
           }

BoxIntValue(address, len = 2).observable(target)
           .observeOn(AndroidSchedulers.mainThread())
           .subscribe { value ->
            //[123]
           } 
// LIKE
val button = ButtonModule()
target.addSerialModule(button)

button.pull(address, true)
button.pull(address, 111.modBusByteArray(2))            
````