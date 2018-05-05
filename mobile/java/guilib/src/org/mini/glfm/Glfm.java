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
public class Glfm {

    public static final int //
            GLFMRenderingAPIOpenGLES2 = 0,
            GLFMRenderingAPIOpenGLES3 = 1,
            GLFMRenderingAPIOpenGLES31 = 2,
            GLFMRenderingAPIOpenGLES32 = 3;
    public static final int //
            GLFMColorFormatRGBA8888 = 0,
            GLFMColorFormatRGB565 = 1;

    public static final int //
            GLFMDepthFormatNone = 0,
            GLFMDepthFormat16 = 1,
            GLFMDepthFormat24 = 2;

    public static final int //
            GLFMStencilFormatNone = 0,
            GLFMStencilFormat8 = 1;

    public static final int //
            GLFMMultisampleNone = 0,
            GLFMMultisample4X = 1;

    public static final int //
            GLFMUserInterfaceChromeNavigation = 0,
            GLFMUserInterfaceChromeNavigationAndStatusBar = 1,
            GLFMUserInterfaceChromeFullscreen = 2;

    public static final int //
            GLFMUserInterfaceOrientationAny = 0,
            GLFMUserInterfaceOrientationPortrait = 1,
            GLFMUserInterfaceOrientationLandscape = 2;

    public static final int //
            GLFMTouchPhaseHover = 0,
            GLFMTouchPhaseBegan = 1,
            GLFMTouchPhaseMoved = 2,
            GLFMTouchPhaseEnded = 3,
            GLFMTouchPhaseCancelled = 4;

    public static final int //
            GLFMMouseCursorAuto = 0,
            GLFMMouseCursorNone = 1,
            GLFMMouseCursorDefault = 2,
            GLFMMouseCursorPointer = 3,
            GLFMMouseCursorCrosshair = 4,
            GLFMMouseCursorText = 5;

    public static final int //
            GLFMKeyBackspace = 0x08,
            GLFMKeyTab = 0x09,
            GLFMKeyEnter = 0x0d,
            GLFMKeyEscape = 0x1b,
            GLFMKeySpace = 0x20,
            GLFMKeyLeft = 0x25,
            GLFMKeyUp = 0x26,
            GLFMKeyRight = 0x27,
            GLFMKeyDown = 0x28,
            GLFMKeyNavBack = 0x1000,
            GLFMKeyNavMenu = 0x1001,
            GLFMKeyNavSelect = 0x1002,
            GLFMKeyPlayPause = 0x2000;

    public static final int //
            GLFMKeyModifierShift = (1 << 0),
            GLFMKeyModifierCtrl = (1 << 1),
            GLFMKeyModifierAlt = (1 << 2),
            GLFMKeyModifierMeta = (1 << 3);

    public static int //
            GLFMKeyActionPressed = 0,
            GLFMKeyActionRepeated = 1,
            GLFMKeyActionReleased = 2;

    public static native void glfmSetCallBack(long display, GlfmCallBack app);

/// Init the display condifuration. Should only be called in glfmMain.
/// If the device does not support the preferred rendering API, the next available rendering API is
/// chosen (OpenGL ES 3.0 if OpenGL ES 3.1 is not available, and OpenGL ES 2.0 if OpenGL ES 3.0 is
/// not available). Call glfmGetRenderingAPI in the GLFMSurfaceCreatedFunc to see which rendering
/// API was chosen.
//void glfmSetDisplayConfig(GLFMDisplay *display,
//                          GLFMRenderingAPI preferredAPI,
//                          GLFMColorFormat colorFormat,
//                          GLFMDepthFormat depthFormat,
//                          GLFMStencilFormat stencilFormat,
//                          GLFMMultisample multisample);
    public static native void glfmSetDisplayConfig(long display,
            int preferredAPI,
            int colorFormat,
            int depthFormat,
            int stencilFormat,
            int multisample);

    public static native void glfmSetUserInterfaceOrientation(long display, int allowedOrientations);

    public static native int glfmGetUserInterfaceOrientation(long display);

    public static native void glfmSetMultitouchEnabled(long display, boolean multitouchEnabled);

    public static native boolean glfmGetMultitouchEnabled(long display);

    public static native int glfmGetDisplayWidth(long display);

    public static native int glfmGetDisplayHeight(long display);

    public static native double glfmGetDisplayScale(long display);

    public static native void glfmGetDisplayChromeInsets(long display, double[] top_right_bottom_left);

    public static native int glfmGetDisplayChrome(long display);

    public static native void glfmSetDisplayChrome(long display, int uiChrome);

    public static native int glfmGetRenderingAPI(long display);

    public static native boolean glfmHasTouch(long display);

    public static native void glfmSetMouseCursor(long display, int mouseCursor);

    public static native boolean glfmExtensionSupported(String extension);

    public static native void glfmSetKeyboardVisible(long display, boolean visible);

    public static native boolean glfmGetKeyboardVisible(long display);

    public static native String glfmGetResRoot();

    public static native String glfmGetSaveRoot();
    
    public static native String glfmGetClipBoardContent();
    
    public static native void glfmSetClipBoardContent(String str);

}
