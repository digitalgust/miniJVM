/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package java.util.zip;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.mini.zip.Zip;

/**
 * @author Gust
 */
public class GZIPOutputStream {

    OutputStream out;
    ByteArrayOutputStream baos = new ByteArrayOutputStream();

    public GZIPOutputStream(OutputStream out) throws IOException {
        this.out = out;
    }

    public synchronized void write(byte[] bytes, int i, int i1) throws IOException {
        baos.write(bytes, i, i1);
    }

    public void finish() throws IOException {
        byte[] src = baos.toByteArray();
        byte[] dst = Zip.gzipCompress0(src);
        out.write(dst);
        out.flush();
    }

    public void close() throws IOException {
        finish();
        out.close();
    }

}
