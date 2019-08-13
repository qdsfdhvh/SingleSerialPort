package com.seiko.serial.modbus;

class Crc16Utils {

    static byte[] crc16ByteArray(byte[] bytes) {
        short crcShort = crc16(bytes);
        return new byte[] {
                (byte) toPositiveInt(crcShort),
                (byte) (toPositiveInt(crcShort) >> 8)
        };
    }

    private static short crc16(byte[] bytes) {
        int crc = 0xFFFF;

        for (byte b : bytes) {
            int convertedByte = b & 0xFF;
            crc ^= convertedByte;

            int num = 0;
            for (byte len = 7; num <= len; ++num) {
                if ((crc & 1) == 1){
                    crc >>= 1;
                    crc ^= 0xA001;
                } else {
                    crc >>= 1;
                }
            }
        }
        return (short) crc;
    }

    private static int toPositiveInt(short num) {
        return num & 0xFFFF;
    }

}
