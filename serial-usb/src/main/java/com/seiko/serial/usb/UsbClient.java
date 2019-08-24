package com.seiko.serial.usb;

import android.hardware.usb.UsbDevice;
import android.util.Log;

import com.felhr.usbserial.UsbSerialDevice;
import com.seiko.serial.core.SerialPort;
import com.seiko.serial.core.SerialSetting;

import java.util.Locale;

/**
 * 单个Usb串口对象封装
 */
class UsbClient {

    private static final String TAG = "UsbClient";

    private UsbSerialService service;
    private SerialSetting setting;
    private SerialPort.Callback callback;

    UsbClient(UsbSerialService service, SerialSetting setting, SerialPort.Callback callback) {
        this.service = service;
        this.setting = setting;
        this.callback = callback;
    }

    private int currentIndex = 0;
    private UsbSerialDevice currentSerial;
    private UsbDevice currentDevice;

    /**
     * 重新检查
     */
    void refreshCheckDevice() {
        service.readUsbDevices();
        currentIndex = 0;
        checkDevice();
    }

    /**
     * 检查下一个UsbDevice
     */
    void checkNextDevice() {
        currentIndex++;
        checkDevice();
    }

    /**
     * 检查当前UsbDevice
     */
    void checkDevice() {
        Log.d(TAG, "检查驱动：" + currentIndex);
        UsbDevice device = service.getUsbDevice(currentIndex);
        checkDevice(device);
    }

    private void checkDevice(UsbDevice device) {
        if (device == null) {
            callback.onError(new RuntimeException("Bad to open usb serial。"));
            return;
        }

        if (service.hasPermission(device)) {
            Log.v(TAG, "开始加载驱动：" + currentIndex);
            boolean bool = startOpenDevice(device);
            if (!bool) {
                currentIndex++;
                checkDevice();
            }
        } else {
            Log.v(TAG, "请求驱动权限：" + currentIndex);
            service.requestUserPermission(device);
        }
    }

    private boolean startOpenDevice(UsbDevice device) {
        UsbSerialDevice serial = service.openDevice(device);
        if (serial == null) {
            return false;
        }

        Log.v(TAG, "获取Serial：" + currentIndex);
        try {
            if (serial.open()) {
                currentSerial = serial;
                currentDevice = device;

                /*
                 * 与lib中的值一样，直接导入即可
                 */
                serial.setBaudRate(setting.getBaudRate());
                serial.setDataBits(setting.getData());
                serial.setStopBits(setting.getStop());
                serial.setParity(setting.getParity());
                serial.setFlowControl(setting.getFlowControl());
                serial.read(callback::onResult);

                String msg = String.format(Locale.CHINA, "成功开启Serial(%d,%d)，参数(%s, %s, %s, %s)，类型：%s",
                        device.getVendorId(), device.getProductId(),
                        setting.getBaudRate(), setting.getData(), setting.getStop(), setting.getParity(),
                        serial.getClass().getSimpleName());
                Log.d(TAG, msg);

                callback.onSuccess();
                return true;
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    void sendBytes(byte[] bytes) {
        if (currentSerial != null) {
            currentSerial.write(bytes);
        }
    }

    /**
     * 解绑
     */
    void detach() {
        callback.onError(new RuntimeException("Usb Serial Is Detached."));
        release();
    }

    void release() {
        if (currentSerial != null) {
            currentSerial.close();
            currentSerial = null;
            currentDevice = null;
        }
    }

    void setBaudRate(int baudRate) {
        if (currentSerial != null) {
            currentSerial.setBaudRate(baudRate);
        }
    }

    boolean isEqual(UsbDevice device) {
        return device != null && currentDevice != null
                && device.getVendorId() == currentDevice.getVendorId()
                && device.getProductId() == currentDevice.getProductId();
    }

    boolean isEmpty() {
        return currentDevice == null;
    }

}