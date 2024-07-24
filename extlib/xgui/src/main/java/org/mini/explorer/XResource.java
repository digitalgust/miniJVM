package org.mini.explorer;

import org.mini.gui.GImage;

public class XResource {

    public static final int TYPE_UNKNOWN = 0;
    public static final int TYPE_IMAGE = 1;
    public static final int TYPE_XML = 2;
    public static final int TYPE_JSON = 3;


    String url;
    int type = TYPE_UNKNOWN;
    byte[] data;
    GImage image;
    String xml;


    /**
     * 根据content-type获取类型
     * "application/xml;charset=utf-8"
     * "application/json;charset=utf-8"
     * "image/png"
     * "image/jpeg"
     *
     * @param type
     * @return
     */
    public static int getTypeByString(String type) {
        if (type == null) {
            return TYPE_UNKNOWN;
        }
        type = type.toLowerCase();
        if (type.startsWith("application/json")) {
            return TYPE_JSON;
        } else if (type.startsWith("application/xml")) {
            return TYPE_XML;
        } else if (type.startsWith("image/")) {
            return TYPE_IMAGE;
        }
        return TYPE_UNKNOWN;
    }
}
