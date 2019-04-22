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
public class InnerFileStat {

    /* FIFO */
    static public final int _S_IFIFO = 0x1000;
    /* Character */
    static public final int _S_IFCHR = 0x2000;
    /* Directory */
    static public final int _S_IFDIR = 0x4000;
    /* Regular */
    static public final int _S_IFREG = 0x8000;

    static public final int _S_IFMT = 0xF000;//文件类型遮罩

    static public final int S_IRUSR = 0x00400;// 文件所有者具可读取权限 
    static public final int S_IWUSR = 0x00200;// 文件所有者具可写入权限 
    static public final int S_IXUSR = 0x00100;// 文件所有者具可执行权限 
    static public final int S_IRGRP = 0x00040;// 用户组具可读取权限 
    static public final int S_IWGRP = 0x00020;// 用户组具可写入权限 
    static public final int S_IXGRP = 0x00010;// 用户组具可执行权限 
    static public final int S_IROTH = 0x00004;// 其他用户具可读取权限 
    static public final int S_IWOTH = 0x00002;// 其他用户具可写入权限 
    static public final int S_IXOTH = 0x00001;// 其他用户具可执行权限上述的文件类型在 POSIX 中定义了检查这些类型的宏定义     

    public boolean exists;

    public int st_dev;       //文件的设备编号
    public short st_ino;       //节点
    public short st_mode;      //文件的类型和存取的权限
    public short st_nlink;     //连到该文件的硬连接数目，刚建立的文件值为1
    public short st_uid;       //用户ID
    public short st_gid;       //组ID
    public short st_rdev;      //(设备类型)若此文件为设备文件，则为其设备编号
    public long st_size;      //文件字节数(文件大小)
    public long st_atime;     //最后一次访问时间
    public long st_mtime;     //最后一次修改时间
    public long st_ctime;     //最后一次改变时间(指属性)

    public boolean isFile() {
        return (st_mode & _S_IFMT) == _S_IFREG;
    }

    public boolean isDirectory() {
        return (st_mode & _S_IFMT) == _S_IFDIR;
    }

    public boolean canRead() {
        return (st_mode & S_IROTH) != 0;
    }

    public boolean canWrite() {
        return (st_mode & S_IWOTH) != 0;
    }
}
