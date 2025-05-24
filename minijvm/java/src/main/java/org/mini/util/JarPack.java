package org.mini.util;

import org.mini.zip.Zip;

import java.io.File;
import java.io.FileFilter;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;

public class JarPack {

    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("usage: JarPack jarPath srcPath");
            return;
        }
        jar(args[0], args[1]);
    }

    public static void jar(String jarPath, String srcPath) {

        //delete old jar
        deleteTree(new File(jarPath));
        File home = new File(srcPath);
        String homePath = home.getAbsolutePath();

        //pack out/ files
        List<File> files = new ArrayList<>();
        File file = new File(srcPath);
        if (file.exists()) {
            getDirAndFiles(file.getAbsolutePath(), files);
        }

        for (int i = 0; i < files.size(); i++) {
            File f = files.get(i);
            String fn = f.getAbsolutePath();
            if (fn.length() == homePath.length()) continue;
            fn = fn.substring(homePath.length() + 1);
            byte[] bytes = null;
            if (f.isDirectory()) {
                fn += "/";
            } else {
                bytes = getFileBytes(f.getAbsolutePath());
            }
            if (bytes != null) {
                Zip.putEntry(jarPath, fn, bytes);
            }
        }

        System.out.println("jar file:" + jarPath);
    }

    public static byte[] getFileBytes(String path) {
        File cf = new File(path);
        if (cf.exists()) {
            RandomAccessFile fis = null;
            try {
                byte[] data = new byte[(int) cf.length()];
                fis = new RandomAccessFile(cf, "r");
                fis.read(data, 0, data.length);
                return data;
            } catch (Exception e) {
            } finally {
                try {
                    fis.close();
                } catch (Exception e) {
                }
            }
        }
        return null;
    }


    public static void listTreeFile(String path, List<File> filePaths, FileFilter filter) {
        File f = new File(path);
        if (!f.exists()) return;
        if (f.isFile()) {
            filePaths.add(f);
        } else {
            File[] files = f.listFiles(filter);
            if (files == null) return;
            for (int i = 0; i < files.length; i++) {
                File f1 = files[i];
                listTreeFile(f1.getAbsolutePath(), filePaths, filter);
            }
        }
    }

    public static void getDirAndFiles(String path, List<File> fileList) {
        listTreeFile(path, fileList, f -> true);
    }


    public static boolean deleteTree(File f) {
        if (f.isDirectory()) {
            File[] files = f.listFiles();
            for (File sf : files) {
                //System.out.println("file:" + sf.getAbsolutePath());
                deleteTree(sf);
            }
        }
        return f.delete();
        //System.out.println("delete " + f.getAbsolutePath() + " state:" + s);
    }
}
