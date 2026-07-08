package java.net;

import javax.microedition.io.Connection;
import javax.microedition.io.Connector;
import javax.microedition.io.HttpConnection;
import java.io.*;
import java.util.*;

public abstract class HttpURLConnection extends URLConnection {
    protected String HTTP_GET = "GET";
    protected String HTTP_POST = "POST";
    protected String HTTP_HEAD = "HEAD";
    protected String HTTP_PUT = "PUT";
    protected String HTTP_DELETE = "DELETE";
    protected String HTTP_TRACE = "TRACE";
    protected String HTTP_OPTIONS = "OPTIONS";

    String[] methods = {
            HTTP_GET,
            HTTP_POST,
            HTTP_HEAD,
            HTTP_PUT,
            HTTP_DELETE,
            HTTP_TRACE,
            HTTP_OPTIONS
    };
    private static final String HKEY_CONTENT_LENGTH = "content-length";

    protected javax.microedition.io.HttpConnection connection;
    protected Map<String, List<String>> outHeader = new HashMap<>();
    protected ByteArrayOutputStream outData;

    protected int status;
    protected String responseMessage;
    protected Map<String, List<String>> rcvHeader = new HashMap<>();
    protected List<String> headerKeys = new ArrayList<>();
    protected List<String> headerValues = new ArrayList<>();
    protected String cacheFilePath; // Store cache file path instead of data

    String method = HTTP_GET;

    protected HttpURLConnection(URL url) {
        super(url);
    }

    @Override
    public void connect() throws IOException {
        if (cacheFilePath != null) return;

        String urlStr = url.toString();
        int index = urlStr.indexOf("?");
        if (index > 0) {
            urlStr = urlStr.substring(0, index);
        }
        CachedFile cachedFile = caches.get(urlStr);
        if (cachedFile != null && !cachedFile.isExpired()) {
            if (useCaches) {  //cache hit - load from file
                File cacheFile = new File((String) cachedFile.resource);
                if (cacheFile.exists()) {
                    cacheFilePath = cacheFile.getAbsolutePath();
                    return;
                }
            }
        } else {
            // Remove expired cache entry and delete the file
            if (cachedFile != null && cachedFile.isExpired()) {
                if (cachedFile.resource instanceof String) {
                    try {
                        File file = new File((String) cachedFile.resource);
                        if (file.exists()) {
                            file.delete();
                        }
                    } catch (Exception e) {
                        // Ignore cleanup errors
                    }
                }
            }
            caches.remove(urlStr);
        }
        //request
        if (connection == null) {
            Connection con = Connector.open(url.toString());
            if (con instanceof HttpConnection) {
                connection = (HttpConnection) con;
                applyRequestHeaders();
                if (method.equals(HTTP_POST)) {
                    connection.setRequestMethod(HttpConnection.POST);
                    byte[] data = outData != null ? outData.toByteArray() : new byte[0];
                    connection.setRequestProperty("Content-Length", String.valueOf(data.length));
                    OutputStream output = connection.openDataOutputStream();
                    output.write(data);
                } else {
                    connection.setRequestMethod(HttpConnection.GET);
                }
                status = connection.getResponseCode();
                responseMessage = connection.getResponseMessage();
                headerKeys.clear();
                headerValues.clear();
                rcvHeader.clear();
                for (int i = 0; true; i++) {
                    String k = connection.getHeaderFieldKey(i);
                    String v = connection.getHeaderField(i);
                    if (k == null && v == null) {
                        break;
                    }
                    headerKeys.add(k);
                    headerValues.add(v);
                    if (k != null) {
                        List<String> list = rcvHeader.get(k.toLowerCase());
                        if (list == null) {
                            list = new ArrayList<>();
                            rcvHeader.put(k.toLowerCase(), list);
                        }
                        list.add(v);
                    }
                }
                InputStream is = connection.openDataInputStream();
                // Save response to temporary file
                File tmpFile = File.createTempFile(getCacheFileName(), ".tmp");
                FileOutputStream fos = new FileOutputStream(tmpFile);
                byte[] buf = new byte[1024];
                while (true) {
                    int len = is.read(buf);
                    if (len < 0) break;
                    fos.write(buf, 0, len);
                }
                try {
                    fos.close();
                } catch (Exception e) {
                }
                try {
                    is.close();
                } catch (Exception e) {
                }
                cacheFilePath = tmpFile.getAbsolutePath();

                long exp = connection.getExpiration();
                if (exp <= 0) {
                    exp = System.currentTimeMillis() + CACHE_EXPIRE_TIME; // 24 hours default
                }
                long cur = System.currentTimeMillis();
                if (cur < exp) {
                    // Cache the file if conditions are met
                    CachedFile res = new CachedFile(cacheFilePath, exp);
                    caches.put(urlStr, res);
                }
            }
        }
    }


    @Override
    public InputStream getInputStream() throws IOException {
        connect();
        if (cacheFilePath != null) {
            return new FileInputStream(cacheFilePath);
        } else {
            return connection.openDataInputStream();
        }
    }

    @Override
    public OutputStream getOutputStream() throws IOException {
        if (outData == null) {
            outData = new ByteArrayOutputStream();
        }
        return outData;
    }

    @Override
    public int getContentLength() {
        return getHeaderFieldInt(HKEY_CONTENT_LENGTH, -1);
    }

    @Override
    public long getContentLengthLong() {
        return getHeaderFieldLong(HKEY_CONTENT_LENGTH, -1l);
    }

    @Override
    public Map<String, List<String>> getHeaderFields() {
        return (rcvHeader);
    }

    public int getResponseCode() throws IOException {
        connect();
        return status;
    }

    public String getResponseMessage() throws IOException {
        connect();
        return responseMessage;
    }

    public String getHeaderField(int index) {
        try {
            connect();
        } catch (IOException e) {
            return null;
        }
        if (index < 0 || index >= headerValues.size()) {
            return null;
        }
        return headerValues.get(index);
    }

    public String getHeaderFieldKey(int index) {
        try {
            connect();
        } catch (IOException e) {
            return null;
        }
        if (index < 0 || index >= headerKeys.size()) {
            return null;
        }
        return headerKeys.get(index);
    }

    public void setRequestProperty(String key, String value) {
        List<String> list = outHeader.get(key);
        if (list == null) {
            list = new ArrayList<>();
            outHeader.put(key, list);
        }
        list.add(value);
    }

    public void addRequestProperty(String key, String value) {
        List<String> list = outHeader.get(key);
        if (list == null) {
            list = new ArrayList<>();
            outHeader.put(key, list);
        }
        list.add(value);
    }

    public String getRequestProperty(String key) {
        List<String> list = outHeader.get(key);
        if (list != null && list.size() > 0) {
            return list.get(0);
        } else {
            return null;
        }
    }

    public Map<String, List<String>> getRequestProperties() {
        return outHeader;
    }

    public void setRequestMethod(String method) {
        for (String m : methods) {
            if (m.equals(method)) {
                this.method = method;
                return;
            }
        }
        throw new IllegalArgumentException("method not support");
    }

    private void applyRequestHeaders() throws IOException {
        for (String key : outHeader.keySet()) {
            List<String> values = outHeader.get(key);
            if (values == null || values.isEmpty()) {
                continue;
            }
            StringBuilder joined = new StringBuilder();
            for (int i = 0; i < values.size(); i++) {
                if (i > 0) {
                    joined.append(',');
                }
                joined.append(values.get(i));
            }
            connection.setRequestProperty(key, joined.toString());
        }
    }

    public abstract void disconnect();
}
