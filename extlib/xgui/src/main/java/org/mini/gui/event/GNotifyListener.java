/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.mini.gui.event;

/**
 * @author Gust
 */
public interface GNotifyListener {
    public static final String NOTIFY_KEY_DEVICE_TOKEN = "glfm.device.token";
    public static final String NOTIFY_KEY_IOS_PURCHASE = "glfm.ios.purchase";

    void onNotify(String key, String val);
}
