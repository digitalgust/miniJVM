/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.mini.fs;

/**
 *
 * @author gust
 */
public class FileSystemWin extends FileSystemImpl {

    @Override
    public String normalize(String path) {
        path = path.replace('/', getSeparator());
        path = super.normalize(path);
        return path;
    }

    @Override
    public boolean isAbsolute(String path) {
        if (path.charAt(0) == getSeparator()) {
            return true;
        }
        if (path.length() >= 2 && path.charAt(1) == ':') {
            return true;
        }
        return false;
    }

    public String getRegexParentTag() {
        return "\\\\[^\\\\]+\\\\\\.\\.";
    }

    @Override
    public int prefixLength(String path) {
        if (path.indexOf(':') > 0) {
            return 2;
        }
        return 0;
    }
}
