package test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import org.mini.media.AudioCallback;
import org.mini.media.AudioDevice;
import org.mini.media.AudioMgr;

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
        byte[] b = readFile("./bibi.flac");
        AudioMgr.playData(b);
    }

    static void t2() {

        int format = AudioDevice.mal_format_s16;
        int channels = 2;
        int ratio = 22050;

        
        AudioCallback callback=new AudioCallback() {
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
        AudioMgr.setCallback(callback);
        AudioMgr.captureStart();

        byte[] b=AudioMgr.getCaptureData();
        AudioMgr.playData(b);

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
