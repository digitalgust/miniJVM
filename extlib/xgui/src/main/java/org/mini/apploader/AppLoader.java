/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.mini.apploader;

import org.mini.gui.callback.GCallBack;
import org.mini.gui.GLanguage;
import org.mini.gui.style.GStyle;
import org.mini.gui.GToolkit;
import org.mini.vm.ThreadLifeHandler;
import org.mini.vm.VmUtil;
import org.mini.zip.Zip;

import java.io.*;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;

/**
 * @author gust
 */
public class AppLoader {

    static final String BASE_INFO_FILE = "/res/base.properties";
    static final String APP_INFO_FILE = "/appinfo.properties";
    static final String APP_LIST_FILE = "/applist.properties";
    static final String APP_CONFIG = "config.txt";
    static final String APP_DIR = "/apps/";
    static final String APP_FILE_EXT = ".jar";
    static final String APP_DATA_DIR = "/appdata/";
    static final String TMP_DIR = "/tmp/";
    static final String EXAMPLE_APP_FILE = "ExApp.jar";
    static final String KEY_BOOT = "boot";
    static final String KEY_DOWNLOADURL = "downloadurl";
    static final String KEY_LANGUAGE = "language";
    static final String KEY_GUISTYLE = "guistyle";
    static final String KEY_HOMEICON_X = "homeiconx";
    static final String KEY_HOMEICON_Y = "homeicony";
    static final String KEY_TOKEN = "TOKEN";
    static Properties appinfo = new Properties();
    static Properties applist = new Properties();
    static Properties baseinfo = new Properties();


    static boolean inited = false;

    public static boolean isInited() {
        return inited;
    }

    public static void cb_init() {
        if (inited) return;
        inited = true;

        loadJarProp(BASE_INFO_FILE, baseinfo);
        //System.out.println("start loader");
        //
        checkDir();
        loadProp(APP_INFO_FILE, appinfo);
        loadProp(APP_LIST_FILE, applist);

        for (String s : getAppList()) {
            try {
                if (!isJarExists(s)) {
                    removeApp(s);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        saveProp(APP_LIST_FILE, applist);

        //设置 创建线程的handler
        VmUtil.addThreadLifeHandler(new ThreadLifeHandler() {
            @Override
            public void threadCreated(Thread thread) {

                //从调用栈中查找创建者
                Throwable callStack = new Throwable();
                GApplication creator = findCreator(callStack);
                if (creator != null) {
                    //System.out.println(creator.getClass().getClassLoader() + " CREATED+++++ " + thread);
                    creator.addThread(thread);
                }
            }

            @Override
            public void threadDestroy(Thread thread) {
                //谁创建的线程
                GApplication creator = findCreator(thread);
                if (creator != null) {
                    //System.out.println(creator.getClass().getClassLoader() + " DESTROYED----- " + thread);
                    creator.removeThread(thread);
                }
            }

            private GApplication findCreator(Throwable callStack) {
                for (GApplication a : AppManager.getInstance().getRunningApps()) {
                    ClassLoader appClassLoader = a.getClass().getClassLoader();
                    for (StackTraceElement e : callStack.getStackTrace()) {
                        String cname = e.getClassName();
                        try {
                            Class c = Class.forName(cname, true, appClassLoader);
                            if (c != null && c.getClassLoader() == appClassLoader) {//调用栈中如果有和app的classloader相同的类，则说明此线程为这个app创建的
                                return a;
                            }
                        } catch (Exception ex) {
                            //ex.printStackTrace();
                        }
                    }
                }
                return null;
            }

            private GApplication findCreator(Thread thread) {
                for (GApplication a : AppManager.getInstance().getRunningApps()) {
                    if (a.threads.contains(thread)) {
                        return a;
                    }
                }
                return null;
            }
        });

        copyExApp();
        String bootApp = appinfo.getProperty(KEY_BOOT);
        if (bootApp == null || !isJarExists(bootApp)) {
            setBootApp(EXAMPLE_APP_FILE);
            bootApp = EXAMPLE_APP_FILE;
        }
        bootApp = null;//"block the setup bootapp auto boot";
        runApp(bootApp);
    }

    static void checkDir() {
        File f = new File(GCallBack.getInstance().getAppSaveRoot() + APP_DIR);
        if (!f.exists()) {
            f.mkdirs();
        }

        reloadAppList();

        f = new File(GCallBack.getInstance().getAppSaveRoot() + APP_DATA_DIR);
        if (!f.exists()) {
            f.mkdirs();
        }
        //clear tmp files
        f = new File(GCallBack.getInstance().getAppSaveRoot() + TMP_DIR);
        deleteTree(f);
        //check tmp dir
        f = new File(GCallBack.getInstance().getAppSaveRoot() + TMP_DIR);
        if (!f.exists()) {
            f.mkdirs();
        }
    }

    static public void reloadAppList() {
        File f = new File(GCallBack.getInstance().getAppSaveRoot() + APP_DIR);

        //reload all jar
        applist.clear();
        File[] jars = f.listFiles(pathname -> pathname.getName().endsWith(APP_FILE_EXT));
        for (File file : jars) {
            applist.put(file.getName(), "");
        }
    }

    static public String getTmpDirPath() {
        return GCallBack.getInstance().getAppSaveRoot() + TMP_DIR;
    }

    static public String getAppJarPath(String jarName) {
        String s = GCallBack.getInstance().getAppSaveRoot() + APP_DIR + jarName;
        File f = new File(s);
        return f.getAbsolutePath();
    }

    static public String getAppDataPath(String jarName) {
        String s = GCallBack.getInstance().getAppSaveRoot() + APP_DATA_DIR + jarName + "/";
        File f = new File(s);
        if (!f.exists()) {
            f.mkdirs();
        }
        return s;
    }

    static void copyExApp() {
        String srcPath = GCallBack.getInstance().getAppResRoot() + "/resfiles/" + EXAMPLE_APP_FILE;
        //if (!applist.contains(EXAMPLE_APP_FILE) || !isJarExists(srcPath)) {
        addApp(EXAMPLE_APP_FILE, srcPath);
        //System.out.println("copy exapp");
        //}
    }

    public static void loadJarProp(String filePath, Properties prop) {
        try {
            byte[] b = GToolkit.readFileFromJar(filePath);
            ByteArrayInputStream bais = new ByteArrayInputStream(b);
            prop.load(bais);
            //System.out.println(fname + " size: " + prop.size());
            bais.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private static void loadProp(String fname, Properties prop) {
        String s = GCallBack.getInstance().getAppSaveRoot() + fname;
        loadPropFile(s, prop);
    }

    public static void loadPropFile(String path, Properties prop) {
        try {
            File f = new File(path);
            if (f.exists()) {
                FileInputStream fis = new FileInputStream(f);
                prop.load(fis);
                //System.out.println(fname + " size: " + prop.size());
                fis.close();
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private static void saveProp(String name, Properties prop) {
        String s = GCallBack.getInstance().getAppSaveRoot() + name;
        savePropFile(s, prop);
    }

    public static void savePropFile(String path, Properties prop) {
        try {
            File f = new File(path);

            FileOutputStream fos = new FileOutputStream(f);
            prop.store(fos, "");
            fos.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public static String getBootApp() {
        String defaultApp = appinfo.getProperty(KEY_BOOT);
        return defaultApp;
    }

    public static void setBootApp(String jarName) {
        appinfo.put(KEY_BOOT, jarName);
        saveProp(APP_INFO_FILE, appinfo);
    }

    public static String getDownloadUrl() {
        String defaultApp = appinfo.getProperty(KEY_DOWNLOADURL);
        return defaultApp;
    }

    public static void setDownloadUrl(String downloadUrl) {
        appinfo.put(KEY_DOWNLOADURL, downloadUrl);
        saveProp(APP_INFO_FILE, appinfo);
    }

    public static int getDefaultLang() {
        String langstr = appinfo.getProperty(KEY_LANGUAGE);

        String sysLang = System.getProperty("user.language");
        System.out.println("[INFO]sysLang:" + sysLang);
        int lang = GLanguage.getIdByShortName(sysLang);
        try {
            lang = Integer.parseInt(langstr.trim());
        } catch (Exception e) {
        }
        return lang;
    }

    public static void setDefaultLang(int lang) {
        appinfo.put(KEY_LANGUAGE, "" + lang);
        saveProp(APP_INFO_FILE, appinfo);
    }

    public static int getGuiStyle() {
        String langstr = appinfo.getProperty(KEY_GUISTYLE);
        int lang = 0;// 0 bright , 1 dark
        try {
            lang = Integer.parseInt(langstr.trim());
        } catch (Exception e) {
        }
        return lang;
    }

    public static void setGuiStyle(int style) {
        appinfo.put(KEY_GUISTYLE, "" + style);
        saveProp(APP_INFO_FILE, appinfo);
    }


    public static int getHomeIconX() {
        String langstr = appinfo.getProperty(KEY_HOMEICON_X);
        int v = 0;//
        try {
            v = Integer.parseInt(langstr.trim());
        } catch (Exception e) {
        }
        return v;
    }

    public static void setHomeIconX(int x) {
        appinfo.put(KEY_HOMEICON_X, "" + x);
        saveProp(APP_INFO_FILE, appinfo);
    }

    public static int getHomeIconY() {
        String langstr = appinfo.getProperty(KEY_HOMEICON_Y);
        int v = 0;//
        try {
            v = Integer.parseInt(langstr.trim());
        } catch (Exception e) {
        }
        return v;
    }

    public static void setHomeIconY(int y) {
        appinfo.put(KEY_HOMEICON_Y, "" + y);
        saveProp(APP_INFO_FILE, appinfo);
    }

    public static String getProperty(String key) {
        String s = appinfo.getProperty(key);
        return s == null ? "" : s;
    }

    public static void setProperty(String key, String val) {
        appinfo.setProperty(key, val);
        saveProp(APP_INFO_FILE, appinfo);
    }

    public static boolean isJarExists(String jarName) {
        File f = new File(getAppJarPath(jarName));
        return f.exists();
    }

    public static Class getApplicationClass(String jarName) {
        try {
            String className = getAppConfig(jarName, "app");
            if (className != null && className.length() > 0) {
                //System.out.println("className:" + className);

                String extractedJarsPath = getAppDataPath(jarName) + "/lib";
                File[] files = new File(extractedJarsPath).listFiles(new FileFilter() {
                    @Override
                    public boolean accept(File file) {
                        return file.getName().endsWith(".jar");
                    }
                });
                int filesCnt = (files != null ? files.length : 0);
                String[] paths = new String[filesCnt + 1];
                if (filesCnt > 0) {
                    for (int i = 0; i < filesCnt; i++) {
                        paths[i] = files[i].getAbsolutePath();
                    }
                }
                paths[paths.length - 1] = getAppJarPath(jarName);

                StandalongGuiAppClassLoader sgacl = new StandalongGuiAppClassLoader(paths, ClassLoader.getSystemClassLoader());
                Thread.currentThread().setContextClassLoader(sgacl);
                Class c = sgacl.loadClass(className);

                return c;
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

    public static byte[] getApplicationIcon(String jarName) {
        String iconName = getAppConfig(jarName, "icon");
        String jarFullPath = getAppJarPath(jarName);
        return Zip.getEntry(jarFullPath, iconName);
    }

    public static String getApplicationDesc(String jarName) {
        String desc = getAppConfig(jarName, "desc");
        if (desc == null) {
            desc = AppManager.getInstance().getString("No description.");
        } else {
            desc = desc.replace("\\n", "\n");
        }
        return desc;
    }

    public static String getApplicationName(String jarName) {
        String name = getAppConfig(jarName, "name");
        if (name == null) {
            name = jarName;
        }
        return name;
    }

    public static String getApplicationVersion(String jarName) {
        String v = getAppConfig(jarName, "version");
        if (v == null) {
            v = "";
        }
        return v;
    }

    public static String getApplicationOrientation(String jarName) {
        String v = getAppConfig(jarName, "orientation");
        if (v == null) {
            v = "v";
        }
        return v.toLowerCase();
    }

    public static String getApplicationUpgradeurl(String jarName) {
        String url = getAppConfig(jarName, "upgradeurl");
        if (url == null) {
            url = "";
        }
        return url;
    }

    public static long getApplicationFileSize(String jarName) {
        String path = getAppJarPath(jarName);
        File f = new File(path);
        if (f.exists()) {
            return f.length();
        }
        return -1;
    }

    public static long getApplicationFileDate(String jarName) {
        String path = getAppJarPath(jarName);
        File f = new File(path);
        if (f.exists()) {
            return f.lastModified();
        }
        return -1;
    }

    static String getAppConfig(String jarName, String key) {
        try {
            String jarFullPath = getAppJarPath(jarName);
            File f = new File(jarFullPath);
            if (f.exists()) {
                //System.out.println("jar path:" + jarFullPath + "  " + key);
                byte[] b = Zip.getEntry(jarFullPath, APP_CONFIG);
                //System.out.println("b=" + b);
                if (b != null) {

                    String s = new String(b, "utf-8");
                    //System.out.println("file contents :" + s);
                    s = s.replace("\r", "\n");
                    String[] ss = s.split("\n");
                    for (String line : ss) {
                        int pos = line.indexOf("=");
                        if (pos > 0) {
                            String k = line.substring(0, pos).trim();
                            String v = line.substring(pos + 1).trim();
                            if (k.equalsIgnoreCase(key)) {
                                return v;
                            }
                        }
                    }
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

    static public List<String> getAppList() {
        List<String> list = new ArrayList();
        for (Enumeration e = applist.keys(); e.hasMoreElements(); ) {
            try {
                String s = (String) e.nextElement();
                list.add(s.trim());
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        return list;
    }

    static public void putAppList(List<String> list) {
        //clear old
        applist.clear();
        for (String s : list) {
            applist.put(s, "");
        }
    }

    public static GApplication runApp(String jarName) {
        GApplication app = null;
        try {
            if (jarName != null) {
                extractFatJar(jarName); //extract dependence lib
                GStyle oldStyle = GToolkit.getStyle();
                Class c = getApplicationClass(jarName);
                if (c != null) {
                    app = (GApplication) c.newInstance();
                    app.init(jarName);
                    app.setOldStyle(oldStyle);
                    GCallBack.getInstance().setApplication(app);
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            if (app == null) {
                app = AppManager.getInstance();
                app.setSaveRoot(getAppDataPath("Home"));
                AppManager.getInstance().active();
                //GForm.addMessage(GLanguage.getString(AppManager.STR_OPEN_APP_FAIL) + ": " + jarName);
            }
        }
        return app;
    }

    public static boolean addApp(String jarName, String srcJarFullPath) {
        try {
            //copy file
            //System.out.println("copy from: " + jarPath + "  to :" + getAppJarPath(jarName));
            FileInputStream fis = new FileInputStream(srcJarFullPath);
            FileOutputStream fos = new FileOutputStream(getAppJarPath(jarName));
            byte[] b = new byte[1024];
            int read;
            while ((read = fis.read(b)) != -1) {
                fos.write(b, 0, read);
            }
            fis.close();
            fos.close();

            reloadAppList();

            saveProp(APP_INFO_FILE, appinfo);
            saveProp(APP_LIST_FILE, applist);
            return true;
        } catch (Exception exception) {
            //exception.printStackTrace();
            System.out.println("[INFO] " + exception.getMessage());
        }
        return false;
    }

    public static boolean addApp(String jarName, byte[] jarData) {
        try {
            if (jarName != null && jarData != null && jarData.length > 0) {
                //copy file
                //System.out.println("add from: " + jarData.length + "  to :" + getAppJarPath(jarName));
                FileOutputStream fos = new FileOutputStream(getAppJarPath(jarName));
                fos.write(jarData);
                fos.close();

                reloadAppList();

                saveProp(APP_INFO_FILE, appinfo);
                saveProp(APP_LIST_FILE, applist);
                extractFatJar(jarName);
                return true;
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }
        return false;
    }

    static private void extractFatJar(String jarName) {
        try {
            String fatPath = getAppJarPath(jarName);
            String[] fns = Zip.listFiles(fatPath);
            String libdir = getAppDataPath(jarName) + "/lib";
            File libfile = new File(libdir);
            libfile.mkdirs();
            for (int i = 0; i < fns.length; i++) {
                String name = fns[i];
                if (name.startsWith("lib/") && name.endsWith(".jar")) {
                    byte[] bytes = Zip.getEntry(fatPath, name);
                    String exjar = libdir + "/" + name.substring(name.lastIndexOf('/'));
                    FileOutputStream fos = new FileOutputStream(exjar);
                    fos.write(bytes);
                    fos.close();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void removeApp(String jarName) {
        applist.remove(jarName);
        //delete jar
        String jarFullPath = getAppJarPath(jarName);
        File f = new File(jarFullPath);
        if (f.exists()) {
            f.delete();
        }
        //delete data
        f = new File(getAppDataPath(jarName));
        deleteTree(f);

        saveProp(APP_INFO_FILE, appinfo);
        saveProp(APP_LIST_FILE, applist);
    }


    public static boolean deleteTree(File f) {
        if (f.isDirectory()) {
            File[] files = f.listFiles();
            for (File sf : files) {
                //System.out.println("file:" + sf.getAbsolutePath());
                deleteTree(sf);
            }
        }
        boolean s = f.delete();
        //System.out.println("delete " + f.getAbsolutePath() + " state:" + s);
        return s;
    }

    public static String getBaseInfo(String key) {
        String s = baseinfo.getProperty(key);
        return s == null ? "" : s;
    }

}
