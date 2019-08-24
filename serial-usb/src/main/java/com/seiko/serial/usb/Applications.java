package com.seiko.serial.usb;

import android.annotation.SuppressLint;
import android.app.Application;
import android.util.Log;

import java.lang.reflect.Method;

/**
 * 一种调用全局context的实现。
 * 由于library并不能很方便得到项目中的application，使用网上的代码，通过映射获得全局context。
 */
enum Applications {
    INSTANCES;

    private Application current;

    Applications() {
        try {
            Object activityThread = getActivityThread();
            Object app = activityThread.getClass().getMethod("getApplication").invoke(activityThread);
            current = (Application) app;
        } catch (Throwable e) {
            throw new IllegalStateException("Can not access Application context by magic code, boom!", e);
        }
    }

    @SuppressLint("PrivateApi")
    private Object getActivityThread() {
        Object activityThread = null;
        try {
            Method method = Class.forName("android.app.ActivityThread").getMethod("currentActivityThread");
            method.setAccessible(true);
            activityThread = method.invoke(null);
        } catch (final Exception e) {
            Log.w("Applications", e);
        }
        return activityThread;
    }

    Application getContext() {
        return current;
    }
}