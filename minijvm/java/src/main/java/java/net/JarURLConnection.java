/* Copyright (c) 2008-2015, Avian Contributors

   Permission to use, copy, modify, and/or distribute this software
   for any purpose with or without fee is hereby granted, provided
   that the above copyright notice and this permission notice appear
   in all copies.

   There is NO WARRANTY for this software.  See license.txt for
   details. */

package java.net;

import java.io.IOException;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public abstract class JarURLConnection extends URLConnection {
  public JarURLConnection(URL url) {
    super(url);
  }

  /**
   * Returns the URL for the Jar file for this connection.
   *
   * @return the URL for the Jar file for this connection.
   */
  public URL getJarFileURL() {
    return url;
  }


  public abstract JarEntry getJarEntry() throws IOException ;

  public abstract JarFile getJarFile() throws IOException;
}
