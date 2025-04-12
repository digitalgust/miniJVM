package org.mini.net.https;

import org.mini.net.SocketNative;
import org.mini.net.http.Properties;

import javax.microedition.io.*;
import java.io.*;


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
     * Create a new instance of this class. Override some of the values
     * in our super class.
     */
    public Protocol() {
        protocol = "https";
        default_port = 443; // 443 is the default port for HTTPS

        parseProxy();
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


        public void connectImpl() throws IOException {

            if (proxyHost != null && proxyHost.length() > 0 && proxyPort > 0) {
                // Connect through proxy
                org.mini.net.socket.Protocol conn = new org.mini.net.socket.Protocol();
                conn.openPrim("//" + proxyHost + ":" + proxyPort, Connector.READ_WRITE, true);

                // Get streams for proxy communication
                DataOutputStream out = conn.openDataOutputStream();
                DataInputStream in = conn.openDataInputStream();

                try {
                    // Send CONNECT request to proxy
                    StringBuilder connectRequest = new StringBuilder();
                    connectRequest.append(String.format("CONNECT %s:%d HTTP/1.1\r\n", getHost(), getPort()));
                    connectRequest.append(String.format("Host: %s:%d\r\n", getHost(), getPort()));
                    connectRequest.append("User-agent: Mozilla/5.0\r\n");

                    // Add any additional proxy headers
                    for (int i = 0; i < proxyHeaders.size(); i++) {
                        String key = proxyHeaders.getKeyAt(i);
                        String val = proxyHeaders.getValueAt(i);
                        connectRequest.append(key + ": " + val + "\r\n");
                    }

                    connectRequest.append("\r\n");

                    // Send the CONNECT request
                    out.write(connectRequest.toString().getBytes());
                    out.flush();

                    // Read proxy response
                    String response = readLine(in);
                    if (response == null || !response.contains(" 200 ")) {
                        throw new IOException("Proxy tunnel setup failed: " + response);
                    }

                    // Skip the rest of proxy response headers
                    while (true) {
                        String line = readLine(in);
                        if (line == null || line.trim().length() == 0) {
                            break;
                        }
                    }
                    //String line=readLine(in);

                    // Now establish SSL connection through the tunnel
                    httpsinfo = SocketNative.sslc_construct_entry();
                    int ret = SocketNative.sslc_init(httpsinfo);
                    if (httpsinfo == null || ret < 0) {
                        throw new IOException("https proxy init error");
                    }
                    ret = SocketNative.sslc_wrap(httpsinfo, conn.getHandle(), SocketNative.toCStyle(getHost()));
                    if (ret < 0) {
                        out.close();
                        in.close();
                        conn.close();
                        throw new IOException("https proxy wrap error");
                    }

                } catch (IOException e) {
                    try {
                        conn.close();
                    } catch (IOException ce) {
                        // Ignore close exception
                    }
                    throw e;
                }
            } else {

                httpsinfo = SocketNative.sslc_construct_entry();
                int ret = SocketNative.sslc_init(httpsinfo);
                if (httpsinfo == null || ret < 0) {
                    throw new IOException("https init error");
                }
                ret = SocketNative.sslc_connect(httpsinfo, SocketNative.toCStyle(getHost()), SocketNative.toCStyle(Integer.toString(getPort())));
                if (ret < 0) {
                    throw new IOException("https open error");
                }
            }
        }

        @Override
        public InputStream openInputStream() throws IOException {
            if (in == null) {
                in = new InputStream() {
                    byte[] buf = new byte[1];

                    @Override
                    public int read() throws IOException {
                        int ret = SocketNative.sslc_read(httpsinfo, buf, 0, 1);
                        if (ret < 0) {
                            return -1;
                        }
                        return buf[0] & 0xff;
                    }

                    @Override
                    public int read(byte[] b, int off, int len) throws IOException {

                        int ret = SocketNative.sslc_read(httpsinfo, b, off, len);
                        if (ret < 0) {
                            return -1;
                        }
                        return ret;
                    }

                    @Override
                    public void close() throws IOException {
                        int ret = SocketNative.sslc_close(httpsinfo);
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
                        int ret = SocketNative.sslc_write(httpsinfo, b, offset, len);
                        if (ret <= 0) {
                            SocketNative.sslc_close(httpsinfo);
                            throw new IOException("https write error");
                        }
                    }

                    @Override
                    public void close() throws IOException {
                        int ret = SocketNative.sslc_close(httpsinfo);
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
            SocketNative.sslc_close(httpsinfo);
        }
    }

    /**
     * Connect to the underlying secure socket transport.
     * If proxy is configured, first establish a tunnel through the proxy using CONNECT.
     *
     * @return SSL/TCP stream connection
     * @throws IOException if the connection cannot be opened
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
