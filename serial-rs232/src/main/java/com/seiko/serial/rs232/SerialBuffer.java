package com.seiko.serial.rs232;

import java.io.EOFException;

import okio.Buffer;

class SerialBuffer {
    static final long DEFAULT_READ_BUFFER_SIZE = 16 * 1024;
    private static final int MAX_BULK_BUFFER = 16 * 1024;

    private Buffer readBuffer;
    private final SynchronizedBuffer writeBuffer;
//    private byte[] readBufferCompatible; // Read buffer for android < 4.2

    SerialBuffer() {
        writeBuffer = new SynchronizedBuffer();
        readBuffer = new Buffer();
    }

    Buffer getReadBuffer() {
        synchronized (this) {
            return readBuffer;
        }
    }

    byte[] getDataReceived() {
        synchronized (this) {
            return readBuffer.readByteArray();
        }
    }

    void clearReadBuffer() {
        synchronized (this) {
            readBuffer.clear();
        }
    }

    byte[] getWriteBuffer() {
        return writeBuffer.get();
    }

    void putWriteBuffer(byte[] data) {
        writeBuffer.put(data);
    }

//    public byte[] getBufferCompatible() {
//        return readBufferCompatible;
//    }

//    public byte[] getDataReceivedCompatible(int numberBytes) {
//        return Arrays.copyOfRange(readBufferCompatible, 0, numberBytes);
//    }

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
