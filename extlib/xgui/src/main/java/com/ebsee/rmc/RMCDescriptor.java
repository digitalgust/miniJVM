package com.ebsee.rmc;

/**
 *
 */
public class RMCDescriptor {
    String className;  // 类名  eg: "org.minijvm.activity.JvmNativeActivity"
    String methodDesc; // 方法名 payV2(Ljava/lang/String;)Ljava/util/Map;
    String paraJson; // 参数 eg: "[\"abc\"]"
    String insJson; // 实例 eg: "{\"uid\":\"123\"}" , or null

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getMethodDesc() {
        return methodDesc;
    }

    public void setMethodDesc(String methodDesc) {
        this.methodDesc = methodDesc;
    }

    public String getParaJson() {
        return paraJson;
    }

    public void setParaJson(String paraJson) {
        this.paraJson = paraJson;
    }

    public String getInsJson() {
        return insJson;
    }

    public void setInsJson(String insJson) {
        this.insJson = insJson;
    }
}
