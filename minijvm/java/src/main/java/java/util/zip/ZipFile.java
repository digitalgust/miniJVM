package java.util.zip;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.Vector;

import org.mini.zip.Zip;

public class ZipFile {

    protected String zipFileName;

    //打开供阅读的 ZIP 文件，由指定的 File 对象给出。 
    public ZipFile(File file) throws IOException {
        zipFileName = file.getAbsolutePath();
    }
    //打开新的 ZipFile 以使用指定模式从指定 File 对象读取。 

    public ZipFile(File file, int mode) throws IOException {
        zipFileName = file.getAbsolutePath();
    }

    public ZipFile(String name) throws IOException {
        zipFileName = name;
    }

    //关闭 ZIP 文件。 
    public void close() throws IOException {

    }
    //返回 ZIP 文件条目的枚举。 

    public Enumeration<? extends ZipEntry> entries() {
        Vector<ZipEntry> files;
        String[] fns = Zip.listFiles(zipFileName);
        files = new Vector();
        for (String s : fns) {
            ZipEntry entry = new ZipEntry(s);
            files.add(entry);
        }
        return files.elements();
    }
    //确保不再引用此 ZIP 文件时调用它的 close 方法。 

    protected void finalize() {

    }
    //返回指定名称的 ZIP 文件条目；如果未找到，则返回 null。 

    public ZipEntry getEntry(String entryName) {
        if (entryName == null) {
            return null;
        }
        while (entryName.startsWith("/")) entryName = entryName.substring(1);
        boolean exist = Zip.isEntryExist(zipFileName, entryName);
        if (exist) {
            ZipEntry entry = new ZipEntry(entryName);
            return entry;
        }
        return null;
    }
    //返回输入流以读取指定 ZIP 文件条目的内容。 

    public InputStream getInputStream(ZipEntry entry) throws IOException {
        entry.load(zipFileName);
        return new ByteArrayInputStream(entry.contents);
    }
    //返回 ZIP 文件的路径名。 

    public String getName() {
        return zipFileName;
    }
    //返回 ZIP 文件中的条目数。 

    public int size() {
        return Zip.fileCount(zipFileName);
    }

}
