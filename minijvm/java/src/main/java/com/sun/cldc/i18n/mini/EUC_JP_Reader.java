/*
 * @(#)EUC_JP_Reader.java	1.9 02/07/24 @(#)
 *
 * Copyright (c) 1999-2001 Sun Microsystems, Inc.  All rights reserved.
 * PROPRIETARY/CONFIDENTIAL
 * Use is subject to license terms.
 */

package com.sun.cldc.i18n.mini;
/**
 * EUC_JP character encoded Reader.
 */
public class EUC_JP_Reader extends Gen_Reader {
    /**
     * Constructor for EUC_JP Reader.
     * @exception ClassNotFoundException is thrown if the conversion
     * class is not available
     */
    public EUC_JP_Reader() throws ClassNotFoundException {
        super("EUC_JP");
    }
}
