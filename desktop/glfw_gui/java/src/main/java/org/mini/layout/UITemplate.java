package org.mini.layout;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * UITemplate used for construct a UI with variables.
 * <p>
 * String uistr = "<frame title='{TITLE}'></frame>";
 * UITemplate uit = new UITemplate(uistr);
 * uit.setVar("TITLE", "This is title");
 * String result = uit.parse();
 * //then result would be : <frame title='This is title'></frame>
 */
public class UITemplate {

    private static ThreadLocal<HashMap> vars = new ThreadLocal() {

        @Override
        protected HashMap initialValue() {
            return new HashMap();
        }
    };
    /**
     * 分割字符串
     */
    String[] parts;
    /**
     * 变量集合
     */
    private String[] variable;

    public static void main(String[] args) {
        String uistr = "<frame title='{TITLE}'></frame>";
        UITemplate uit = new UITemplate(uistr);
        uit.setVar("TITLE", "This is title");
        String result = uit.parse();
        System.out.println(result);
        //then result would be : <frame title='This is title'></frame>
        String s;
        s = "b";
        switch (s) {
            case "a":
                s += "2";
                break;
            case "b":
                s += "3";
                break;
        }
        System.out.println(s);
        System.out.println((int)Long.parseLong("801010ff",16));
        System.out.println(Integer.parseUnsignedInt("801010ff",16));
    }

    /**
     * @param data
     */
    public UITemplate(String data) {
        load(data);
    }

    /**
     * 载入模板数据
     *
     * @param data
     */
    void load(String data) {
        Pattern p = Pattern.compile("\\{[^\\{^\\}]+\\}");
        Matcher m = p.matcher(data);
        ArrayList<String> p_array = new ArrayList();
        ArrayList<String> v_array = new ArrayList();
        int index = 0;
        // 使用循环将句子里所有的匹配字符串找出
        while (m.find()) {
            p_array.add(data.substring(index, m.start()));
            v_array.add(data.substring(m.start() + 1, m.end() - 1));
            index = m.end();
        }
        p_array.add(data.substring(index, data.length()));
        parts = new String[p_array.size()];
        variable = new String[v_array.size()];
        p_array.toArray(parts);
        v_array.toArray(variable);
    }


    /**
     * 解析模板，生成xml string
     *
     * @param vars
     * @return
     */
    public String parse(HashMap vars) {
        StringBuilder sb = new StringBuilder();
        sb.append(parts[0]);
        for (int i = 0; i < getVariable().length; i++) {
            Object s = getVariable(i, vars);
            sb.append(s == null ? "" : s);
            sb.append(parts[i + 1]);
        }
        return sb.toString();
    }

    public String parseNumber(HashMap vars) {
        StringBuilder sb = new StringBuilder();
        sb.append(parts[0]);
        for (int i = 0; i < getVariable().length; i++) {
            Object s = getVariable(i, vars);
            sb.append(s == null ? "0" : s);
            sb.append(parts[i + 1]);
        }
        return sb.toString();
    }

    /**
     * 解析模板，生成xml string
     *
     * @param vars
     * @param place : 如果字段没有找到，用place代替
     * @return
     */
    public String parse(HashMap vars, String place) {
        StringBuilder sb = new StringBuilder();
        sb.append(parts[0]);
        for (int i = 0; i < getVariable().length; i++) {
            Object s = getVariable(i, vars);
            sb.append(s == null ? place : s);
            sb.append(parts[i + 1]);
        }
        return sb.toString();
    }

    /**
     * @return
     */
    public String parse() {
        HashMap map = vars.get();
        String ui = parse(map);
        map.clear();
        return ui;
    }

    /**
     * @return
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(parts[0]);
        for (int i = 0; i < getVariable().length; i++) {
            sb.append("[").append(getVariable()[i]).append("]");
            sb.append(parts[i + 1]);
        }
        return sb.toString();
    }

    /**
     * 变量集合
     */
    private Object getVariable(int index, HashMap vars) {
        String v = variable[index];
        Object result = "";
        if (vars != null)
            result = vars.get(v);
        if (result == null) {
            return "";
        }
        return result;
    }

    /**
     * 变量集合
     */
    public String[] getVariable() {
        return variable;
    }

    public static void setVar(Object key, Object value) {
        vars.get().put(key, value);
    }

    public static HashMap getVarMap() {
        return vars.get();
    }

    public UITemplate clone() {
        UITemplate copy = null;
        try {
            copy = (UITemplate) super.clone();

        } catch (CloneNotSupportedException e) {
        }
        return copy;
    }

}
