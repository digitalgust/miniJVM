package org.mini.media.engine;

import org.mini.media.MaDataSource;
import org.mini.media.MaNativeObject;
import org.mini.media.MiniAudio;

import static org.mini.glwrap.GLUtil.toUtf8;
import static org.mini.media.MiniAudio.MA_SOUND_FLAG_DECODE;

public class MaSound extends MaNativeObject {
    MaDataSource dataSource;
    MaEngine engine;

    public MaSound(MaEngine engine, String filePath) {
        if (engine == null || filePath == null) {
            throw new RuntimeException("init error , engine and fielPath can't be null");
        }

        this.engine = engine;
        handle = MiniAudio.ma_sound_init_from_file(engine.getHandle(), toUtf8(filePath), MA_SOUND_FLAG_DECODE, 0, 0);
    }

    public MaSound(MaEngine engine, MaSound src) {
        if (engine == null || src == null) {
            throw new RuntimeException("init error , engine and source sound can't be null");
        }

        this.engine = engine;
        handle = MiniAudio.ma_sound_init_copy(engine.getHandle(), src.getHandle(), MA_SOUND_FLAG_DECODE, 0);
    }

    public MaSound(MaEngine engine, MaDataSource dataSource) {
        if (engine == null || dataSource == null) {
            throw new RuntimeException("init error , engine and decoder can't be null");
        }

        this.engine = engine;
        this.dataSource = dataSource;//hold the decoder, else it would be gc
        handle = MiniAudio.ma_sound_init_from_data_source(engine.getHandle(), dataSource.getHandle(), 0, 0);
    }

    public MaSound(MaEngine engine, MaDataSource dataSource, int initFlag) {
        if (engine == null || dataSource == null) {
            throw new RuntimeException("init error , engine and decoder can't be null");
        }

        this.engine = engine;
        this.dataSource = dataSource;//hold the decoder, else it would be gc
        handle = MiniAudio.ma_sound_init_from_data_source(engine.getHandle(), dataSource.getHandle(), initFlag, 0);
    }

    public void start() {
        MiniAudio.ma_sound_start(handle);
    }

    public void stop() {
        MiniAudio.ma_sound_stop(handle);
    }

    public boolean isPlaying() {
        return MiniAudio.ma_sound_is_playing(handle);
    }

    public boolean isPlayEnd() {
        return MiniAudio.ma_sound_at_end(handle);
    }

    public void setSpatialization(boolean enable) {
        MiniAudio.ma_sound_set_spatialization_enabled(handle, enable);
    }

    public void setVolume(float v) {
        MiniAudio.ma_sound_set_volume(handle, v);
    }

    public float getVolume() {
        return MiniAudio.ma_sound_get_volume(handle);
    }

    public void setLooping(boolean v) {
        MiniAudio.ma_sound_set_looping(handle, v);
    }

    public boolean isLooping() {
        return MiniAudio.ma_sound_is_looping(handle);
    }

    public void setMinDistance(float v) {
        MiniAudio.ma_sound_set_min_distance(handle, v);
    }

    public void setMaxDistance(float v) {
        MiniAudio.ma_sound_set_max_distance(handle, v);
    }


    public void setPosition(float x, float y, float z) {
        MiniAudio.ma_sound_set_position(handle, x, y, z);
    }

    public float[] getPosition() {
        float[] position = new float[3];
        MiniAudio.ma_sound_get_position(handle, position);
        return position;
    }

    public void setFadeIn(long ms, float volume) {
        if (ms <= 0) return;
        MiniAudio.ma_sound_set_fade_in_milliseconds(handle, 0f, volume, ms);
    }

    public void setFadeOut(long ms) {
        if (ms <= 0) return;
        MiniAudio.ma_sound_set_fade_in_milliseconds(handle, -1f, 0f, ms);
    }

    @Override
    public void finalize() {
        System.out.println("clean " + this + " " + handle);
        MiniAudio.ma_sound_uninit(handle);
        handle = 0;
    }
}
