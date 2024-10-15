package org.mini.layout.xwebview;

import org.mini.gui.GImage;

import java.io.UnsupportedEncodingException;

public class XuiResource {

    public static final int TYPE_UNKNOWN = 0;
    public static final int TYPE_IMAGE = 1;
    public static final int TYPE_XML = 2;
    public static final int TYPE_JSON = 3;
    public static final int TYPE_TEXT = 4;


    String url;
    int type = TYPE_UNKNOWN;
    byte[] data;
    GImage image;
    String xml;

    String json;


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
        } else if (type.startsWith("text/")) {
            return TYPE_TEXT;
        }
        return TYPE_UNKNOWN;
    }

    public int getType() {
        return type;
    }

    public String getUrl() {
        return url;
    }

    public byte[] getData() {
        return data;
    }

    public GImage getImage() {
        return image;
    }

    public String getXml() {
        return xml;
    }

    public String getString() {
        try {
            if (type == TYPE_JSON || type == TYPE_TEXT || type == TYPE_XML) {
                return new String(data, "utf-8");
            }
        } catch (UnsupportedEncodingException e) {
        }
        return null;
    }
}
