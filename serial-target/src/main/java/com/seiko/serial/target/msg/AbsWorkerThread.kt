package com.seiko.serial.target.msg

/**
 * github [https://github.com/felHR85/UsbSerial/blob/master/usbserial/src/main/java/com/felhr/usbserial/AbstractWorkerThread.java]
 */
internal abstract class AbsWorkerThread : Thread() {
    //    boolean firstTime = true;
    @Volatile
    private var keep = true
    @Volatile
    private var workingThread: Thread? = null

    open fun stopThread() {
        keep = false
        if (this.workingThread != null) {
            this.workingThread!!.interrupt()
        }
    }

    override fun run() {
        if (!this.keep) {
            return
        }
        this.workingThread = Thread.currentThread()
        while (this.keep && !this.workingThread!!.isInterrupted) {
            doRun()
        }
    }

    abstract fun doRun()
}