/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.sun.cldc.io;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

/**
 *
 * @author gust
 */
public class ConsoleInputStream extends InputStream {

    @Override
    public native int read() throws IOException;

    public String readLine() {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            int ch = 0;
            while ((ch = read()) != '\n') {
                baos.write(ch);
            }
            String s = new String(baos.toByteArray(), "utf-8");
            return s;
        } catch (Exception ex) {
        }
        return "";
    }
}
