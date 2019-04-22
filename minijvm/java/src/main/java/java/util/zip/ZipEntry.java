/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package java.util.zip;

import org.mini.zip.Zip;

/**
 *
 * @author Gust
 */
public class ZipEntry {

    String name;
    byte[] contents;
    String comment;
    boolean isDir;
    //使用指定名称创建新的 ZIP 条目。 

    public ZipEntry(String name) {
        this.name = name;
    }
    //使用从指定 ZIP 条目获取的字段创建新的 ZIP 条目。 

    public ZipEntry(ZipEntry e) {
        this.name = e.name;
        this.contents = e.contents;
        this.isDir = e.isDir;
    }

    void load(String zipFile) {
        contents = Zip.getEntry(zipFile, name);
    }
    //
    //返回条目的注释字符串；如果没有，则返回 null。 

    public String getComment() {
        return comment;
    }
    //返回压缩条目数据的大小；如果未知，则返回 -1。 

    public long getCompressedSize() {
        if (contents != null) {
            return contents.length;
        }
        return 0;
    }
    //返回未压缩条目数据的 CRC-32 校验和；如果未知，则返回 -1。 

    public long getCrc() {
        return -1;
    }
    //返回条目的额外字段数据；如果没有，则返回 null。 

    public byte[] getExtra() {
        return null;
    }
    //返回条目的压缩方法；如果未指定，则返回 -1。 

    public int getMethod() {
        return -1;
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
        return 0;
    }
    //返回条目的修改时间；如果未指定，则返回 -1。 

    public long getTime() {
        return System.currentTimeMillis();
    }

    //如果为目录条目，则返回 true。 
    public boolean isDirectory() {
        return isDir;
    }
    //为条目设置可选的注释字符串。 

    public void setComment(String comment) {
        this.comment = comment;
    }
    //设置压缩条目数据的大小。 

    public void setCompressedSize(long csize) {

    }
    //设置未压缩条目数据的 CRC-32 校验和。 

    public void setCrc(long crc) {

    }
    //为条目设置可选的额外字段数据。 

    public void setExtra(byte[] extra) {

    }
    //设置条目的压缩方法。 

    public void setMethod(int method) {

    }
    //设置条目数据的未压缩大小。 

    public void setSize(long size) {

    }
    //设置条目的修改时间。 

    public void setTime(long time) {

    }
    //返回 ZIP 条目的字符串表示形式。 

    public String toString() {
        return name + ":" + super.toString();
    }

}
