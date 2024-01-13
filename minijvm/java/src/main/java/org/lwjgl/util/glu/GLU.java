package org.lwjgl.util.glu;

public class GLU {
    public static native void gluPerspective(float r, float g, float b, float a4);
    public static native String gluErrorString(int a1);
    public static native void gluPickMatrix(float a1, float a2, float a3, float a4, java.nio.IntBuffer a5);
}
