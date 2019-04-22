/*
 * @(#)MS932_Writer.java	1.7 02/07/24 @(#)
 *
 * Copyright (c) 1999-2001 Sun Microsystems, Inc.  All rights reserved.
 * PROPRIETARY/CONFIDENTIAL
 * Use is subject to license terms.
 */

package com.sun.cldc.i18n.mini;
/**
 * MS932 character encoded Writer.
 */

public class MS932_Writer extends Gen_Writer {
    /**
     * Constructor for MS932 Writer.
     * @exception ClassNotFoundException is thrown if the conversion
     * class is not available
     */
    public MS932_Writer() throws ClassNotFoundException {
        super("MS932");
    }
}
