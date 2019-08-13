**USE**
````kotlin
val target = Rs232SerialPort(SerialPath.ttyS2, 115200).target()
target.start()

BoxIntArray(123123, 2).observable(target)
           .observeOn(AndroidSchedulers.mainThread())
           .subscribe { array ->
            //
           }
````