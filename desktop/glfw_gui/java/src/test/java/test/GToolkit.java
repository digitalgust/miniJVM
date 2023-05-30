package test;

import org.mini.gl.GL;
import org.mini.nanovg.Nanovg;
import org.mini.reflect.DirectMemObj;
import org.mini.reflect.ReflectArray;

import java.io.FileInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

import static org.mini.gl.GL.*;
import static org.mini.nanovg.Nanovg.stbi_load;

public class GToolkit {

    public static long getArrayDataPtr(Object array) {
        return ReflectArray.getBodyPtr(array);
    }

    public static byte[] readFileFromFile(String path) {
        try {

            InputStream is = new FileInputStream(path);
            if (is != null) {
                int av = is.available();

                if (av >= 0) {
                    byte[] b = new byte[av];
                    int r, read = 0;
                    while (read < av) {
                        r = is.read(b, read, av - read);
                        read += r;
                    }
                    return b;
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        System.out.println("load from file fail : " + path);
        return null;
    }

    public static byte[] readFileFromJar(String path) {
        try {

            InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(path);
            if (is != null) {
                int av = is.available();

                if (av >= 0) {
                    byte[] b = new byte[av];
                    int r, read = 0;
                    while (read < av) {
                        r = is.read(b, read, av - read);
                        read += r;
                    }
                    return b;
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        System.out.println("load from jar fail : " + path);
        return null;
    }

    public static String readFileFromJarAsString(String path, String encode) {
        try {
            byte[] cont = readFileFromJar(path);
            String s = new String(cont, encode);
            return s;
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

    static public byte[] toCstyleBytes(String s) {
        if (s == null) {
            return null;
        }
        if (s.length() == 0 || s.charAt(s.length() - 1) != '\000') {
            s += '\000';
        }
        byte[] barr = null;
        try {
            barr = s.getBytes("utf-8");
        } catch (UnsupportedEncodingException ex) {
        }
        return barr;
    }


    public static void checkGlError(String tag) {
        int err = glGetError();
        if (err != 0) {
            System.out.println("gl error tag:" + tag + "  code:" + err + "[" + Integer.toHexString(err) + "]");
            new Throwable().printStackTrace();
        }
    }

    public static byte[] image_parse(long raw_data_handle, int[] w_h_d) {
        if (raw_data_handle == 0) {
            System.out.println("ERROR: image raw data is null");
            return null;
        }
        int w = w_h_d[0];
        int h = w_h_d[1];
        int de = w_h_d[2];
        DirectMemObj dmo = new DirectMemObj(raw_data_handle, w * h * de);

        //find pow 2
        byte[] d = new byte[w * h * de];

        dmo.copyTo(0, d, 0, d.length);
        return d;
    }


    public static byte[] image_parse_from_file_content(byte[] fileCont, int start, int len, int[] w_h_d) {
        if (fileCont == null) {
            System.out.println("ERROR: image parse file content is null");
        }
        int[] x = {0}, y = {0}, n = {0};
        long ptr = ReflectArray.getBodyPtr(fileCont);
        long raw_data_handle = Nanovg.stbi_load_from_memory(ptr + start, len, x, y, n, 0);
        if (raw_data_handle == 0) {
            System.out.println("ERROR: failed to load image from file content start:" + start + ", size:" + len);
            new Exception().printStackTrace();
            return null;
        }
        w_h_d[0] = x[0];
        w_h_d[1] = y[0];
        w_h_d[2] = n[0];

        byte[] b = image_parse(raw_data_handle, w_h_d);
        Nanovg.stbi_image_free(raw_data_handle);
        return b;
    }

    public static byte[] image_parse_from_file_path(String filename, int[] w_h_d) {
        int[] x = {0}, y = {0}, n = {0};
        byte[] fb = toCstyleBytes(filename);
        long raw_data_handle = stbi_load(fb, x, y, n, 4);
        if (raw_data_handle == 0) {
            System.out.println("ERROR: failed to load image: " + filename);
            return null;
        }
        w_h_d[0] = x[0];
        w_h_d[1] = y[0];
        w_h_d[2] = n[0];

        byte[] b = image_parse(raw_data_handle, w_h_d);
        Nanovg.stbi_image_free(raw_data_handle);
        return b;
    }

    public static byte[] image_parse_from_file_content(byte[] fileCont, int[] w_h_d) {
        return image_parse_from_file_content(fileCont, 0, fileCont.length, w_h_d);
    }

    public static int gl_image_load(byte[] fileCont, int[] w_h_d) {

        byte[] d = image_parse_from_file_content(fileCont, w_h_d);
        int format = w_h_d[2] < 4 ? GL_RGB : GL_RGBA;
        return genTexture2D(d, w_h_d[0], w_h_d[1], format, format);
    }

    public static int gl_image_load(String filename, int[] w_h_d) {

        byte[] d = image_parse_from_file_path(filename, w_h_d);

        int format = w_h_d[2] < 4 ? GL_RGB : GL_RGBA;
        return genTexture2D(d, w_h_d[0], w_h_d[1], format, format);
    }


    static public int genTexture2D(byte[] data, int w, int h, int gl_inner_format, int gl_format) {
        int[] tex = {0};
        glGenTextures(1, tex, 0);
        glBindTexture(GL_TEXTURE_2D, tex[0]);
        glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glTexImage2D(GL_TEXTURE_2D, 0, gl_inner_format, w, h, 0, gl_format, GL_UNSIGNED_BYTE, data, 0);
        glGenerateMipmap(GL_TEXTURE_2D);
        return tex[0];
    }

    static public void printMat4(float[] mat4) {
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                System.out.print(((i == 0 && j == 0) ? " " : ", ") + mat4[i * 4 + j]);
            }
            System.out.println();
        }
        System.out.println();
    }

    static public void printGlVersion() {
        byte[] b;
        String name = new String(glGetString(GL.GL_VENDOR)); //返回负责当前OpenGL实现厂商的名字
        String biaoshifu = new String(glGetString(GL_RENDERER)); //返回一个渲染器标识符，通常是个硬件平台
        String OpenGLVersion = new String(glGetString(GL_VERSION)); //返回当前OpenGL实现的版本号
        b = glGetString(GL.GL_MAJOR_VERSION);
        String majorVersion = b == null ? "" : new String(b);
        b = glGetString(GL.GL_MINOR_VERSION);
        String minorVersion = b == null ? "" : new String(b);
        System.out.println("OpenGL vendor：" + name);
        System.out.println("OpenGL renderer：" + biaoshifu);
        System.out.println("OpenGL version：" + OpenGLVersion);
        System.out.println("OpenGL version：" + majorVersion + "." + minorVersion);
    }
}
