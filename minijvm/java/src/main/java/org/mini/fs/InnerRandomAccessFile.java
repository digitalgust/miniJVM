/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.mini.fs;

import java.io.IOException;

/**
 *
 * open file for random access
 *
 * filemode : r rw rws rwd
 *
 * <pre>
 *    void t17() {
 *        try {
 *            RandomAccessFile c = new RandomAccessFile("./c.txt", "rw");
 *            c.seek(0);
 *            String r = "这是一个测试";
 *            System.out.println(r);
 *            byte[] carr = r.getBytes("utf-8");
 *            c.write(carr, 0, carr.length);
 *            c.close();
 *            RandomAccessFile c1 = new RandomAccessFile("./c.txt", "r");
 *            c1.seek(0);
 *            byte[] barr = new byte[256];
 *            int len;
 *            len = c1.read(barr, 0, 256);
 *            System.out.println("len=" + len);
 *            c1.close();
 *            String s = new String(barr, 0, len, "utf-8");
 *            System.out.println(s);
 *        } catch (IOException ex) {
 *            System.out.println(ex.getMessage());
 *        }
 *    }
 * </pre>
 *
 * @author gust
 */
public class InnerRandomAccessFile extends InnerFile {

    boolean flush = false;

    public InnerRandomAccessFile(String ppath, String pmode) {
        //System.out.println("pmode:" + pmode);
        this.path = ppath;
        if ("r".equals(pmode)) {
            this.mode = "rb";
        } else if ("rw".equals(pmode)) {
            this.mode = "rb+";
        } else if ("rws".equals(pmode)) {
            this.mode = "rb+";
            flush = true;
        } else if ("rwd".equals(pmode)) {
            this.mode = "rb+";
            flush = true;
        } else {
            this.mode = "rb+";
        }
        filePointer = openFile(InnerFile.getPathBytesForNative(path), mode.getBytes());
        if (filePointer == 0 && "rb+".equals(this.mode)) {// file not exists , create new 
            this.mode = "wb+";
            filePointer = openFile(InnerFile.getPathBytesForNative(path), mode.getBytes());
        }
        if (filePointer == 0) {
            throw new RuntimeException("open file error:" + path);
        }
    }

    public void close() throws IOException {
        closeFile(getFilePointer());
        filePointer = 0;
    }

    public int read(byte[] b, int off, int len) throws IOException {
        return readbuf(getFilePointer(), b, off, len);
    }

    public int write(byte[] b, int off, int len) throws IOException {
        int ret = writebuf(getFilePointer(), b, off, len);
        if (flush) {
            flush0(getFilePointer());
        }
        return ret;
    }

    public int seek(long pos) throws IOException {
        return seek0(getFilePointer(), pos);
    }

    public int setLength(long length) throws IOException {
        return setLength0(getFilePointer(), length);
    }
}
