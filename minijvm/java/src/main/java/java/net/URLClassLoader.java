/* Copyright (c) 2008-2015, Avian Contributors

   Permission to use, copy, modify, and/or distribute this software
   for any purpose with or without fee is hereby granted, provided
   that the above copyright notice and this permission notice appear
   in all copies.

   There is NO WARRANTY for this software.  See license.txt for
   details. */

package java.net;

import org.mini.vm.VmUtil;


public class URLClassLoader extends ClassLoader {

    URL[] urls;
    private final String[] paths;

    public URLClassLoader(URL[] urls) {
        this(urls, Thread.currentThread().getContextClassLoader());
    }


    public URLClassLoader(URL[] urls, ClassLoader parent) {
        super(parent);
        this.urls = urls;

        paths = new String[urls.length];
        for (int i = 0; i < urls.length; i++) {
            if (!urls[i].getProtocol().equals("file")) {
                throw new UnsupportedOperationException(urls[i].getProtocol());
            }
            this.paths[i] = urls[i].getFile();
        }
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {

        // 加载D盘根目录下指定类名的class
        String classname = name.replace('.', '/') + ".class";
        byte[] classData = VmUtil.getFileData(classname, paths);
        if (classData == null) {
            throw new ClassNotFoundException();
        } else {
            return defineClass(name, classData, 0, classData.length);
        }
    }

    protected URL findResource(String path) {
        URL url = VmUtil.getFileUrl(path, paths);
        return url;
    }

}