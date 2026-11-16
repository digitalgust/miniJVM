/* SPDX-License-Identifier: MIT */

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.mini.media;

import org.mini.reflect.DirectMemObj;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * @author Gust
 */
public class MaDevice extends MaNativeObject {

    MaContext context;

    int deviceType;

    public int format;
    public int channels;
    public int sampleRate;
    public volatile MaDeviceListener listener;
    Object userdata;

    public MaDevice(int deviceType, int format, int channels, int sampleRate) {
        this.deviceType = deviceType;

        this.format = format;
        this.channels = channels;
        this.sampleRate = sampleRate;
        init();
    }

    public void setDeviceListener(MaDeviceListener listener) {
        this.listener = listener;

    }

    public void setUserData(Object userdata) {
        this.userdata = userdata;

    }

    public Object getUserData() {
        return userdata;
    }

    public int getDeviceType() {
        return deviceType;
    }

    public int getChannels() {
        return channels;
    }

    public int getSampleRate() {
        return sampleRate;
    }

    final void init() {
        checkThread();
        context = new MaContext();
        handle = MiniAudio.ma_device_init(context.getHandle(), deviceType, 0, format, channels, sampleRate);
        if (handle == 0) {
            throw new RuntimeException("MiniAL: init device error");
        } else {
            devices.put(handle, this);
            synchronized (callbackDevices) {
                callbackDevices.add(this);
                callbackDeviceSnapshot = callbackDevices.toArray(new MaDevice[callbackDevices.size()]);
            }
        }
        //System.out.println("audio init " + Long.toHexString(handle_device));
    }

    public void start() {
        checkThread();
        MiniAudio.ma_device_start(handle);
    }

    public void stop() {
        checkThread();
        if (MiniAudio.ma_device_is_started(handle) == 1) {
            MiniAudio.ma_device_stop(handle);
            //System.out.println("audio init " + Long.toHexString(handle_device));
        }
    }

    public boolean isStarted() {
        return MiniAudio.ma_device_is_started(handle) == 1;
    }

    //cant call start stop in callback thread
    void checkThread() {
        if (Thread.currentThread() == curThread) {
            throw new RuntimeException("cant call method in callback, need call this method in other thread.");
        }
    }

    /**
     * uninit this device and its private context; a device dereferences its
     * context on uninit, so the context must go down after the device
     */
    public synchronized void close() {
        if (handle != 0) {
            if (MiniAudio.ma_device_is_started(handle) == 1) {
                MiniAudio.ma_device_stop(handle);
            }
            MiniAudio.ma_device_uninit(handle);
            devices.remove(handle);
            synchronized (callbackDevices) {
                callbackDevices.remove(this);
                callbackDeviceSnapshot = callbackDevices.toArray(new MaDevice[callbackDevices.size()]);
            }
            handle = 0;
        }
        if (context != null) {
            context.dispose();
            context = null;
        }
    }

    @Override
    public void finalize() {
        close();
    }

    public static int getFormatBytes(int format) {
        switch (format) {
            case MiniAudio.mal_format_f32:
            case MiniAudio.mal_format_s32:
                return 4;
            case MiniAudio.mal_format_s16:
                return 2;
            case MiniAudio.mal_format_u8:
                return 1;
            case MiniAudio.mal_format_s24:
                return 3;
        }
        return 0;
    }

    public static void putDevice(long deviceHandle) {
        devices.put(deviceHandle, null);
    }

    public static void removeDevice(long deviceHandle) {
        devices.remove(deviceHandle);
    }

    public static void stopAll() {
        for (Iterator<Long> it = devices.keySet().iterator(); it.hasNext(); ) {
            Long deviceHanle = it.next();
            if (deviceHanle != null) {
                MiniAudio.ma_device_stop(deviceHanle.longValue());
            }
        }
    }

    /**
     * =================================================================================================
     * <p>
     * follow on... methods would call by native
     * =================================================================================================
     */
    static Map<Long, MaDevice> devices = new HashMap();
    // Native audio callbacks run through MiniJVM's JNI bridge. Avoid map key
    // boxing/hash operations there and resolve the native handle by primitive
    // comparison instead.
    static final ArrayList<MaDevice> callbackDevices = new ArrayList<MaDevice>();
    private static volatile MaDevice[] callbackDeviceSnapshot = new MaDevice[0];
    static volatile Thread curThread;

    private static MaDevice findCallbackDevice(long deviceHandle) {
        MaDevice[] snapshot = callbackDeviceSnapshot;
        for (int i = 0; i < snapshot.length; i++) {
            MaDevice device = snapshot[i];
            if (device != null && device.handle == deviceHandle) {
                return device;
            }
        }
        return null;
    }

    /**
     * @param pDevice
     * @param frameCount
     * @param pSamples
     */
    static void onReceiveFrames(long pDevice, int frameCount, long pSamples) {
        //System.out.println("audio onReceiveFrames : " + Long.toHexString(pDevice));
        curThread = Thread.currentThread();
        try {
            if (pDevice != 0 && pSamples != 0 && frameCount > 0) {
                MaDevice dev = findCallbackDevice(pDevice);
                if (dev != null) {
                    MaDeviceListener callback = dev.listener;
                    int bytesPerSample = getFormatBytes(dev.format);
                    long len = (long) frameCount * dev.channels * bytesPerSample;
                    if (callback != null && dev.channels > 0 && bytesPerSample > 0
                            && len > 0 && len <= Integer.MAX_VALUE) {
                        DirectMemObj dmo = new DirectMemObj(pSamples, (int) len);
                        callback.onCapture(dev, frameCount, dmo);
                    }
                }
            }
        } catch (Throwable ignored) {
            // A Java callback must never unwind through MiniAudio's native thread.
        }
        curThread = null;
    }

    static int onSendFrames(long pDevice, int frameCount, long pSamples) {
        //System.out.println("audio onSendFrames : " + Long.toHexString(pDevice));
        curThread = Thread.currentThread();
        int result = 0;
        try {
            if (pDevice != 0 && pSamples != 0 && frameCount > 0) {
                MaDevice dev = findCallbackDevice(pDevice);
                if (dev != null) {
                    MaDeviceListener callback = dev.listener;
                    int bytesPerSample = getFormatBytes(dev.format);
                    long len = (long) frameCount * dev.channels * bytesPerSample;
                    if (callback != null && dev.channels > 0 && bytesPerSample > 0
                            && len > 0 && len <= Integer.MAX_VALUE) {
                        result = callback.onPlayback(dev, frameCount, new DirectMemObj(pSamples, (int) len));
                    }
                }
            }
        } catch (Throwable ignored) {
            // Do not allow Java exceptions to escape into MiniAudio's native callback.
        }
        curThread = null;
        return result;
    }

    static void onStop(long pDevice) {
        //System.out.println("audio onStop : " + Long.toHexString(pDevice));
        curThread = Thread.currentThread();
        try {
            if (pDevice != 0) {
                MaDevice dev = findCallbackDevice(pDevice);
                if (dev != null) {
                    MaDeviceListener callback = dev.listener;
                    if (callback != null) callback.onStop(dev);
                }
            }
        } catch (Throwable ignored) {
            // Do not allow Java exceptions to escape into MiniAudio's native callback.
        }
        curThread = null;
    }

}
