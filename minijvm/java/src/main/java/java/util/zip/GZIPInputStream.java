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
public class GZIPInputStream {

    InputStream in;
    ByteArrayInputStream bais;

    public GZIPInputStream(InputStream in) throws IOException {
        this.in = in;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int b;
        while ((b = in.read()) != -1) {
            baos.write(b);
        }
        byte[] e = Zip.gzipExtract0(baos.toByteArray());
        bais = new ByteArrayInputStream(e);
    }

    public int read() throws IOException {
        return bais.read();
    }

    public void close() throws IOException {
        in.close();
    }

}
