
package org.mini.net.https;

import org.mini.net.SocketNative;
import org.mini.net.http.Properties;

import javax.cldc.io.HttpsConnection;
import javax.cldc.io.SecurityInfo;
import javax.cldc.io.StreamConnection;
import java.io.*;
import java.nio.ByteBuffer;


public class Protocol extends org.mini.net.http.Protocol
        implements HttpsConnection {

    /**
     * Common name label.
     */
    private static final String COMMON_NAME_LABEL = "CN=";

    /**
     * Common name label length.
     */
    private static final int COMMON_NAME_LABEL_LENGTH =
            COMMON_NAME_LABEL.length();


    /**
     * Parse the common name out of a distinguished name.
     *
     * @param name distinguished name
     * @return common name attribute without the label
     */
    private static String getCommonName(String name) {
        int start;
        int end;

        if (name == null) {
            return null;
        }

        /* The common name starts with "CN=" label */
        start = name.indexOf(COMMON_NAME_LABEL);
        if (start < 0) {
            return null;
        }

        start += COMMON_NAME_LABEL_LENGTH;
        end = name.indexOf(';', start);
        if (end < 0) {
            end = name.length();
        }

        return name.substring(start, end);
    }

    /**
     * Check to see if the site name given by the user matches the site
     * name of subject in the certificate. The method supports the wild card
     * character for the machine name if a domain name is included after it.
     *
     * @param siteName site name the user provided
     * @param certName site name of the subject from a certificate
     * @return true if the common name checks out, else false
     */
    private static boolean checkSiteName(String siteName, String certName) {
        int startOfDomain;
        int domainLength;

        if (certName == null) {
            return false;
        }

        // try the easy way first, ignoring case
        if ((siteName.length() == certName.length()) &&
                siteName.regionMatches(true, 0, certName, 0,
                        certName.length())) {
            return true;
        }

        if (!certName.startsWith("*.")) {
            // not a wild card, done
            return false;
        }

        startOfDomain = siteName.indexOf('.');
        if (startOfDomain == -1) {
            // no domain name
            return false;
        }

        // skip past the '.'
        startOfDomain++;

        domainLength = siteName.length() - startOfDomain;
        if ((certName.length() - 2) != domainLength) {
            return false;
        }

        // compare the just the domain names, ignoring case
        if (siteName.regionMatches(true, startOfDomain, certName, 2,
                domainLength)) {
            return true;
        }

        return false;
    }

    /**
     * collection of "Proxy-" headers as name/value pairs
     */
    private Properties proxyHeaders = new Properties();

    /**
     * Underlying SSL connection.
     */
    private SSLStreamConnection sslConnection;

    /**
     * Create a new instance of this class. Override the some of the values
     * in our super class.
     */
    public Protocol() {
        protocol = "https";
        default_port = 443; // 443 is the default port for HTTPS

        //requiredPermission = Permissions.HTTPS;//gust
    }

    /**
     * Get the request header value for the named property.
     *
     * @param key property name of specific HTTP 1.1 header field
     * @return value of the named property, if found, null otherwise.
     */
    public String getRequestProperty(String key) {
        /* https handles the proxy fields in a different way */
        if (key.startsWith("Proxy-")) {
            return proxyHeaders.getProperty(key);
        }

        return super.getRequestProperty(key);
    }

    /**
     * Add the named field to the list of request fields.
     *
     * @param key   key for the request header field.
     * @param value the value for the request header field.
     */
    protected void setRequestField(String key, String value) {
        /* https handles the proxy fields in a different way */
        if (key.startsWith("Proxy-")) {
            proxyHeaders.setProperty(key, value);
            return;
        }

        super.setRequestField(key, value);
    }

    class SSLStreamConnection implements StreamConnection {
        byte[] httpsinfo;
        InputStream in;
        OutputStream out;

        byte[] toutf8bytes(String s) {
            try {
                return (s + "\0").getBytes("utf-8");
            } catch (Exception e) {
            }
            return null;
        }

        public void connectImpl() throws IOException {
            httpsinfo = SocketNative.http_construct_httpinfo();
            int ret = SocketNative.http_init(httpsinfo, false);
            if (httpsinfo == null || ret < 0) {
                throw new IOException("https init error");
            }
            ret = SocketNative.http_open(httpsinfo, toutf8bytes(getURL()));
            if (ret < 0) {
                throw new IOException("https open error");
            }
        }

        @Override
        public InputStream openInputStream() throws IOException {
            if (in == null) {
                in = new InputStream() {
                    ByteBuffer buf = ByteBuffer.allocate(4096);

                    {
                        buf.flip();
                    }

                    byte[] b = new byte[1024];

                    @Override
                    public int read() throws IOException {
                        int ret;
                        while (buf.remaining() == 0) {
                            ret = SocketNative.https_read(httpsinfo, b, 0, b.length);
                            if (ret < 0) {
                                SocketNative.http_close(httpsinfo);
                                return -1;
                            }
                            if (ret == 0) {
                                Thread.yield();
                                continue;
                            }
                            buf.compact();
                            buf.put(b, 0, ret);
                            buf.flip();
                        }
                        return buf.get() & 0xff;
                    }

                    @Override
                    public void close() throws IOException {
                        int ret = SocketNative.http_close(httpsinfo);
                        if (ret < 0) throw new IOException("https inputstream close error");
                    }
                };
            }
            return in;
        }

        @Override
        public DataInputStream openDataInputStream() throws IOException {
            return new DataInputStream(openInputStream());
        }

        @Override
        public OutputStream openOutputStream() throws IOException {
            if (out == null) {
                out = new OutputStream() {
                    byte[] b = new byte[1];

                    @Override
                    public void write(int b) throws IOException {
                        this.b[0] = (byte) b;
                        write(this.b, 0, 1);
                    }

                    @Override
                    public void write(byte[] b, int offset, int len) throws IOException {
                        int ret = SocketNative.https_write(httpsinfo, b, offset, len);
                        if (ret <= 0) {
                            SocketNative.http_close(httpsinfo);
                            throw new IOException("https write error");
                        }
                    }

                    @Override
                    public void close() throws IOException {
                        int ret = SocketNative.http_close(httpsinfo);
                        if (ret < 0) throw new IOException("https outputstream close error");
                    }
                };
            }
            return out;
        }

        @Override
        public DataOutputStream openDataOutputStream() throws IOException {
            return new DataOutputStream(openOutputStream());
        }

        @Override
        public void close() throws IOException {
            try {
                if (in != null) {
                    in.close();
                }
            } catch (Exception e) {
            }
            try {
                if (out != null) {
                    out.close();
                }
            } catch (Exception e) {
            }
            SocketNative.http_close(httpsinfo);
        }
    }

    /**
     * Connect to the underlying secure socket transport.
     * Perform the SSL handshake and then proceded to the underlying
     * HTTP protocol connect semantics.
     *
     * @return SSL/TCP stream connection
     * @throws IOException is thrown if the connection cannot be opened
     */
    protected StreamConnection connect() throws IOException {
        if (sslConnection == null) {
            sslConnection = new SSLStreamConnection();
            sslConnection.connectImpl();
        }
        return sslConnection;
    }

    /**
     * disconnect the current connection.
     *
     * @param connection   connection return from {@link #connect()}
     * @param inputStream  input stream opened from <code>connection</code>
     * @param outputStream output stream opened from <code>connection</code>
     * @throws IOException if an I/O error occurs while
     *                     the connection is terminated.
     */
    protected void disconnect(StreamConnection connection,
                              InputStream inputStream, OutputStream outputStream)
            throws IOException {
        try {
            try {
                inputStream.close();
            } finally {
                try {
                    outputStream.close();
                } finally {
                    connection.close();
                }
            }
        } catch (IOException e) {
        } catch (NullPointerException e) {
        }
    }

    /**
     * Return the security information associated with this connection.
     * If the connection is still in <CODE>Setup</CODE> state then
     * the connection is initiated to establish the secure connection
     * to the server.  The method returns when the connection is
     * established and the <CODE>Certificate</CODE> supplied by the
     * server has been validated.
     * The <CODE>SecurityInfo</CODE> is only returned if the
     * connection has been successfully made to the server.
     *
     * @return the security information associated with this open connection.
     * supplied by the server cannot be validated.
     * The <code>CertificateException</code> will contain
     * the information about the error and indicate the certificate in the
     * validation chain with the error.
     * @throws IOException if an arbitrary connection failure occurs
     */
    public SecurityInfo getSecurityInfo() throws IOException {
        ensureOpen();

        sendRequest();

        return new SecurityInfo() {
            @Override
            public String getProtocolVersion() {
                return "https 1.1";
            }

            @Override
            public String getProtocolName() {
                return "https";
            }

            @Override
            public String getCipherSuite() {
                return "";
            }
        };
    }
}
