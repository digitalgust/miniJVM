/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.mini.gui;

import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author Gust
 */
public class GLanguage {

    static public final int ID_ENG = 0;
    static public final int ID_CHN = 1;
    static public final int ID_CHT = 2;

    static String[][] lang_str = {//
        {"Select All", "全选", "全選"},//
        {"Copy", "复制", "復制"},//
        {"Select", "选择", "選擇"},//
        {"Paste", "粘贴", "粘貼"},//
        {"Cut", "剪切", "剪切"},//
    };

    static Map<String, String[]> lang;

    static int cur_lang = ID_ENG;

    static public void setCurLang(int langType) {
        cur_lang = langType;
    }

    static public String getString(String key) {
        return getString(key, cur_lang);
    }

    static public String getString(String key, int langType) {
        if (lang == null) {
            lang = new HashMap();
            for (String[] ss : lang_str) {
                lang.put(ss[ID_ENG], ss);
            }
        }
        String[] ss = lang.get(key);
        if (ss == null) {
            return "NA";
        }
        return ss[langType];
    }

}
