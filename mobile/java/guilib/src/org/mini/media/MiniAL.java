/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.mini.media;

/**
 *
 * @author Gust
 */
public class MiniAL {

    static native long mal_context_init();

    static native void mal_context_uninit(long handle_context);

    static native long mal_device_config_init(int format, int channels, int sampleRate);

    static native void mal_device_config_uninit(long handle);

    static native long mal_decoder_init_file(byte[] b);

    static native long mal_decoder_init_memory(byte[] data);

    static native int mal_decoder_read(long handle_device, int frameCount, long pSamples);

    static native void mal_decoder_uninit(long handle_decoder);

    static native long mal_device_init(long handle_context, int deviceType, long handle_config, long handle_decode);

    static native void mal_device_uninit(long handle_device);

    static native void mal_device_start(long handle_device);

    static native void mal_device_stop(long handle_device);

    static native int mal_device_is_started(long handle_device);

}
