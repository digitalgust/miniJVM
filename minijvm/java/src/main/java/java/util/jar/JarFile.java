/* Copyright (c) 2008-2015, Avian Contributors

   Permission to use, copy, modify, and/or distribute this software
   for any purpose with or without fee is hereby granted, provided
   that the above copyright notice and this permission notice appear
   in all copies.

   There is NO WARRANTY for this software.  See license.txt for
   details. */

package java.util.jar;

import org.mini.zip.Zip;

import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Vector;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class JarFile extends ZipFile {
    public JarFile(String name) throws IOException {
        super(name);
    }

    public JarFile(File file) throws IOException {
        super(file);
    }

    public JarEntry getEntry(String entryName) {
        ZipEntry ze = super.getEntry(entryName);
        if (ze == null) return null;
        return new JarEntry(ze.getName());
    }

    public JarEntry getJarEntry(String name) {
        return (JarEntry)getEntry(name);
    }

    public Enumeration<JarEntry> entries() {
        Vector<JarEntry> files;
        String[] fns = Zip.listFiles(zipFileName);
        files = new Vector();
        for (String s : fns) {
            JarEntry entry = new JarEntry(s);
            files.add(entry);
        }
        return files.elements();
    }
}
