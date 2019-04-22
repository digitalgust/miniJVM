/* Copyright (c) 2008-2015, Avian Contributors

   Permission to use, copy, modify, and/or distribute this software
   for any purpose with or without fee is hereby granted, provided
   that the above copyright notice and this permission notice appear
   in all copies.

   There is NO WARRANTY for this software.  See license.txt for
   details. */
package java.lang;

import java.io.InputStream;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Map;
import java.util.HashMap;
import java.security.ProtectionDomain;
import org.mini.reflect.SystemClassLoader;
import org.mini.reflect.vm.RefNative;

public abstract class ClassLoader {

    static ClassLoader systemClassLoader;

    private final ClassLoader parent;
    private Map<String, Package> packages;

    protected ClassLoader(ClassLoader parent) {
        if (parent == null) {
            if (!getClass().equals(SystemClassLoader.class)) {
                this.parent = getSystemClassLoader();
            } else {
                this.parent = null;
            }
        } else {
            this.parent = parent;
        }
    }

    protected ClassLoader() {
        if (!getClass().equals(SystemClassLoader.class)) {
                this.parent = getSystemClassLoader();
            } else {
                this.parent = null;
            }
    }

    private Map<String, Package> packages() {
        if (packages == null) {
            packages = new HashMap();
        }
        return packages;
    }

    protected Package getPackage(String name) {
        Package p;
        synchronized (this) {
            p = packages().get(name);
        }

        if (parent != null) {
            p = parent.getPackage(name);
        } else {
            // todo: load attributes from JAR manifest if available
            p = definePackage(name, null, null, null, null, null, null, null);
        }

        if (p != null) {
            synchronized (this) {
                Package p2 = packages().get(name);
                if (p2 != null) {
                    p = p2;
                } else {
                    packages().put(name, p);
                }
            }
        }

        return p;
    }

    protected Package[] getPackages() {
        synchronized (this) {
            return packages().values().toArray(new Package[packages().size()]);
        }
    }

    protected Package definePackage(String name,
            String specificationTitle,
            String specificationVersion,
            String specificationVendor,
            String implementationTitle,
            String implementationVersion,
            String implementationVendor,
            URL sealBase) {
        Package p = new Package(name, implementationTitle, implementationVersion,
                implementationVendor, specificationTitle, specificationVersion,
                specificationVendor, sealBase, this);

        synchronized (this) {
            packages().put(name, p);
            return p;
        }
    }

    public static ClassLoader getSystemClassLoader() {
        if (systemClassLoader == null) {
            systemClassLoader = new SystemClassLoader(null);
        }
        return systemClassLoader;
    }

    protected Class defineClass(String name, byte[] b, int offset, int length) {
        if (b == null) {
            throw new NullPointerException();
        }

        if (offset < 0 || offset > length || offset + length > b.length) {
            throw new IndexOutOfBoundsException();
        }

        return RefNative.defineClass(this, name, b, offset, length);
    }

    protected Class defineClass(String name, byte[] b, int offset, int length,
            ProtectionDomain domain) {
        return defineClass(name, b, offset, length);
    }

    protected Class findClass(String name) throws ClassNotFoundException {
        return parent.findClass(name);
    }

    protected final Class findLoadedClass(String name) {
        try {
            Class c = findClass(name);
            if (c.getClassLoader() == this) {
                return c;
            }
        } catch (ClassNotFoundException ex) {
        }
        return null;
    }

    public Class loadClass(String name) throws ClassNotFoundException {
        return loadClass(name, false);
    }

    protected Class loadClass(String name, boolean resolve)
            throws ClassNotFoundException {
        Class c = findLoadedClass(name);
        if (c == null) {
            if (parent != null) {
                try {
                    c = parent.loadClass(name);
                } catch (ClassNotFoundException ok) {
                }
            }

            if (c == null) {
                c = findClass(name);
            }
        }

        if (resolve) {
            resolveClass(c);
        }

        return c;
    }

    protected void resolveClass(Class c) {
        //donothing
    }

    public final ClassLoader getParent() {
        return parent;
    }

    protected URL findResource(String path) {
        return null;
    }

    protected Enumeration<URL> findResources(String name) throws IOException {
        return Collections.enumeration(new ArrayList<URL>(0));
    }

    public URL getResource(String path) {
        URL url = null;
        if (parent != null) {
            url = parent.getResource(path);
        }

        if (url == null) {
            url = findResource(path);
        }

        return url;
    }

    public InputStream getResourceAsStream(String path) {
        URL url = getResource(path);
        try {
            return (url == null ? null : url.openStream());
        } catch (IOException e) {
            return null;
        }
    }

    public static URL getSystemResource(String path) {
        return getSystemClassLoader().getResource(path);
    }

    public static InputStream getSystemResourceAsStream(String path) {
        return getSystemClassLoader().getResourceAsStream(path);
    }

    public static Enumeration<URL> getSystemResources(String name) throws IOException {
        return getSystemClassLoader().getResources(name);
    }

    public Enumeration<URL> getResources(String name)
            throws IOException {
        Collection<URL> resources = collectResources(name);
        return Collections.enumeration(resources);
    }

    private Collection<URL> collectResources(String name) {
        Collection<URL> urls = parent != null ? parent.collectResources(name) : new ArrayList<URL>(5);
        URL url = findResource(name);
        if (url != null) {
            urls.add(url);
        }
        return urls;
    }

    protected String findLibrary(String name) {
        return null;
    }

    static native Class getCaller();

    static native void load(String name, Class caller, boolean mapName);
}
