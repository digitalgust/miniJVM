/*
 * Copyright C 2003 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package com.sun.cldc.i18n;

import java.io.*;

/**
 * This class provides general helper functions for the J2ME environment.
 * <p>
 * <em>No application code should reference this class directly.</em>
 *
 * @author  Nik Shaylor, Antero Taivalsaari
 * @version CLDC 1.1 03/29/02
 */
public class Helper {

    /**
     * The name of the default character encoding
     */
    private static String defaultEncoding;

    /**
     * Class initializer
     */
    static {
        /* Get the default encoding name */
        defaultEncoding = System.getProperty("microedition.encoding");
        if (defaultEncoding == null) {
            defaultEncoding = "ISO-8859-1";
        }
    }

/*------------------------------------------------------------------------------*/
/*                               Character encoding                             */
/*------------------------------------------------------------------------------*/

    /**
     * Get a reader for an InputStream
     *
     * @param  is              The input stream the reader is for
     * @return                 A new reader for the stream
     */
    public static Reader getStreamReader(InputStream is) {
        try {
            return getStreamReader(is, defaultEncoding);
        } catch(UnsupportedEncodingException x) {
            try {
                defaultEncoding = "ISO8859_1";
                return getStreamReader(is, defaultEncoding);
            } catch(UnsupportedEncodingException e) {
                throw new RuntimeException("Missing default encoding "+defaultEncoding);
            }
        }
    }

    /**
     * Get a reader for an InputStream
     *
     * @param  is              The input stream the reader is for
     * @param  name            The name of the decoder
     * @return                 A new reader for the stream
     * @exception UnsupportedEncodingException  If the encoding is not known
     */
    public static Reader getStreamReader(InputStream is, String name)
        throws UnsupportedEncodingException {

        /* Test for null arguments */
        if (is == null || name == null) {
            throw new NullPointerException();
        }

        /* Get the reader from the encoding */
        StreamReader fr = getStreamReaderPrim(name);

        /* Open the connection and return*/
        return fr.open(is, name);
    }

    /**
     * Get a reader for an InputStream
     *
     * @param  is              The input stream the reader is for
     * @param  name            The name of the decoder
     * @return                 A new reader for the stream
     * @exception UnsupportedEncodingException  If the encoding is not known
     */
    private static StreamReader getStreamReaderPrim(String name)
        throws UnsupportedEncodingException {

        if (name == null) {
            throw new NullPointerException();
        }

        name = internalNameForEncoding(name);

        try {
            String className;

            /* Get the reader class name */
            className = "com.sun.cldc.i18n.mini" + '.' + name + "_Reader";

            /* Using the decoder names lookup a class to implement the reader */
            Class clazz = Class.forName(className);

            /* Return a new instance */
            return (StreamReader)clazz.newInstance();

        } catch(ClassNotFoundException x) {
            throw new UnsupportedEncodingException("Encoding "+name+" not found");
        } catch(InstantiationException x) {
            throw new RuntimeException("InstantiationException "+x.getMessage());
        } catch(IllegalAccessException x) {
            throw new RuntimeException("IllegalAccessException "+x.getMessage());
        } catch(ClassCastException x) {
            throw new RuntimeException("ClassCastException "+x.getMessage());
        }
    }

    /**
     * Get a writer for an OutputStream
     *
     * @param  os              The output stream the reader is for
     * @return                 A new writer for the stream
     */
    public static Writer getStreamWriter(OutputStream os) {
        try {
            return getStreamWriter(os, defaultEncoding);
        } catch(UnsupportedEncodingException x) {
            try {
                defaultEncoding = "ISO8859_1";
                return getStreamWriter(os, defaultEncoding);
            } catch(UnsupportedEncodingException e) {
                throw new RuntimeException("Missing default encoding "+defaultEncoding);
            }
        }
    }

    /**
     * Get a writer for an OutputStream
     *
     * @param  os              The output stream the reader is for
     * @param  name            The name of the decoder
     * @return                 A new writer for the stream
     * @exception UnsupportedEncodingException  If the encoding is not known
     */
    public static Writer getStreamWriter(OutputStream os, String name)
        throws UnsupportedEncodingException {

        /* Test for null arguments */
        if (os == null || name == null) {
            throw new NullPointerException();
        }

        /* Get the writer from the encoding */
        StreamWriter sw = getStreamWriterPrim(name);

        /* Open it on the output stream and return */
        return sw.open(os, name);
    }

    /**
     * Get a writer for an OutputStream
     *
     * @param  os              The output stream the reader is for
     * @param  name            The name of the decoder
     * @return                 A new writer for the stream
     * @exception UnsupportedEncodingException  If the encoding is not known
     */
    private static StreamWriter getStreamWriterPrim(String name)
        throws UnsupportedEncodingException {

        if (name == null) {
            throw new NullPointerException();
        }

        name = internalNameForEncoding(name);

        try {
            String className;

            /* Get the writer class name */
            className = "com.sun.cldc.i18n.mini" + '.' + name +"_Writer";

            /* Using the decoder names lookup a class to implement the writer */
            Class clazz = Class.forName(className);

            /* Construct a new instance */
            return (StreamWriter)clazz.newInstance();

        } catch(ClassNotFoundException x) {
            throw new UnsupportedEncodingException("Encoding "+name+" not found");
        } catch(InstantiationException x) {
            throw new RuntimeException("InstantiationException "+x.getMessage());
        } catch(IllegalAccessException x) {
            throw new RuntimeException("IllegalAccessException "+x.getMessage());
        } catch(ClassCastException x) {
            throw new RuntimeException("ClassCastException "+x.getMessage());
        }
    }

    /**
     * Convert a byte array to a char array
     *
     * @param  buffer          The byte array buffer
     * @param  offset          The offset
     * @param  length          The length
     * @return                 A new char array
     */
    public static char[] byteToCharArray(byte[] buffer, int offset, int length) {
        try {
            return byteToCharArray(buffer, offset, length, defaultEncoding);
        } catch(UnsupportedEncodingException x) {
            throw new RuntimeException("Missing default encoding "+defaultEncoding);
        }
    }

    /**
     * Convert a char array to a byte array
     *
     * @param  buffer          The char array buffer
     * @param  offset          The offset
     * @param  length          The length
     * @return                 A new byte array
     */
    public static byte[] charToByteArray(char[] buffer, int offset, int length) {
        try {
            return charToByteArray(buffer, offset, length, defaultEncoding);
        } catch(UnsupportedEncodingException x) {
            throw new RuntimeException("Missing default encoding "+defaultEncoding);
        }
    }

    /*
     * Cached variables for byteToCharArray
     */
    private static String lastReaderEncoding;
    private static StreamReader  lastReader;

    /**
     * Convert a byte array to a char array
     *
     * @param  buffer          The byte array buffer
     * @param  offset          The offset
     * @param  length          The length
     * @param  enc             The character encoding
     * @return                 A new char array
     * @exception UnsupportedEncodingException  If the encoding is not known
     */
    public static synchronized char[] byteToCharArray(byte[] buffer, int offset,
        int length, String enc) throws UnsupportedEncodingException {

        if (offset < 0) {
            throw new IndexOutOfBoundsException(Integer.toString(offset));
        }

        if (length < 0) {
            throw new IndexOutOfBoundsException(Integer.toString(length));
        }

        /* Note: offset or length might be near -1>>>1 */
        if (offset > buffer.length - length) {
            throw new IndexOutOfBoundsException(
                Integer.toString(offset + length));
        }

        /* If we don't have a cached reader then make one */
        if (lastReaderEncoding == null || !lastReaderEncoding.equals(enc)) {
            lastReader = getStreamReaderPrim(enc);
            lastReaderEncoding = enc;
        }

        /* Ask the reader for the size the output will be */
        int size = lastReader.sizeOf(buffer, offset, length);

        /* Allocate a buffer of that size */
        char[] outbuf = new char[size];

        /* Open the reader on a ByteArrayInputStream */
        lastReader.open(new ByteArrayInputStream(buffer, offset, length), enc);

        try {
            /* Read the input */
            lastReader.read(outbuf, 0, size);
            /* Close the reader */
            lastReader.close();
        } catch(IOException x) {
            throw new RuntimeException("IOException reading reader "+x.getMessage());
        }

        /* And return the buffer */
        return outbuf;
    }

    /*
     * Cached variables for charToByteArray
     */
    private static String lastWriterEncoding;
    private static StreamWriter lastWriter;

    /**
     * Convert a char array to a byte array
     *
     * @param  buffer          The char array buffer
     * @param  offset          The offset
     * @param  length          The length
     * @param  enc             The character encoding
     * @return                 A new byte array
     * @exception UnsupportedEncodingException  If the encoding is not known
     */
    public static synchronized byte[] charToByteArray(char[] buffer, int offset,
        int length, String enc) throws UnsupportedEncodingException {

        /* If we don't have a cached writer then make one */
        if (lastWriterEncoding == null || !lastWriterEncoding.equals(enc)) {
            lastWriter = getStreamWriterPrim(enc);
            lastWriterEncoding = enc;
        }

        /* Ask the writer for the size the output will be */
        int size = lastWriter.sizeOf(buffer, offset, length);

        /* Get the output stream */
        ByteArrayOutputStream os = new ByteArrayOutputStream(size);

        /* Open the writer */
        lastWriter.open(os, enc);

        try {
            /* Convert */
            lastWriter.write(buffer, offset, length);
            /* Close the writer */
            lastWriter.close();
        } catch(IOException x) {
            throw new RuntimeException("IOException writing writer "+x.getMessage());
        }

        /* Close the output stream */
        try {
            os.close();
        } catch(IOException x) {};

        /* Return the array */
        return os.toByteArray();
    }

    /**
     * Get the internal name for an encoding.
     *
     * @param encodingName encoding name
     *
     * @return internal name for this encoding
     */
    private static String internalNameForEncoding(String encodingName) {
        String internalName;
        String property;

        internalName = normalizeEncodingName(encodingName);

        // The preferred MIME name according to the IANA Charset Registry.
        if (internalName.equals("US_ASCII")) {
            /*
             * US-ASCII is subclass of ISO-8859-1 so we do not need a
             * separate reader for it.
             */
            return "ISO8859_1";
        }

        // The preferred MIME name according to the IANA Charset Registry.
        if (internalName.equals("ISO_8859_1")) {
            return "ISO8859_1";
        }

        /*
         * Since IANA character encoding names can start with a digit
         * and that some Reader class names that do not match the standard
         * name, we have a way to configure alternate names for encodings.
         *
         * Note: The names must normalized, digits, upper case only with "_"
         *       and "_" substituted for ":" and "-".
         *
         * Use the code below only if your system really needs it:
         *
         * property = System.getProperty(internalName + "_InternalEncodingName");
         * if (property != null) {
         *     return property;
         * }
         */

        return internalName;
    }

    /**
     * Converts "-" and ":" in a string to "_" and converts the name
     * to upper case.
     * This is needed because the names of IANA character encodings have
     * characters that are not allowed for java class names and
     * IANA encoding names are not case sensitive.
     *
     * @param encodingName encoding name
     *
     * @return normalized name
     */
    private static String normalizeEncodingName(String encodingName) {
        StringBuffer normalizedName;
        char currentChar;

        normalizedName = new StringBuffer(encodingName);

        for (int i = 0; i < normalizedName.length(); i++) {
            currentChar = normalizedName.charAt(i);

            if (currentChar == '-' || currentChar == ':') {
                normalizedName.setCharAt(i, '_');
            } else {
                normalizedName.setCharAt(i, Character.toUpperCase(currentChar));
            }
        }

        return normalizedName.toString();
    }

}

