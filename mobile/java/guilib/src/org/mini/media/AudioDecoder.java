/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.mini.media;

import static org.mini.media.AudioDevice.processors;
import static org.mini.nanovg.Gutil.toUtf8;

/**
 *
 * @author Gust
 */
public class AudioDecoder {

    long handle_decoder;

    public AudioDecoder(String path) {
        byte[] b = toUtf8(path);
        handle_decoder = MiniAL.mal_decoder_init_file(b);
    }

    public AudioDecoder(byte[] data) {
        handle_decoder = MiniAL.mal_decoder_init_memory(data);
    }

    public void finalize() {
        if (handle_decoder != 0) {
            MiniAL.mal_decoder_uninit(handle_decoder);
        }

    }

    public int decode(AudioDevice dev, int frameCount, long pSample) {
        return MiniAL.mal_decoder_read(handle_decoder, frameCount, pSample);
    }

    /**
     * @return the handle_decoder
     */
    public long getHandle_decoder() {
        return handle_decoder;
    }
}
