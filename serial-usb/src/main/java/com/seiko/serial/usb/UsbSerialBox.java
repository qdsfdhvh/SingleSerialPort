package com.seiko.serial.usb;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;

import static android.content.Context.BIND_AUTO_CREATE;

/**
 * 获取UsbService的Binder
 */
class UsbSerialBox {

    private Context context = Applications.INSTANCES.getContext();

    private UsbSerialService.UsbBinder binder;

    void startBindServiceAndDone(Callback callback) {
        if (binder != null) {
            callback.apply(binder);
            return;
        }

        Intent intent = new Intent(context, UsbSerialService.class);
        context.startService(intent);
        context.bindService(intent, new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                binder = (UsbSerialService.UsbBinder) service;
                callback.apply(binder);
                context.unbindService(this);
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                binder = null; // 系统回收
            }
        }, BIND_AUTO_CREATE);
    }

    interface Callback {
        void apply(UsbSerialService.UsbBinder binder);
    }

    /****************************************************
     *                    Instance                      *
     ****************************************************/

    @SuppressLint("StaticFieldLeak")
    private static class Holder {
        private static final UsbSerialBox INSTANCE = new UsbSerialBox();
    }

    static UsbSerialBox getInstance() {
        return Holder.INSTANCE;
    }

}
