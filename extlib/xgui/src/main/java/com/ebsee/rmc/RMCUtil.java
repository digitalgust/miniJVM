package com.ebsee.rmc;

import org.mini.glfm.Glfm;
import org.mini.json.JsonParser;
import org.mini.json.JsonPrinter;

import java.util.Map;

public class RMCUtil {

    /**
     * 调用glfm模块的 remoteMethodCall，远程调用android 方法
     *
     * @param className  "org.minijvm.activity.JvmNativeActivity"
     * @param methodDesc "playVideo(Ljava/lang/String;Ljava/lang/String;)J"
     * @param para       new Object[]{"http://abc.com/x.mov","mov"}
     * @param instance   null or instance of class
     * @return
     */
    public static Map<String, String> remoteMethodCall(String className, String methodDesc, Object[] para, Object instance) {
        JsonPrinter printer = new JsonPrinter();

        RMCDescriptor desc = new RMCDescriptor();
        desc.setClassName(className);
        desc.setMethodDesc(methodDesc);
        desc.setInsJson(printer.serial(instance));
        desc.setParaJson(printer.serial(para));

        String s = printer.serial(desc);
        //JsonParser<RMCDescriptor> jp1 = new JsonParser();
        //RMCDescriptor desc1 = jp1.deserial(s, RMCDescriptor.class);

        String ret = Glfm.glfmRemoteMethodCall(s);
        if (ret != null) {
            JsonParser<Map> parser = new JsonParser<>();
            Map map = parser.deserial(ret, Map.class);
            return map;
        } else {
            return null;
        }
    }

}
