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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
     *                retrieval by the {@link #getMessage()} method.
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


    // Setting this static field introduces an acceptable
    // initialization dependency on a few java.util classes.
    private static final List<Throwable> SUPPRESSED_SENTINEL =
            Collections.unmodifiableList(new ArrayList<Throwable>(0));

    /**
     * The list of suppressed exceptions, as returned by {@link
     * #getSuppressed()}.  The list is initialized to a zero-element
     * unmodifiable sentinel list.  When a serialized Throwable is
     * read in, if the {@code suppressedExceptions} field points to a
     * zero-element list, the field is reset to the sentinel value.
     *
     * @serial
     * @since 1.7
     */
    private List<Throwable> suppressedExceptions = SUPPRESSED_SENTINEL;

    /** Message for trying to suppress a null exception. */
    private static final String NULL_CAUSE_MESSAGE = "Cannot suppress a null exception.";

    /** Message for trying to suppress oneself. */
    private static final String SELF_SUPPRESSION_MESSAGE = "Self-suppression not permitted";

    /** Caption  for labeling causative exception stack traces */
    private static final String CAUSE_CAPTION = "Caused by: ";

    /** Caption for labeling suppressed exception stack traces */
    private static final String SUPPRESSED_CAPTION = "Suppressed: ";

    /**
     * Appends the specified exception to the exceptions that were
     * suppressed in order to deliver this exception. This method is
     * thread-safe and typically called (automatically and implicitly)
     * by the {@code try}-with-resources statement.
     *
     * <p>The suppression behavior is enabled <em>unless</em> disabled
     * {@linkplain #Throwable(String, Throwable, boolean, boolean) via
     * a constructor}.  When suppression is disabled, this method does
     * nothing other than to validate its argument.
     *
     * <p>Note that when one exception {@linkplain
     * #initCause(Throwable) causes} another exception, the first
     * exception is usually caught and then the second exception is
     * thrown in response.  In other words, there is a causal
     * connection between the two exceptions.
     *
     * In contrast, there are situations where two independent
     * exceptions can be thrown in sibling code blocks, in particular
     * in the {@code try} block of a {@code try}-with-resources
     * statement and the compiler-generated {@code finally} block
     * which closes the resource.
     *
     * In these situations, only one of the thrown exceptions can be
     * propagated.  In the {@code try}-with-resources statement, when
     * there are two such exceptions, the exception originating from
     * the {@code try} block is propagated and the exception from the
     * {@code finally} block is added to the list of exceptions
     * suppressed by the exception from the {@code try} block.  As an
     * exception unwinds the stack, it can accumulate multiple
     * suppressed exceptions.
     *
     * <p>An exception may have suppressed exceptions while also being
     * caused by another exception.  Whether or not an exception has a
     * cause is semantically known at the time of its creation, unlike
     * whether or not an exception will suppress other exceptions
     * which is typically only determined after an exception is
     * thrown.
     *
     * <p>Note that programmer written code is also able to take
     * advantage of calling this method in situations where there are
     * multiple sibling exceptions and only one can be propagated.
     *
     * @param exception the exception to be added to the list of
     *        suppressed exceptions
     * @throws IllegalArgumentException if {@code exception} is this
     *         throwable; a throwable cannot suppress itself.
     * @throws NullPointerException if {@code exception} is {@code null}
     * @since 1.7
     */
    public final synchronized void addSuppressed(Throwable exception) {
        if (exception == this)
            throw new IllegalArgumentException(SELF_SUPPRESSION_MESSAGE, exception);

        if (exception == null)
            throw new NullPointerException(NULL_CAUSE_MESSAGE);

        if (suppressedExceptions == null) // Suppressed exceptions not recorded
            return;

        if (suppressedExceptions == SUPPRESSED_SENTINEL)
            suppressedExceptions = new ArrayList<>(1);

        suppressedExceptions.add(exception);
    }

    private static final Throwable[] EMPTY_THROWABLE_ARRAY = new Throwable[0];

    /**
     * Returns an array containing all of the exceptions that were
     * suppressed, typically by the {@code try}-with-resources
     * statement, in order to deliver this exception.
     *
     * If no exceptions were suppressed or {@linkplain
     * #Throwable(String, Throwable, boolean, boolean) suppression is
     * disabled}, an empty array is returned.  This method is
     * thread-safe.  Writes to the returned array do not affect future
     * calls to this method.
     *
     * @return an array containing all of the exceptions that were
     *         suppressed to deliver this exception.
     * @since 1.7
     */
    public final synchronized Throwable[] getSuppressed() {
        if (suppressedExceptions == SUPPRESSED_SENTINEL ||
                suppressedExceptions == null)
            return EMPTY_THROWABLE_ARRAY;
        else
            return suppressedExceptions.toArray(EMPTY_THROWABLE_ARRAY);
    }
    /**
     * Prints this <code>Throwable</code> and its backtrace to the standard
     * error stream. This method prints a stack trace for this
     * <code>Throwable</code> object on the error output stream that is the
     * value of the field <code>System.err</code>. The first line of output
     * contains the result of the {@link #toString()} method for this object.
     * <p>
     * <p>
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
                    Class clazz = Class.forName(sf.getClassName(), false, sf.getDeclaringClass().getClassLoader());
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
                    Class clazz = Class.forName(sf.getClassName(), false, sf.getDeclaringClass().getClassLoader());
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
                    Class clazz = Class.forName(sf.getClassName(), false, sf.getDeclaringClass().getClassLoader());
                    if (!clazz.isAssignableFrom(Throwable.class)) {
                        stack.append("    at ").append(sf.getClassName());
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
