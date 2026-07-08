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
public class GZIPOutputStream extends OutputStream {

    private final OutputStream out;
    private final ByteArrayOutputStream baos = new ByteArrayOutputStream();
    private boolean finished;
    private boolean closed;

    public GZIPOutputStream(OutputStream out) throws IOException {
        if (out == null) {
            throw new NullPointerException();
        }
        this.out = out;
    }

    @Override
    public synchronized void write(int b) throws IOException {
        ensureOpen();
        baos.write(b);
    }

    @Override
    public synchronized void write(byte[] bytes, int i, int i1) throws IOException {
        ensureOpen();
        baos.write(bytes, i, i1);
    }

    public synchronized void finish() throws IOException {
        ensureOpen();
        if (finished) {
            return;
        }
        byte[] src = baos.toByteArray();
        byte[] dst = Zip.gzipCompress0(src);
        if (dst == null) {
            throw new IOException("GZIP compression failed.");
        }
        out.write(dst);
        out.flush();
        finished = true;
    }

    @Override
    public void flush() throws IOException {
        out.flush();
    }

    @Override
    public void close() throws IOException {
        if (closed) {
            return;
        }
        try {
            finish();
        } finally {
            closed = true;
            out.close();
        }
    }

    private void ensureOpen() throws IOException {
        if (closed) {
            throw new IOException("Stream closed.");
        }
    }

}
