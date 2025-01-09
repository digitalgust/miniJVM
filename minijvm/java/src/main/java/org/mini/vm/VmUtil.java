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

    /**
     * thread create handler
     * 用于把线程创建事件通知给应用
     */
    static class ThreadHandler implements ThreadLifeHandler {
        ThreadLifeHandler[] handlers;

        void addHandler(ThreadLifeHandler handler) {
            if (handlers == null) {
                handlers = new ThreadLifeHandler[]{handler};
            } else {
                ThreadLifeHandler[] nhandlers = new ThreadLifeHandler[handlers.length + 1];
                System.arraycopy(handlers, 0, nhandlers, 0, handlers.length);
                nhandlers[handlers.length] = handler;
                handlers = nhandlers;
            }
        }

        void removeHandler(ThreadLifeHandler handler) {
            for (int i = 0; i < handlers.length; i++) {
                if (handlers[i] == handler) {
                    ThreadLifeHandler[] nhandlers = new ThreadLifeHandler[handlers.length - 1];
                    System.arraycopy(handlers, 0, nhandlers, 0, i);
                    System.arraycopy(handlers, i + 1, nhandlers, i, handlers.length - i - 1);
                    handlers = nhandlers;
                    break;
                }
            }
        }

        @Override
        public synchronized void threadCreated(Thread t) {
            //System.out.println("threadCreated:" + t.getName());
            for (ThreadLifeHandler handler : handlers) {
                try {
                    handler.threadCreated(t);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }


        @Override
        public synchronized void threadDestroy(Thread t) {
            //System.out.println("threadCreated:" + t.getName());
            for (ThreadLifeHandler handler : handlers) {
                try {
                    handler.threadDestroy(t);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    static ThreadHandler threadHandler = new ThreadHandler();

    public synchronized static void addThreadLifeHandler(ThreadLifeHandler r) {
        threadHandler.addHandler(r);
        Thread.setThreadLifeHandler(threadHandler);
    }

    public synchronized static void removeThreadLifeHandler(ThreadLifeHandler r) {
        threadHandler.removeHandler(r);
    }
}
