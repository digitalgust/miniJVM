/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.mini.glfm;

/**
 *
 * @author gust
 */
public interface GlfmCallBack {

    //void onFrame(GLFMDisplay *display, double frameTime)
    public void mainLoop(long display, double frameTime);

    //bool onTouch(GLFMDisplay *display, int touch, GLFMTouchPhase phase, double x, double y)
    public boolean onTouch(long display, int touch, int phase, double x, double y);

    //bool onKey(GLFMDisplay *display, GLFMKey keyCode, GLFMKeyAction action, int modifiers)
    public boolean onKey(long display, int keyCode, int action, int modifiers);

    public void onCharacter(long display, String str, int modifiers);

    public void onKeyboardVisible(long display, boolean visible, double x, double y, double w, double h);

    public void onSurfaceError(long display, String description);

    //void onSurfaceCreated(GLFMDisplay *display, int width, int height)
    public void onSurfaceCreated(long display, int width, int height);

    public void onSurfaceResize(long display, int width, int height);

    //void onSurfaceDestroyed(GLFMDisplay *display);
    public void onSurfaceDestroyed(long display);

    public void onMemWarning(long display);

    public void onAppFocus(long display, boolean focused);

    public void onPhotoPicked(long display, int uid, String url, byte[] data);

    public void onNotify(long display, String key, String val);

}
