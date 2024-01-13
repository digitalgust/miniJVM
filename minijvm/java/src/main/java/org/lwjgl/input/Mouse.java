package org.lwjgl.input;

public class Mouse {
    public static native void create();
    public static native void destroy();
    public static native int getDX();
    public static native int getDY();
    public static native int getEventButton();
    public static native boolean getEventButtonState();
    public static native boolean next();
    public static native void setGrabbed(boolean a1);
}
