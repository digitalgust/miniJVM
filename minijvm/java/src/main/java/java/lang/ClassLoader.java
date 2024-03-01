/* Copyright (c) 2008-2015, Avian Contributors

   Permission to use, copy, modify, and/or distribute this software
   for any purpose with or without fee is hereby granted, provided
   that the above copyright notice and this permission notice appear
   in all copies.

   There is NO WARRANTY for this software.  See license.txt for
   details. */
package java.lang;

import sun.misc.Launcher;
import org.mini.vm.RefNative;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.ProtectionDomain;
import java.util.*;


/**
 * Classloader 本JVM实现了 和ORACLE类似的两亲委托类加载机制，
 * 并由Thread.setContextClassLoader(),实现基于Classloader的应用加载机制，
 * 如Tomcat的webapp， 加载时，所有class都由某一个Classloader应用实例加载，应用退出时，
 * 此classloader加载的所有类都会卸载。
 * 由于先前类库设计过程中的一个BUG，父加载器（SystemClassloader）加载的类A的有一静态容器A.c，
 * 子加载器(StandalongGuiAppClassLoader)加载的类B(1)的对象实例B(1)ins存入A.c，
 * 同时StandalongGuiAppClassLoader加载的类B(2)也向A.c存入了B(2)ins，
 * 获取A.c时，结果包含了B(1)ins，导致不同类加载器的类和实例混淆
 * 为解决这类问题，则A中不能有静态容器A.c,改变设计
 *
 */
public abstract class ClassLoader {


    private final ClassLoader parent;
    private Map<String, Package> packages;
    private Vector holderClasses = new Vector();//hold classes avoid  gc

    protected ClassLoader(ClassLoader parent) {
        if (parent == null) {
            if (!getClass().equals(Launcher.ExtClassLoader.class)) {
                this.parent = getSystemClassLoader();
            } else {
                this.parent = null;
            }
        } else {
            this.parent = parent;
        }
        RefNative.initNativeClassLoader(this, parent);
    }

    protected ClassLoader() {
        this(null);
    }

    /**
     * this will release all loaded class by this classloader
     *
     * @throws Throwable
     */
    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        RefNative.destroyNativeClassLoader(this);
        System.out.println(this + " finalized");
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
        return Launcher.getSystemClassLoader();
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

    protected Class defineClass(String name, byte[] b, int offset, int length, ProtectionDomain domain) {
        return defineClass(name, b, offset, length);
    }

    protected Class findClass(String name) throws ClassNotFoundException {
        return null;
    }

    protected final Class findLoadedClass(String name) {
        return RefNative.findLoadedClass0(this, name);
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
            if (url == null) {
                url = RefNative.findResource0(this, path);//for j2c find resource
            }
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

}
