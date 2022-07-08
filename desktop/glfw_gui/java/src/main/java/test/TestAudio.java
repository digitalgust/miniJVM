package test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import org.mini.media.MaDecoder;
import org.mini.media.MiniAudio;
import org.mini.media.audio.AudioListener;
import org.mini.media.MaDevice;
import org.mini.media.audio.AudioManager;

class TestAudio {

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

    static void t1() {
        MaDecoder decoder = new MaDecoder("./bibi.flac");
        AudioManager.playDecoder(decoder);
    }

    static void t2() {

        int format = MiniAudio.mal_format_s16;
        int channels = 2;
        int ratio = 22050;


        AudioListener callback = new AudioListener() {
            @Override
            public void onCapture(int millSecond, byte[] data) {
            }

            @Override
            public void onPlayback(int millSecond, byte[] data) {
            }

            @Override
            public void onStop() {
            }
        };
        AudioManager.setAudioListener(callback);
        AudioManager.captureStart();

        byte[] b = AudioManager.getCaptureData();
        AudioManager.playData(b);

    }

    static void printArr(byte[] b, int pos) {
        for (int i = 0; i < 20 && pos + i < b.length; i++) {
            System.out.print((char) (65 + (b[pos + i] % 26)));
        }
        System.out.println();
    }

    public static void main(String[] args) {
        t1();
//        t2();
    }

}
