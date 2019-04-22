/*
 * Copyright C 2003 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package com.sun.cldc.io;

import java.io.OutputStream;
import java.io.IOException;

public class ConsoleOutputStream extends OutputStream {

    /**
     * Writes the specified byte to this output stream.
     *
     * @param      b   the <code>byte</code>.
     * @exception  IOException  if an I/O error occurs. In particular,
     *             an <code>IOException</code> may be thrown if the
     *             output stream has been closed.
     */
     public native synchronized void write(int c) throws IOException;
}

