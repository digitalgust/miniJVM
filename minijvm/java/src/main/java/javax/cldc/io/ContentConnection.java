/*
 * Copyright C 2003 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package javax.cldc.io;

import java.io.*;

/**
 * This interface defines the stream connection over which
 * content is passed.
 *
 * @author  Nik Shaylor
 * @version 12/17/01 (CLDC 1.1)
 * @since   CLDC 1.0
 */
public interface ContentConnection extends StreamConnection {

    /**
     * Returns the type of content that the resource connected to is
     * providing.  For instance, if the connection is via HTTP, then
     * the value of the <code>content-type</code> header field is returned.
     *
     * @return  the content type of the resource that the URL references,
     *          or <code>null</code> if not known.
     */
    public String getType();

    /**
     * Returns a string describing the encoding of the content which
     * the resource connected to is providing.
     * E.g. if the connection is via HTTP, the value of the
     * <code>content-encoding</code> header field is returned.
     *
     * @return  the content encoding of the resource that the URL
     *          references, or <code>null</code> if not known.
     */
    public String getEncoding();

    /**
     * Returns the length of the content which is being provided.
     * E.g. if the connection is via HTTP, then the value of the
     * <code>content-length</code> header field is returned.
     *
     * @return  the content length of the resource that this connection's
     *          URL references, or <code>-1</code> if the content length
     *          is not known.
     */
    public long getLength();

}

