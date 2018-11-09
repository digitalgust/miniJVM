/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.mini.media;

import org.mini.glfw.Glfw;

/**
 *
 * @author Gust
 */
public class MiniAL {

    static {
        Glfw.loadLib();
    }

    static native long mal_context_init();

    static native void mal_context_uninit(long handle_context);

    static native long mal_decoder_init_file(byte[] b, int format, int channels, int sampleRate);

    static native long mal_decoder_init_memory(byte[] data, int format, int channels, int sampleRate);

    static native void mal_decoder_get_para(long handle_decoder, int[] arr);

    static native int mal_decoder_read(long handle_device, int frameCount, long pSamples);

    static native void mal_decoder_uninit(long handle_decoder);

    static native long mal_device_init(long handle_context, int deviceType, long handle_decode, int format, int channels, int sampleRate);

    static native void mal_device_uninit(long handle_device);

    static native void mal_device_start(long handle_device);

    static native void mal_device_stop(long handle_device);

    static native int mal_device_is_started(long handle_device);

}
