/*
 * Copyright C 2003 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package javax.cldc.io;

import java.io.*;

/**
 * This interface defines the capabilities that an input stream
 * connection must have.
 *
 * @author  Nik Shaylor
 * @version 12/17/01 (CLDC 1.1)
 * @since   CLDC 1.0
 */
public interface InputConnection extends Connection {

    /**
     * Open and return an input stream for a connection.
     *
     * @return                 An input stream
     * @exception IOException  If an I/O error occurs
     */
    public InputStream openInputStream() throws IOException;

    /**
     * Open and return a data input stream for a connection.
     *
     * @return                 An input stream
     * @exception IOException  If an I/O error occurs
     */
    public DataInputStream openDataInputStream() throws IOException;
}

