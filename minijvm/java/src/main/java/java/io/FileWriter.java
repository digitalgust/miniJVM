/*
 * @(#)FileWriter.java	1.11 00/02/02
 *
 * Copyright 1996-2000 Sun Microsystems, Inc. All Rights Reserved.
 * 
 * This software is the proprietary information of Sun Microsystems, Inc.  
 * Use is subject to license terms.
 * 
 */

package java.io;


/**
 * Convenience class for writing character files.  The constructors of this
 * class assume that the default character encoding and the default byte-buffer
 * size are acceptable.  To specify these values yourself, construct an
 * OutputStreamWriter on a FileOutputStream.
 *
 * @see OutputStreamWriter
 * @see FileOutputStream
 *
 * @version 	1.11, 00/02/02
 * @author	Mark Reinhold
 * @since	JDK1.1
 */

public class FileWriter extends OutputStreamWriter {

    /**
     * Constructs a FileWriter object given a file name.
     *
     * @param fileName  String The system-dependent filename.
     * @throws IOException if the specified file is not found or if some other
     *                     I/O error occurs.
     */
    public FileWriter(String fileName) throws IOException {
	super(new FileOutputStream(fileName));
    }

    /**
     * Constructs a FileWriter object given a file name with a boolean
     * indicating whether or not to append the data written.
     *
     * @param fileName  String The system-dependent filename.
     * @param append    boolean if <code>true</code>, then data will be written
     *                  to the end of the file rather than the beginning.
     * @throws IOException if the specified file is not found or if some other
     *                     I/O error occurs.
     */
    public FileWriter(String fileName, boolean append) throws IOException {
	super(new FileOutputStream(fileName, append));
    }

    /**
     * Constructs a FileWriter object given a File object.
     *
     * @param file  a File object to write to.
     * @throws IOException if the specified file is not found or if some other
     *                     I/O error occurs.
     */
    public FileWriter(File file) throws IOException {
	super(new FileOutputStream(file));
    }

    /**
     * Constructs a FileWriter object associated with a file descriptor.
     *
     * @param fd  FileDescriptor object to write to.
     */
    public FileWriter(FileDescriptor fd) {
	super(new FileOutputStream(fd));
    }

}
