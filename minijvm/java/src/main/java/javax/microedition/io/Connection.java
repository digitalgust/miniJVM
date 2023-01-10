/*
 *
 * Copyright  1990-2008 Sun Microsystems, Inc. All Rights Reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License version
 * 2 only, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License version 2 for more details (a copy is
 * included at /legal/license.txt).
 *
 * You should have received a copy of the GNU General Public License
 * version 2 along with this work; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA
 *
 * Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa
 * Clara, CA 95054 or visit www.sun.com if you need additional
 * information or have any questions.
 *
 */



package javax.microedition.io;

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

