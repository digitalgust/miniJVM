/* Copyright (c) 2008-2015, Avian Contributors

   Permission to use, copy, modify, and/or distribute this software
   for any purpose with or without fee is hereby granted, provided
   that the above copyright notice and this permission notice appear
   in all copies.

   There is NO WARRANTY for this software.  See license.txt for
   details. */

package org.mini.urlhandler;

import java.net.URL;
import java.net.MalformedURLException;
import java.net.URLStreamHandler;
import java.net.JarURLConnection;
import java.net.URLConnection;
import java.io.IOException;
import java.io.InputStream;
import java.util.jar.JarFile;
import java.util.jar.JarEntry;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class JarHandler extends URLStreamHandler {
  protected URLConnection openConnection(URL url) {
    return new MyJarURLConnection(url);
  }

  protected void parseURL(URL url, String s, int start, int end)
    throws MalformedURLException
  {
    // skip "jar:"
    s = s.toString().substring(4);
    int index = s.indexOf("!/");
    if (index < 0) {
      throw new MalformedURLException();
    }

    URL file = new URL(s.substring(0, index));
    if (! "file".equals(file.getProtocol())) {
      throw new RuntimeException
        ("protocol " + file.getProtocol() + " not yet supported");
    }

    url.set("jar", null, -1, s, null);
  }

  private static class MyJarURLConnection extends JarURLConnection {
    private final ZipFile file;
    private final ZipEntry entry;

    public MyJarURLConnection(URL url) {
      super(url);

      String s = url.getFile();
      int index = s.indexOf("!/");

      try {
        this.file = new ZipFile(new URL(s.substring(0, index)).getFile());
        this.entry = this.file.getEntry(s.substring(index + 2));
      } catch (MalformedURLException e) {
        throw new RuntimeException(e);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }

    public ZipFile getJarFile() throws IOException {
      return file;
    }

    public int getContentLength() {
      return (int)entry.getSize();
    }

    public InputStream getInputStream() throws IOException {
      return file.getInputStream(entry);
    }

    public void connect() {
      // ignore
    }
  }
}
