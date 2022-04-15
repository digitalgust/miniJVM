package org.mini.glfw;

import org.mini.apploader.AppManager;
import org.mini.gui.GCallBack;


/**
 * GUI main
 *
 * @author gust
 */
public class GlfwMain {

    public static void main(String[] args) {
        GCallBack ccb = GCallBack.getInstance();
        ccb.init(700, 320);//window size  568 320
        //ccb.init(812, 375);//ip 12 pro max

        AppManager.getInstance().active();

        ccb.mainLoop();
        ccb.destroy();
    }
}
