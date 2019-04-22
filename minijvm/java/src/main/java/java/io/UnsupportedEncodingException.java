/*
 * Copyright C 2003 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 * 
 */
package java.io;

/**
 * The Character Encoding is not supported.
 *
 * @author  Asmus Freytag
 * @version 12/17/01 (CLDC 1.1)
 * @since   JDK1.1, CLDC 1.0
 */
public class UnsupportedEncodingException
    extends IOException
{
    /**
     * Constructs an UnsupportedEncodingException without a detail message.
     */
    public UnsupportedEncodingException() {
        super();
    }

    /**
     * Constructs an UnsupportedEncodingException with a detail message.
     * @param s Describes the reason for the exception.
     */
    public UnsupportedEncodingException(String s) {
        super(s);
    }
}

