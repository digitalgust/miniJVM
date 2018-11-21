
package app;

import org.mini.glfm.Glfm;
import org.mini.gui.GApplication;
import org.mini.gui.impl.GuiCallBack;
import test.MyApp;

/**
 *
 * This class MUST be app.GlfmMain
 *
 * And this jar MUST be resfiles/ExApp.jar
 * 
 * Or you can change the file name and class name in source glfmapp/main.c 
 *
 * @author gust
 */
public class GlfmMain {

    public static void main(String[] args) {
    }

    static public void glinit(long display) {

        Glfm.glfmSetDisplayConfig(display,
                Glfm.GLFMRenderingAPIOpenGLES3,
                Glfm.GLFMColorFormatRGBA8888,
                Glfm.GLFMDepthFormat16,
                Glfm.GLFMStencilFormat8,
                Glfm.GLFMMultisampleNone);
        GuiCallBack.getInstance().setDisplay(display);
        Glfm.glfmSetCallBack(display, GuiCallBack.getInstance());

        GApplication app = new MyApp();
        GuiCallBack.getInstance().setApplication(app);
    }

}
