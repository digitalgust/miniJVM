/*
 * Copyright C 2003 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package javax.cldc.io;

import java.io.*;

/**
 * This is the most basic type of generic connection. Only the
 * <code>close</code> method is defined.  No open method is 
 * defined here because opening is always done using the 
 * <code>Connector.open()</code> methods.
 *
 * @author  Nik Shaylor
 * @version 12/17/01 (CLDC 1.1)
 * @since   CLDC 1.0
 */
public interface Connection {

    /**
     * Close the connection.
     * <p>
     * When a connection has been closed, access to any of its methods
     * that involve an I/O operation will cause an <code>IOException</code>
     * to be thrown.
     * Closing an already closed connection has no effect. Streams 
     * derived from the connection may be open when method is called.
     * Any open streams will cause the connection to be held open
     * until they themselves are closed. In this latter case access
     * to the open streams is permitted, but access to the connection
     * is not.
     *
     * @exception IOException  If an I/O error occurs
     */
    public void close() throws IOException;
}

