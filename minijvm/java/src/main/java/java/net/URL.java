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
import org.mini.urlhandler.FileHandler;
import org.mini.urlhandler.HttpHandler;
import org.mini.urlhandler.JarHandler;
import org.mini.urlhandler.ResourceHandler;

public final class URL {
  private final URLStreamHandler handler;
  private String protocol;
  private String host;
  private int port;
  private String file;
  private String path;
  private String query;
  private String ref;

  public URL(String s) throws MalformedURLException {
    int colon = s.indexOf(':');
    int slash = s.indexOf('/');
    if (colon > 0 && (slash < 0 || colon < slash)) {
      handler = findHandler(s.substring(0, colon));
      handler.parseURL(this, s, colon + 1, s.length());
    } else {
      throw new MalformedURLException(s);
    }
  }

  public URL(URL context, String spec) throws MalformedURLException {
    this(resolve(context, spec));
  }

    public URL(String protocol, String host, String file)
            throws MalformedURLException {
        handler = findHandler(protocol);
        set(protocol, host, -1, file, null);
    }

  public String toString() {
    return handler.toExternalForm(this);
  }

  public String getProtocol() {
    return protocol;
  }

  public String getHost() {
    return host;
  }

  public int getPort() {
    return port;
  }

  public String getFile() {
    return file;
  }

  public String getRef() {
    return ref;
  }

  public String getPath() {
    return path;
  }

  public String getQuery() {
    return query;
  }

  public URLConnection openConnection() throws IOException {
    return handler.openConnection(this);
  }

  public InputStream openStream() throws IOException {
    return openConnection().getInputStream();
  }

  public Object getContent() throws IOException {
    return openStream();
  }

  private static String resolve(URL context, String spec)
    throws MalformedURLException
  {
    if (spec == null) {
      throw new NullPointerException();
    }

    if (hasScheme(spec)) {
      return spec;
    }

    if (context == null) {
      throw new MalformedURLException(spec);
    }

    if (spec.startsWith("#")) {
      String base = stripRef(context.toString());
      return base + spec;
    }

    if (spec.startsWith("//")) {
      return context.getProtocol() + ":" + spec;
    }

    if ("jar".equals(context.getProtocol())) {
      return resolveJar(context, spec);
    }

    String authority = buildAuthority(context);
    if (spec.startsWith("/")) {
      return context.getProtocol() + ":" + authority + normalizePath(spec);
    }

    String basePath = context.getPath();
    if (basePath == null || basePath.length() == 0) {
      basePath = "/";
    }
    return context.getProtocol() + ":" + authority
      + normalizePath(resolveRelativePath(basePath, spec));
  }

  private static String resolveJar(URL context, String spec)
    throws MalformedURLException
  {
    String file = context.getFile();
    int bang = file == null ? -1 : file.indexOf("!/");
    if (bang < 0) {
      throw new MalformedURLException(context.toString());
    }

    String jarRoot = file.substring(0, bang + 2);
    String entry = file.substring(bang + 1);
    if (spec.startsWith("/")) {
      return "jar:" + jarRoot + spec.substring(1);
    }
    return "jar:" + jarRoot
      + trimLeadingSlash(normalizePath(resolveRelativePath(entry, spec)));
  }

  private static String resolveRelativePath(String basePath, String spec) {
    int slash = basePath.lastIndexOf('/');
    if (slash >= 0) {
      return basePath.substring(0, slash + 1) + spec;
    } else {
      return spec;
    }
  }

  private static String normalizePath(String path) {
    if (path == null || path.length() == 0) {
      return path;
    }

    boolean absolute = path.charAt(0) == '/';
    String[] parts = split(path, '/');
    String[] stack = new String[parts.length];
    int size = 0;

    for (int i = 0; i < parts.length; i++) {
      String part = parts[i];
      if (part.length() == 0 || ".".equals(part)) {
        continue;
      }
      if ("..".equals(part)) {
        if (size > 0 && !"..".equals(stack[size - 1])) {
          size--;
        } else if (!absolute) {
          stack[size++] = part;
        }
      } else {
        stack[size++] = part;
      }
    }

    StringBuilder sb = new StringBuilder(path.length());
    if (absolute) {
      sb.append('/');
    }
    for (int i = 0; i < size; i++) {
      if (i > 0) {
        sb.append('/');
      }
      sb.append(stack[i]);
    }

    if (path.charAt(path.length() - 1) == '/' && sb.length() > 0
      && sb.charAt(sb.length() - 1) != '/')
    {
      sb.append('/');
    }

    if (sb.length() == 0 && absolute) {
      sb.append('/');
    }
    return sb.toString();
  }

  private static String[] split(String value, char ch) {
    int count = 1;
    for (int i = 0; i < value.length(); i++) {
      if (value.charAt(i) == ch) {
        count++;
      }
    }

    String[] parts = new String[count];
    int start = 0;
    int index = 0;
    for (int i = 0; i <= value.length(); i++) {
      if (i == value.length() || value.charAt(i) == ch) {
        parts[index++] = value.substring(start, i);
        start = i + 1;
      }
    }
    return parts;
  }

  private static boolean hasScheme(String spec) {
    int colon = spec.indexOf(':');
    int slash = spec.indexOf('/');
    return colon > 0 && (slash < 0 || colon < slash);
  }

  private static String buildAuthority(URL url) {
    if (url.getHost() == null) {
      return "";
    }

    StringBuilder sb = new StringBuilder();
    sb.append("//").append(url.getHost());
    if (url.getPort() >= 0) {
      sb.append(":").append(url.getPort());
    }
    return sb.toString();
  }

  private static String stripRef(String value) {
    int hash = value.indexOf('#');
    return hash >= 0 ? value.substring(0, hash) : value;
  }

  private static String trimLeadingSlash(String value) {
    return value != null && value.startsWith("/") ? value.substring(1) : value;
  }

  private static URLStreamHandler findHandler(String protocol)
    throws MalformedURLException
  {
    if ("http".equals(protocol) || "https".equals(protocol)) {
      return new HttpHandler();
    } else if ("avianvmresource".equals(protocol)) {
      return new ResourceHandler();
    } else if ("file".equals(protocol)) {
      return new FileHandler();
    } else if ("jar".equals(protocol)) {
      return new JarHandler();
    } else {
      throw new MalformedURLException("unknown protocol: " + protocol);
    }
  }

  public void set(String protocol, String host, int port, String file,
                  String ref)
  {
    this.protocol = protocol;
    this.host = host;
    this.port = port;
    this.file = file;
    this.ref = ref;

    int q = file == null ? -1 : file.lastIndexOf('?');
    if (q != -1) {
      this.query = file.substring(q + 1);
      this.path = file.substring(0, q);
    } else {
      this.path = file;
    }
  }
}
