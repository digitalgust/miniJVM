/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.mini.media;

import org.mini.reflect.DirectMemObj;

import java.util.HashMap;
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
    public MaDeviceListener listener;
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

    @Override
    public void finalize() {
        if (handle != 0) {
            MiniAudio.ma_device_uninit(handle);
            handle = 0;
        }
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

    /**
     * =================================================================================================
     * <p>
     * follow on... methods would call by native
     * =================================================================================================
     */
    static Map<Long, MaDevice> devices = new HashMap();
    static Thread curThread;

    /**
     * @param pDevice
     * @param frameCount
     * @param pSamples
     */
    static void onReceiveFrames(long pDevice, int frameCount, long pSamples) {
        //System.out.println("audio onReceiveFrames : " + Long.toHexString(pDevice));
        curThread = Thread.currentThread();
        MaDevice dev = devices.get(pDevice);
        if (dev != null && pSamples != 0) {
            if (dev.listener != null) {

                int sampleCount = frameCount * dev.channels;
                int len = sampleCount * getFormatBytes(dev.format);
                DirectMemObj dmo = new DirectMemObj(pSamples, len);
                dev.listener.onCapture(dev, frameCount, dmo);
            }
        }
        curThread = null;
    }

    static int onSendFrames(long pDevice, int frameCount, long pSamples) {
        //System.out.println("audio onSendFrames : " + Long.toHexString(pDevice));
        curThread = Thread.currentThread();
        MaDevice dev = devices.get(pDevice);
        if (dev != null) {
            if (dev.listener != null) {
                int samplesToRead = frameCount * dev.channels;
                if (samplesToRead == 0) {
                    curThread = null;
                    return 0;
                }
                int len = samplesToRead * getFormatBytes(dev.format);
                DirectMemObj dmo = new DirectMemObj(pSamples, len);
                int v = dev.listener.onPlayback(dev, frameCount, dmo);
                curThread = null;
                return v;
            }
        }
        curThread = null;
        return 0;
    }

    static void onStop(long pDevice) {
        //System.out.println("audio onStop : " + Long.toHexString(pDevice));
        curThread = Thread.currentThread();
        MaDevice dev = devices.get(pDevice);
        if (dev != null) {
            if (dev.listener != null) {
                dev.listener.onStop(dev);
            }
        }
        curThread = null;
    }

}
