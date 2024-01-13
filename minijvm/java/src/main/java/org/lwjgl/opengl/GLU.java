package org.lwjgl.opengl;

public class GLU {
    public static native void gluPerspective(float r, float g, float b, float a4);
    public static native String gluErrorString(int a1);
    public static native void gluPickMatrix(float, float, float, float, java.nio.IntBuffer);
}
