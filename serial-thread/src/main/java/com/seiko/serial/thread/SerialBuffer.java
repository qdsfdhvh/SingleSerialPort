package com.seiko.serial.thread;

import java.io.EOFException;

import okio.Buffer;

public class SerialBuffer {
    public static final long DEFAULT_READ_BUFFER_SIZE = 16 * 1024;
    private static final int MAX_BULK_BUFFER = 16 * 1024;

    private Buffer readBuffer;
    private final SynchronizedBuffer writeBuffer;

    public SerialBuffer() {
        writeBuffer = new SynchronizedBuffer();
        readBuffer = new Buffer();
    }

    public Buffer getReadBuffer() {
        synchronized (this) {
            return readBuffer;
        }
    }

    public byte[] getDataReceived() {
        synchronized (this) {
            return readBuffer.readByteArray();
        }
    }

    public void clearReadBuffer() {
        synchronized (this) {
            readBuffer.clear();
        }
    }

    public byte[] getWriteBuffer() {
        return writeBuffer.get();
    }

    public void putWriteBuffer(byte[] data) {
        writeBuffer.put(data);
    }

    private class SynchronizedBuffer {
        private final Buffer buffer;

        SynchronizedBuffer() {
            buffer = new Buffer();
        }

        synchronized void put(byte[] src) {
            if (src == null || src.length == 0) return;
            buffer.write(src);
            notify();
        }

        synchronized byte[] get() {
            if(buffer.size() ==  0) {
                try {
                    wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    Thread.currentThread().interrupt();
                }
            }

            byte[] dst;
            if (buffer.size() <= MAX_BULK_BUFFER) {
                dst = buffer.readByteArray();
            } else {
                try {
                    dst = buffer.readByteArray(MAX_BULK_BUFFER);
                } catch (EOFException e) {
                    e.printStackTrace();
                    return new byte[0];
                }
            }

            return dst;
        }
    }
}
