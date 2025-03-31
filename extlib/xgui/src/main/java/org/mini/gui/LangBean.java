package org.mini.gui;

import java.util.HashMap;
import java.util.Map;

public class LangBean {
    private Map<String, String[]> lang = new HashMap();

    public LangBean() {
    }

    public Map<String, String[]> getLang() {
        return lang;
    }

    public void setLang(Map<String, String[]> lang) {
        this.lang = lang;
    }
}

