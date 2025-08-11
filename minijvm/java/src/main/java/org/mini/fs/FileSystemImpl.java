/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.mini.fs;

import org.mini.net.SocketNative;
import org.mini.util.SysLog;

import java.io.File;
import java.io.IOException;

/**
 * @author gust
 */
abstract public class FileSystemImpl extends org.mini.fs.FileSystem {

    static String PARENT_DIR = "..";
    static String CUR_DIR = ".";

    @Override
    public String getDefaultParent() {
        String p = InnerFile.getcwd();
        if (p == null) return "";
        else return p;
    }

    abstract boolean isAbsolute(String path);

    @Override
    public boolean isAbsolute(File f) {
        String path = f.getPath();
        return isAbsolute(path);
    }

    abstract String getRegexParentTag();

    private String removeParentTag(String path) {

        if (path.endsWith(getSeparator() + PARENT_DIR)) {  // "/tmp/.."  ->  "/tmp/../"
            path = path + getSeparator();
        }

        //  "/tmp/../a/b/../c"  ->  "/a/c"
        while (path.indexOf(getSeparator() + PARENT_DIR) >= 0) {
            path = path.replaceAll(getRegexParentTag(), getSeparator() + "");
        }

        return path;
    }

    @Override
    public String normalize(String path) {
        String ds = "" + getSeparator() + getSeparator();   //remove all "//" to "/"
        String ss = "" + getSeparator();
        while (path.indexOf(ds) >= 0) {
            path = path.replace(ds, ss);
        }

        if (path.endsWith(getSeparator() + CUR_DIR)) { //  "/tmp/abc/."   ->  "/tmp/abc/./"
            path = path + getSeparator();
        }
        // https://github.com/digitalgust/miniJVM/issues/31
        //   "/tmp/abc/./"   ->  "/tmp/abc"
        while (path.indexOf(getSeparator() + CUR_DIR + getSeparator()) >= 0) {
            path = path.replace(getSeparator() + CUR_DIR + getSeparator(), getSeparator() + "");
        }

//        path = path.replace(getSeparator() + PARENT_DIR + getSeparator(), "\uffff\uffff\uffff");
//        path = path.replace(getSeparator() + CUR_DIR + getSeparator(), getSeparator() + "");  //remove all "./" to ""
//        path = path.replace("\uffff\uffff\uffff", getSeparator() + PARENT_DIR + getSeparator());
        if (path.length() > 1 && path.lastIndexOf(getSeparator()) == path.length() - 1) {//remove last char if it's '/'
            path = path.substring(0, path.length() - 1);
        }
        return path;
    }

    protected String getFullPath(String path) {
        String parent = getDefaultParent();
        if (!isAbsolute(path)) {
            path = parent + getSeparator() + path;    //   replace   "/tmp/abc/../a.txt" to "/tmp/a.txt"
        }
        // MUST remove current dir first   /./   ,like "/a/./../b"  -> "/a/../b"
        path = normalize(path);

        //remove parent tag /../
        path = removeParentTag(path);
        return path;
    }

    @Override
    public String resolve(File f) {
        return getFullPath(f.getPath());
    }

    @Override
    public String resolve(String parent, String child) {
        return getFullPath(parent + "/" + child);
    }

    @Override
    public String canonicalize(String path) throws IOException {
        return getFullPath(path);
    }

    @Override
    public int getBooleanAttributes(File f) {
        int att = 0;
        InnerFileStat ifa = new InnerFileStat();
        InnerFile.loadFS(SocketNative.toCStyle(f.getPath()), ifa);
        att |= ifa.isDirectory() ? BA_DIRECTORY : 0;
        att |= ifa.exists ? BA_EXISTS : 0;
        att |= ifa.isFile() ? BA_REGULAR : 0;
        return att;
    }

    @Override
    public boolean checkAccess(File f, boolean write) {
        InnerFileStat ifa = new InnerFileStat();
        InnerFile.loadFS(SocketNative.toCStyle(f.getPath()), ifa);
        return write ? ifa.canWrite() : ifa.canRead();
    }

    @Override
    public long getLastModifiedTime(File f) {
        InnerFileStat ifa = new InnerFileStat();
        InnerFile.loadFS(SocketNative.toCStyle(f.getPath()), ifa);
        return ifa.st_mtime * 1000;
    }

    @Override
    public long getLength(File f) {
        InnerFileStat ifa = new InnerFileStat();
        InnerFile.loadFS(SocketNative.toCStyle(f.getPath()), ifa);
        return ifa.st_size;
    }

    @Override
    public boolean createFileExclusively(String pathname) throws IOException {
        InnerFileStat ifa = new InnerFileStat();
        InnerFile.loadFS(SocketNative.toCStyle(pathname), ifa);
        if (ifa.exists) {
            throw new IOException("file exists.");
        }
        long fd = InnerFile.openFile(SocketNative.toCStyle(pathname), SocketNative.toCStyle("w"));
        if (fd != 0) {
            InnerFile.closeFile(fd);
            return true;
        }
        return false;
    }

    @Override
    public boolean delete(File f) {
        if (f.getPath() != null) {
            java.lang.SecurityManager sm = System.getSecurityManager();
            if (sm != null) {
                sm.checkDelete(f.getPath());
            }
        }
        return InnerFile.delete0(SocketNative.toCStyle(f.getPath())) == 0;
    }

    @Override
    public boolean deleteOnExit(File f) {
        SysLog.warn("deleteOnExit not supported");
        return false;
    }

    @Override
    public String[] list(File f) {
        return InnerFile.listDir(SocketNative.toCStyle(f.getPath()));
    }

    @Override
    public boolean createDirectory(File f) {
        String ap = f.getAbsolutePath();
        byte[] bp1 = SocketNative.toCStyle(ap);
        return InnerFile.mkdir0(bp1) == 0;
    }

    @Override
    public boolean rename(File f1, File f2) {

        return InnerFile.rename0(
                SocketNative.toCStyle(f1.getPath()),
                SocketNative.toCStyle(f2.getPath())
        ) == 0;
    }

    @Override
    public boolean setLastModifiedTime(File f, long time) {
        SysLog.warn("setLastModifiedTime not supported");
        return false;
    }

    @Override
    public boolean setReadOnly(File f) {
        SysLog.warn("setReadOnly not supported");
        return false;
    }

    @Override
    public File[] listRoots() {
        File f = new File("/");
        String[] strs = list(f);
        if (strs != null) {
            File[] files = new File[strs.length];
            for (int i = 0, iLen = files.length; i < iLen; i++) {
                files[i] = new File(strs[i]);
            }
            return files;
        }
        return new File[0];
    }

    @Override
    public int compare(File f1, File f2) {
        return f1.getAbsolutePath().compareTo(f2.getAbsolutePath());
    }

    @Override
    public int hashCode(File f) {
        return f.getAbsolutePath().hashCode();
    }

    @Override
    public File getTempDir() {
        String s = InnerFile.getTmpDir();
        if (s != null) {
            File f = new File(s);
            return f;
        }
        return null;
    }


    @Override
    public char getSeparator() {
        return System.getProperty("file.separator").charAt(0);
    }

    @Override
    public char getPathSeparator() {
        return System.getProperty("path.separator").charAt(0);
    }

}
