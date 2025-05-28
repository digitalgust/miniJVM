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
    protected Map<String, List<String>> rcvHeader = new HashMap<>();
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
                if (method.equals(HTTP_POST)) {
                    connection.setRequestMethod(HttpConnection.POST);
                    byte[] data = outData.toByteArray();
                    for (String k : outHeader.keySet()) {
                        List<String> vals = outHeader.get(k);
                        StringBuilder sb = new StringBuilder();
                        for (String v : vals) {
                            sb.append(v).append(',');
                        }
                        if (sb.length() > 0) {
                            sb.deleteCharAt(sb.length() - 1);
                        }
                        connection.setRequestProperty(k, sb.toString());
                    }
                    connection.setRequestProperty("Content-Length", String.valueOf(data.length));
                    connection.openDataOutputStream().write(data);
                } else {
                    connection.setRequestMethod(HttpConnection.GET);
                }
                status = connection.getResponseCode();
                if (status != HttpConnection.HTTP_OK) {
                    throw new IOException("Http status: " + status);
                }
                for (int i = 0; true; i++) {
                    String k = connection.getHeaderFieldKey(i);
                    if (k == null) break;
                    String v = connection.getHeaderField(k);
                    List<String> list = new ArrayList<>();
                    list.add(v);
                    rcvHeader.put(k.toLowerCase(), list);
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

    public void setRequestProperty(String key, String value) {
        List<String> list = outHeader.get(key);
        if (list == null) {
            list = new ArrayList<>();
            outHeader.put(key, list);
        }
        list.add(value);
    }

    public void addRequestProperty(String key, String value) {
        List<String> list = outHeader.get(key.toLowerCase());
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

    public abstract void disconnect();
}
