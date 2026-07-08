/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package java.util.zip;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.mini.zip.Zip;

/**
 * @author Gust
 */
public class GZIPInputStream extends InputStream {

    private final InputStream in;
    private final ByteArrayInputStream bais;

    public GZIPInputStream(InputStream in) throws IOException {
        if (in == null) {
            throw new NullPointerException();
        }
        this.in = in;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int b;
        while ((b = in.read()) != -1) {
            baos.write(b);
        }
        byte[] e = Zip.gzipExtract0(baos.toByteArray());
        if (e == null) {
            throw new IOException("Invalid GZIP data.");
        }
        bais = new ByteArrayInputStream(e);
    }

    @Override
    public int read() throws IOException {
        return bais.read();
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        return bais.read(b, off, len);
    }

    @Override
    public int available() throws IOException {
        return bais.available();
    }

    @Override
    public void close() throws IOException {
        bais.close();
        in.close();
    }
}

