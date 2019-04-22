/*
 * Copyright C 2003 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package javax.cldc.io;

import java.io.*;

/**
 * This interface defines the capabilities that a stream connection
 * must have.
 * <p>
 * In a typical implementation of this interface (for instance
 * in MIDP 2.0), all <code>StreamConnections</code> have one 
 * underlying <code>InputStream</code> and one <code>OutputStream</code>.
 * Opening a <code>DataInputStream</code> counts as opening an
 * <code>InputStream</code> and opening a <code>DataOutputStream</code>
 * counts as opening an <code>OutputStream</code>.  Trying to open
 * another <code>InputStream</code> or <code>OutputStream</code>
 * causes an <code>IOException</code>.  Trying to open the
 * <code>InputStream</code> or <code>OutputStream</code> after
 * they have been closed causes an <code>IOException</code>.
 * <p>
 * The methods of <code>StreamConnection</code> are not 
 * synchronized.  The only stream method that can be called safely
 * in another thread is <code>close</code>.
 *
 * @author  Nik Shaylor, Antero Taivalsaari
 * @version 12/17/01 (CLDC 1.1)
 * @since   CLDC 1.0
 */
public interface StreamConnection extends InputConnection, OutputConnection {
}

