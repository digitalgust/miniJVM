/*
 * @(#)HttpsConnection.java	1.31 02/07/24 @(#)
 *
 * Copyright (c) 2001-2002 Sun Microsystems, Inc.  All rights reserved.
 * PROPRIETARY/CONFIDENTIAL
 * Use is subject to license terms.
 */

package javax.cldc.io;

import java.lang.String;
import java.io.IOException;

/**
 * This interface defines the necessary methods
 * and constants to establish a secure network connection.
 * The URI format with scheme <CODE>https</CODE> when passed to
 * <CODE>Connector.open</CODE> will return a 
 * <code>HttpsConnection</code>.
 * <A HREF="http://www.ietf.org/rfc/rfc2818.txt">RFC 2818</A> 
 * defines the scheme.
 * <p>
 * A secure connection MUST be implemented by one or more
 * of the following specifications:
 * <UL>
 * <LI>HTTP over TLS as documented in
 *  <A HREF="http://www.ietf.org/rfc/rfc2818.txt">RFC 2818</A>
 * and TLS Protocol Version 1.0 as specified in
 * <A HREF="http://www.ietf.org/rfc/rfc2246.txt">RFC 2246</A>.
 *
 * <LI>SSL V3 as specified in
 *    <A HREF="http://home.netscape.com/eng/ssl3/draft302.txt">
 *    The SSL Protocol Version 3.0</A>
 * </LI>
 *
 * <LI>WTLS as specified in
 * 	<A HREF="http://www.wapforum.org/what/technical_1_2_1.htm">
 * 	WAP Forum Specifications June 2000 (WAP 1.2.1) conformance release</A>
 * 	- Wireless Transport Layer Security document WAP-199.
 * <LI>WAP(TM) TLS Profile and Tunneling Specification as specified
 *	in <A HREF="http://www.wapforum.com/what/technical.htm">
 *	WAP-219-TLS-20010411-a</A>
 * </UL>
 * <p>
 * HTTPS is the secure version of HTTP (IETF RFC2616), 
 * a request-response protocol in which the parameters of the request must 
 * be set before the request is sent.
 * <p>
 * In addition to the normal <code>IOExceptions</code> that may occur during 
 * invocation of the various methods that cause a transition to the Connected
 * state, <code>CertificateException</code>
 * (a subtype of <code>IOException</code>) may be thrown to indicate various
 * failures related to establishing the secure link.  The secure link
 * is necessary in the <code>Connected</code> state so the headers
 * can be sent securely. The secure link may be established as early as the
 * invocation of <code>Connector.open()</code> and related  
 * methods for opening input and output streams and failure related to
 * certificate exceptions may be reported.
 *
 * </p><br>
 * <b>Example</b><br>
 *
 * <p>Open a HTTPS connection, set its parameters, then read the HTTP
 * response.</p>
 *
 * <code>Connector.open</code> is used to open the URL 
 * and an <code>HttpsConnection</code> is returned.
 * The HTTP
 * headers are read and processed. If the length is available, it is used
 * to read the data in bulk. From the 
 * <code>HttpsConnection</code> the <code>InputStream</code> is
 * opened. It is used to read every character until end of file (-1). If
 * an exception is thrown the connection and stream are closed.
 * 
 * <code><PRE>
 *     void getViaHttpsConnection(String url) 
 *            throws CertificateException, IOException {
 *         HttpsConnection c = null;
 *         InputStream is = null;
 *         try {
 *             c = (HttpsConnection)Connector.open(url);
 * 
 *             // Getting the InputStream ensures that the connection
 *             // is opened (if it was not already handled by
 *             // Connector.open()) and the SSL handshake is exchanged,
 *             // and the HTTP response headers are read.
 *             // These are stored until requested.
 *             is = c.openDataInputStream();
 * 
 *             if c.getResponseCode() == HttpConnection.HTTP_OK) {
 *                 // Get the length and process the data
 *                 int len = (int)c.getLength();
 *                 if (len &gt; 0) {
 *                     byte[] data = new byte[len];
 *                     int actual = is.readFully(data);
 *                     ...
 *                 } else {
 *                     int ch;
 *                     while ((ch = is.read()) != -1) {
 *                         ...
 *                     }
 *                 }
 *             } else {
 *               ...
 *             }
 *         } finally {
 *             if (is != null)
 *                 is.close();
 *             if (c != null)
 *                 c.close();
 *         }
 *     }
 * </PRE> </code>
 * @since MIDP 2.0
 */

public interface HttpsConnection extends HttpConnection {
    /**
     * Return the security information associated with this
     * successfully opened connection.
     * If the connection is still in <CODE>Setup</CODE> state then
     * the connection is initiated to establish the secure connection
     * to the server.  The method returns when the connection is
     * established and the <CODE>Certificate</CODE> supplied by the
     * server has been validated.
     * The <CODE>SecurityInfo</CODE> is only returned if the
     * connection has been successfully made to the server.
     *
     * @return the security information associated with this open connection.
     *
     * @exception IOException if an arbitrary connection failure occurs
     */
    public SecurityInfo getSecurityInfo()
	throws IOException;
    /**
     * Returns the network port number of the URL for this
     * <code>HttpsConnection</code>.
     *
     * @return  the network port number of the URL for this
     * <code>HttpsConnection</code>.
     * The default HTTPS port number (443) is returned if there was
     * no port number in the string passed to <code>Connector.open</code>.
     */
    public int getPort();

}
