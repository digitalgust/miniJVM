/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.mini.media;

import static org.mini.glwrap.GLUtil.toCstyleBytes;

/**
 * @author Gust
 */
public class MaDecoder extends MaDataSource {

    static final int PARA_FORMAT = 0;
    static final int PARA_CHANNELS = 1;
    static final int PARA_SAMPLERATE = 2;

    int[] para = {0, 0, 0};

    byte[] audioData;

    public MaDecoder(String path) {
        byte[] b = toCstyleBytes(path);
        handle = MiniAudio.ma_decoder_init_file(b);
        MiniAudio.ma_decoder_get_para(handle, para);
    }

    public MaDecoder(String path, int format, int channels, int sampleRate) {
        byte[] b = toCstyleBytes(path);
        handle = MiniAudio.ma_decoder_init_file_ex(b, format, channels, sampleRate);
        MiniAudio.ma_decoder_get_para(handle, para);
    }

    public MaDecoder(byte[] data) {
        if (data == null) {
            throw new NullPointerException();
        }
        this.audioData = data;
        handle = MiniAudio.ma_decoder_init_memory(data);
        MiniAudio.ma_decoder_get_para(handle, para);
    }

    public MaDecoder(byte[] data, int format, int channels, int sampleRate) {
        if (data == null) {
            throw new NullPointerException();
        }
        handle = MiniAudio.ma_decoder_init_memory_ex(data, format, channels, sampleRate);
        MiniAudio.ma_decoder_get_para(handle, para);
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
        if (handle != 0) {
            //System.out.println("clean " + this + " " + handle);
            MiniAudio.ma_decoder_uninit(handle);
            handle = 0;
        }

    }

    public int decode(int frameCount, long pSample) {
        return MiniAudio.ma_decoder_read(handle, frameCount, pSample);
    }

    /**
     * @return the handle_decoder
     */
    public long getHandle_decoder() {
        return handle;
    }
}
