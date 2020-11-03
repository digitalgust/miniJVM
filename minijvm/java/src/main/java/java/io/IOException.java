/*
 * Copyright (c) 2003, 2006, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */


package java.io;

/**
 * Signals that an I/O exception of some sort has occurred. This class is the
 * general class of exceptions produced by failed or interrupted I/O operations.
 *
 * @author unascribed
 * @version 12/17/01 (CLDC 1.1)
 * @see java.io.InputStream
 * @see java.io.OutputStream
 * @since JDK1.0, CLDC 1.0
 */
public class IOException extends Exception {

    /**
     * Constructs an <code>IOException</code> with <code>null</code> as its
     * error detail message.
     */
    public IOException() {
        super();
    }

    /**
     * Constructs an <code>IOException</code> with the specified detail message.
     * The error message string <code>s</code> can later be retrieved by the
     * <code>{@link java.lang.Throwable#getMessage}</code> method of class
     * <code>java.lang.Throwable</code>.
     *
     * @param s the detail message.
     */
    public IOException(String s) {
        super(s);
    }

    public IOException(String message, Throwable cause) {
        super(message, cause);
    }

    public IOException(Throwable cause) {
        this(null, cause);
    }
}
