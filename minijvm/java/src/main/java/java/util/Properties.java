/*
 * @(#)Properties.java	1.84 04/05/18
 *
 * Copyright 2004 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package java.util;

import java.io.IOException;
import java.io.PrintStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.BufferedWriter;
import java.io.Writer;

/**
 * The <code>Properties</code> class represents a persistent set of
 * properties. The <code>Properties</code> can be saved to a stream
 * or loaded from a stream. Each key and its corresponding value in
 * the property list is a string.
 * <p>
 * A property list can contain another property list as its
 * "defaults"; this second property list is searched if
 * the property key is not found in the original property list.
 * <p>
 * Because <code>Properties</code> inherits from <code>Hashtable</code>, the
 * <code>put</code> and <code>putAll</code> methods can be applied to a
 * <code>Properties</code> object.  Their use is strongly discouraged as they
 * allow the caller to insert entries whose keys or values are not
 * <code>Strings</code>.  The <code>setProperty</code> method should be used
 * instead.  If the <code>store</code> or <code>save</code> method is called
 * on a "compromised" <code>Properties</code> object that contains a
 * non-<code>String</code> key or value, the call will fail.
 * <p>
 * <a name="encoding"></a>
 * <p> The {@link #load load} and {@link #store store} methods load and store
 * properties in a simple line-oriented format specified below.  This format
 * uses the ISO 8859-1 character encoding.  Characters that cannot be directly
 * represented in this encoding can be written using
 * <a href="http://java.sun.com/docs/books/jls/html/3.doc.html#100850">Unicode escapes</a>
 * ; only a single 'u' character is allowed in an escape
 * sequence. The native2ascii tool can be used to convert property files to and
 * from other character encodings.
 * <p>
* <p> The {@link #loadFromXML(InputStream)} and {@link
 * #storeToXML(OutputStream, String, String)} methods load and store properties
 * in a simple XML format.  By default the UTF-8 character encoding is used,
 * however a specific encoding may be specified if required.  An XML properties
 * document has the following DOCTYPE declaration:
 *
 * <pre>
 * &lt;!DOCTYPE properties SYSTEM "http://java.sun.com/dtd/properties.dtd"&gt;
 * </pre>
 * Note that the system URI (http://java.sun.com/dtd/properties.dtd) is
 * <i>not</i> accessed when exporting or importing properties; it merely
 * serves as a string to uniquely identify the DTD, which is:
 * <pre>
 *    &lt;?xml version="1.0" encoding="UTF-8"?&gt;
 *
 *    &lt;!-- DTD for properties --&gt;
 *
 *    &lt;!ELEMENT properties ( comment?, entry* ) &gt;
 *
 *    &lt;!ATTLIST properties version CDATA #FIXED "1.0"&gt;
 *
 *    &lt;!ELEMENT comment (#PCDATA) &gt;
 *
 *    &lt;!ELEMENT entry (#PCDATA) &gt;
 *
 *    &lt;!ATTLIST entry key CDATA #REQUIRED&gt;
 * </pre>
 * 
 * @see <a href="../../../tooldocs/solaris/native2ascii.html">native2ascii tool for Solaris</a>
 * @see <a href="../../../tooldocs/windows/native2ascii.html">native2ascii tool for Windows</a>
 *
 * @author  Arthur van Hoff
 * @author  Michael McCloskey
 * @version 1.84, 05/18/04
 * @since   JDK1.0
 */
public
class Properties extends Hashtable<Object,Object> {
    /**
     * use serialVersionUID from JDK 1.1.X for interoperability
     */
     private static final long serialVersionUID = 4112578634029874840L;

    /**
     * A property list that contains default values for any keys not
     * found in this property list.
     *
     * @serial
     */
    protected Properties defaults;

    /**
     * Creates an empty property list with no default values.
     */
    public Properties() {
	this(null);
    }

    /**
     * Creates an empty property list with the specified defaults.
     *
     * @param   defaults   the defaults.
     */
    public Properties(Properties defaults) {
	this.defaults = defaults;
    }

    /**
     * Calls the <tt>Hashtable</tt> method <code>put</code>. Provided for
     * parallelism with the <tt>getProperty</tt> method. Enforces use of
     * strings for property keys and values. The value returned is the
     * result of the <tt>Hashtable</tt> call to <code>put</code>.
     *
     * @param key the key to be placed into this property list.
     * @param value the value corresponding to <tt>key</tt>.
     * @return     the previous value of the specified key in this property
     *             list, or <code>null</code> if it did not have one.
     * @see #getProperty
     * @since    1.2
     */
    public synchronized Object setProperty(String key, String value) {
        return put(key, value);
    }

    /**
     * Reads a property list (key and element pairs) from the input
     * stream.  The stream is assumed to be using the ISO 8859-1
     * character encoding; that is each byte is one Latin1 character.
     * Characters not in Latin1, and certain special characters, can
     * be represented in keys and elements using escape sequences
     * similar to those used for character and string literals (see <a
     * href="http://java.sun.com/docs/books/jls/second_edition/html/lexical.doc.html#100850">&sect;3.3</a>
     * and <a
     * href="http://java.sun.com/docs/books/jls/second_edition/html/lexical.doc.html#101089">&sect;3.10.6</a>
     * of the <i>Java Language Specification</i>).
     *
     * The differences from the character escape sequences used for
     * characters and strings are:
     *
     * <ul>
     * <li> Octal escapes are not recognized.
     *
     * <li> The character sequence <code>\b</code> does <i>not</i>
     * represent a backspace character.
     *
     * <li> The method does not treat a backslash character,
     * <code>\</code>, before a non-valid escape character as an
     * error; the backslash is silently dropped.  For example, in a
     * Java string the sequence <code>"\z"</code> would cause a
     * compile time error.  In contrast, this method silently drops
     * the backslash.  Therefore, this method treats the two character
     * sequence <code>"\b"</code> as equivalent to the single
     * character <code>'b'</code>.
     *
     * <li> Escapes are not necessary for single and double quotes;
     * however, by the rule above, single and double quote characters
     * preceded by a backslash still yield single and double quote
     * characters, respectively.
     *
     * </ul>
     *
     * An <code>IllegalArgumentException</code> is thrown if a
     * malformed Unicode escape appears in the input.
     *
     * <p>
     * This method processes input in terms of lines.  A natural line
     * of input is terminated either by a set of line terminator
     * characters (<code>\n</code> or <code>\r</code> or
     * <code>\r\n</code>) or by the end of the file.  A natural line
     * may be either a blank line, a comment line, or hold some part
     * of a key-element pair.  The logical line holding all the data
     * for a key-element pair may be spread out across several adjacent
     * natural lines by escaping the line terminator sequence with a
     * backslash character, <code>\</code>.  Note that a comment line
     * cannot be extended in this manner; every natural line that is a
     * comment must have its own comment indicator, as described
     * below.  If a logical line is continued over several natural
     * lines, the continuation lines receive further processing, also
     * described below.  Lines are read from the input stream until
     * end of file is reached.
     *
     * <p>
     * A natural line that contains only white space characters is
     * considered blank and is ignored.  A comment line has an ASCII
     * <code>'#'</code> or <code>'!'</code> as its first non-white
     * space character; comment lines are also ignored and do not
     * encode key-element information.  In addition to line
     * terminators, this method considers the characters space
     * (<code>' '</code>, <code>'&#92;u0020'</code>), tab
     * (<code>'\t'</code>, <code>'&#92;u0009'</code>), and form feed
     * (<code>'\f'</code>, <code>'&#92;u000C'</code>) to be white
     * space.
     *
     * <p>
     * If a logical line is spread across several natural lines, the
     * backslash escaping the line terminator sequence, the line
     * terminator sequence, and any white space at the start the
     * following line have no affect on the key or element values.
     * The remainder of the discussion of key and element parsing will
     * assume all the characters constituting the key and element
     * appear on a single natural line after line continuation
     * characters have been removed.  Note that it is <i>not</i>
     * sufficient to only examine the character preceding a line
     * terminator sequence to see if the line terminator is
     * escaped; there must be an odd number of contiguous backslashes
     * for the line terminator to be escaped.  Since the input is
     * processed from left to right, a non-zero even number of
     * 2<i>n</i> contiguous backslashes before a line terminator (or
     * elsewhere) encodes <i>n</i> backslashes after escape
     * processing.
     *
     * <p>
     * The key contains all of the characters in the line starting
     * with the first non-white space character and up to, but not
     * including, the first unescaped <code>'='</code>,
     * <code>':'</code>, or white space character other than a line
     * terminator. All of these key termination characters may be
     * included in the key by escaping them with a preceding backslash
     * character; for example,<p>
     *
     * <code>\:\=</code><p>
     *
     * would be the two-character key <code>":="</code>.  Line
     * terminator characters can be included using <code>\r</code> and
     * <code>\n</code> escape sequences.  Any white space after the
     * key is skipped; if the first non-white space character after
     * the key is <code>'='</code> or <code>':'</code>, then it is
     * ignored and any white space characters after it are also
     * skipped.  All remaining characters on the line become part of
     * the associated element string; if there are no remaining
     * characters, the element is the empty string
     * <code>&quot;&quot;</code>.  Once the raw character sequences
     * constituting the key and element are identified, escape
     * processing is performed as described above.
     *
     * <p>
     * As an example, each of the following three lines specifies the key
     * <code>"Truth"</code> and the associated element value
     * <code>"Beauty"</code>:
     * <p>
     * <pre>
     * Truth = Beauty
     *	Truth:Beauty
     * Truth			:Beauty
     * </pre>
     * As another example, the following three lines specify a single
     * property:
     * <p>
     * <pre>
     * fruits                           apple, banana, pear, \
     *                                  cantaloupe, watermelon, \
     *                                  kiwi, mango
     * </pre>
     * The key is <code>"fruits"</code> and the associated element is:
     * <p>
     * <pre>"apple, banana, pear, cantaloupe, watermelon, kiwi, mango"</pre>
     * Note that a space appears before each <code>\</code> so that a space
     * will appear after each comma in the final result; the <code>\</code>,
     * line terminator, and leading white space on the continuation line are
     * merely discarded and are <i>not</i> replaced by one or more other
     * characters.
     * <p>
     * As a third example, the line:
     * <p>
     * <pre>cheeses
     * </pre>
     * specifies that the key is <code>"cheeses"</code> and the associated
     * element is the empty string <code>""</code>.<p>
     *
     * @param      inStream   the input stream.
     * @exception  IOException  if an error occurred when reading from the
     *               input stream.
     * @throws	   IllegalArgumentException if the input stream contains a
     * 		   malformed Unicode escape sequence.
     */
    public synchronized void load(InputStream inStream) throws IOException {
        char[] convtBuf = new char[1024];
        LineReader lr = new LineReader(inStream);

        int limit;
        int keyLen;
        int valueStart;
        char c;
        boolean hasSep;
        boolean precedingBackslash;

        while ((limit = lr.readLine()) >= 0) {
            c = 0;
            keyLen = 0;
            valueStart = limit;
            hasSep = false;

	    //System.out.println("line=<" + new String(lineBuf, 0, limit) + ">");
            precedingBackslash = false;
            while (keyLen < limit) {
                c = lr.lineBuf[keyLen];
                //need check if escaped.
                if ((c == '=' ||  c == ':') && !precedingBackslash) {
                    valueStart = keyLen + 1;
                    hasSep = true;
                    break;
                } else if ((c == ' ' || c == '\t' ||  c == '\f') && !precedingBackslash) {
                    valueStart = keyLen + 1;
                    break;
                } 
                if (c == '\\') {
                    precedingBackslash = !precedingBackslash;
                } else {
                    precedingBackslash = false;
                }
                keyLen++;
            }
            while (valueStart < limit) {
                c = lr.lineBuf[valueStart];
                if (c != ' ' && c != '\t' &&  c != '\f') {
                    if (!hasSep && (c == '=' ||  c == ':')) {
                        hasSep = true;
                    } else {
                        break;
                    }
                }
                valueStart++;
            }
            String key = loadConvert(lr.lineBuf, 0, keyLen, convtBuf);
            String value = loadConvert(lr.lineBuf, valueStart, limit - valueStart, convtBuf);
	    put(key, value);
	}
    }

    /* read in a "logical line" from input stream, skip all comment and
     * blank lines and filter out those leading whitespace characters 
     * (\u0020, \u0009 and \u000c) from the beginning of a "natural line". 
     * Method returns the char length of the "logical line" and stores 
     * the line in "lineBuf". 
     */
    class LineReader {
        public LineReader(InputStream inStream) {
            this.inStream = inStream;
	}
        byte[] inBuf = new byte[8192]; 
        char[] lineBuf = new char[1024];
        int inLimit = 0;
        int inOff = 0;
        InputStream inStream;

        int readLine() throws IOException {
            int len = 0;
            char c = 0;

            boolean skipWhiteSpace = true;
            boolean isCommentLine = false;
            boolean isNewLine = true;
            boolean appendedLineBegin = false;
            boolean precedingBackslash = false;
	    boolean skipLF = false;

            while (true) {
                if (inOff >= inLimit) {
                    inLimit = inStream.read(inBuf);
		    inOff = 0;
		    if (inLimit <= 0) {
			if (len == 0 || isCommentLine) { 
			    return -1; 
			}
			return len;
		    }
		}     
                //The line below is equivalent to calling a 
                //ISO8859-1 decoder.
		c = (char) (0xff & inBuf[inOff++]);
                if (skipLF) {
                    skipLF = false;
		    if (c == '\n') {
		        continue;
		    }
		}
		if (skipWhiteSpace) {
		    if (c == ' ' || c == '\t' || c == '\f') {
			continue;
		    }
		    if (!appendedLineBegin && (c == '\r' || c == '\n')) {
			continue;
		    }
		    skipWhiteSpace = false;
		    appendedLineBegin = false;
		}
		if (isNewLine) {
		    isNewLine = false;
		    if (c == '#' || c == '!') {
			isCommentLine = true;
			continue;
		    }
		}
		
		if (c != '\n' && c != '\r') {
		    lineBuf[len++] = c;
		    if (len == lineBuf.length) {
		        int newLength = lineBuf.length * 2;
		        if (newLength < 0) {
		            newLength = Integer.MAX_VALUE;
		        }
			char[] buf = new char[newLength];
			System.arraycopy(lineBuf, 0, buf, 0, lineBuf.length);
			lineBuf = buf;
		    }
		    //flip the preceding backslash flag
		    if (c == '\\') {
			precedingBackslash = !precedingBackslash;
		    } else {
			precedingBackslash = false;
		    }
		}
		else {
		    // reached EOL
		    if (isCommentLine || len == 0) {
			isCommentLine = false;
			isNewLine = true;
			skipWhiteSpace = true;
			len = 0;
			continue;
		    }
		    if (inOff >= inLimit) {
			inLimit = inStream.read(inBuf);
			inOff = 0;
			if (inLimit <= 0) {
			    return len;
			}
		    }
		    if (precedingBackslash) {
			len -= 1;
			//skip the leading whitespace characters in following line
			skipWhiteSpace = true;
			appendedLineBegin = true;
			precedingBackslash = false;
			if (c == '\r') {
                            skipLF = true;
			}
		    } else {
			return len;
		    }
		}
	    }
	}
    }    
    
    /*
     * Converts encoded &#92;uxxxx to unicode chars
     * and changes special saved chars to their original forms
     */
    private String loadConvert (char[] in, int off, int len, char[] convtBuf) {
        if (convtBuf.length < len) {
            int newLen = len * 2;
            if (newLen < 0) {
	        newLen = Integer.MAX_VALUE;
	    } 
	    convtBuf = new char[newLen];
        }
        char aChar;
        char[] out = convtBuf; 
        int outLen = 0;
        int end = off + len;

        while (off < end) {
            aChar = in[off++];
            if (aChar == '\\') {
                aChar = in[off++];   
                if(aChar == 'u') {
                    // Read the xxxx
                    int value=0;
		    for (int i=0; i<4; i++) {
		        aChar = in[off++];  
		        switch (aChar) {
		          case '0': case '1': case '2': case '3': case '4':
		          case '5': case '6': case '7': case '8': case '9':
		             value = (value << 4) + aChar - '0';
			     break;
			  case 'a': case 'b': case 'c':
                          case 'd': case 'e': case 'f':
			     value = (value << 4) + 10 + aChar - 'a';
			     break;
			  case 'A': case 'B': case 'C':
                          case 'D': case 'E': case 'F':
			     value = (value << 4) + 10 + aChar - 'A';
			     break;
			  default:
                              throw new IllegalArgumentException(
                                           "Malformed \\uxxxx encoding.");
                        }
                     }
                    out[outLen++] = (char)value;
                } else {
                    if (aChar == 't') aChar = '\t'; 
                    else if (aChar == 'r') aChar = '\r';
                    else if (aChar == 'n') aChar = '\n';
                    else if (aChar == 'f') aChar = '\f'; 
                    out[outLen++] = aChar;
                }
            } else {
	        out[outLen++] = (char)aChar;
            }
        }
        return new String (out, 0, outLen);
    }

    /*
     * Converts unicodes to encoded &#92;uxxxx and escapes
     * special characters with a preceding slash
     */
    private String saveConvert(String theString, boolean escapeSpace) {
        int len = theString.length();
        int bufLen = len * 2;
        if (bufLen < 0) {
            bufLen = Integer.MAX_VALUE;
        }
        StringBuffer outBuffer = new StringBuffer(bufLen);

        for(int x=0; x<len; x++) {
            char aChar = theString.charAt(x);
            // Handle common case first, selecting largest block that
            // avoids the specials below
            if ((aChar > 61) && (aChar < 127)) {
                if (aChar == '\\') {
                    outBuffer.append('\\'); outBuffer.append('\\');
                    continue;
                }
                outBuffer.append(aChar);
                continue;
            }
            switch(aChar) {
		case ' ':
		    if (x == 0 || escapeSpace) 
			outBuffer.append('\\');
		    outBuffer.append(' ');
		    break;
                case '\t':outBuffer.append('\\'); outBuffer.append('t');
                          break;
                case '\n':outBuffer.append('\\'); outBuffer.append('n');
                          break;
                case '\r':outBuffer.append('\\'); outBuffer.append('r');
                          break;
                case '\f':outBuffer.append('\\'); outBuffer.append('f');
                          break;
                case '=': // Fall through
                case ':': // Fall through
                case '#': // Fall through
                case '!':
                    outBuffer.append('\\'); outBuffer.append(aChar);
                    break;
                default:
                    if ((aChar < 0x0020) || (aChar > 0x007e)) {
                        outBuffer.append('\\');
                        outBuffer.append('u');
                        outBuffer.append(toHex((aChar >> 12) & 0xF));
                        outBuffer.append(toHex((aChar >>  8) & 0xF));
                        outBuffer.append(toHex((aChar >>  4) & 0xF));
                        outBuffer.append(toHex( aChar        & 0xF));
                    } else {
                        outBuffer.append(aChar);
                    }
            }
        }
        return outBuffer.toString();
    }

    /**
     * Calls the <code>store(OutputStream out, String comments)</code> method
     * and suppresses IOExceptions that were thrown.
     *
     * @deprecated This method does not throw an IOException if an I/O error
     * occurs while saving the property list.  The preferred way to save a
     * properties list is via the <code>store(OutputStream out, 
     * String comments)</code> method or the 
     * <code>storeToXML(OutputStream os, String comment)</code> method.
     *
     * @param   out      an output stream.
     * @param   comments   a description of the property list.
     * @exception  ClassCastException  if this <code>Properties</code> object
     *             contains any keys or values that are not 
     *             <code>Strings</code>.
     */
    @Deprecated
    public synchronized void save(OutputStream out, String comments)  {
        try {
            store(out, comments);
        } catch (IOException e) {
        }
    }

    /**
     * Writes this property list (key and element pairs) in this
     * <code>Properties</code> table to the output stream in a format suitable
     * for loading into a <code>Properties</code> table using the
     * {@link #load(InputStream) load} method.
     * The stream is written using the ISO 8859-1 character encoding.
     * <p>
     * Properties from the defaults table of this <code>Properties</code>
     * table (if any) are <i>not</i> written out by this method.
     * <p>
     * If the comments argument is not null, then an ASCII <code>#</code>
     * character, the comments string, and a line separator are first written
     * to the output stream. Thus, the <code>comments</code> can serve as an
     * identifying comment.
     * <p>
     * Next, a comment line is always written, consisting of an ASCII
     * <code>#</code> character, the current date and time (as if produced
     * by the <code>toString</code> method of <code>Date</code> for the
     * current time), and a line separator as generated by the Writer.
     * <p>
     * Then every entry in this <code>Properties</code> table is
     * written out, one per line. For each entry the key string is
     * written, then an ASCII <code>=</code>, then the associated
     * element string. Each character of the key and element strings
     * is examined to see whether it should be rendered as an escape
     * sequence. The ASCII characters <code>\</code>, tab, form feed,
     * newline, and carriage return are written as <code>\\</code>,
     * <code>\t</code>, <code>\f</code> <code>\n</code>, and
     * <code>\r</code>, respectively. Characters less than
     * <code>&#92;u0020</code> and characters greater than
     * <code>&#92;u007E</code> are written as
     * <code>&#92;u</code><i>xxxx</i> for the appropriate hexadecimal
     * value <i>xxxx</i>.  For the key, all space characters are
     * written with a preceding <code>\</code> character.  For the
     * element, leading space characters, but not embedded or trailing
     * space characters, are written with a preceding <code>\</code>
     * character. The key and element characters <code>#</code>,
     * <code>!</code>, <code>=</code>, and <code>:</code> are written
     * with a preceding backslash to ensure that they are properly loaded.
     * <p>
     * After the entries have been written, the output stream is flushed.  The
     * output stream remains open after this method returns.
     *
     * @param   out      an output stream.
     * @param   comments   a description of the property list.
     * @exception  IOException if writing this property list to the specified
     *             output stream throws an <tt>IOException</tt>.
     * @exception  ClassCastException  if this <code>Properties</code> object
     *             contains any keys or values that are not <code>Strings</code>.
     * @exception  NullPointerException  if <code>out</code> is null.
     * @since 1.2
     */
    public synchronized void store(OutputStream out, String comments)
    throws IOException
    {
        BufferedWriter awriter;
        awriter = new BufferedWriter(new OutputStreamWriter(out, "iso8859_1"));
        if (comments != null)
            writeln(awriter, "#" + comments);
        writeln(awriter, "#" + new Date().toString());
        for (Enumeration e = keys(); e.hasMoreElements();) {
            String key = (String)e.nextElement();
            String val = (String)get(key);
            key = saveConvert(key, true);

	    /* No need to escape embedded and trailing spaces for value, hence
	     * pass false to flag.
	     */
            val = saveConvert(val, false);
            writeln(awriter, key + "=" + val);
        }
        awriter.flush();
    }

    private static void writeln(BufferedWriter bw, String s) throws IOException {
        bw.write(s);
        bw.newLine();
    }

    /**
     * Loads all of the properties represented by the XML document on the
     * specified input stream into this properties table.
     *
     * <p>The XML document must have the following DOCTYPE declaration:
     * <pre>
     * &lt;!DOCTYPE properties SYSTEM "http://java.sun.com/dtd/properties.dtd"&gt;
     * </pre>
     * Furthermore, the document must satisfy the properties DTD described
     * above.
     *
     * <p>The specified stream remains open after this method returns.
     *
     * @param in the input stream from which to read the XML document.
     * @throws IOException if reading from the specified input stream
     *         results in an <tt>IOException</tt>.
     * @throws InvalidPropertiesFormatException Data on input stream does not
     *         constitute a valid XML document with the mandated document type.
     * @throws NullPointerException if <code>in</code> is null.
     * @see    #storeToXML(OutputStream, String, String)
     * @since 1.5
     */
//    public synchronized void loadFromXML(InputStream in)
//        throws IOException, InvalidPropertiesFormatException 
//    {
//        if (in == null)
//            throw new NullPointerException();
//        XMLUtils.load(this, in);
//    }

    /**
     * Emits an XML document representing all of the properties contained
     * in this table.
     *
     * <p> An invocation of this method of the form <tt>props.storeToXML(os,
     * comment)</tt> behaves in exactly the same way as the invocation
     * <tt>props.storeToXML(os, comment, "UTF-8");</tt>.
     *
     * @param os the output stream on which to emit the XML document.
     * @param comment a description of the property list, or <code>null</code>
     *        if no comment is desired.
     * @throws IOException if writing to the specified output stream
     *         results in an <tt>IOException</tt>.
     * @throws NullPointerException if <code>os</code> is null.
     * @see    #loadFromXML(InputStream)
     * @since 1.5
     */
//    public synchronized void storeToXML(OutputStream os, String comment)
//        throws IOException
//    {
//        if (os == null)
//            throw new NullPointerException();
//        storeToXML(os, comment, "UTF-8");
//    }

    /**
     * Emits an XML document representing all of the properties contained
     * in this table, using the specified encoding.
     *
     * <p>The XML document will have the following DOCTYPE declaration:
     * <pre>
     * &lt;!DOCTYPE properties SYSTEM "http://java.sun.com/dtd/properties.dtd"&gt;
     * </pre>
     *
     *<p>If the specified comment is <code>null</code> then no comment
     * will be stored in the document.
     *
     * <p>The specified stream remains open after this method returns.
     *
     * @param os the output stream on which to emit the XML document.
     * @param comment a description of the property list, or <code>null</code>
     *        if no comment is desired.
     * @throws IOException if writing to the specified output stream
     *         results in an <tt>IOException</tt>.
     * @throws NullPointerException if <code>os</code> is <code>null</code>,
     *         or if <code>encoding</code> is <code>null</code>.
     * @see    #loadFromXML(InputStream)
     * @since 1.5
     */
//    public synchronized void storeToXML(OutputStream os, String comment, 
//                                       String encoding)
//        throws IOException
//    {
//        if (os == null)
//            throw new NullPointerException();
//        XMLUtils.save(this, os, comment, encoding);
//    }

    /**
     * Searches for the property with the specified key in this property list.
     * If the key is not found in this property list, the default property list,
     * and its defaults, recursively, are then checked. The method returns
     * <code>null</code> if the property is not found.
     *
     * @param   key   the property key.
     * @return  the value in this property list with the specified key value.
     * @see     #setProperty
     * @see     #defaults
     */
    public String getProperty(String key) {
	Object oval = super.get(key);
	String sval = (oval instanceof String) ? (String)oval : null;
	return ((sval == null) && (defaults != null)) ? defaults.getProperty(key) : sval;
    }

    /**
     * Searches for the property with the specified key in this property list.
     * If the key is not found in this property list, the default property list,
     * and its defaults, recursively, are then checked. The method returns the
     * default value argument if the property is not found.
     *
     * @param   key            the hashtable key.
     * @param   defaultValue   a default value.
     *
     * @return  the value in this property list with the specified key value.
     * @see     #setProperty
     * @see     #defaults
     */
    public String getProperty(String key, String defaultValue) {
	String val = getProperty(key);
	return (val == null) ? defaultValue : val;
    }

    /**
     * Returns an enumeration of all the keys in this property list,
     * including distinct keys in the default property list if a key
     * of the same name has not already been found from the main
     * properties list.
     *
     * @return  an enumeration of all the keys in this property list, including
     *          the keys in the default property list.
     * @see     java.util.Enumeration
     * @see     java.util.Properties#defaults
     */
    public Enumeration<?> propertyNames() {
	Hashtable h = new Hashtable();
	enumerate(h);
	return h.keys();
    }

    /**
     * Prints this property list out to the specified output stream.
     * This method is useful for debugging.
     *
     * @param   out   an output stream.
     */
    public void list(PrintStream out) {
	out.println("-- listing properties --");
	Hashtable h = new Hashtable();
	enumerate(h);
	for (Enumeration e = h.keys() ; e.hasMoreElements() ;) {
	    String key = (String)e.nextElement();
	    String val = (String)h.get(key);
	    if (val.length() > 40) {
                val = val.substring(0, 37) + "...";
	    }
	    out.println(key + "=" + val);
	}
    }

    /**
     * Prints this property list out to the specified output stream.
     * This method is useful for debugging.
     *
     * @param   out   an output stream.
     * @since   JDK1.1
     */
    /*
     * Rather than use an anonymous inner class to share common code, this
     * method is duplicated in order to ensure that a non-1.1 compiler can
     * compile this file.
     */
//    public void list(PrintWriter out) {
//	out.println("-- listing properties --");
//	Hashtable h = new Hashtable();
//	enumerate(h);
//	for (Enumeration e = h.keys() ; e.hasMoreElements() ;) {
//	    String key = (String)e.nextElement();
//	    String val = (String)h.get(key);
//	    if (val.length() > 40) {
//		val = val.substring(0, 37) + "...";
//	    }
//	    out.println(key + "=" + val);
//	}
//    }

    /**
     * Enumerates all key/value pairs in the specified hashtable.
     * @param h the hashtable
     */
    private synchronized void enumerate(Hashtable h) {
	if (defaults != null) {
	    defaults.enumerate(h);
	}
	for (Enumeration e = keys() ; e.hasMoreElements() ;) {
	    String key = (String)e.nextElement();
	    h.put(key, get(key));
	}
    }

    /**
     * Convert a nibble to a hex character
     * @param	nibble	the nibble to convert.
     */
    private static char toHex(int nibble) {
	return hexDigit[(nibble & 0xF)];
    }

    /** A table of hex digits */
    private static final char[] hexDigit = {
	'0','1','2','3','4','5','6','7','8','9','A','B','C','D','E','F'
    };
}
