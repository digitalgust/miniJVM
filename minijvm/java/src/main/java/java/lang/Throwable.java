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

import java.io.IOException;
import java.io.Writer;

/**
 * The <code>Throwable</code> class is the superclass of all errors and
 * exceptions in the Java language. Only objects that are instances of this
 * class (or of one of its subclasses) are thrown by the Java Virtual Machine or
 * can be thrown by the Java <code>throw</code> statement. Similarly, only this
 * class or one of its subclasses can be the argument type in a
 * <code>catch</code> clause.
 * <p>
 * Instances of two subclasses, {@link java.lang.Error} and
 * {@link java.lang.Exception}, are conventionally used to indicate that
 * exceptional situations have occurred. Typically, these instances are freshly
 * created in the context of the exceptional situation so as to include relevant
 * information (such as stack trace data).
 * <p>
 * By convention, class <code>Throwable</code> and its subclasses have two
 * constructors, one that takes no arguments and one that takes a
 * <code>String</code> argument that can be used to produce an error message.
 * <p>
 * A <code>Throwable</code> class contains a snapshot of the execution stack of
 * its thread at the time it was created. It can also contain a message string
 * that gives more information about the error.
 * <p>
 * Here is one example of catching an exception:
 * <p>
 * <blockquote><pre>
 *     try {
 *         int a[] = new int[2];
 *         int b = a[4];
 *     } catch (ArrayIndexOutOfBoundsException e) {
 *         System.out.println("exception: " + e.getMessage());
 *         e.printStackTrace();
 *     }
 * </pre></blockquote>
 *
 * @author unascribed
 * @version 12/17/01 (CLDC 1.1)
 * @since JDK1.0, CLDC 1.0
 */
public class Throwable {

    Throwable cause;
    /**
     * WARNING: this must be the first variable. Specific details about the
     * <code>Throwable</code> object.
     */
    private String detailMessage;

    /**
     * WARNING: this must be the second variable. Native code saves some
     * indication of the stack backtrace in this slot.
     */
    private transient Object backtrace = buildStackElement();//

    /**
     * Constructs a new <code>Throwable</code> with <code>null</code> as its
     * error message string.
     */
    public Throwable() {
    }

    /**
     * Constructs a new <code>Throwable</code> with the specified error message.
     *
     * @param message the error message. The error message is saved for later
     * retrieval by the {@link #getMessage()} method.
     */
    public Throwable(String message) {
        detailMessage = message;
    }

    public Throwable(Throwable cause) {
        this.cause = cause;
    }

    public Throwable(String message, Throwable cause) {
        detailMessage = message;
        this.cause = cause;
    }

    /**
     * Returns the error message string of this <code>Throwable</code> object.
     *
     * @return the error message string of this <code>Throwable</code> object if
     * it was {@link #Throwable(String) created} with an error message string;
     * or <code>null</code> if it was {@link #Throwable() created} with no error
     * message.
     *
     */
    public String getMessage() {
        return detailMessage;
    }

    public Throwable getCause() {
        return cause;
    }

    /**
     * Returns a short description of this <code>Throwable</code> object. If
     * this <code>Throwable</code> object was {@link #Throwable(String) created}
     * with an error message string, then the result is the concatenation of
     * three strings:
     * <ul>
     * <li>The name of the actual class of this object
     * <li>": " (a colon and a space)
     * <li>The result of the {@link #getMessage} method for this object
     * </ul>
     * If this <code>Throwable</code> object was {@link #Throwable() created}
     * with no error message string, then the name of the actual class of this
     * object is returned.
     *
     * @return a string representation of this <code>Throwable</code>.
     */
    public String toString() {
        String s = getClass().getName();
        String message = getMessage();
        return (message != null) ? (s + ": " + message) : s;
    }

    /**
     * Prints this <code>Throwable</code> and its backtrace to the standard
     * error stream. This method prints a stack trace for this
     * <code>Throwable</code> object on the error output stream that is the
     * value of the field <code>System.err</code>. The first line of output
     * contains the result of the {@link #toString()} method for this object.
     * <p>
     *
     * The format of the backtrace information depends on the implementation.
     */
    public void printStackTrace() {
        java.io.PrintStream err = System.err;
        err.print(getCodeStack());

    }

    public void printStackTrace(Writer writer) {
        try {
            writer.write(getCodeStack());
        } catch (IOException ex) {
        }

    }

    public StackTraceElement[] getStackTrace() {

        if (backtrace != null) {
            int count = 0;
            StackTraceElement sf = (StackTraceElement) backtrace;
            while (sf != null) {
                try {
                    Class clazz = Class.forName(sf.getDeclaringClass());
                    if (!clazz.isAssignableFrom(Throwable.class)) {
                        count++;
                    }
                } catch (ClassNotFoundException ex) {
                }
                sf = sf.parent;
            }
            StackTraceElement[] arr = new StackTraceElement[count];
            sf = (StackTraceElement) backtrace;
            count = 0;
            while (sf != null) {
                try {
                    Class clazz = Class.forName(sf.getDeclaringClass());
                    if (!clazz.isAssignableFrom(Throwable.class)) {
                        arr[count++] = sf;
                    }
                } catch (ClassNotFoundException ex) {
                }
                sf = sf.parent;
            }
            return arr;
        }
        return new StackTraceElement[0];
    }

    public String getCodeStack() {
        StringBuilder stack = new StringBuilder();
        String msg = getMessage();
        stack.append(this.getClass().getName()).append(": ").append(msg == null ? "" : msg).append("\n");
        if (backtrace != null) {
            StackTraceElement sf = (StackTraceElement) backtrace;
            while (sf != null) {
                try {
                    Class clazz = Class.forName(sf.getDeclaringClass());
                    if (!clazz.isAssignableFrom(Throwable.class)) {
                        stack.append("    at ").append(sf.getDeclaringClass());
                        stack.append(".").append(sf.getMethodName());
                        stack.append("(").append(sf.getFileName());
                        stack.append(":").append(sf.getLineNumber());
                        stack.append(")\n");
                    }
                    sf = sf.parent;
                } catch (Exception e) {
                }
            }
        }
        return stack.toString();
    }

    private native StackTraceElement buildStackElement();

    /* The given object must have a void println(char[]) method */
    //private native void printStackTrace0(Object s);
}
