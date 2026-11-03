/* SPDX-License-Identifier: MIT */
package com.sun.cldc.io;

import java.io.IOException;
import java.io.OutputStream;

/** Native-backed standard output stream. */
public final class ConsoleOutputStream extends OutputStream {
    public native void write(int value) throws IOException;
}
