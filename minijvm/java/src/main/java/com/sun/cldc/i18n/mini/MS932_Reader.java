/*
 * @(#)MS932_Reader.java	1.7 02/07/24 @(#)
 *
 * Copyright (c) 1999-2001 Sun Microsystems, Inc.  All rights reserved.
 * PROPRIETARY/CONFIDENTIAL
 * Use is subject to license terms.
 */

package com.sun.cldc.i18n.mini;
/**
 * MS932 character encoded Reader.
 */

public class MS932_Reader extends Gen_Reader {
    /**
     * Constructor for MS932 Reader.
     * @exception ClassNotFoundException is thrown if the conversion
     * class is not available
     */
    public MS932_Reader() throws ClassNotFoundException {
        super("MS932");
    }
}
