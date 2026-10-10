/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package java.util.zip;

import org.mini.zip.Zip;

/**
 * @author Gust
 */
public class ZipEntry {

    /**
     * Compression method for uncompressed (STORED) entries.
     */
    public static final int STORED = 0;

    /**
     * Compression method for compressed (DEFLATED) entries.
     */
    public static final int DEFLATED = 8;

    String name;
    byte[] contents;
    String comment;
    private int method = -1;
    private long size = -1, compressedSize = -1, crc = -1, time = -1;
    private byte[] extra;
    //使用指定名称创建新的 ZIP 条目。

    public ZipEntry(String name) {
        if (name == null) throw new NullPointerException();
        if (name.length() > 65535) throw new IllegalArgumentException("entry name too long");
        this.name = name;
    }
    //使用从指定 ZIP 条目获取的字段创建新的 ZIP 条目。 

    public ZipEntry(ZipEntry e) {
        this.name = e.name;
        this.contents = e.contents;
        this.comment = e.comment;
        this.method = e.method;
        this.size = e.size;
        this.compressedSize = e.compressedSize;
        this.crc = e.crc;
        this.time = e.time;
        this.extra = e.extra == null ? null : e.extra.clone();
    }

    void load(String zipFile) {
        if (contents == null) {
            contents = Zip.getEntry(zipFile, name);
        }
    }
    //
    //返回条目的注释字符串；如果没有，则返回 null。 

    public String getComment() {
        return comment;
    }
    //返回压缩条目数据的大小；如果未知，则返回 -1。 

    public long getCompressedSize() {
        return compressedSize;
    }
    //返回未压缩条目数据的 CRC-32 校验和；如果未知，则返回 -1。 

    public long getCrc() {
        return crc;
    }
    //返回条目的额外字段数据；如果没有，则返回 null。 

    public byte[] getExtra() {
        return extra;
    }
    //返回条目的压缩方法；如果未指定，则返回 -1。 

    public int getMethod() {
        return method;
    }
    //返回条目名称。 

    public String getName() {
        return name;
    }
    //返回条目数据的未压缩大小；如果未知，则返回 -1。 

    public long getSize() {
        if (contents != null) {
            return contents.length;
        }
        return size;
    }
    //返回条目的修改时间；如果未指定，则返回 -1。 

    public long getTime() {
        return time;
    }

    //如果为目录条目，则返回 true。 
    public boolean isDirectory() {
        return name.endsWith("/");
    }
    //为条目设置可选的注释字符串。 

    public void setComment(String comment) {
        this.comment = comment;
    }
    //设置压缩条目数据的大小。 

    public void setCompressedSize(long csize) {
        compressedSize = csize;
    }
    //设置未压缩条目数据的 CRC-32 校验和。 

    public void setCrc(long crc) {
        if (crc < 0 || crc > 0xffffffffL) throw new IllegalArgumentException("invalid CRC-32");
        this.crc = crc;
    }
    //为条目设置可选的额外字段数据。 

    public void setExtra(byte[] extra) {
        if (extra != null && extra.length > 65535) throw new IllegalArgumentException("extra too long");
        this.extra = extra;
    }
    //设置条目的压缩方法。 

    public void setMethod(int method) {
        if (method != STORED && method != DEFLATED) throw new IllegalArgumentException("invalid method");
        this.method = method;
    }
    //设置条目数据的未压缩大小。 

    public void setSize(long size) {
        if (size < 0) throw new IllegalArgumentException("invalid size");
        this.size = size;
    }
    //设置条目的修改时间。 

    public void setTime(long time) {
        this.time = time;
    }
    //返回 ZIP 条目的字符串表示形式。 

    public String toString() {
        return name + ":" + super.toString();
    }

}
