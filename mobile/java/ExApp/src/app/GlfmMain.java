/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package app;

import org.mini.glfm.Glfm;
import static org.mini.glfm.Glfm.GLFMDepthFormat16;
import static org.mini.glfm.Glfm.GLFMMultisampleNone;
import static org.mini.glfm.Glfm.GLFMRenderingAPIOpenGLES2;
import static org.mini.glfm.Glfm.GLFMStencilFormat8;
import org.mini.gui.GuiCallBack;
import test.App1;

/**
 *
 * This class MUST be app.GlfmMain 
 * 
 * And this jar MUST be resfiles/ExApp.jar
 * 
 * it used in c source glfmapp/main.c
 * 
 * @author gust
 */
public class GlfmMain {



    public static void main(String[] args) {
    }


    static public void glinit(long display) {

        Glfm.glfmSetDisplayConfig(display,
                GLFMRenderingAPIOpenGLES2,
                Glfm.GLFMColorFormatRGBA8888,
                GLFMDepthFormat16,
                GLFMStencilFormat8,
                GLFMMultisampleNone);
        App1 app = new App1();
        GuiCallBack ccb = new GuiCallBack(display, app);
        Glfm.glfmSetCallBack(display, ccb);

    }

}
