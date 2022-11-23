package org.mini.glfw;

import org.mini.apploader.AppManager;
import org.mini.gui.GCallBack;
import org.mini.gui.GToolkit;
import org.mini.gui.gscript.Interpreter;


/**
 * GUI main
 *
 * @author gust
 */
public class GlfwMain {

    public static void main(String[] args) {
//        String s = GToolkit.readFileFromJarAsString("/res/app.txt", "utf-8");
//        Interpreter inp = new Interpreter();
//        inp.loadFromString(s);
//        inp.callSub("main()");

        GCallBack ccb = GCallBack.getInstance();
        ccb.init(1280, 960);//window size  568 320
        //ccb.init(812, 375);//ip 12 pro max

        AppManager.getInstance().active();

        ccb.mainLoop();
        ccb.destroy();
    }
}
