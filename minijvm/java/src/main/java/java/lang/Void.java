/*
 * @(#)Void.java	1.13 03/12/19
 *
 * Copyright 2004 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package java.lang;

/**
 * The Void class is an uninstantiable placeholder class to hold a
 * reference to the Class object representing the Java keyword
 * void.
 *
 * @author  unascribed
 * @version 1.13, 12/19/03
 * @since   JDK1.1
 */
public final
class Void {

    /**
     * The Class object representing the primitive Java type void.
     */
    public static final Class TYPE = Class.getPrimitiveClass("void");

    /*
     * The Void class cannot be instantiated.
     */
    private Void() {}
}
