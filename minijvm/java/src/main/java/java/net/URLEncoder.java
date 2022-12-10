/*
 * @(#)URLEncoder.java	1.30 07/05/17
 *
 * Copyright 2007 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package java.net;

import java.io.ByteArrayOutputStream;
import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.io.CharArrayWriter;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;
import java.util.BitSet;
import java.security.AccessController;
import java.security.PrivilegedAction;
import sun.security.action.GetBooleanAction;
import sun.security.action.GetPropertyAction;

public class URLEncoder {
    static BitSet dontNeedEncoding;
    static final int caseDiff = ('a' - 'A');
    static String dfltEncName = null;

    static {

	/* The list of characters that are not encoded has been
	 * determined as follows:
	 *
	 * RFC 2396 states:
	 * -----
	 * Data characters that are allowed in a URI but do not have a
	 * reserved purpose are called unreserved.  These include upper
	 * and lower case letters, decimal digits, and a limited set of
	 * punctuation marks and symbols. 
	 *
	 * unreserved  = alphanum | mark
	 *
	 * mark        = "-" | "_" | "." | "!" | "~" | "*" | "'" | "(" | ")"
	 *
	 * Unreserved characters can be escaped without changing the
	 * semantics of the URI, but this should not be done unless the
	 * URI is being used in a context that does not allow the
	 * unescaped character to appear.
	 * -----
	 *
	 * It appears that both Netscape and Internet Explorer escape
	 * all special characters from this list with the exception
	 * of "-", "_", ".", "*". While it is not clear why they are
	 * escaping the other characters, perhaps it is safest to
	 * assume that there might be contexts in which the others
	 * are unsafe if not escaped. Therefore, we will use the same
	 * list. It is also noteworthy that this is consistent with
	 * O'Reilly's "HTML: The Definitive Guide" (page 164).
	 *
	 * As a last note, Intenet Explorer does not encode the "@"
	 * character which is clearly not unreserved according to the
	 * RFC. We are being consistent with the RFC in this matter,
	 * as is Netscape.
	 *
	 */

	dontNeedEncoding = new BitSet(256);
	int i;
	for (i = 'a'; i <= 'z'; i++) {
	    dontNeedEncoding.set(i);
	}
	for (i = 'A'; i <= 'Z'; i++) {
	    dontNeedEncoding.set(i);
	}
	for (i = '0'; i <= '9'; i++) {
	    dontNeedEncoding.set(i);
	}
	dontNeedEncoding.set(' '); /* encoding a space to a + is done
				    * in the encode() method */
	dontNeedEncoding.set('-');
	dontNeedEncoding.set('_');
	dontNeedEncoding.set('.');
	dontNeedEncoding.set('*');

    	dfltEncName = (String)AccessController.doPrivileged (
	    new GetPropertyAction("file.encoding")
    	);
    }

    /**
     * You can't call the constructor.
     */
    private URLEncoder() { }

    /**
     * Translates a string into <code>x-www-form-urlencoded</code>
     * format. This method uses the platform's default encoding
     * as the encoding scheme to obtain the bytes for unsafe characters.
     *
     * @param   s   <code>String</code> to be translated.
     * @deprecated The resulting string may vary depending on the platform's
     *             default encoding. Instead, use the encode(String,String)
     *             method to specify the encoding.
     * @return  the translated <code>String</code>.
     */
    public static String encode(String s) {

	String str = null;

	try {
	    str = encode(s, dfltEncName);
	} catch (UnsupportedEncodingException e) {
	    // The system should always have the platform default
	}

	return str;
    }

    /**
     * Translates a string into <code>application/x-www-form-urlencoded</code>
     * format using a specific encoding scheme. This method uses the
     * supplied encoding scheme to obtain the bytes for unsafe
     * characters.
     * <p>
     * <em><strong>Note:</strong> The <a href=
     * "http://www.w3.org/TR/html40/appendix/notes.html#non-ascii-chars">
     * World Wide Web Consortium Recommendation</a> states that
     * UTF-8 should be used. Not doing so may introduce
     * incompatibilites.</em>
     *
     * @param   s   <code>String</code> to be translated.
     * @param   enc   The name of a supported 
     *    <a href="../lang/package-summary.html#charenc">character
     *    encoding</a>.
     * @return  the translated <code>String</code>.
     * @exception  UnsupportedEncodingException
     *             If the named encoding is not supported
     * @see URLDecoder#decode(String, String)
     * @since 1.4
     */
    public static String encode(String s, String enc) 
	throws UnsupportedEncodingException {

       boolean needToChange = false;
       StringBuffer out = new StringBuffer(s.length());
       Charset charset;
       CharArrayWriter charArrayWriter = new CharArrayWriter();

       if (enc == null)
           throw new NullPointerException("charsetName");

       try {
           charset = Charset.forName(enc);
       } catch (IllegalCharsetNameException e) {
           throw new UnsupportedEncodingException(enc);
       } catch (UnsupportedCharsetException e) {
           throw new UnsupportedEncodingException(enc);
       }

       for (int i = 0; i < s.length();) {
           int c = (int) s.charAt(i);
           //System.out.println("Examining character: " + c);
           if (dontNeedEncoding.get(c)) {
               if (c == ' ') {
                   c = '+';
                   needToChange = true;
               }
               //System.out.println("Storing: " + c);
               out.append((char)c);
               i++;
           } else {
               // convert to external encoding before hex conversion
               do {
                   charArrayWriter.write(c);
                   /*
                    * If this character represents the start of a Unicode
                    * surrogate pair, then pass in two characters. It's not
                    * clear what should be done if a bytes reserved in the
                    * surrogate pairs range occurs outside of a legal
                    * surrogate pair. For now, just treat it as if it were
                    * any other character.
                    */
                   if (c >= 0xD800 && c <= 0xDBFF) {
                       /*
                         System.out.println(Integer.toHexString(c)
                         + " is high surrogate");
                        */
                        if ( (i+1) < s.length()) {
                            int d = (int) s.charAt(i+1);
                            /*
                              System.out.println("\tExamining "
                              + Integer.toHexString(d));
                            */
                            if (d >= 0xDC00 && d <= 0xDFFF) {
                                /*
                                  System.out.println("\t"
                                  + Integer.toHexString(d)
                                  + " is low surrogate");
                                */
                                charArrayWriter.write(d);
                                i++;
                            }
                        }
                    }
                    i++;
                } while (i < s.length() && !dontNeedEncoding.get((c = (int) s.charAt(i))));
                charArrayWriter.flush();
                String str = new String(charArrayWriter.toCharArray());
                byte[] ba = str.getBytes(charset.name());
                for (int j = 0; j < ba.length; j++) {
                    out.append('%');
                    char ch = Character.forDigit((ba[j] >> 4) & 0xF, 16);
                    // converting to use uppercase letter as part of
                    // the hex value if ch is a letter.
                    if (Character.isLetter(ch)) {
                        ch -= caseDiff;
                    }
                    out.append(ch);
                    ch = Character.forDigit(ba[j] & 0xF, 16);
                    if (Character.isLetter(ch)) {
                        ch -= caseDiff;
                    }
                    out.append(ch);
                }
                charArrayWriter.reset();
                needToChange = true;
            }
        }

        return (needToChange? out.toString() : s);
    }
}

