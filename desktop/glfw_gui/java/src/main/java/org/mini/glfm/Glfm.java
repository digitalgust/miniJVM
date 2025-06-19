/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.mini.glfm;

import org.mini.glfw.Glfw;
import org.mini.net.SocketNative;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

/**
 * @author gust
 */
public class Glfm {

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

    public static String glfmGetClipBoardContent() {
        return null;
    }

    public static void glfmSetClipBoardContent(String str) {

    }

    public static void glfmSetKeyboardVisible(long display, boolean visible) {

    }

    public static boolean glfmIsKeyboardVisible(long display) {
        return true;
    }

    public static void glfmPickPhotoAlbum(long display, int uid, int type) {

    }

    public static void glfmPickPhotoCamera(long display, int uid, int type) {

    }

    public static void glfmImageCrop(long display, int uid, String uris, int x, int y, int width, int height) {
        String saveRoot = glfmGetSaveRoot();
        File src = new File(uris);
        if (src.exists()) {
            // copy file
            File dst = new File(saveRoot + "/tmp/" + src.getName());
            try {
                byte[] buf = new byte[4096];
                FileInputStream fis = new FileInputStream(src);
                FileOutputStream fos = new FileOutputStream(dst);
                while (true) {
                    int len = fis.read(buf);
                    if (len <= 0) {
                        break;
                    }
                    fos.write(buf, 0, len);

                }
                fos.flush();
                fos.close();
                fis.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void glfmSetDisplayConfig(long display,
                                            int preferredAPI,
                                            int colorFormat,
                                            int depthFormat,
                                            int stencilFormat,
                                            int multisample) {

    }

    public static void glfmSwapBuffers(long display) {
    }

    public static boolean glfmIsHapticFeedbackSupported(long display) {
        return false;
    }

    public static void glfmPerformHapticFeedback(long display, int style) {

    }

    public static boolean glfmIsMetalSupported(long display) {
        return false;
    }

    public static long glfmGetMetalView(long display) {
        return 0;
    }


    public static void glfmSetCallBack(long display, GlfmCallBack app) {

    }

    public static String glfmGetSaveRoot() {
        return new File("./").getAbsolutePath();
    }

    public static String glfmGetResRoot() {
        return glfmGetSaveRoot();
    }

    public static long glfmPlayVideo(long display, String uris, String mimeType) {

        uris = "file://" + uris;
        Glfm.glfmOpenOtherApp(SocketNative.toCStyle(uris), SocketNative.toCStyle(""), 0);
        return 0;
    }

    public static void glfmStartVideo(long display, long handle) {

    }

    public static void glfmStopVideo(long display, long handle) {

    }

    public static void glfmPauseVideo(long display, long handle) {

    }

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
    public static int glfmGetSupportedInterfaceOrientation(long display) {
        return GLFMInterfaceOrientationAll;
    }

    /// Sets the supported user interface orientations. Typical values are GLFMInterfaceOrientationAll,
/// GLFMInterfaceOrientationPortrait, or GLFMInterfaceOrientationLandscape.
/// Actualy support may be limited by the device or platform.
    public static void glfmSetSupportedInterfaceOrientation(long display, int supportedOrientations) {

    }

    /// Gets the current user interface orientation. Returns either GLFMInterfaceOrientationPortrait,
/// GLFMInterfaceOrientationPortraitUpsideDown, GLFMInterfaceOrientationLandscapeRight,
/// GLFMInterfaceOrientationLandscapeLeft, or GLFMInterfaceOrientationUnknown.
    public static int glfmGetInterfaceOrientation(long display) {
        return GLFMInterfaceOrientationAll;
    }

    public static void glfmSetMultitouchEnabled(long display, boolean multitouchEnabled) {

    }

    public static boolean glfmGetMultitouchEnabled(long display) {
        return false;
    }

    public static int glfmGetDisplayWidth(long display) {
        return Glfw.glfwGetWindowWidth(display);
    }

    public static int glfmGetDisplayHeight(long display) {
        return Glfw.glfwGetWindowHeight(display);
    }

    public static double glfmGetDisplayScale(long display) {
        return Glfw.glfwGetFramebufferWidth(display) / Glfw.glfwGetWindowWidth(display);
    }

    public static void glfmGetDisplayChromeInsets(long display, double[] top_right_bottom_left) {

    }

    public static int glfmGetDisplayChrome(long display) {
        return GLFMUserInterfaceChromeNavigation;
    }

    public static void glfmSetDisplayChrome(long display, int uiChrome) {

    }

    public static int glfmGetRenderingAPI(long display) {
        return GLFMRenderingAPIOpenGLES3;
    }

    public static boolean glfmHasTouch(long display) {
        return false;
    }

    public static void glfmSetMouseCursor(long display, int mouseCursor) {

    }

    public static boolean glfmExtensionSupported(String extension) {
        return false;
    }

    public static int glfmOpenOtherApp(byte[] cStyleURL, byte[] cStyleMore, int detectAppInstalled) {
        try {
            String url = new String(cStyleURL, 0, cStyleURL.length - 1, "utf-8");
            String more = new String(cStyleMore, 0, cStyleMore.length - 1, "utf-8");
            String osName = System.getProperty("os.name", "");// 获取操作系统的名字
            if (osName.startsWith("Mac OS")) {
                // Mac OS
                if (more.length() > 0) {
                    Runtime.getRuntime().exec("open " + url + " " + more);
                } else {
                    Runtime.getRuntime().exec("open " + url);
                }
            } else if (osName.startsWith("Windows")) {
                // Windows
                Runtime.getRuntime().exec("rundll32 url.dll,FileProtocolHandler " + url);
            } else if (osName.startsWith("Linux")) {
                // Linux
                if (more.length() > 0) {
                    Runtime.getRuntime().exec("xdg-open " + url + " " + more);
                } else {
                    Runtime.getRuntime().exec("xdg-open " + url);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return 1;
    }


    public static String glfmRemoteMethodCall(String inJsonStr) {
        return null;
    }


    public static void glfmBuyAppleProductById(long display, String productId, String base64HandleScript) {

    }
}
