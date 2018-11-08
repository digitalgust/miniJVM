/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.mini.media;

import java.util.HashMap;
import java.util.Map;
import org.mini.reflect.DirectMemObj;
import org.mini.reflect.ReflectArray;

/**
 *
 * @author Gust
 */
public class AudioDevice {

    long handle_context;
    long handle_config;
    long handle_device;

    public static final int //
            mal_format_unknown = 0, // Mainly used for indicating an error, but also used as the default for the output format for decoders.
            mal_format_u8 = 1,
            mal_format_s16 = 2, // Seems to be the most widely supported format.
            mal_format_s24 = 3, // Tightly packed. 3 bytes per sample.
            mal_format_s32 = 4,
            mal_format_f32 = 5;
    public static final int //
            mal_device_type_playback = 0,
            mal_device_type_capture = 1;

    public int format;
    public int channels;
    public int sampleRate;
    public AudioFrameListener listener;
    AudioDecoder decoder;

    public void config(int format, int channels, int sampleRate, AudioFrameListener listener) {
        this.format = format;
        this.channels = channels;
        this.sampleRate = sampleRate;
        this.listener = listener;
        handle_config = MiniAL.mal_device_config_init(format, channels, sampleRate);
        if (handle_config == 0) {
            throw new RuntimeException("MiniAL: init device config error");
        }
    }

    public void init(int deviceType, AudioDecoder decoder) {
        this.decoder = decoder;
        handle_context = MiniAL.mal_context_init();
        if (handle_context == 0) {
            throw new RuntimeException("MiniAL: init context error");
        }
        long handle_decode = decoder == null ? 0 : decoder.getHandle_decoder();
        handle_device = MiniAL.mal_device_init(handle_context, deviceType, handle_config, handle_decode);
        if (handle_device == 0) {
            throw new RuntimeException("MiniAL: init device error");
        } else {
            processors.put(handle_device, this);
        }
    }

    public void start() {
        MiniAL.mal_device_start(handle_device);
    }

    public void stop() {
        MiniAL.mal_device_stop(handle_device);
    }

    public boolean isStarted() {
        return MiniAL.mal_device_is_started(handle_device) == 1;
    }

    public void finalize() {
        if (handle_context != 0) {
            MiniAL.mal_context_uninit(handle_context);
        }
        if (handle_config != 0) {
            MiniAL.mal_device_config_uninit(handle_config);
        }
        if (handle_device != 0) {
            MiniAL.mal_device_uninit(handle_device);
        }
        processors.remove(handle_config);
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
     *
     * follow on... methods would call by native
     * =================================================================================================
     *
     */
    static Map<Long, AudioDevice> processors = new HashMap();

    /**
     *
     * @param pDevice
     * @param frameCount
     * @param pSamples
     */
    static void onReceiveFrames(long pDevice, int frameCount, long pSamples) {
        AudioDevice dev = processors.get(pDevice);
        if (dev != null && pSamples != 0) {
            if (dev.listener != null) {

                int sampleCount = frameCount * dev.channels;
                int len = sampleCount * getFormatBytes(dev.format);
                DirectMemObj dmo = new DirectMemObj(pSamples, len);
                dev.listener.onReceiveFrames(dev, frameCount, dmo);
            }
        }
    }

    static int onSendFrames(long pDevice, int frameCount, long pSamples) {
        AudioDevice dev = processors.get(pDevice);
        if (dev != null && pSamples != 0) {
            if (dev.decoder != null) {
                System.out.println("onSendFrames: " + frameCount + " , " + pSamples + "  status:" + dev.isStarted());
                int v = MiniAL.mal_decoder_read(pDevice, frameCount, pSamples);
                if (v == 0) {
                    dev.stop();
                }
                return v;
            } else if (dev.listener != null) {
                int samplesToRead = frameCount * dev.channels;
                if (samplesToRead == 0) {
                    return 0;
                }
                int len = samplesToRead * getFormatBytes(dev.format);
                DirectMemObj dmo = new DirectMemObj(pSamples, len);
                int v = dev.listener.onSendFrames(dev, frameCount, dmo);
                return samplesToRead / dev.channels;
            }
        }
        return 0;
    }

    static void onStop(long pDevice) {
        AudioDevice dev = processors.get(pDevice);
        if (dev != null) {
            if (dev.listener != null) {
                dev.listener.onStop(dev);
            }
        }
    }

}
