package com.seiko.serial.usb;

import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import android.util.Pair;

import com.felhr.usbserial.UsbSerialDevice;
import com.seiko.serial.core.SerialPort;
import com.seiko.serial.core.SerialSetting;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * UsbService
 */
public class UsbSerialService extends Service {

    private static final String TAG = "RxUsbService";
    private static final String ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";
    private static final String ACTION_USB_ATTACHED = "android.hardware.usb.action.USB_DEVICE_ATTACHED";
    private static final String ACTION_USB_DETACHED = "android.hardware.usb.action.USB_DEVICE_DETACHED";

    private UsbBinder binder = new UsbBinder();

    private UsbManager usbManager;
    private List<UsbDevice> deviceList;
    private UsbClient currentClient;

    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null) return;

            try {
                switch (intent.getAction()) {
                    case ACTION_USB_PERMISSION:
                        Log.i(TAG, "ACTION_USB_PERMISSION");
                        boolean granted = intent.getExtras().getBoolean(
                                UsbManager.EXTRA_PERMISSION_GRANTED, false);
                        if (granted) {
                            if (currentClient != null) {
                                currentClient.checkDevice();
                            }
                        } else {
                            if (currentClient != null) {
                                currentClient.checkNextDevice();
                            }
                        }
                        break;
                    case ACTION_USB_ATTACHED:
                        Log.i(TAG, "ACTION_USB_ATTACHED");
                        UsbDevice device1 = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                        if (currentClient != null) {
                            if (currentClient.isEmpty()) {
                                currentClient.refreshCheckDevice();
                            }
                            else if (currentClient.isEqual(device1)) {
                                currentClient.refreshCheckDevice();
                            }
                        }
                        return;
                    case ACTION_USB_DETACHED:
                        Log.i(TAG, "ACTION_USB_DETACHED");
                        UsbDevice device2 = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                        if (currentClient != null && currentClient.isEqual(device2)) {
                            currentClient.detach();
                        }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            usbManager = getSystemService(UsbManager.class);
        } else {
            usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        }

        deviceList = new ArrayList<>();
        readUsbDevices();

        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_USB_PERMISSION);
        filter.addAction(ACTION_USB_DETACHED);
        filter.addAction(ACTION_USB_ATTACHED);
        registerReceiver(receiver, filter);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return Service.START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "onBind");
        return binder;
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");
        unregisterReceiver(receiver);
        super.onDestroy();
    }

    /***************************************************************
     *                          UsbBinder                          *
     ***************************************************************/

    class UsbBinder extends Binder {
        void open(SerialSetting setting, SerialPort.Callback callback) {
            currentClient = new UsbClient(UsbSerialService.this, setting, callback);
            currentClient.checkDevice();
        }

        void send(byte[] bytes) {
            if (currentClient != null) {
                currentClient.sendBytes(bytes);
            }
        }

        void close() {
            if (currentClient != null) {
                currentClient.release();
                currentClient = null;
            }
        }

        void setBaudRate(int baudRate) {
            if (currentClient != null) {
                currentClient.setBaudRate(baudRate);
            }
        }
    }

    /***************************************************************
     *                 Share For UdbClient                         *
     ***************************************************************/

    /**
     * 获得第position位的UsbDevice
     */
    UsbDevice getUsbDevice(int position) {
        int size = deviceList.size();
        if (position >= size) return null;
        return deviceList.get(position);
    }

    /**
     * 是否拥有权限
     */
    Boolean hasPermission(UsbDevice device) {
        return usbManager.hasPermission(device);
    }

    /**
     * 请求Usb权限
     */
    void requestUserPermission(UsbDevice device) {
        PendingIntent mPendingIntent = PendingIntent.getBroadcast(this, 0,
                new Intent(ACTION_USB_PERMISSION), 0);
        usbManager.requestPermission(device, mPendingIntent);
    }

    /**
     * 生成UsbSerial
     */
    UsbSerialDevice openDevice(UsbDevice device) {
        try {
            UsbDeviceConnection connection = usbManager.openDevice(device);
            return UsbSerialDevice.createUsbSerialDevice(device, connection);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 读取UsbDevices
     */
    void readUsbDevices() {
        if (!deviceList.isEmpty()) deviceList.clear();
        for (Map.Entry<String,UsbDevice> map : usbManager.getDeviceList().entrySet()) {
            if (isUseDevice(map.getValue())) {
                deviceList.add(map.getValue());
            }
        }
    }

    /***************************************************************
     *                            Other                            *
     ***************************************************************/

    private static boolean isUseDevice(UsbDevice device) {
        int deviceVID = device.getVendorId();
        int devicePID = device.getProductId();
        for (Pair<Integer, Integer> pair : Holder.filterDevices) {
            if (deviceVID == pair.first && devicePID == pair.second) {
                return false;
            }
        }
        return deviceVID != 0x1d6b && (devicePID != 0x0001
                && devicePID != 0x0002
                && devicePID != 0x0003);
    }

    private static class Holder {
        private static List<Pair<Integer, Integer>> filterDevices = new ArrayList<>();
    }

    public static void addFilterDevice(int vid, int pid) {
        Holder.filterDevices.add(new Pair<>(vid, pid));
    }

}
