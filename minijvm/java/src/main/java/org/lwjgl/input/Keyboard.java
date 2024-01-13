package org.lwjgl.input;

public class Keyboard {
    public static native void create();
    public static native void destroy();

    public static native int getEventKey();
    public static native boolean getEventKeyState();
    public static native boolean isKeyDown(int key);
    public static native boolean next();
}
