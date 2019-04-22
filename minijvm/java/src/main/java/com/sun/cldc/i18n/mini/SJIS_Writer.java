/*
 * @(#)SJIS_Writer.java	1.10 02/07/24 @(#)
 *
 * Copyright (c) 1999-2001 Sun Microsystems, Inc.  All rights reserved.
 * PROPRIETARY/CONFIDENTIAL
 * Use is subject to license terms.
 */

package com.sun.cldc.i18n.mini;

/**
 * SJIS character encoded Writer.
 */
public class SJIS_Writer extends Gen_Writer {
    /**
     * Constructor for SJIS Writer.
     * @exception ClassNotFoundException is thrown if the conversion
     * class is not available
     */
    public SJIS_Writer() throws ClassNotFoundException {
        super("SJIS");
    }
}
