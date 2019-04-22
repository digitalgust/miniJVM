/*
 * @(#)SJIS_Reader.java	1.10 02/07/24 @(#)
 *
 * Copyright (c) 1999-2001 Sun Microsystems, Inc.  All rights reserved.
 * PROPRIETARY/CONFIDENTIAL
 * Use is subject to license terms.
 */

package com.sun.cldc.i18n.mini;

/**
 * SJIS character encoded Reader.
 */
public class SJIS_Reader extends Gen_Reader {
    /**
     * Constructor for SJIS Reader.
     * @exception ClassNotFoundException is thrown if the conversion
     * class is not available
     */
    public SJIS_Reader() throws ClassNotFoundException {
        super("SJIS");
    }
}
