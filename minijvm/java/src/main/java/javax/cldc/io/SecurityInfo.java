/*
 * @(#)SecurityInfo.java	1.22 02/09/06 @(#)
 *
 * Copyright (c) 2001-2002 Sun Microsystems, Inc.  All rights reserved.
 * PROPRIETARY/CONFIDENTIAL
 * Use is subject to license terms.
 */

package javax.cldc.io;

import java.lang.String;
import java.io.IOException;

/**
 * This interface defines methods
 * to access information about a secure network connection.
 * Protocols that implement secure connections may use this interface
 * to report the security parameters of the connection.
 * <p>
 * It provides the certificate, protocol, version, and cipher suite,
 * etc. in use.
 *
 * @see HttpsConnection
 * @since MIDP 2.0
 */
public interface SecurityInfo {

    /**
     * Returns the <CODE>Certificate</CODE> used to establish the
     * secure connection with the server.
     *
     * @return the <CODE>Certificate</CODE> used to establish the
     * secure connection with the server.
     */
//    public Certificate getServerCertificate();//gust

    /**
     * Returns the protocol version.
     * If appropriate, it should contain the major and minor versions
     * for the protocol separated with a "." (Unicode U+002E).
     * <pre>
     *     For SSL V3 it MUST return "3.0"
     *     For TLS 1.0 it MUST return "3.1"
     *     For WTLS (WAP-199) it MUST return "1"
     *     For WAP TLS Profile and Tunneling Specification it MUST return "3.1"
     *</pre>
     * @return a String containing the version of the protocol;
     *		the return value MUST NOT be <CODE>null</CODE>.
     */
    public String getProtocolVersion();

    /**
     * Returns the secure protocol name.
     *
     * @return a <code>String</code> containing the secure protocol identifier;
     * if TLS (RFC 2246) or WAP TLS Profile and Tunneling (WAP-219-TLS)
     * is used for the connection the return value is "TLS";
     * if SSL V3 (The SSL Protocol Version 3.0) is used for the connection;
     * the return value is "SSL");
     * if WTLS (WAP 199) is used for the connection the return value is "WTLS".
     */
    public String getProtocolName();

    /**
     * Returns the name of the cipher suite in use for the connection.
     * The name returned is from the CipherSuite column of the CipherSuite
     * definitions table in Appendix C of RFC 2246. If the cipher suite is
     * not in Appendix C, the name returned is non-null and its contents
     * are not specified. For non-TLS implementions the cipher suite name
     * should be selected according to the actual key exchange, cipher,
     * and hash
     * combination used to establish the connection, so that regardless of
     * whether the secure connection uses SSL V3
     * or TLS 1.0 or WTLS or WAP TLS Profile and Tunneling,
     * equivalent cipher suites have the same name.
     *
     * @return a <code>String</code> containing the name of the cipher suite
     * in use.
     */
    public String getCipherSuite();
}
