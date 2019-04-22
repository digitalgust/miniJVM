/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.mini.media;

import static org.mini.nanovg.Gutil.toUtf8;

/**
 *
 * @author Gust
 */
public class AudioDecoder {

    static final int PARA_FORMAT = 0;
    static final int PARA_CHANNELS = 1;
    static final int PARA_SAMPLERATE = 2;

    long handle_decoder;
    int[] para = {0, 0, 0};

    public AudioDecoder(String path, int format, int channels, int sampleRate) {
        byte[] b = toUtf8(path);
        handle_decoder = MiniAL.ma_decoder_init_file(b, format, channels, sampleRate);
        MiniAL.ma_decoder_get_para(handle_decoder, para);
    }

    public AudioDecoder(byte[] data, int format, int channels, int sampleRate) {
        if (data == null) {
            throw new NullPointerException();
        }
        handle_decoder = MiniAL.ma_decoder_init_memory(data, format, channels, sampleRate);
        MiniAL.ma_decoder_get_para(handle_decoder, para);
    }

    public int getFormat() {
        return para[PARA_FORMAT];
    }

    public int getChannels() {
        return para[PARA_CHANNELS];
    }

    public int getSampleRate() {
        return para[PARA_SAMPLERATE];
    }

    public void finalize() {
        if (handle_decoder != 0) {
            MiniAL.ma_decoder_uninit(handle_decoder);
            handle_decoder = 0;
        }

    }

    public int decode(AudioDevice dev, int frameCount, long pSample) {
        return MiniAL.ma_decoder_read(handle_decoder, frameCount, pSample);
    }

    /**
     * @return the handle_decoder
     */
    public long getHandle_decoder() {
        return handle_decoder;
    }
}
