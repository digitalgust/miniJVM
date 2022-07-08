package org.mini.media;


public class MaContext extends MaNativeObject {

    public MaContext() {
        handle = MiniAudio.ma_context_init();
        if (handle == 0) {
            throw new RuntimeException("MiniAL: init context error");
        }
    }


    @Override
    public void finalize() {
        if (handle != 0) {
            MiniAudio.ma_context_uninit(handle);
            handle = 0;
        }
    }
}
