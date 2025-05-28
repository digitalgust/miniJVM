/* Copyright (c) 2008-2015, Avian Contributors

   Permission to use, copy, modify, and/or distribute this software
   for any purpose with or without fee is hereby granted, provided
   that the above copyright notice and this permission notice appear
   in all copies.

   There is NO WARRANTY for this software.  See license.txt for
   details. */

package java.net;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.File;
import java.util.*;

public abstract class URLConnection {
    protected final URL url;
    protected boolean doInput = true;
    protected boolean doOutput = false;
    protected boolean useCaches = false;
    protected static final long CACHE_EXPIRE_TIME = 1000 * 60 * 60 * 24;
    protected static final int MAX_CACHE_SIZE = 200;
    protected static final String CACHE_FILE_PREFIX = "http_cache_";
    protected static final Map<String, CachedFile> caches = Collections.synchronizedMap(new LinkedHashMap() {
        @Override
        protected boolean removeEldestEntry(Map.Entry eldest) {
            if (size() > MAX_CACHE_SIZE) {
                // Clean up the file when removing cache entry
                CachedFile cachedFile = (CachedFile) eldest.getValue();
                if (cachedFile != null && cachedFile.resource instanceof String) {
                    try {
                        File file = new File((String) cachedFile.resource);
                        if (file.exists()) {
                            file.delete();
                        }
                    } catch (Exception e) {
                        // Ignore cleanup errors
                    }
                }
                return true;
            }
            return false;
        }
    });

    protected URLConnection(URL url) {
        this.url = url;
    }

    public Object getContent() throws IOException {
        return getInputStream();
    }

    public String getContentType() {
        return getHeaderField("content-type");
    }

    public int getContentLength() {
        return -1;
    }

    public long getContentLengthLong() {
        return -1l;
    }

    public abstract void connect() throws IOException;

    public InputStream getInputStream() throws IOException {
        throw new UnknownServiceException();
    }

    public OutputStream getOutputStream() throws IOException {
        throw new UnknownServiceException();
    }

    public boolean getDoInput() {
        return doInput;
    }

    public boolean getDoOutput() {
        return doOutput;
    }

    public void setDoInput(boolean v) {
        doInput = v;
    }

    public void setDoOutput(boolean v) {
        doInput = v;
    }

    public void setUseCaches(boolean v) {
        useCaches = v;
        if (!v) {
            cleanExpiredCaches();
        }
    }

    public boolean getUseCaches() {
        return useCaches;
    }

    public String getHeaderField(String name) {
        String result = null;
        if (name != null) {
            List<String> values = getHeaderFields().get(name.toLowerCase());
            if (values != null && values.size() > 0) {
                result = values.get(0);
            }
        }

        return result;
    }

    public int getHeaderFieldInt(String name, int Default) {
        return (int) getHeaderFieldLong(name, Default);
    }

    public long getHeaderFieldLong(String name, long Default) {
        long result = Default;
        try {
            result = Long.parseLong(getHeaderField(name));
        } catch (Exception e) {
            // Do nothing, default will be returned
        }

        return result;
    }

    public Map<String, List<String>> getHeaderFields() {
        return Collections.emptyMap();
    }

    public long getHeaderFieldDate(String name, long Default) {
        String value = getHeaderField(name);
        try {
            Calendar cal = Calendar.getInstance();
            return Date.parse(value);
        } catch (Exception e) {
        }
        return Default;
    }

    public long getLastModified() {
        return getHeaderFieldDate("last-modified", 0);
    }

    public long getExpiration() {
        return getHeaderFieldDate("expires", 0);
    }

    /**
     * Clean up expired cache files
     */
    static void cleanExpiredCaches() {
        synchronized (caches) {
            Iterator<Map.Entry<String, CachedFile>> iterator = caches.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<String, CachedFile> entry = iterator.next();
                CachedFile cachedFile = entry.getValue();
                if (cachedFile.isExpired()) {
                    // Delete the cache file
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
                    iterator.remove();
                }
            }
        }
    }

    protected String getCacheFileName() {
        return CACHE_FILE_PREFIX + System.currentTimeMillis() + "_" + Math.random();
    }

    protected static class CachedFile {
        public final Object resource;
        public final long expireMs;

        public CachedFile(Object resource, long expireMs) {
            this.resource = resource;
            this.expireMs = expireMs;
        }

        public boolean isExpired() {
            return System.currentTimeMillis() > expireMs;
        }

        public void setRequestProperty(String key, String value) {

        }

        public void addRequestProperty(String key, String value) {

        }

        public String getRequestProperty(String key) {
            return null;
        }

        public Map<String, List<String>> getRequestProperties() {
            return Collections.emptyMap();
        }

    }
}
