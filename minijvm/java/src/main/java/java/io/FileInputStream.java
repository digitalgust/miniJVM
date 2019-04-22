/*
 * @(#)FileInputStream.java	1.45 00/02/02
 *
 * Copyright 1994-2000 Sun Microsystems, Inc. All Rights Reserved.
 * 
 * This software is the proprietary information of Sun Microsystems, Inc.  
 * Use is subject to license terms.
 * 
 */
package java.io;

import org.mini.fs.InnerFile;

/**
 * A <code>FileInputStream</code> obtains input bytes from a file in a file
 * system. What files are available depends on the host environment.
 *
 * @author Arthur van Hoff
 * @version 1.45, 02/02/00
 * @see java.io.File
 * @see java.io.FileDescriptor
 * @see	java.io.FileOutputStream
 * @since JDK1.0
 */
public class FileInputStream extends InputStream {

    /* File Descriptor - handle to the open file */
    private FileDescriptor fd;
    InputStream ifis;

    /**
     * Creates a <code>FileInputStream</code> by opening a connection to an
     * actual file, the file named by the path name <code>name</code> in the
     * file system. A new <code>FileDescriptor</code> object is created to
     * represent this file connection.
     * <p>
     * First, if there is a security manager, its <code>checkRead</code> method
     * is called with the <code>name</code> argument as its argument.
     * <p>
     * If the named file does not exist, is a directory rather than a regular
     * file, or for some other reason cannot be opened for reading then a
     * <code>IOException</code> is thrown.
     *
     * @param name the system-dependent file name.
     * @exception IOException if the file does not exist, is a
     * directory rather than a regular file, or for some other reason cannot be
     * opened for reading.
     * @exception SecurityException if a security manager exists and its
     * <code>checkRead</code> method denies read access to the file.
     * @see java.lang.SecurityManager#checkRead(java.lang.String)
     */
    public FileInputStream(String name) throws IOException {
        fd = new FileDescriptor();
        open(name);
        fd.fd = (int) ((InnerFile.InnerFileInputStream) ifis).getInnerFile().getFilePointer();
    }

    /**
     * Creates a <code>FileInputStream</code> by opening a connection to an
     * actual file, the file named by the <code>File</code> object
     * <code>file</code> in the file system. A new <code>FileDescriptor</code>
     * object is created to represent this file connection.
     * <p>
     * First, if there is a security manager, its <code>checkRead</code> method
     * is called with the path represented by the <code>file</code> argument as
     * its argument.
     * <p>
     * If the named file does not exist, is a directory rather than a regular
     * file, or for some other reason cannot be opened for reading then a
     * <code>IOException</code> is thrown.
     *
     * @param file the file to be opened for reading.
     * @exception IOException if the file does not exist, is a
     * directory rather than a regular file, or for some other reason cannot be
     * opened for reading.
     * @exception SecurityException if a security manager exists and its
     * <code>checkRead</code> method denies read access to the file.
     * @see java.io.File#getPath()
     * @see java.lang.SecurityManager#checkRead(java.lang.String)
     */
    public FileInputStream(File file) throws IOException {
        this(file.getPath());
    }

    /**
     * Creates a <code>FileInputStream</code> by using the file descriptor
     * <code>fdObj</code>, which represents an existing connection to an actual
     * file in the file system.
     * <p>
     * If there is a security manager, its <code>checkRead</code> method is
     * called with the file descriptor <code>fdObj</code> as its argument to see
     * if it's ok to read the file descriptor. If read access is denied to the
     * file descriptor a <code>SecurityException</code> is thrown.
     * <p>
     * If <code>fdObj</code> is null then a <code>NullPointerException</code> is
     * thrown.
     *
     * @param fdObj the file descriptor to be opened for reading.
     * @throws SecurityException if a security manager exists and its
     * <code>checkRead</code> method denies read access to the file descriptor.
     * @see SecurityManager#checkRead(java.io.FileDescriptor)
     */
    public FileInputStream(FileDescriptor fdObj) {
        if (fdObj == null) {
            throw new NullPointerException();
        }
        fd = fdObj;
        ((InnerFile.InnerFileInputStream) ifis).getInnerFile().setFilePointer(fd.fd);
    }

    /**
     * Opens the specified file for reading.
     *
     * @param name the name of the file
     */
    private void open(String name) throws IOException {
        ifis = new InnerFile(name).getInputStream();
    }

    /**
     * Reads a byte of data from this input stream. This method blocks if no
     * input is yet available.
     *
     * @return the next byte of data, or <code>-1</code> if the end of the file
     * is reached.
     * @exception IOException if an I/O error occurs.
     */
    public int read() throws IOException {
        return ifis.read();
    }

    /**
     * Reads a subarray as a sequence of bytes.
     *
     * @param b the data to be written
     * @param off the start offset in the data
     * @param len the number of bytes that are written
     * @exception IOException If an I/O error has occurred.
     */
    private int readBytes(byte b[], int off, int len) throws IOException {
        return ifis.read(b, off, len);
    }

    /**
     * Reads up to <code>b.length</code> bytes of data from this input stream
     * into an array of bytes. This method blocks until some input is available.
     *
     * @param b the buffer into which the data is read.
     * @return the total number of bytes read into the buffer, or
     * <code>-1</code> if there is no more data because the end of the file has
     * been reached.
     * @exception IOException if an I/O error occurs.
     */
    public int read(byte b[]) throws IOException {
        return readBytes(b, 0, b.length);
    }

    /**
     * Reads up to <code>len</code> bytes of data from this input stream into an
     * array of bytes. This method blocks until some input is available.
     *
     * @param b the buffer into which the data is read.
     * @param off the start offset of the data.
     * @param len the maximum number of bytes read.
     * @return the total number of bytes read into the buffer, or
     * <code>-1</code> if there is no more data because the end of the file has
     * been reached.
     * @exception IOException if an I/O error occurs.
     */
    public int read(byte b[], int off, int len) throws IOException {
        return readBytes(b, off, len);
    }

    /**
     * Skips over and discards <code>n</code> bytes of data from the input
     * stream. The <code>skip</code> method may, for a variety of reasons, end
     * up skipping over some smaller number of bytes, possibly <code>0</code>.
     * The actual number of bytes skipped is returned.
     *
     * @param n the number of bytes to be skipped.
     * @return the actual number of bytes skipped.
     * @exception IOException if an I/O error occurs.
     */
    public long skip(long n) throws IOException {
        return ifis.skip(n);
    }

    /**
     * Returns the number of bytes that can be read from this file input stream
     * without blocking.
     *
     * @return the number of bytes that can be read from this file input stream
     * without blocking.
     * @exception IOException if an I/O error occurs.
     */
    public int available() throws IOException {
        return ifis.available();
    }

    /**
     * Closes this file input stream and releases any system resources
     * associated with the stream.
     *
     * @exception IOException if an I/O error occurs.
     */
    public void close() throws IOException {
        ifis.close();
    }

    /**
     * Returns the <code>FileDescriptor</code> object that represents the
     * connection to the actual file in the file system being used by this
     * <code>FileInputStream</code>.
     *
     * @return the file descriptor object associated with this stream.
     * @exception IOException if an I/O error occurs.
     * @see java.io.FileDescriptor
     */
    public final FileDescriptor getFD() throws IOException {
        if (fd != null) {
            return fd;
        }
        throw new IOException();
    }

    /**
     * Ensures that the <code>close</code> method of this file input stream is
     * called when there are no more references to it.
     *
     * @exception IOException if an I/O error occurs.
     * @see java.io.FileInputStream#close()
     */
    protected void finalize() throws IOException {
        if (fd != null) {
            if (fd != fd.in) {
                close();
            }
        }
    }
}
