/* Copyright (c) 2008-2015, Avian Contributors

   Permission to use, copy, modify, and/or distribute this software
   for any purpose with or without fee is hereby granted, provided
   that the above copyright notice and this permission notice appear
   in all copies.

   There is NO WARRANTY for this software.  See license.txt for
   details. */

package org.mini.urlhandler;

import java.io.*;
import java.net.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class JarHandler extends URLStreamHandler {
    URL fileUrl;

    protected URLConnection openConnection(URL url) {
        JarURLConnectionImpl con = new JarURLConnectionImpl(url);
        con.openConnection();
        return con;
    }


    protected void parseURL(URL url, String s, int start, int end)
            throws MalformedURLException {
        // skip "jar:"
        s = s.toString().substring(4);
        int index = s.indexOf("!/");
        if (index < 0) {
            throw new MalformedURLException();
        }

        URL file = new URL(s.substring(0, index));
        switch (file.getProtocol()) {
            case "jar":
            case "file":
            case "http":
            case "https":
                url.set("jar", null, -1, s, null);
                break;
            default:
                throw new RuntimeException
                        ("protocol " + file.getProtocol() + " not yet supported");
        }
    }

    private static class JarURLConnectionImpl extends JarURLConnection {
        private JarFile file;
        private JarEntry entry;

        public JarURLConnectionImpl(URL url) {
            super(url);

        }

        @Override
        public JarEntry getJarEntry() throws IOException {
            return entry;
        }

        public JarFile getJarFile() throws IOException {
            return file;
        }

        public int getContentLength() {
            try {
                connect();
                file.getInputStream(entry);
            } catch (IOException e) {
            }
            return (int) entry.getSize();
        }

        public InputStream getInputStream() throws IOException {
            connect();
            return file.getInputStream(entry);
        }

        public void connect() {
            if (file != null) return;

            String s = url.getFile();
            int index = s.indexOf("!/");
            if (index < 0) index = s.length();

            try {
                String tmps = s.substring(0, index);//like http://www.google.com/abc.jar
                URL fileUrl = new URL(tmps);
                File f;
                if (fileUrl.getProtocol().equals("http") || fileUrl.getProtocol().equals("https")) {
                    //find in caches
                    CachedFile cachedFile = caches.get(fileUrl.toString());
                    if (cachedFile != null && !cachedFile.isExpired()) {//found file in caches
                        f = new File((String) cachedFile.resource);
                    } else { //not found
                        URLConnection con = fileUrl.openConnection();
                        long expire = con.getExpiration();
                        InputStream is = (InputStream) fileUrl.getContent();

                        File tmpFile = new File(fileUrl.getFile());
                        f = File.createTempFile("", tmpFile.getName());
                        FileOutputStream fos = new FileOutputStream(f);
                        byte[] buf = new byte[1024];
                        while (true) {
                            int len = is.read(buf);
                            if (len == -1) {
                                break;
                            }
                            fos.write(buf, 0, len);
                        }

                        fos.close();
                        is.close();
                        if (expire <= 0) {
                            expire = System.currentTimeMillis() + 1000 * 60 * 60 * 24;
                        }
                        cachedFile = new CachedFile(f.getAbsolutePath(), expire);
                        caches.put(fileUrl.toString(), cachedFile);
                    }
                } else { // like   "/d:\\abc.jar"
                    f = new File(fileUrl.getFile());
                }
                this.file = new JarFile(f);
                this.entry = this.file.getEntry(s.substring(index + 2));
            } catch (MalformedURLException e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        }

        public void openConnection() {
        }
    }
}
