/*
 * Copyright C 2003 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package javax.cldc.io;

import java.io.*;

/**
 * This class is used to signal that a connection target
 * cannot be found, or the protocol type is not supported. 
 *
 * @author  Nik Shaylor
 * @version 12/17/01 (CLDC 1.1)
 * @since   CLDC 1.0
 */
public class ConnectionNotFoundException extends IOException {
    /**
     * Constructs a ConnectionNotFoundException with no detail 
     * message.
     */
    public ConnectionNotFoundException() {
        super();
    }

    /**
     * Constructs a ConnectionNotFoundException with the 
     * specified detail message.  A detail message is a String 
     * that describes this particular exception.
     * @param s the detail message
     */
    public ConnectionNotFoundException(String s) {
        super(s);
    }
}

