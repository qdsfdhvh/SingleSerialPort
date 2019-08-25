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
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.util.Pair;

import com.felhr.usbserial.UsbSerialDevice;
import com.seiko.serial.core.SerialPort;

import java.util.ArrayList;
import java.util.HashMap;
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

    static final String DEFAULT_CLIENTS = "DEFAULT";

    private UsbBinder binder = new UsbBinder();

    private UsbManager usbManager;
    private List<UsbDevice> deviceList;
    private HashMap<String, UsbClient> usbClients;

    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null) return;
            String action = intent.getAction();
            if (action == null) return;

            UsbDevice device;
            UsbClient client;

            try {
                switch (action) {
                    case ACTION_USB_PERMISSION:
                        Log.i(TAG, "ACTION_USB_PERMISSION");

                        Bundle bundle = intent.getExtras();
                        boolean granted;
                        if (bundle != null) {
                            granted = bundle.getBoolean(UsbManager.EXTRA_PERMISSION_GRANTED, false);
                        } else {
                            granted = false;
                        }

                        device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                        for (Map.Entry<String, UsbClient> entry : usbClients.entrySet()) {
                            client = entry.getValue();
                            if (client.isEqual(device)) {
                                if (granted) {
                                    if (client.notOpenDevice()) {
                                        usbClients.remove(entry.getKey());
                                    }
                                } else {
                                    client.checkNextDevice();
                                }
                                break;
                            }
                        }
                        return;
                    case ACTION_USB_ATTACHED:
                        Log.i(TAG, "ACTION_USB_ATTACHED");

                        device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                        for (Map.Entry<String, UsbClient> entry : usbClients.entrySet()) {
                            client = entry.getValue();
                            if (client.getKey().equals(DEFAULT_CLIENTS) && client.isEqual(device)) {
                                client.refreshCheckDevice();
                            } else {
                                if (client.notOpenDevice()) {
                                    usbClients.remove(client.getKey());
                                }
                            }
                        }
                        return;
                    case ACTION_USB_DETACHED:
                        Log.i(TAG, "ACTION_USB_DETACHED");

                        device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                        for (Map.Entry<String, UsbClient> entry : usbClients.entrySet()) {
                            client = entry.getValue();
                            if (client.isEqual(device)) {
                                client.detach();
                            }
                        }
                        break;
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
        usbClients = new HashMap<>();
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
        return binder;
    }

    @Override
    public void onDestroy() {
        unregisterReceiver(receiver);
        super.onDestroy();
    }

    /***************************************************************
     *                          UsbBinder                          *
     ***************************************************************/

    class UsbBinder extends Binder {
        void open(UsbSerialSetting setting, SerialPort.Callback callback) {
            String key = setting.getKey();
            UsbClient client = new UsbClient(UsbSerialService.this, setting, callback);

            if (key.equals(DEFAULT_CLIENTS)) {
                // 遍历usb设备
                if (!usbClients.containsKey(key) && client.checkDevice()) {
                    usbClients.put(key, client);
                }
            } else {
                // 尝试开启指定usb设备
                UsbDevice device = getUsbDevice(setting.getVid(), setting.getPid());
                if (!usbClients.containsKey(key) && client.openDevice(device)) {
                    usbClients.put(key, client);
                }
            }
        }

        void send(String key, byte[] bytes) {
            UsbClient currentClient = usbClients.get(key);
            if (currentClient != null) {
                currentClient.sendBytes(bytes);
            }
        }

        void close(String key) {
            UsbClient currentClient = usbClients.get(key);
            if (currentClient != null) {
                currentClient.release();
                usbClients.remove(key);
            }
        }

        void setBaudRate(String key, int baudRate) {
            UsbClient currentClient = usbClients.get(key);
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
     * 尝试读取有效的UsbDevice
     */
    void readUsbDevices() {
        if (!deviceList.isEmpty()) deviceList.clear();
        UsbDevice device;
        for (Map.Entry<String,UsbDevice> map : usbManager.getDeviceList().entrySet()) {
            device = map.getValue();
            if (isUseDevice(device)) {
                deviceList.add(device);
            }
        }
    }

    /**
     * @param vid usb版本id
     * @param pid usb设备id
     * @return 可能存在的usb设备
     */
    UsbDevice getUsbDevice(int vid, int pid) {
        UsbDevice device;
        for (Map.Entry<String,UsbDevice> map : usbManager.getDeviceList().entrySet()) {
            device = map.getValue();
            if (device.getVendorId() == vid && device.getProductId() == pid) {
                return device;
            }
        }
        return null;
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
        return true;
//        return deviceVID != 0x1d6b && (devicePID != 0x0001
//                && devicePID != 0x0002
//                && devicePID != 0x0003);
    }

    private static class Holder {
        private static List<Pair<Integer, Integer>> filterDevices = new ArrayList<>();
    }

    public static void addFilterDevice(int vid, int pid) {
        Holder.filterDevices.add(new Pair<>(vid, pid));
    }

}
