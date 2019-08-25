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
    private UsbSerialSetting setting;
    private SerialPort.Callback callback;

    private UsbSerialDevice currentSerial;
    private UsbDevice currentDevice;

    UsbClient(UsbSerialService service, UsbSerialSetting setting, SerialPort.Callback callback) {
        this.service = service;
        this.setting = setting;
        this.callback = callback;
    }

    /***************************************************
     *            按序依次尝试打开可用的串口设备           *
     ***************************************************/

    private int currentIndex = 0;

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
    boolean checkDevice() {
        Log.d(TAG, "检查驱动：" + currentIndex);
        currentDevice = service.getUsbDevice(currentIndex);

        //不存在有效的usb设备，返回错误。
        if (currentDevice == null) {
            callback.onError(new Exception("Bad to open usb serial。"));
            return false;
        }

        if (service.hasPermission(currentDevice)) {
            Log.v(TAG, "开始加载驱动：" + currentIndex);
            // 遍历不返回开启错误，返回无效设备错误。
            if (!startOpenDevice(false)) {
                currentIndex++;
                return checkDevice();
            }
            return true;
        } else {
            Log.v(TAG, "请求驱动权限：" + currentIndex);
            service.requestUserPermission(currentDevice);
            return true;
        }

    }

    /**
     * 没有成功开启设备
     * PS:成功不需要做什么，失败则需要做些事情，调整下bool输出。
     */
    boolean notOpenDevice() {
        return !startOpenDevice(true);
    }

    /***************************************************
     *            打开指定串口设备                       *
     ***************************************************/

    /**
     * 开启指定Usb设备
     */
    boolean openDevice(UsbDevice device) {
        currentDevice = device;
        return startOpenDevice(true);
    }

    private boolean startOpenDevice(boolean callbackWarn) {
        UsbDevice device = currentDevice;
        if (device == null) {
            if (callbackWarn) callback.onError(new Exception("UsbDevice is null."));
            return false;
        }

        UsbSerialDevice serial = service.openDevice(device);
        if (serial == null) {
            if (callbackWarn) callback.onError(new Exception("UsbSerialDevice is null."));
            return false;
        }

        try {
            if (serial.open()) {
                currentSerial = serial;

                /*
                 * 与lib中的值一样，直接导入即可
                 */
                SerialSetting setting = this.setting.getSetting();
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
            if (callbackWarn) callback.onError(new Exception("Open UsbSerialDevice failed."));
        } catch (Exception e) {
            if (callbackWarn) callback.onError(e);
        }
        return false;
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

    String getKey() {
        return setting.getKey();
    }

}