package com.seiko.serial.rs232;

/**
 * github [https://github.com/felHR85/UsbSerial/blob/master/usbserial/src/main/java/com/felhr/usbserial/AbstractWorkerThread.java]
 */
abstract class AbsWorkerThread extends Thread {
//    boolean firstTime = true;
    private volatile boolean keep = true;
    private volatile Thread workingThread;

    public void stopThread() {
        keep = false;
        if (this.workingThread != null) {
            this.workingThread.interrupt();
        }
    }

    public final void run() {
        if (!this.keep) {
            return;
        }
        this.workingThread = Thread.currentThread();
        while (this.keep && (!this.workingThread.isInterrupted())) {
            doRun();
        }
    }

    abstract void doRun();
}