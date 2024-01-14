package org.lwjgl.opengl;

public class GL11 {
    public static native int glGetError();
    public static native void glEnable(int a1);
    public static native void glDisable(int a1);
    public static native void glShadeModel(int a1);
    public static native void glClearColor(float r, float g, float b, float a);
    public static native void glClearDepth(double a1);
    public static native void glDepthFunc(int a1);
    public static native void glMatrixMode(int a1);
    public static native void glLoadIdentity();
    public static native void glTranslatef(float r, float g, float b);
    public static native void glRotatef(float r, float g, float b, float a4);
    public static native int glRenderMode(int a1);
    public static native void glClear(int a1);
    public static native void glFogi(int a1, int a2);
    public static native void glFogf(int a1, float a2);
    public static native int glGenLists(int a1);
    public static native int glGenTextures(int a1);
    public static native void glBindTexture(int a1, int a2);
    public static native void glTexParameteri(int a1, int a2, int a3);
    public static native void glNewList(int a1, int a2);
    public static native void glEndList();
    public static native void glCallList(int a1);
    public static native void glInitNames();
    public static native void glPushName(int a1);
    public static native void glPopName();
    public static native void glBlendFunc(int a1, int a2);
    public static native void glColor4f(float r, float g, float b, float a);
    
    
    public static native void glGetFloat(int a1, java.nio.FloatBuffer a2);
    public static native void glGetInteger(int a1, java.nio.IntBuffer a2);
    public static native void glFog(int a1, java.nio.FloatBuffer a2);
    public static native void glSelectBuffer(java.nio.IntBuffer a1);
    public static native void glVertexPointer(int a1, int a2, java.nio.FloatBuffer a3);
    public static native void glColorPointer(int a1, int a2, java.nio.FloatBuffer a3);
    public static native void glTexCoordPointer(int a1, int a2, java.nio.FloatBuffer a3);
    public static native void glEnableClientState(int a1);
    public static native void glDisableClientState(int a1);
    public static native void glDrawArrays(int a1, int a2, int a3);
}
