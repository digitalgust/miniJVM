package org.mini.media.engine;

import org.mini.media.MaDevice;
import org.mini.media.MaNativeObject;
import org.mini.media.MiniAudio;
import org.mini.util.SysLog;

import java.util.ArrayList;
import java.util.List;

import static org.mini.glwrap.GLUtil.toCstyleBytes;

public class MaEngine extends MaNativeObject {
    int format;
    int channels;
    int ratio;
    long device;

    /* ma_sound_uninit dereferences the engine, every sound born here is swept
       in finalize() before the engine itself goes down */
    private final List<MaSound> sounds = new ArrayList<>();

    public MaEngine() {
        handle = MiniAudio.ma_engine_init();
        if (handle != 0) {
            format = MiniAudio.ma_engine_get_format(handle);
            channels = MiniAudio.ma_engine_get_channels(handle);
            ratio = MiniAudio.ma_engine_get_sample_rate(handle);
            device = MiniAudio.ma_engine_get_device(handle);
            MaDevice.putDevice(device);
        }
    }

    void addSound(MaSound sound) {
        synchronized (sounds) {
            if (!sounds.contains(sound)) {
                sounds.add(sound);
            }
        }
    }

    void removeSound(MaSound sound) {
        synchronized (sounds) {
            sounds.remove(sound);
        }
    }

    public int getChannels() {
        return channels;
    }

    public int getRatio() {
        return ratio;
    }

    public int getFormat() {
        return format;
    }

    public void playSound(String filepath) {
        if (filepath == null) return;
        MiniAudio.ma_engine_play_sound(handle, toCstyleBytes(filepath));
    }

    public void setListenerPosition(int listener, float x, float y, float z) {
        MiniAudio.ma_engine_listener_set_position(handle, listener, x, y, z);
    }

    public void setListenerDirection(int listener, float x, float y, float z) {
        MiniAudio.ma_engine_listener_set_direction(handle, listener, x, y, z);
    }

    public void setWorldUp(int listener, float x, float y, float z) {
        MiniAudio.ma_engine_listener_set_world_up(handle, listener, x, y, z);
    }

    /**
     * the engine default is started on init
     */
    public void start() {
        MiniAudio.ma_engine_start(handle);
    }

    public void stop() {
        MiniAudio.ma_engine_stop(handle);
    }


    public void setVolume(float v) {
        MiniAudio.ma_engine_set_volume(handle, v);
    }

    public float getVolume() {
        return MiniAudio.ma_engine_get_volume(handle);
    }

    @Override
    public void finalize() {
        SysLog.info("clean " + this + " " + handle);
        /* sounds must be uninitialized while the engine is still alive */
        List<MaSound> pending;
        synchronized (sounds) {
            pending = new ArrayList<>(sounds);
            sounds.clear();
        }
        for (MaSound sound : pending) {
            sound.dispose();
        }
        MiniAudio.ma_engine_stop(handle);
        MiniAudio.ma_engine_uninit(handle);
        MaDevice.removeDevice(device);
        handle = 0;
    }

}
