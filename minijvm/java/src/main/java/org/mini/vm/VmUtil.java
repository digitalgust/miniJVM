package org.mini.vm;

import org.mini.zip.Zip;

import java.io.File;
import java.io.RandomAccessFile;
import java.net.URL;

public class VmUtil {
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

        while (sourceName.startsWith("/")) sourceName = sourceName.substring(1);
        for (String s : paths) {
            if (s != null && s.length() > 0) {
                File f = new File(s);
                if (f.isFile()) {
                    try {
                        boolean exist = Zip.isEntryExist(f.getAbsolutePath(), sourceName);
                        if (exist) {
                            String us = "jar:file:///" + f.getAbsolutePath() + "!/" + sourceName;
                            URL url = new URL(us);
                            return url;
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    File cf = new File(s + "/" + sourceName);
                    if (cf.exists()) {
                        try {
                            String us = "file:///" + cf.getAbsolutePath();
                            //System.out.println(us);
                            return new URL(us);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
        return null;
    }
}
