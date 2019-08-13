package com.seiko.serial.modbus;

class HexUtils {

//    public static String bytesToHexString(byte[] bytes) {
//        return bytesToHexString(bytes, " ");
//    }

    static String bytesToHexString(byte[] bytes, String sep) {
        if (bytes == null) {
            return "";
        } else {
            int var1 = bytes.length - 1;
            if (var1 == -1) {
                return "";
            } else {
                StringBuilder sb = new StringBuilder();

                int i = 0;
                while(true) {
                    int v = bytes[i] & 0xFF;
                    String hv = Integer.toHexString(v).toUpperCase();
                    if (hv.length() < 2) {
                        sb.append(0);
                    }
                    sb.append(hv);

                    if (i == var1) {
                        return sb.toString();
                    } else {
                        sb.append(sep);
                        ++i;
                    }
                }
            }
        }
    }

    public static byte[] hexStringToBytes(String hexString) {
        if (hexString == null || hexString.equals("")) {
            return null;
        }
        hexString = hexString.toUpperCase();
        int length = hexString.length() / 2;
        char[] hexChars = hexString.toCharArray();
        byte[] d = new byte[length];
        for (int i = 0; i < length; i++) {
            int pos = i * 2;
            d[i] = (byte) (charToByte(hexChars[pos]) << 4 | charToByte(hexChars[pos + 1]));
        }
        return d;
    }

    /**
     * Convert char to byte
     *
     * @param c char
     * @return byte
     */
    private static byte charToByte(char c) {
        return (byte) "0123456789ABCDEF".indexOf(c);
    }

}
