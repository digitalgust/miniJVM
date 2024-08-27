/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.mini.glfm;

import java.io.UnsupportedEncodingException;

/**
 * @author gust
 */
public class Glfm {

    static {
        System.setProperty("gui.driver", "org.mini.glfm.GlfmCallBackImpl");
    }


    static byte[] toCstyleBytes(String s) {
        if (s == null) {
            return null;
        }
        if (s.length() == 0 || s.charAt(s.length() - 1) != '\000') {
            s += '\000';
        }
        byte[] barr = null;
        try {
            barr = s.getBytes("utf-8");
        } catch (UnsupportedEncodingException ex) {
        }
        return barr;
    }


    public static final int //
            MAX_SIMULTANEOUS_TOUCHES = 10;

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
            GLFMInterfaceOrientationUnknown = 0,
            GLFMInterfaceOrientationPortrait = (1 << 0),
            GLFMInterfaceOrientationPortraitUpsideDown = (1 << 1),
            GLFMInterfaceOrientationLandscapeLeft = (1 << 2),
            GLFMInterfaceOrientationLandscapeRight = (1 << 3),
            GLFMInterfaceOrientationLandscape = (GLFMInterfaceOrientationLandscapeLeft |
                    GLFMInterfaceOrientationLandscapeRight),
            GLFMInterfaceOrientationAll = (GLFMInterfaceOrientationPortrait |
                    GLFMInterfaceOrientationPortraitUpsideDown |
                    GLFMInterfaceOrientationLandscapeLeft |
                    GLFMInterfaceOrientationLandscapeRight),
            GLFMInterfaceOrientationAllButUpsideDown = (GLFMInterfaceOrientationPortrait |
                    GLFMInterfaceOrientationLandscapeLeft |
                    GLFMInterfaceOrientationLandscapeRight);

    @Deprecated
    public static final int //
            GLFMUserInterfaceOrientationAny = GLFMInterfaceOrientationAll,
            GLFMUserInterfaceOrientationPortrait = GLFMInterfaceOrientationPortrait,
            GLFMUserInterfaceOrientationLandscape = GLFMInterfaceOrientationLandscape;

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

    public static int //
            GLFMHapticFeedbackLight = 0,
            GLFMHapticFeedbackMedium = 1,
            GLFMHapticFeedbackHeavy = 2;
    public static int //
            GLFMSensorAccelerometer = 0, // Events are a vector in G's
            GLFMSensorMagnetometer = 1, // Events are a vector in microteslas
            GLFMSensorGyroscope = 2, // Events are a vector in radians/sec
            GLFMSensorRotationMatrix = 3 // Events are a rotation matrix
                    ;

    /**
     * photo pick
     */
    public static int GLFMPickPhotoZoom_MASK = 0;
    public static int GLFMPickPhotoZoom_1024 = 0;
    public static int GLFMPickPhotoZoom_No = 1;
    //
    public static int GLFMPickPhotoSave_MASK = 1;
    public static int GLFMPickPhotoSave_yes = 2;
    public static int GLFMPickPhotoSave_no = 0;

    public static int GLFMPickupTypeNoDef = 0;
    public static int GLFMPickupTypeImage = 1;
    public static int GLFMPickupTypeVideo = 2;
    //

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

    public static native void glfmSwapBuffers(long display);

    public static native boolean glfmIsHapticFeedbackSupported(long display);

    public static native void glfmPerformHapticFeedback(long display, int style);

    public static native boolean glfmIsMetalSupported(long display);

    public static native long glfmGetMetalView(long display);

    @Deprecated
    public static void glfmSetUserInterfaceOrientation(long display, int allowedOrientations) {
        glfmSetSupportedInterfaceOrientation(display, allowedOrientations);
    }

    @Deprecated
    public static int glfmGetUserInterfaceOrientation(long display) {
        return glfmGetSupportedInterfaceOrientation(display);
    }

    /// Returns the supported user interface orientations. Default is GLFMInterfaceOrientationAll.
    /// Actualy support may be limited by the device or platform.
    public static native int glfmGetSupportedInterfaceOrientation(long display);

    /// Sets the supported user interface orientations. Typical values are GLFMInterfaceOrientationAll,
    /// GLFMInterfaceOrientationPortrait, or GLFMInterfaceOrientationLandscape.
    /// Actualy support may be limited by the device or platform.
    public static native void glfmSetSupportedInterfaceOrientation(long display, int supportedOrientations);

    /// Gets the current user interface orientation. Returns either GLFMInterfaceOrientationPortrait,
    /// GLFMInterfaceOrientationPortraitUpsideDown, GLFMInterfaceOrientationLandscapeRight,
    /// GLFMInterfaceOrientationLandscapeLeft, or GLFMInterfaceOrientationUnknown.
    public static native int glfmGetInterfaceOrientation(long display);

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

    public static native boolean glfmIsKeyboardVisible(long display);

    public static native String glfmGetResRoot();

    public static native String glfmGetSaveRoot();

    public static native String glfmGetClipBoardContent();

    public static native void glfmSetClipBoardContent(String str);

    public static native void glfmPickPhotoAlbum(long display, int uid, int type);

    public static native void glfmPickPhotoCamera(long display, int uid, int type);

    public static native void glfmImageCrop(long display, int uid, String uris, int x, int y, int width, int height);

    public static native long glfmPlayVideo(long display, String uris, String mimeType);

    public static native void glfmStartVideo(long display, long handle);

    public static native void glfmStopVideo(long display, long handle);

    public static native void glfmPauseVideo(long display, long handle);

    public static native int glfmOpenOtherApp(byte[] cStyleURL, byte[] cStyleMore, int detectAppInstalled);

    static native String glfmRemoteMethodCall(byte[] inJsonStr);

    public static String glfmRemoteMethodCall(String inJsonStr) {
        byte[] inJsonStrBytes = toCstyleBytes(inJsonStr.toString());
        return glfmRemoteMethodCall(inJsonStrBytes);
    }


}
