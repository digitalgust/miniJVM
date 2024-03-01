package sun.misc;

import org.mini.vm.RefNative;
import org.mini.vm.VmUtil;

import java.io.File;
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
            //for (String ps : paths) System.out.println("AppCL:" + ps);
        }

        @Override
        protected Class<?> findClass(String name) throws ClassNotFoundException {

            // 加载D盘根目录下指定类名的class
            String classname = name.replace('.', '/') + ".class";
            byte[] classData = VmUtil.getFileData(classname, paths);
            if (classData == null) {
                throw new ClassNotFoundException(name);
            } else {
                return defineClass(name, classData, 0, classData.length);
            }
        }

        protected URL findResource(String path) {
            URL url = VmUtil.getFileUrl(path, paths);
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
            //for (String ps : paths) System.out.println("ExtCL:" + ps);
        }

        protected Class findClass(String name) throws ClassNotFoundException {
            return RefNative.getBootstrapClassByName(name);
        }

        protected URL findResource(String path) {
            URL url = VmUtil.getFileUrl(path, paths);
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
        name = name.replace('/', '.');
        Class<?> c = (classLoader == null ? getSystemClassLoader() : classLoader).loadClass(name);
        return c;
    }

}
