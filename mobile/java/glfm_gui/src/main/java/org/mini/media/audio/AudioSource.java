package org.mini.media.audio;

import org.mini.media.MaDecoder;

class AudioSource {

    public AudioSource(byte[] data, int pos) {
        this.rawdata = data;
        this.pos = pos;
    }

    public AudioSource(MaDecoder decoder) {
        this.decoder = decoder;
    }

    public MaDecoder decoder;
    public byte[] rawdata;
    public int pos;
    public AudioListener callback;
}