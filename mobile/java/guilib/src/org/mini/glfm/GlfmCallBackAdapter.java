package org.mini.glfm;

public class GlfmCallBackAdapter implements GlfmCallBack {

    @Override
    public void mainLoop(long display, double frameTime) {
    }

    @Override
    public boolean onTouch(long display, int touch, int phase, double x, double y) {
        return false;
    }

    @Override
    public boolean onKey(long display, int keyCode, int action, int modifiers) {
        return false;
    }

    @Override
    public void onCharacter(long display, String str, int modifiers) {
    }

    @Override
    public void onKeyboardVisible(long display, boolean visible, double x, double y, double w, double h) {
    }

    @Override
    public void onSurfaceError(long display, String description) {
    }

    @Override
    public void onSurfaceCreated(long display, int width, int height) {
    }

    @Override
    public void onSurfaceResize(long display, int width, int height) {
    }

    @Override
    public void onSurfaceDestroyed(long display) {
    }

    @Override
    public void onMemWarning(long display) {
    }

    @Override
    public void onAppFocus(long display, boolean focused) {
    }

}
