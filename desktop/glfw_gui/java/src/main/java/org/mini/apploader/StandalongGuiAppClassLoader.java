package org.mini.apploader;

import org.mini.reflect.Launcher;

import java.io.File;
import java.net.URL;

public class StandalongGuiAppClassLoader extends ClassLoader {
    String[] jarPath = new String[1];

    public StandalongGuiAppClassLoader(String jarPath, ClassLoader parentCL) {
        super(parentCL);
        this.jarPath[0] = jarPath;
    }


    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {

        // 加载D盘根目录下指定类名的class
        String classname = name.replace('.', '/') + ".class";
        byte[] classData = Launcher.getFileData(classname, jarPath);
        if (classData == null) {
            throw new ClassNotFoundException();
        } else {
            return defineClass(name, classData, 0, classData.length);
        }
    }

    protected URL findResource(String path) {
        URL url = Launcher.getFileUrl(path, jarPath);
        return url;
    }
}
