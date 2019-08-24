package com.seiko.serial.rs232;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;

public abstract class AbsSerialPort {

    private FileDescriptor mFd;
    private FileInputStream mFileInputStream;
    private FileOutputStream mFileOutputStream;

    protected final FileDescriptor getMFd() {
        return this.mFd;
    }

    protected final void setMFd(FileDescriptor value) {
        this.mFd = value;
        if (value != null) {
            this.mFileInputStream = new FileInputStream(value);
            this.mFileOutputStream = new FileOutputStream(value);
        } else {
            this.mFileInputStream = null;
            this.mFileOutputStream = null;
        }
    }

    public InputStream getInputStream() {
        return mFileInputStream;
    }

    public OutputStream getOutputStream() {
        return mFileOutputStream;
    }

    /***********************************************
     *                  CHECK FILE                 *
     ***********************************************/

    protected final boolean chmod777(File file) {
        if (!file.exists()) return false;

        if (is777(file)) return true;

        try {
            // get root
            Process su = Runtime.getRuntime().exec("/system/bin/su");
            //set file to write and read
            String cmd = "chmod 777 ${file.absolutePath}\nexit\n";
            su.getOutputStream().write(cmd.getBytes(Charset.forName("UTF-8")));
            return 0 == su.waitFor() && is777(file);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    private boolean is777(File file) {
        return file.canRead() && file.canWrite();
    }

    /***********************************************
     *                    Native                   *
     ***********************************************/

    protected final native FileDescriptor deviceOpen(String path, int rate, int dataBit, int stopBit, int parity);

    protected final native void deviceClose();

    protected final native void deviceBaudRate(int rate);

    static {
        System.loadLibrary("serial_port");
    }
}
