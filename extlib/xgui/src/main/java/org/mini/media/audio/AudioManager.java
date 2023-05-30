/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.mini.media.audio;

import org.mini.media.MaDecoder;
import org.mini.media.MaDevice;
import org.mini.media.MaDeviceListener;
import org.mini.media.MiniAudio;
import org.mini.reflect.DirectMemObj;
import org.mini.zip.Zip;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;


/**
 * 一个低层api
 * 音频播放器和采集器,
 * 可播放声音及录音, 直接控制设备回调进行播放时装填帧数据, 录音时获取数据
 * 所有混音需要手动实现, 数据需要自行管理, 应用程序实现并注册AudioListener可控制数据流
 * <p>
 * 高级api请使用, MaEngine 和 MaSound
 *
 * @author Gust
 */
public class AudioManager implements MaDeviceListener {

    public static int format = MiniAudio.mal_format_s16;
    public static int channels = 2;
    public static int ratio = 22050;

    static AudioManager instace = new AudioManager();


    /**
     * ========================================================================================================
     * device listener
     * ========================================================================================================
     */

    Timer timer = new Timer();
    static AudioListener audioListener;

    static public void setAudioListener(AudioListener listener) {
        audioListener = listener;
    }

    /**
     * @param pDevice
     * @param frameCount
     * @param dmo
     */
    @Override
    public void onCapture(MaDevice pDevice, int frameCount, DirectMemObj dmo) {
        int readNeed = frameCount * pDevice.channels;
        int bytes = readNeed * MaDevice.getFormatBytes(pDevice.format);
        byte[] b = new byte[bytes];
        dmo.copyTo(b);
        //System.out.println("onCapture: " + Long.toHexString(pDevice.handle_device) + " , " + frameCount + "  b.len:" + b.length);
        try {
            baos.write(b);
        } catch (IOException ex) {
        }

        if (audioListener != null) {
            audioListener.onCapture((int) (System.currentTimeMillis() - AudioManager.capStartAt), b);
        }
    }

    @Override
    public int onPlayback(MaDevice pDevice, int frameCount, DirectMemObj dmo) {
        Object obj = pDevice.getUserData();
        if (obj == null) {
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    AudioManager.playStop();
                }
            }, 0);
            return 0;
        } else {
            AudioSource src = (AudioSource) obj;
            if (src.decoder != null) {
                MaDecoder decoder = src.decoder;
                int v = decoder.decode(frameCount, dmo.getDataPtr());
                //System.out.println("onsend: " + v);
                if (v == 0) {
                    pDevice.setUserData(null);
                    if (src.callback != null) {
                        src.callback.onStop();
                    }
                }
                if (audioListener != null) {
                    audioListener.onPlayback(0, null);
                }
                return v;
            } else if (src.rawdata != null) {

                byte[] b = src.rawdata;
                int bytes = dmo.getLength();
                if (src.pos + bytes > b.length) {
                    bytes = b.length - src.pos;
                }
                //System.out.println("sent bytes: " + bytes + " pos: " + raw.pos);
                dmo.copyFrom(src.pos, b, 0, bytes);
                //printArr(b, pos);
                src.pos += bytes;
                if (src.pos >= b.length) {
                    pDevice.setUserData(null);
                    if (src.callback != null) {
                        src.callback.onStop();
                    }
                }
                if (audioListener != null) {
                    int time = (int) (src.pos / AudioManager.getDataTime(b));
                    audioListener.onPlayback(time, null);
                }
                return bytes / MaDevice.getFormatBytes(pDevice.format) / pDevice.channels;
            }
        }
        return 0;
    }

    @Override
    public void onStop(MaDevice pDevice) {
        if (audioListener != null) {
            audioListener.onStop();
        }
    }

    /**
     * ========================================================================================================
     * playback
     * ========================================================================================================
     */
    static MaDevice playDevice;

    public static byte[] readFile(String s) {
        try {
            File f = new File(s);
            byte[] b = new byte[(int) f.length()];

            FileInputStream dis = new FileInputStream(f);
            dis.read(b);
            dis.close();
            return b;
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return null;
    }

    public static void playData(byte[] rawdata) {
        if (canPlay() && rawdata != null) {
            playDevice.setUserData(new AudioSource(rawdata, 0));
            //System.out.println("total len:" + data.length);
            playImpl();
        }
    }

    /**
     * play flac , mp3 file rawdata
     *
     * @param decoder
     */
    public static void playDecoder(MaDecoder decoder) {
        if (canPlay()) {
            playDevice.setUserData(new AudioSource(decoder));
            playImpl();
        }
    }

    public static boolean isPlaying(MaDevice device) {
        return device.isStarted();
    }

    static private void playImpl() {
        if (!playDevice.isStarted()) {
            playDevice.start();
        }
    }

    static private boolean canPlay() {
        if (capDevice != null && capDevice.isStarted()) {
            return false;
        }
        if (playDevice == null) {
            playDevice = new MaDevice(MiniAudio.mal_device_type_playback, format, channels, ratio);
            playDevice.setDeviceListener(instace);
        }
        return true;
    }

    public static void playStop() {
        if (playDevice != null && playDevice.isStarted()) {
            playDevice.stop();
        }
        playDevice = null;
    }

    /**
     * ========================================================================================================
     * capture
     * ========================================================================================================
     */

    static ByteArrayOutputStream baos = new ByteArrayOutputStream(1024 * 1024);
    static byte[] capData;
    static byte[] capZipData;
    static MaDevice capDevice = null;
    static long capStartAt;

    public static void captureStart() {

        playStop();
        captureStop();

        baos.reset();
        capData = null;
        capZipData = null;
        if (capDevice == null) {
            capDevice = new MaDevice(MiniAudio.mal_device_type_capture, format, channels, ratio);
            capDevice.setDeviceListener(instace);
        }
        //System.out.println("begin capture ");

        capStartAt = System.currentTimeMillis();
        capDevice.start();

    }

    public static void captureStop() {
        if (capDevice != null) {
            capStartAt = 0;
            instace.setAudioListener(null);
            capDevice.stop();
            capDevice = null;//release capDevice for uninit
        }
    }

    public static byte[] getCaptureData() {
        if (capData == null && baos.size() > 0) {
            capData = baos.toByteArray();
        }
        return capData;
    }

    public static byte[] getCaptureZipData() {
        if (capZipData == null && baos.size() > 0) {
            capZipData = Zip.compress0(baos.toByteArray());
        }
        return capZipData;
    }

    public static void playCapAudio() {
        captureStop();

        byte[] b = getCaptureData();

        playData(b);
    }

    public static boolean isCapturing() {
        if (capDevice != null) {
            return capDevice.isStarted();
        }
        return false;
    }

    public static float getZipDataTime(byte[] zipdata) {
        if (zipdata == null) {
            return 0;
        }
        byte[] b = Zip.extract0(zipdata);
        return getDataTime(b);
    }

    public static float getDataTime(byte[] data) {
        return (float) data.length / channels / ratio / MaDevice.getFormatBytes(format);
    }
}
