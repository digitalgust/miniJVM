package org.lwjgl.opengl;

public class GL11 {
    public static native int glGetError();
    public static native void glEnable(int a1);
    public static native void glDisable(int a1);
    public static native void glShadeModel(int a1);
    public static native void glClearColor(float r, float g, float b, float a);
    public static native void glClearDepth(int a1);
    public static native void glDepthFunc(int a1);
    public static native void glMatrixMode(int a1);
    public static native void glLoadIdentity();
    public static native void glTranslatef(float r, float g, float b);
    public static native void glRotatef(float r, float g, float b, float a4);
    public static native void glRenderMode(int a1);
    public static native void glClear(int a1);
    public static native void glFogi(int a1, int a2);
    public static native void glFogf(int a1, float a2);
    
    /*
    GL11.glGetInteger(ILjava/nio/IntBuffer;)V
    GL11.glFog(ILjava/nio/FloatBuffer;)V
    GLU.gluPickMatrix(FFFFLjava/nio/IntBuffer;)V
    GL11.glSelectBuffer(Ljava/nio/IntBuffer;)V
    */
}

public class GLU {
    public static native void gluPerspective(float r, float g, float b, float a4);
    public static native String gluErrorString(int a1);
    
}
