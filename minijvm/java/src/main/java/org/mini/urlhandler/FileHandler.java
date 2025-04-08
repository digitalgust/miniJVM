/* Copyright (c) 2008-2015, Avian Contributors

   Permission to use, copy, modify, and/or distribute this software
   for any purpose with or without fee is hereby granted, provided
   that the above copyright notice and this permission notice appear
   in all copies.

   There is NO WARRANTY for this software.  See license.txt for
   details. */

package org.mini.urlhandler;

import java.net.URL;
import java.net.URLStreamHandler;
import java.net.URLConnection;
import java.io.IOException;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

public class FileHandler extends URLStreamHandler {
  protected URLConnection openConnection(URL url) {
    return new FileURLConnection(url);
  }

  private static class FileURLConnection extends URLConnection {
    public FileURLConnection(URL url) {
      super(url);
    }

    public int getContentLength() {
      return (int) new File(url.getFile()).length();
    }

    public InputStream getInputStream() throws IOException {
      return new FileInputStream(new File(url.getPath()));
    }

    public void connect() {
      // ignore
    }
  }
}
