package org.mini.reflect;

import org.mini.reflect.vm.RefNative;
import org.mini.zip.Zip;

import java.io.File;
import java.io.RandomAccessFile;
import java.net.URL;

public class Launcher {

    static ExtClassLoader extcl = new ExtClassLoader(null);
    static ClassLoader systemClassLoader = new AppClassLoader(extcl);

    /**
     * the console application main class loader
     */
    static public class AppClassLoader extends ClassLoader {
        String[] paths;

        public AppClassLoader(ClassLoader p) {
            super(p);
            String s = System.getProperty("java.class.path");
            paths = s.split(File.pathSeparator);
        }

        @Override
        protected Class<?> findClass(String name) throws ClassNotFoundException {

            // 加载D盘根目录下指定类名的class
            String classname = name.replace('.', File.separatorChar) + ".class";
            byte[] classData = Launcher.getFileData(classname, paths);
            if (classData == null) {
                throw new ClassNotFoundException();
            } else {
                return defineClass(name, classData, 0, classData.length);
            }
        }

        protected URL findResource(String path) {
            URL url = Launcher.getFileUrl(path, paths);
            return url;
        }
    }

    /**
     * the jvm runtime class loader
     */
    static public class ExtClassLoader extends ClassLoader {
        String[] paths;

        public ExtClassLoader(ClassLoader parent) {
            String s = System.getProperty("sun.boot.class.path");
            paths = s.split(File.pathSeparator);
        }

        protected Class findClass(String name) throws ClassNotFoundException {
            return RefNative.getBootstrapClassByName(name);
        }

        protected URL findResource(String path) {
            URL url = Launcher.getFileUrl(path, paths);
            return url;
        }
    }

    //=======================================================================

    public static ExtClassLoader getExtClassLoader() {
        return extcl;
    }

    public static ClassLoader getSystemClassLoader() {
        return systemClassLoader;
    }

    public static String getBootstrapClassPath() {
        return System.getProperty("sun.boot.class.path");
    }

    /**
     * this method is used for vm
     *
     * @param name
     * @param classLoader
     * @return
     * @throws ClassNotFoundException
     */
    static Class<?> loadClass(String name, ClassLoader classLoader) throws ClassNotFoundException {
        return (classLoader == null ? getSystemClassLoader() : classLoader).loadClass(name);
    }

    /**
     * find a file bytes from classes paths
     *
     * @param name
     * @param paths
     * @return
     * @throws ClassNotFoundException
     */
    static public byte[] getFileData(String name, String[] paths) throws ClassNotFoundException {

        // 加载D盘根目录下指定类名的class
        byte[] classData = null;
        for (String s : paths) {
            if (s != null && s.length() > 0) {
                File f = new File(s);
                if (f.isFile()) {
                    classData = Zip.getEntry(s, name);
                    if (classData != null) {
                        break;
                    }
                } else {
                    File cf = new File(s + "/" + name);
                    if (cf.exists()) {
                        try {
                            classData = new byte[(int) cf.length()];
                            RandomAccessFile fis = new RandomAccessFile(cf, "r");
                            fis.read(classData, 0, classData.length);
                        } catch (Exception e) {
                            classData = null;
                        }
                    }
                }
            }
        }
        return classData;
    }

    /**
     * find a file url from paths ,the paths may contains jar and directory
     *
     * @param sourceName
     * @param paths
     * @return
     */
    static public URL getFileUrl(String sourceName, String[] paths) {
        URL url = null;
        while (sourceName.startsWith("/")) sourceName = sourceName.substring(1);
        for (String s : paths) {
            if (s != null && s.length() > 0) {
                File f = new File(s);
                if (f.isFile()) {
                    try {
                        String[] files = Zip.listFiles0(f.getAbsolutePath());
                        for (int i = 0, imax = files.length; i < imax; i++) {
                            String fn = files[i];
                            if (fn != null && fn.equals(sourceName)) {
                                String us = "jar:file:" + f.getAbsolutePath() + "!/" + sourceName;
                                url = new URL(us);
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    File cf = new File(s + "/" + sourceName);
                    if (cf.exists()) {
                        try {
                            String us = "file:" + cf.getAbsolutePath();
                            System.out.println(us);
                            url = new URL(us);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
        return url;
    }
}
