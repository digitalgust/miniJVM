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
    /**
     * ios IAP
     * val FORMAT:     code:receipt  例子： ０:9E32...
     */
    public static final String NOTIFY_KEY_IOS_PURCHASE = "glfm.ios.purchase";
    public static final int IAPPurchSuccess = 0,       // 购买成功
            IAPPurchFailed = 1,        // 购买失败
            IAPPurchCancel = 2,        // 取消购买
            IAPPurchVerFailed = 3,     // 订单校验失败
            IAPPurchVerSuccess = 4,    // 订单校验成功
            IAPPurchNotArrow = 5;      // 不允许内购

    void onNotify(String key, String val);
}
