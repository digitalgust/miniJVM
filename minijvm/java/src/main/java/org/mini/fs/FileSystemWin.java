/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.mini.fs;

import java.io.File;

/**
 * @author gust
 */
public class FileSystemWin extends FileSystemImpl {

    @Override
    public String normalize(String path) {
        path = path.replace('/', getSeparator());
        path = super.normalize(path);
        if (path.endsWith(":")) path += File.separator;
        return path;
    }

    @Override
    public boolean isAbsolute(String path) {
        if (path.length() == 0) {
            return false;
        }
        if (path.charAt(0) == getSeparator()) {
            return true;
        }
        if (path.length() >= 2 && path.charAt(1) == ':') {
            return true;
        }
        return false;
    }

    public String getRegexParentTag() {
        return "\\\\[^\\\\]+\\\\\\.\\.\\\\";
    }

    @Override
    public int prefixLength(String path) {
        if (path.indexOf(':') > 0) {
            return 2;
        }
        return 0;
    }

    @Override
    public File[] listRoots() {
        String str = InnerFile.listWinDrivers();
        if (str == null) {
            return super.listRoots();
        } else {
            String[] strs = str.split(" ");
            File[] files = new File[strs.length];
            for (int i = 0; i < files.length; i++) {
                files[i] = new File(strs[i]);
            }
            return files;
        }
    }

    protected String getFullPath(String path) {
        String p = super.getFullPath(path);
        if (p.endsWith(":")) p += File.separator;
        return p;
    }
}
