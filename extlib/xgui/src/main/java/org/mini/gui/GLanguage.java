/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.mini.gui;

import org.mini.apploader.AppManager;
import org.mini.apploader.GApplication;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Gust
 */
public class GLanguage {

    static public final int ID_NO_DEF = -1;
    static public final int ID_ENG = 0;
    static public final int ID_CHN = 1;
    static public final int ID_CHT = 2;
    static public final int ID_KOR = 3;
    static public final int ID_FRA = 4;
    static public final int ID_SPA = 5;
    static public final int ID_ITA = 6;
    static public final int ID_JPA = 7;
    static public final int ID_GER = 8;
    static public final int ID_RUS = 9;

    static Map<String, String[]> lang = new HashMap();
    static Map<String, Map<String, String[]>> app2ext = new HashMap();

    static int cur_lang = ID_ENG;

    /**
     * api range language strings
     */
    static {
        initGuiSrings();
    }

    static private void initGuiSrings() {
        addStringInner("SeleAll", new String[]{"SeleAll", "全选", "全選"});
        addStringInner("Copy", new String[]{"Copy", "复制", "復制"});
        addStringInner("Select", new String[]{"Select", "选择", "選擇"});
        addStringInner("Paste", new String[]{"Paste", "粘贴", "粘貼"});
        addStringInner("Cut", new String[]{"Cut", "剪切", "剪切"});
        addStringInner("Perform", new String[]{"Perform", "完成", "完成"});
        addStringInner("Cancel", new String[]{"Cancel", "取消", "取消"});
        addStringInner("Ok", new String[]{"Ok", "确定", "確認"});
        addStringInner("Save to album", new String[]{"Save to album", "存入相册", "存入相冊"});
        addStringInner("Message", new String[]{"Message", "消息", "訊息"});
    }

    static public void setCurLang(int langType) {
        cur_lang = langType;
    }

    static public int getCurLang() {
        return cur_lang;
    }

    static public String getString(String appId, String key) {
        return getString(appId, key, cur_lang);
    }

    static private void addStringInner(String key, String[] values) {
        if (key != null && values != null) {
            lang.put(key, values);
        }
    }

    static public void addString(String appId, String key, String[] values) {
        if (appId != null && key != null && values != null) {
            Map<String, String[]> ext = app2ext.get(appId);
            if (ext == null) {
                ext = new HashMap();
                app2ext.put(appId, ext);
            }
            ext.put(key, values);
        }
    }

    static public String getString(String appId, String key, int langType) {
        String[] ss = null;
        Map<String, String[]> ext = app2ext.get(appId);
        if (ext != null) {
            ss = ext.get(key);
        }
        if (ss == null) {
            ss = lang.get(key);
        }
        if (ss == null || langType < 0 || langType >= ss.length) {
            return key;
        }
        return ss[langType];
    }

    static public void clear(String appId) {
        Map<String, String[]> ext = app2ext.get(appId);
        if (ext != null) {
            app2ext.remove(appId);
        }
    }
}
