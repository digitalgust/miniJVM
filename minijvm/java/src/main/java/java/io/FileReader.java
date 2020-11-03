/*
 * Copyright (c) 2003, 2006, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */



package java.io;


/**
 * Convenience class for reading character files.  The constructors of this
 * class assume that the default character encoding and the default byte-buffer
 * size are appropriate.  To specify these values yourself, construct an
 * InputStreamReader on a FileInputStream.
 *
 * @see InputStreamReader
 * @see FileInputStream
 *
 * @version 	1.10, 00/02/02
 * @author	Mark Reinhold
 * @since	JDK1.1
 */
public class FileReader extends InputStreamReader {

   /**
    * Creates a new <tt>FileReader</tt>, given the name of the
    * file to read from.
    *
    * @param fileName the name of the file to read from
    * @throws <tt>IOException</tt> if the specified 
    * file is not found
    */
    public FileReader(String fileName) throws IOException {
	super(new FileInputStream(fileName));
    }

   /**
    * Creates a new <tt>FileReader</tt>, given the <tt>File</tt> 
    * to read from.
    *
    * @param file the <tt>File</tt> to read from
    * @throws <tt>IOException</tt> if the specified 
    * file is not found
    */
    public FileReader(File file) throws IOException {
	super(new FileInputStream(file));
    }

   /**
    * Creates a new <tt>FileReader</tt>, given the 
    * <tt>FileDescriptor</tt> to read from.
    *
    * @param fd the FileDescriptor to read from
    */
    public FileReader(FileDescriptor fd) {
	super(new FileInputStream(fd));
    }

}

