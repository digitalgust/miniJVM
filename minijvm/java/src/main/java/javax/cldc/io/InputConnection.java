
package javax.cldc.io;

import java.io.*;

public interface InputConnection extends Connection {

    /**
     * Open and return an input stream for a connection.
     *
     * @return                 An input stream
     * @exception IOException  If an I/O error occurs
     */
    public InputStream openInputStream() throws IOException;

    /**
     * Open and return a data input stream for a connection.
     *
     * @return                 An input stream
     * @exception IOException  If an I/O error occurs
     */
    public DataInputStream openDataInputStream() throws IOException;
}

