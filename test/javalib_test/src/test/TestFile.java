/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package test;

import com.sun.cldc.i18n.StreamReader;
import com.sun.cldc.i18n.StreamWriter;
import com.sun.cldc.i18n.mini.UTF_8_Reader;
import com.sun.cldc.i18n.mini.UTF_8_Writer;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.util.Enumeration;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.mini.zip.Zip;

/**
 *
 * @author Gust
 */
public class TestFile {

    void printString(String s) {
        for (int i = 0; i < s.length(); i++) {
            System.out.print(" " + Integer.toHexString((int) (s.charAt(i) & 0xffff)));
        }
        System.out.println();
    }

    void printBytes(String s) {
        try {
            byte[] barr = s.getBytes("utf-8");
            for (int i = 0; i < barr.length; i++) {
                System.out.print(" " + Integer.toHexString((int) (barr[i] & 0xff)));
            }
            System.out.println();
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
        }
    }

    void t13() {
        byte[] b = org.mini.zip.Zip.getEntry("../lib/minijvm_rt.jar", "sys.properties");
        for (int i = 0; i < b.length; i++) {
            System.out.print((char) b[i]);
        }

        Zip.putEntry("../tmp.zip", "aaa/sys.properties", b);
        Zip.putEntry("../tmp.zip", "bbb/sys.properties", b);

    }

    void t14() {
        try {
            ZipFile zipFile = new ZipFile("../lib/minijvm_rt.jar");
            Enumeration e = zipFile.entries();
            for (; e.hasMoreElements();) {
                ZipEntry ze = (ZipEntry) e.nextElement();
                System.out.println("entry:" + ze.getName());
            }
            ZipEntry ze = zipFile.getEntry("sys.properties");
            InputStream is = zipFile.getInputStream(ze);
            int ch;
            while ((ch = is.read()) != -1) {
                System.out.print((char) ch);
            }
        } catch (IOException ex) {
        }
    }

    void t15() {

        try {
            String s = "这是一个测试";
            printBytes(s);
            printString(s);
            File test = new File("./\\/./a.txt");
            System.out.println("path:" + test.getPath());
            System.out.println("parent:" + test.getParent());
//            File test = new File("/cygdrive/d/githome/mini_jvm/mini_jvm/cmake-build-debug/./a.txt");
            System.out.println("full path:" + test.getAbsolutePath());
            System.out.println("file exists:" + test.exists());
            StreamWriter writer = new UTF_8_Writer();
            FileOutputStream fos = new FileOutputStream(test);
            System.out.println("fos=" + fos);
            writer.open(fos, "utf-8");
            writer.write(s);
            writer.close();
            System.out.println("file exists:" + test.exists());

            StreamReader reader = new UTF_8_Reader();
            reader.open(new FileInputStream(test), "utf-8");
            char[] buf = new char[100];
            int len = reader.read(buf, 0, 100);
            reader.close();
            String r = new String(buf, 0, len);
            printString(r);

        } catch (IOException ex) {
            System.out.println(ex.getMessage());
        }
        t15_1();
    }

    void t15_1() {
        File file = new File(".");
        System.out.println("isDir:" + file.isDirectory());
        String[] files = file.list();
        for (int i = 0; i < files.length; i++) {
            System.out.println(files[i]);
        }
    }

    void t16() {
        try {
            File b = new File("./b.txt");
            System.out.println("fullpath:" + b.getAbsolutePath());
            String r = "这是一个测试";
            printBytes(r);
            printString(r);
            DataOutputStream dos = new DataOutputStream(new FileOutputStream(b.getPath(), true));
            dos.writeUTF(r);
            dos.close();
            DataInputStream dis = new DataInputStream(new FileInputStream(b));
            String s = dis.readUTF();
            printBytes(s);
            printString(s);
            dis.close();
        } catch (IOException ex) {
            System.out.println(ex.getMessage());
        }
    }

    void t17() {
        try {
            String s = "---1---2---3";
            System.out.println("s=" + s);
            s = s.substring(s.indexOf('1'));
            System.out.println("s=" + s);
            s = s.replaceAll("-", "=");
            System.out.println("s=" + s);

            File dir = new File("./a.txt");
            dir.mkdirs();
            dir.delete();
            File b = new File("./b.txt");
            b.renameTo(new File("./bb.txt"));
            RandomAccessFile c = new RandomAccessFile("./c.txt", "rw");
            c.seek(0);
            String r = "这是一个测试";
            printBytes(r);
            printString(r);
            byte[] carr = r.getBytes("utf-8");
            c.write(carr, 0, carr.length);
            c.close();
            RandomAccessFile c1 = new RandomAccessFile("./c.txt", "r");
            c1.seek(0);
            byte[] barr = new byte[256];
            int len;
            len = c1.read(barr, 0, 256);
            System.out.println("len=" + len);
            c1.close();
            String s1 = new String(barr, 0, len, "utf-8");
            printBytes(s1);
            printString(s1);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    void t18() {
        try {
            File f = File.createTempFile(null, null);
            System.out.println("tmp file:" + f.getAbsolutePath());
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
    
    void t19(){
        File f=new File("../lib/minijvm_rt.jar");
        System.out.println(f.getParent());
        System.out.println(f.getName());
        System.out.println(f.getAbsolutePath());
        System.out.println(f.getAbsolutePath());
        
    }

    public static void main(String[] args) {
        try {
            TestFile tf = new TestFile();
//            tf.t14();
//            tf.t15();
//            tf.t16();
//            tf.t17();
//            tf.t18();
            tf.t19();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
