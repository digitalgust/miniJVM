package test;

import org.mini.glfw.GlfwCallback;

public class GlfwCallbackAdapter implements GlfwCallback {

    @Override
    public void error(int error, String description) {
    }

    @Override
    public void monitor(long monitor, boolean connected) {
    }

    @Override
    public void windowPos(long window, int x, int y) {
    }

    @Override
    public void windowSize(long window, int width, int height) {
    }

    @Override
    public void framebufferSize(long window, int x, int y) {

    }

    @Override
    public boolean windowClose(long window) {
        return true;
    }

    @Override
    public void windowRefresh(long window) {
    }

    @Override
    public void windowFocus(long window, boolean focused) {
    }

    @Override
    public void windowIconify(long window, boolean iconified) {
    }

    @Override
    public void key(long window, int key, int scanCode, int action, int mods) {
    }

    @Override
    public void character(long window, char character) {
    }

    @Override
    public void mouseButton(long window, int button, boolean pressed) {
    }

    @Override
    public void cursorPos(long window, int x, int y) {
    }

    @Override
    public void cursorEnter(long window, boolean entered) {
    }

    @Override
    public void scroll(long window, double scrollX, double scrollY) {
    }
    
    @Override
    public void drop(long window, int count, String[] paths){
    }

    @Override
    public void mainLoop() {
    }
}
