/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package app;

import org.mini.glfm.Glfm;
import org.mini.gui.GApplication;
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
                Glfm.GLFMRenderingAPIOpenGLES2,
                Glfm.GLFMColorFormatRGBA8888,
                Glfm.GLFMDepthFormat16,
                Glfm.GLFMStencilFormat8,
                Glfm.GLFMMultisampleNone);
        GApplication app = new App1();
        GuiCallBack ccb = new GuiCallBack(display, app);
        Glfm.glfmSetCallBack(display, ccb);

    }

}
