/*
 * @(#)EUC_JP_Writer.java	1.9 02/07/24 @(#)
 *
 * Copyright (c) 1999-2001 Sun Microsystems, Inc.  All rights reserved.
 * PROPRIETARY/CONFIDENTIAL
 * Use is subject to license terms.
 */

package com.sun.cldc.i18n.mini;

/**
 * EUC_JP character encoded Writer.
 */
public class EUC_JP_Writer extends Gen_Writer {
    /**
     * Constructor for EUC_JP Writer.
     * @exception ClassNotFoundException is thrown if the conversion
     * class is not available
     */
    public EUC_JP_Writer() throws ClassNotFoundException {
        super("EUC_JP");
    }
}
