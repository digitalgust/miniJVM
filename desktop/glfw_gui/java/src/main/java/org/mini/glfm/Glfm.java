/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.mini.glfm;

import org.mini.glfw.Glfw;
import org.mini.gui.GCallBack;

/**
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

    public static boolean glfmGetKeyboardVisible(long display) {
        return true;
    }

    public static void glfmPickPhotoAlbum(long display, int uid, int type) {

    }

    public static void glfmPickPhotoCamera(long display, int uid, int type) {

    }

    public static void glfmImageCrop(long display, int uid, String uris, int x, int y, int width, int height) {

    }

    public static void glfmSetDisplayConfig(long display,
                                            int preferredAPI,
                                            int colorFormat,
                                            int depthFormat,
                                            int stencilFormat,
                                            int multisample) {

    }

    public static void glfmSetCallBack(long display, GCallBack app) {

    }

    public static String glfmGetSaveRoot() {
        return null;
    }

    public static String glfmGetResRoot() {
        return null;
    }

    public static long glfmPlayVideo(long display, String uris, String mimeType) {
        return 0;
    }

    public static void glfmStartVideo(long display, long handle) {

    }

    public static void glfmStopVideo(long display, long handle) {

    }

    public static void glfmPauseVideo(long display, long handle) {

    }

    public static void glfmSetUserInterfaceOrientation(long display, int allowedOrientations) {

    }

    public static int glfmGetUserInterfaceOrientation(long display) {
        return GLFMUserInterfaceOrientationAny;
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


}
