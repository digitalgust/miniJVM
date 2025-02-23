package org.mini.glfw;

import org.mini.gui.callback.GCallBack;


/**
 * GUI main
 *
 * @author gust
 */
public class GlfwMain {

    public static void main(String[] args) {
        System.out.println("para[0] : window width, para[1] window height");

        int w = 800, h = 480;
        if (args.length >= 2) {
            try {
                w = Integer.parseInt(args[0]);
                h = Integer.parseInt(args[1]);
            } catch (Exception e) {

            }
        }

        GCallBack ccb = GCallBack.getInstance();
        ccb.init(w, h);//window size  568 320
        //ccb.init(812, 375);//ip 12 pro max

        ccb.mainLoop();
        ccb.destroy();
    }
}
