/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.mini.gui;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Gust
 */
public class GLanguage {

    static public final int ID_ENG = 0;
    static public final int ID_CHN = 1;
    static public final int ID_CHT = 2;

    static Map<String, String[]> lang = new HashMap();
    static Map<String, String[]> ext = new HashMap();

    static int cur_lang = ID_ENG;

    static {
        addStringInner("SeleAll", new String[]{"SeleAll", "全选", "全選"});
        addStringInner("Copy", new String[]{"Copy", "复制", "復制"});
        addStringInner("Select", new String[]{"Select", "选择", "選擇"});
        addStringInner("Paste", new String[]{"Paste", "粘贴", "粘貼"});
        addStringInner("Cut", new String[]{"Cut", "剪切", "剪切"});
        addStringInner("Perform", new String[]{"Perform", "完成", "完成"});
        addString("Cancel", new String[]{"Cancel", "取消", "取消"});
    }

    static public void setCurLang(int langType) {
        cur_lang = langType;
    }

    static public int getCurLang() {
        return cur_lang;
    }

    static public String getString(String key) {
        return getString(key, cur_lang);
    }

    static private void addStringInner(String key, String[] values) {
        if (key != null && values != null) {
            lang.put(key, values);
        }
    }

    static public void addString(String key, String[] values) {
        if (key != null && values != null) {
            ext.put(key, values);
        }
    }

    static public String getString(String key, int langType) {
        String[] ss = lang.get(key);
        if (ss == null) {
            ss = ext.get(key);
        }
        if (ss == null || langType >= ss.length) {
            return key;
        }
        return ss[langType];
    }

}
