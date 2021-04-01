/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ebsee.compiler;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

/**
 *
 * @author Gust
 */
public class DyClassLoader extends ClassLoader {

    ArrayList<String> classpath = new ArrayList<String>();

    /**
     * 
     */
    public DyClassLoader() {
        super(DyClassLoader.class.getClassLoader());
    }

    /**
     * 增加类路径
     * @param s
     */
    public void addClassPath(String s) {
        classpath.add(s);
        File f=new File(s);
        find(f.getAbsolutePath(),s);
    }

    private void find(String root,String path) {
        try {
            File dir = new File(path);

            if (dir.exists()) {
                File[] files = dir.listFiles();

                //
                if (files != null) {
                    for (File f : files) {
                        if (f.isFile() && f.getName().endsWith(".class")) {
                            String className = f.getAbsolutePath();
                            className=className.substring(root.length()+1,className.lastIndexOf(".class"));
                            loadFromCustomRepository(f,className);
                        } else {
                            find(root,f.getAbsolutePath());
                        }
                    }
                }
            }
        } catch (Exception ex) {
            System.out.println("clear jhtml output dir error ");
            ex.printStackTrace();
        }
    }
    /**
     * 从自有路径下加载类
     * @param className
     * @return
     */
    public Class loadFromCustomRepository(File file,String className) {
        try {
            Class cls = this.findLoadedClass(className);
            if (cls != null) {
                return cls;
            }
            byte[] classBytes = null;

                //replace '.' in the class name with File.separatorChar & append .class to the name
                String classFileName = className.replace('.', File.separatorChar);
                try {

                    if (file.exists()) {
                        InputStream is = new FileInputStream(file);
                        /**把文件读到字节文件*/
                        classBytes = new byte[is.available()];
                        int read=0;
                        for (; read < classBytes.length; ) {
                            read+=is.read(classBytes);
                        }
                        is.close();

                    }
                } catch (IOException ex) {
                    System.out.println("IOException raised while reading class file data");
                    ex.printStackTrace();
                    return null;
                }

            className=className.replace('/', '.');
            className=className.replace('\\', '.');
            return this.defineClass(className, classBytes, 0, classBytes.length); //加载类
        } catch (Exception ex) {
            System.out.println("ClassNotFoundException load class : "+className);
            ex.printStackTrace();
        }
        return null;

    }

    /**
     * 
     * @param className
     * @return
     */
    public Class loadFromSysAndCustomRepository(String className) {
        /**取环境变量*/
        String classPath = System.getProperty("java.class.path");
        List classRepository = new ArrayList();
        /**取得该路径下的所有文件夹 */
        if ((classPath != null) && !(classPath.equals(""))) {
            StringTokenizer tokenizer = new StringTokenizer(classPath,
                    File.pathSeparator);
            while (tokenizer.hasMoreTokens()) {
                classRepository.add(tokenizer.nextToken());
            }
        }
        Iterator dirs = classRepository.iterator();
        byte[] classBytes = null;
        /**在类路径上查找该名称的类是否存在，如果不存在继续查找*/
        while (dirs.hasNext()) {
            String dir = (String) dirs.next();
            //replace '.' in the class name with File.separatorChar & append .class to the name
            String classFileName = className.replace('.', File.separatorChar);
            classFileName += ".class";
            try {
                File file = new File(dir + File.separatorChar + classFileName);
                if (file.exists()) {
                    InputStream is = new FileInputStream(file);
                    /**把文件读到字节文件*/
                    classBytes = new byte[is.available()];
                    is.read(classBytes);
                    break;
                }
            } catch (IOException ex) {
                System.out.println("IOException raised while reading class file data");
                ex.printStackTrace();
                return null;
            }
        }
        return this.defineClass(className, classBytes, 0, classBytes.length);//加载类

    }
}

