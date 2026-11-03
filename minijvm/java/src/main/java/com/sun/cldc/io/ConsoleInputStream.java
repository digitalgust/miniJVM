/* SPDX-License-Identifier: MIT */
package com.sun.cldc.io;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/** Native-backed standard input stream. */
public final class ConsoleInputStream extends InputStream {
    public native int read() throws IOException;

    public String readLine() {
        ByteArrayOutputStream line = new ByteArrayOutputStream();
        try {
            for (;;) {
                int value = read();
                if (value < 0 || value == '\n') {
                    break;
                }
                if (value != '\r') {
                    line.write(value);
                }
            }
            return new String(line.toByteArray(), "UTF-8");
        } catch (IOException failure) {
            return "";
        }
    }
}
