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


package java.lang;

/**
 * The class <code>Exception</code> and its subclasses are a form of
 * <code>Throwable</code> that indicates conditions that a reasonable
 * application might want to catch.
 *
 * @author Frank Yellin
 * @version 12/17/01 (CLDC 1.1)
 * @see java.lang.Error
 * @since JDK1.0, CLDC 1.0
 */
public class Exception extends Throwable {

    /**
     * Constructs an <code>Exception</code> with no specified detail message.
     */
    public Exception() {
        super();
    }

    /**
     * Constructs an <code>Exception</code> with the specified detail message.
     *
     * @param s the detail message.
     */
    public Exception(String s) {
        super(s);
    }

    public Exception(Throwable cause) {
        this.cause = cause;
    }

    public Exception(String message, Throwable cause) {
        super(message, cause);
    }
}
