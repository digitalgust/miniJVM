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
public class AudioDevice {

    long handle_context;
    long handle_device;

    public static final int //
            mal_format_unknown = 0, // Mainly used for indicating an error, but also used as the default for the output format for decoders.
            mal_format_u8 = 1,
            mal_format_s16 = 2, // Seems to be the most widely supported format.
            mal_format_s24 = 3, // Tightly packed. 3 bytes per sample.
            mal_format_s32 = 4,
            mal_format_f32 = 5;
    public static final int //
            mal_device_type_playback = 1,
            mal_device_type_capture = 2,
            ma_device_type_duplex = mal_device_type_playback | mal_device_type_capture;

    int deviceType;

    public int format;
    public int channels;
    public int sampleRate;
    public DeviceListener listener;
    Object userdata;

    public AudioDevice(int deviceType, int format, int channels, int sampleRate) {
        this.deviceType = deviceType;

        this.format = format;
        this.channels = channels;
        this.sampleRate = sampleRate;
        init();
    }

    public void setDeviceListener(DeviceListener listener) {
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
        handle_context = MiniAL.ma_context_init();
        if (handle_context == 0) {
            throw new RuntimeException("MiniAL: init context error");
        }
        handle_device = MiniAL.ma_device_init(handle_context, deviceType, 0, format, channels, sampleRate);
        if (handle_device == 0) {
            throw new RuntimeException("MiniAL: init device error");
        } else {
            devices.put(handle_device, this);
        }
        //System.out.println("audio init " + Long.toHexString(handle_device));
    }

    public void start() {
        checkThread();
        MiniAL.ma_device_start(handle_device);
    }

    public void stop() {
        checkThread();
        if (MiniAL.ma_device_is_started(handle_device) == 1) {
            MiniAL.ma_device_stop(handle_device);
            //System.out.println("audio init " + Long.toHexString(handle_device));
        }
    }

    public boolean isStarted() {
        return MiniAL.ma_device_is_started(handle_device) == 1;
    }

    //cant call start stop in callback thread
    void checkThread() {
        if (Thread.currentThread() == curThread) {
            throw new RuntimeException("cant call method in callback, need call this method in other thread.");
        }
    }

    @Override
    public void finalize() {
        destory();
    }

    public void destory() {
        //System.out.println("audio finalize : " + Long.toHexString(handle_device));
        if (handle_device != 0) {
            MiniAL.ma_device_uninit(handle_device);
            handle_device = 0;
        }
        if (handle_context != 0) {
            MiniAL.ma_context_uninit(handle_context);
            handle_context = 0;
        }
    }

    public static int getFormatBytes(int format) {
        switch (format) {
            case mal_format_f32:
            case mal_format_s32:
                return 4;
            case mal_format_s16:
                return 2;
            case mal_format_u8:
                return 1;
            case mal_format_s24:
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
    static Map<Long, AudioDevice> devices = new HashMap();
    static Thread curThread;

    /**
     * @param pDevice
     * @param frameCount
     * @param pSamples
     */
    static void onReceiveFrames(long pDevice, int frameCount, long pSamples) {
        //System.out.println("audio onReceiveFrames : " + Long.toHexString(pDevice));
        curThread = Thread.currentThread();
        AudioDevice dev = devices.get(pDevice);
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
        AudioDevice dev = devices.get(pDevice);
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
        AudioDevice dev = devices.get(pDevice);
        if (dev != null) {
            if (dev.listener != null) {
                dev.listener.onStop(dev);
            }
        }
        curThread = null;
    }

}
