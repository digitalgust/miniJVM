package test;

import static java.lang.Math.pow;
import static org.mini.gl.GL.GL_COLOR_BUFFER_BIT;
import static org.mini.gl.GL.GL_DEPTH_BUFFER_BIT;
import static org.mini.gl.GL.GL_STENCIL_BUFFER_BIT;
import static org.mini.gl.GL.GL_TRUE;
import static org.mini.gl.GL.glClear;
import static org.mini.gl.GL.glClearColor;
import static org.mini.gl.GL.glViewport;
import org.mini.glfw.Glfw;
import static org.mini.glfw.Glfw.GLFW_CONTEXT_VERSION_MAJOR;
import static org.mini.glfw.Glfw.GLFW_CONTEXT_VERSION_MINOR;
import static org.mini.glfw.Glfw.GLFW_DEPTH_BITS;
import static org.mini.glfw.Glfw.GLFW_KEY_ESCAPE;
import static org.mini.glfw.Glfw.GLFW_MOUSE_BUTTON_2;
import static org.mini.glfw.Glfw.GLFW_MOUSE_BUTTON_LEFT;
import static org.mini.glfw.Glfw.GLFW_OPENGL_CORE_PROFILE;
import static org.mini.glfw.Glfw.GLFW_OPENGL_FORWARD_COMPAT;
import static org.mini.glfw.Glfw.GLFW_OPENGL_PROFILE;
import static org.mini.glfw.Glfw.GLFW_PRESS;
import static org.mini.glfw.Glfw.GLFW_TRANSPARENT_FRAMEBUFFER;
import static org.mini.glfw.Glfw.GLFW_TRUE;
import static org.mini.glfw.Glfw.glfwCreateWindow;
import static org.mini.glfw.Glfw.glfwGetTime;
import static org.mini.glfw.Glfw.glfwInit;
import static org.mini.glfw.Glfw.glfwMakeContextCurrent;
import static org.mini.glfw.Glfw.glfwPollEvents;
import static org.mini.glfw.Glfw.glfwSetCallback;
import static org.mini.glfw.Glfw.glfwSetWindowShouldClose;
import static org.mini.glfw.Glfw.glfwSwapBuffers;
import static org.mini.glfw.Glfw.glfwTerminate;
import static org.mini.glfw.Glfw.glfwWindowHint;
import static org.mini.glfw.Glfw.glfwWindowShouldClose;
import static org.mini.glfw.Glfw.glfwGetFramebufferWidth;
import static org.mini.glfw.Glfw.glfwGetFramebufferHeight;
import org.mini.nanovg.Nanovg;
import static org.mini.nanovg.Nanovg.NVG_ALIGN_CENTER;
import static org.mini.nanovg.Nanovg.NVG_ALIGN_LEFT;
import static org.mini.nanovg.Nanovg.NVG_ALIGN_MIDDLE;
import static org.mini.nanovg.Nanovg.NVG_ALIGN_RIGHT;
import static org.mini.nanovg.Nanovg.NVG_ALIGN_TOP;
import static org.mini.nanovg.Nanovg.NVG_ANTIALIAS;
import static org.mini.nanovg.Nanovg.NVG_BEVEL;
import static org.mini.nanovg.Nanovg.NVG_BUTT;
import static org.mini.nanovg.Nanovg.NVG_CCW;
import static org.mini.nanovg.Nanovg.NVG_CW;
import static org.mini.nanovg.Nanovg.NVG_DEBUG;
import static org.mini.nanovg.Nanovg.NVG_HOLE;
import static org.mini.nanovg.Nanovg.NVG_MITER;
import static org.mini.nanovg.Nanovg.NVG_ROUND;
import static org.mini.nanovg.Nanovg.NVG_SQUARE;
import static org.mini.nanovg.Nanovg.NVG_STENCIL_STROKES;
import static org.mini.nanovg.Nanovg.nvgAddFallbackFontId;
import static org.mini.nanovg.Nanovg.nvgArc;
import static org.mini.nanovg.Nanovg.nvgBeginFrame;
import static org.mini.nanovg.Nanovg.nvgBeginPath;
import static org.mini.nanovg.Nanovg.nvgBezierTo;
import static org.mini.nanovg.Nanovg.nvgBoxGradient;
import static org.mini.nanovg.Nanovg.nvgCircle;
import static org.mini.nanovg.Nanovg.nvgClosePath;
import static org.mini.nanovg.Nanovg.nvgCreateFont;
import static org.mini.nanovg.Nanovg.nvgCreateGL3;
import static org.mini.nanovg.Nanovg.nvgCreateNVGglyphPosition;
import static org.mini.nanovg.Nanovg.nvgCreateNVGtextRow;
import static org.mini.nanovg.Nanovg.nvgDegToRad;
import static org.mini.nanovg.Nanovg.nvgDeleteNVGglyphPosition;
import static org.mini.nanovg.Nanovg.nvgDeleteNVGtextRow;
import static org.mini.nanovg.Nanovg.nvgEllipse;
import static org.mini.nanovg.Nanovg.nvgEndFrame;
import static org.mini.nanovg.Nanovg.nvgFill;
import static org.mini.nanovg.Nanovg.nvgFillColor;
import static org.mini.nanovg.Nanovg.nvgFillPaint;
import static org.mini.nanovg.Nanovg.nvgFontBlur;
import static org.mini.nanovg.Nanovg.nvgFontFace;
import static org.mini.nanovg.Nanovg.nvgFontSize;
import static org.mini.nanovg.Nanovg.nvgGlobalAlpha;
import static org.mini.nanovg.Nanovg.nvgHSLA;
import static org.mini.nanovg.Nanovg.nvgImagePattern;
import static org.mini.nanovg.Nanovg.nvgImageSize;
import static org.mini.nanovg.Nanovg.nvgIntersectScissor;
import static org.mini.nanovg.Nanovg.nvgLineCap;
import static org.mini.nanovg.Nanovg.nvgLineJoin;
import static org.mini.nanovg.Nanovg.nvgLineTo;
import static org.mini.nanovg.Nanovg.nvgLinearGradient;
import static org.mini.nanovg.Nanovg.nvgMoveTo;
import static org.mini.nanovg.Nanovg.nvgNVGglyphPosition_x;
import static org.mini.nanovg.Nanovg.nvgNVGtextRow_end;
import static org.mini.nanovg.Nanovg.nvgNVGtextRow_next;
import static org.mini.nanovg.Nanovg.nvgNVGtextRow_start;
import static org.mini.nanovg.Nanovg.nvgNVGtextRow_width;
import static org.mini.nanovg.Nanovg.nvgPathWinding;
import static org.mini.nanovg.Nanovg.nvgRadialGradient;
import static org.mini.nanovg.Nanovg.nvgRect;
import static org.mini.nanovg.Nanovg.nvgResetScissor;
import static org.mini.nanovg.Nanovg.nvgRestore;
import static org.mini.nanovg.Nanovg.nvgRotate;
import static org.mini.nanovg.Nanovg.nvgRoundedRect;
import static org.mini.nanovg.Nanovg.nvgSave;
import static org.mini.nanovg.Nanovg.nvgScale;
import static org.mini.nanovg.Nanovg.nvgScissor;
import static org.mini.nanovg.Nanovg.nvgStroke;
import static org.mini.nanovg.Nanovg.nvgStrokeColor;
import static org.mini.nanovg.Nanovg.nvgStrokeWidth;
import static org.mini.nanovg.Nanovg.nvgTextAlign;
import static org.mini.nanovg.Nanovg.nvgTextBoundsJni;
import static org.mini.nanovg.Nanovg.nvgTextBoxBoundsJni;
import static org.mini.nanovg.Nanovg.nvgTextBoxJni;
import static org.mini.nanovg.Nanovg.nvgTextBreakLinesJni;
import static org.mini.nanovg.Nanovg.nvgTextGlyphPositionsJni;
import static org.mini.nanovg.Nanovg.nvgTextJni;
import static org.mini.nanovg.Nanovg.nvgTextLineHeight;
import static org.mini.nanovg.Nanovg.nvgTextMetrics;
import static org.mini.nanovg.Nanovg.nvgTranslate;
import static test.GToolkit.toCstyleBytes;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
/**
 *
 * @author gust
 */
public class TestNanovg {

    long display;

    public static void main(String[] args) {
        TestNanovg gt = new TestNanovg();
        gt.t1();

    }

    void t1() {
        glfwInit();
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
        glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GL_TRUE);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
        glfwWindowHint(GLFW_DEPTH_BITS, 16);
        glfwWindowHint(GLFW_TRANSPARENT_FRAMEBUFFER, GLFW_TRUE);
        display = glfwCreateWindow(1200, 600, "TestNanovg".getBytes(), 0, 0);
        if (display != 0) {
            glfwSetCallback(display, new CallBack());
            glfwMakeContextCurrent(display);
            //glfwSwapInterval(1);

            int w = glfwGetFramebufferWidth(display);
            int h = glfwGetFramebufferHeight(display);
            System.out.println("w=" + w + "  ,h=" + h);
            init();
            long last = System.currentTimeMillis(), now;
            int count = 0;
            while (!glfwWindowShouldClose(display)) {

                display();

                glfwPollEvents();
                glfwSwapBuffers(display);
                count++;
                now = System.currentTimeMillis();
                if (now - last > 1000) {
                    System.out.println("fps:" + count);
                    last = now;
                    count = 0;
                }
                try {
                    Thread.sleep(30);
                } catch (InterruptedException ex) {
                }
            }
            glfwTerminate();
        }
    }

    boolean exit = false;
    long curWin;
    int mx, my;

    class CallBack extends GlfwCallbackAdapter {

        @Override
        public void key(long window, int key, int scancode, int action, int mods) {
            System.out.println("key:" + key + " action:" + action);
            if (key == GLFW_KEY_ESCAPE && action == GLFW_PRESS) {
                glfwSetWindowShouldClose(window, GLFW_TRUE);
            }
        }

        @Override
        public void mouseButton(long window, int button, boolean pressed) {
            if (window == curWin) {
                String bt = button == GLFW_MOUSE_BUTTON_LEFT ? "LEFT" : button == GLFW_MOUSE_BUTTON_2 ? "RIGHT" : "OTHER";
                String press = pressed ? "pressed" : "released";
                System.out.println(bt + " " + mx + " " + my + "  " + press);
            }
        }

        @Override
        public void cursorPos(long window, int x, int y) {
            curWin = window;
            mx = x;
            my = y;
        }

        @Override
        public boolean windowClose(long window) {
            System.out.println("byebye");
            return true;
        }

        @Override
        public void windowSize(long window, int width, int height) {
            System.out.println("resize " + width + " " + height);
        }

        @Override
        public void framebufferSize(long window, int x, int y) {
        }
    }

//---------------------------------------------------------------------  
//  
    long vg;
    int blowup = 0;
    byte[] fontname = "ch".getBytes();
    byte[] iconname = "icon".getBytes();
    byte[] emojiname = "icon".getBytes();
    boolean premult;
    double ct, prevt, dt;
    static int ICON_SEARCH = 0x1F50D;
    static int ICON_CIRCLED_CROSS = 0x2716;
    static int ICON_CHEVRON_RIGHT = 0xE75E;
    static int ICON_CHECK = 0x2713;
    static int ICON_LOGIN = 0xE740;
    static int ICON_TRASH = 0xE729;

    void init() {
        vg = nvgCreateGL3(NVG_ANTIALIAS | NVG_STENCIL_STROKES | NVG_DEBUG);
        if (vg == 0) {
            System.out.println("Could not init nanovg.\n");

        }
        int font;
        font = nvgCreateFont(vg, fontname, toCstyleBytes("../../binary/res/wqymhei.ttc\000"));
        if (font == -1) {
            System.out.println("Could not add font.\n");
        }
        nvgAddFallbackFontId(vg, font, font);

        font = nvgCreateFont(vg, iconname, toCstyleBytes("../../binary/res/entypo.ttf\000"));
        if (font == -1) {
            System.out.println("Could not add font.\n");
        }
        font = nvgCreateFont(vg, emojiname, toCstyleBytes("../../binary/res/NotoEmoji-Regular.ttf\000"));
        if (font == -1) {
            System.out.println("Could not add font.\n");
        }
    }

    byte[] cpToUTF8(int ch) {
        return toCstyleBytes("" + (char) ch);
    }

    void display() {

        float pxRatio;
        int winWidth, winHeight;
        int fbWidth, fbHeight;
        winWidth = Glfw.glfwGetWindowWidth(display);
        winHeight = Glfw.glfwGetWindowHeight(display);
        fbWidth = glfwGetFramebufferWidth(display);
        fbHeight = glfwGetFramebufferHeight(display);
        // Calculate pixel ration for hi-dpi devices.
        pxRatio = (float) fbWidth / (float) winWidth;

        ct = glfwGetTime();
        dt = ct - prevt;
        prevt = ct;
        // Update and render
        glViewport(0, 0, fbWidth, fbHeight);
        if (premult) {
            glClearColor(0, 0, 0, 0);
        } else {
            glClearColor(0.3f, 0.3f, 0.32f, 1.0f);
        }
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT | GL_STENCIL_BUFFER_BIT);

        nvgBeginFrame(vg, winWidth, winHeight, pxRatio);
        renderDemo(vg, mx, my, winWidth, winHeight, (float) ct, blowup);
        nvgEndFrame(vg);
        try {
            Thread.sleep(10);
        } catch (InterruptedException ex) {
        }

    }
    float NVG_PI = 3.1415926f;

    float[] nvgRGBA(int r, int g, int b, int a) {
        return Nanovg.nvgRGBA((byte) r, (byte) g, (byte) b, (byte) a);
    }

    float cosf(float v) {
        return (float) Math.cos(v);
    }

    float sinf(float v) {
        return (float) Math.sin(v);
    }

    float sqrtf(float v) {
        return (float) Math.sqrt(v);
    }

//static float minf(float a, float b) { return a < b ? a : b; }
    static float maxf(float a, float b) {
        return a > b ? a : b;
    }
//static float absf(float a) { return a >= 0.0f ? a : -a; }

    static float clampf(float a, float mn, float mx) {
        return a < mn ? mn : (a > mx ? mx : a);
    }

// Returns 1 if col.rgba is 0.0f,0.0f,0.0f,0.0f, 0 otherwise
    boolean isBlack(float[] col) {
        int r = 0, g = 1, b = 2, a = 3;
        if (col[r] == 0.0f && col[g] == 0.0f && col[b] == 0.0f && col[a] == 0.0f) {
            return true;
        }
        return false;
    }

    void drawWindow(long vg, String title, float x, float y, float w, float h) {
        float cornerRadius = 3.0f;
        byte[] shadowPaint;
        byte[] headerPaint;

        nvgSave(vg);
//	nvgClearState(vg);

        // Window
        nvgBeginPath(vg);
        nvgRoundedRect(vg, x, y, w, h, cornerRadius);
        nvgFillColor(vg, nvgRGBA(28, 30, 34, 192));
//	nvgFillColor(vg, nvgRGBA(0,0,0,128));
        nvgFill(vg);

        // Drop shadow
        shadowPaint = nvgBoxGradient(vg, x, y + 2, w, h, cornerRadius * 2, 10, nvgRGBA(0, 0, 0, 128), nvgRGBA(0, 0, 0, 0));
        nvgBeginPath(vg);
        nvgRect(vg, x - 10, y - 10, w + 20, h + 30);
        nvgRoundedRect(vg, x, y, w, h, cornerRadius);
        nvgPathWinding(vg, NVG_HOLE);
        nvgFillPaint(vg, shadowPaint);
        nvgFill(vg);

        // Header
        headerPaint = nvgLinearGradient(vg, x, y, x, y + 15, nvgRGBA(255, 255, 255, 8), nvgRGBA(0, 0, 0, 16));
        nvgBeginPath(vg);
        nvgRoundedRect(vg, x + 1, y + 1, w - 2, 30, cornerRadius - 1);
        nvgFillPaint(vg, headerPaint);
        nvgFill(vg);
        nvgBeginPath(vg);
        nvgMoveTo(vg, x + 0.5f, y + 0.5f + 30);
        nvgLineTo(vg, x + 0.5f + w - 1, y + 0.5f + 30);
        nvgStrokeColor(vg, nvgRGBA(0, 0, 0, 32));
        nvgStroke(vg);

        nvgFontSize(vg, 18.0f);
        nvgFontFace(vg, fontname);
        nvgTextAlign(vg, NVG_ALIGN_CENTER | NVG_ALIGN_MIDDLE);

        nvgFontBlur(vg, 2);
        nvgFillColor(vg, nvgRGBA(0, 0, 0, 128));
        byte[] b1 = toCstyleBytes(title);
        nvgTextJni(vg, x + w / 2, y + 16 + 1, b1, 0, b1.length);

        nvgFontBlur(vg, 0);
        nvgFillColor(vg, nvgRGBA(220, 220, 220, 160));
        nvgTextJni(vg, x + w / 2, y + 16, b1, 0, b1.length);

        nvgRestore(vg);
    }

    void drawSearchBox(long vg, String text, float x, float y, float w, float h) {
        byte[] bg;
        byte[] icon = new byte[8];
        float cornerRadius = h / 2 - 1;

        // Edit
        bg = nvgBoxGradient(vg, x, y + 1.5f, w, h, h / 2, 5, nvgRGBA(0, 0, 0, 16), nvgRGBA(0, 0, 0, 92));
        nvgBeginPath(vg);
        nvgRoundedRect(vg, x, y, w, h, cornerRadius);
        nvgFillPaint(vg, bg);
        nvgFill(vg);

        /*	nvgBeginPath(vg);
	nvgRoundedRect(vg, x+0.5f,y+0.5f, w-1,h-1, cornerRadius-0.5f);
	nvgStrokeColor(vg, nvgRGBA(0,0,0,48));
	nvgStroke(vg);*/
        nvgFontSize(vg, h * 1.3f);
        nvgFontFace(vg, iconname);
        nvgFillColor(vg, nvgRGBA(255, 255, 255, 64));
        nvgTextAlign(vg, NVG_ALIGN_CENTER | NVG_ALIGN_MIDDLE);
        byte[] b1 = cpToUTF8(ICON_SEARCH);
        nvgTextJni(vg, x + h * 0.55f, y + h * 0.55f, b1, 0, b1.length);

        nvgFontSize(vg, 20.0f);
        nvgFontFace(vg, fontname);
        nvgFillColor(vg, nvgRGBA(255, 255, 255, 32));

        nvgTextAlign(vg, NVG_ALIGN_LEFT | NVG_ALIGN_MIDDLE);
        byte[] b2 = toCstyleBytes(text);
        nvgTextJni(vg, x + h * 1.05f, y + h * 0.5f, b2, 0, b2.length);

        nvgFontSize(vg, h * 1.3f);
        nvgFontFace(vg, iconname);
        nvgFillColor(vg, nvgRGBA(255, 255, 255, 32));
        nvgTextAlign(vg, NVG_ALIGN_CENTER | NVG_ALIGN_MIDDLE);
        byte[] b3 = cpToUTF8(ICON_CIRCLED_CROSS);
        nvgTextJni(vg, x + w - h * 0.55f, y + h * 0.55f, b3, 0, b3.length);
    }

    void drawDropDown(long vg, String text, float x, float y, float w, float h) {
        byte[] bg;

        float cornerRadius = 4.0f;

        bg = nvgLinearGradient(vg, x, y, x, y + h, nvgRGBA(255, 255, 255, 16), nvgRGBA(0, 0, 0, 16));
        nvgBeginPath(vg);
        nvgRoundedRect(vg, x + 1, y + 1, w - 2, h - 2, cornerRadius - 1);
        nvgFillPaint(vg, bg);
        nvgFill(vg);

        nvgBeginPath(vg);
        nvgRoundedRect(vg, x + 0.5f, y + 0.5f, w - 1, h - 1, cornerRadius - 0.5f);
        nvgStrokeColor(vg, nvgRGBA(0, 0, 0, 48));
        nvgStroke(vg);

        nvgFontSize(vg, 20.0f);
        nvgFontFace(vg, fontname);
        nvgFillColor(vg, nvgRGBA(255, 255, 255, 160));
        nvgTextAlign(vg, NVG_ALIGN_LEFT | NVG_ALIGN_MIDDLE);
        byte[] b1 = toCstyleBytes(text);
        nvgTextJni(vg, x + h * 0.3f, y + h * 0.5f, b1, 0, b1.length);

        nvgFontSize(vg, h * 1.3f);
        nvgFontFace(vg, iconname);
        nvgFillColor(vg, nvgRGBA(255, 255, 255, 64));
        nvgTextAlign(vg, NVG_ALIGN_CENTER | NVG_ALIGN_MIDDLE);
        byte[] b2 = cpToUTF8(ICON_CHEVRON_RIGHT);
        nvgTextJni(vg, x + w - h * 0.5f, y + h * 0.5f, b2, 0, b2.length);
    }

    void drawLabel(long vg, String text, float x, float y, float w, float h) {
        //NVG_NOTUSED(w);

        nvgFontSize(vg, 18.0f);
        nvgFontFace(vg, fontname);
        nvgFillColor(vg, nvgRGBA(255, 255, 255, 128));

        nvgTextAlign(vg, NVG_ALIGN_LEFT | NVG_ALIGN_MIDDLE);
        byte[] b1 = toCstyleBytes(text);
        nvgTextJni(vg, x, y + h * 0.5f, b1, 0, b1.length);
    }

    void drawEditBoxBase(long vg, float x, float y, float w, float h) {
        byte[] bg;
        // Edit
        bg = nvgBoxGradient(vg, x + 1, y + 1 + 1.5f, w - 2, h - 2, 3, 4, nvgRGBA(255, 255, 255, 32), nvgRGBA(32, 32, 32, 32));
        nvgBeginPath(vg);
        nvgRoundedRect(vg, x + 1, y + 1, w - 2, h - 2, 4 - 1);
        nvgFillPaint(vg, bg);
        nvgFill(vg);

        nvgBeginPath(vg);
        nvgRoundedRect(vg, x + 0.5f, y + 0.5f, w - 1, h - 1, 4 - 0.5f);
        nvgStrokeColor(vg, nvgRGBA(0, 0, 0, 48));
        nvgStroke(vg);
    }

    void drawEditBox(long vg, String text, float x, float y, float w, float h) {

        drawEditBoxBase(vg, x, y, w, h);

        nvgFontSize(vg, 20.0f);
        nvgFontFace(vg, fontname);
        nvgFillColor(vg, nvgRGBA(255, 255, 255, 64));
        nvgTextAlign(vg, NVG_ALIGN_LEFT | NVG_ALIGN_MIDDLE);
        byte[] b1 = toCstyleBytes(text);
        nvgTextJni(vg, x + h * 0.3f, y + h * 0.5f, b1, 0, b1.length);
    }

    void drawEditBoxNum(long vg,
            String text, String units, float x, float y, float w, float h) {
        float uw;

        drawEditBoxBase(vg, x, y, w, h);

        byte[] b1 = toCstyleBytes(units);
        uw = nvgTextBoundsJni(vg, 0f, 0f, b1, 0, b1.length, null);

        nvgFontSize(vg, 18.0f);
        nvgFontFace(vg, fontname);
        nvgFillColor(vg, nvgRGBA(255, 255, 255, 64));
        nvgTextAlign(vg, NVG_ALIGN_RIGHT | NVG_ALIGN_MIDDLE);
        nvgTextJni(vg, x + w - h * 0.3f, y + h * 0.5f, b1, 0, b1.length);

        nvgFontSize(vg, 20.0f);
        nvgFontFace(vg, fontname);
        nvgFillColor(vg, nvgRGBA(255, 255, 255, 128));
        nvgTextAlign(vg, NVG_ALIGN_RIGHT | NVG_ALIGN_MIDDLE);
        byte[] b2 = toCstyleBytes(text);
        nvgTextJni(vg, x + w - uw - h * 0.5f, y + h * 0.5f, b2, 0, b2.length);
    }

    void drawCheckBox(long vg, String text, float x, float y, float w, float h) {
        byte[] bg;

        //NVG_NOTUSED(w);
        nvgFontSize(vg, 18.0f);
        nvgFontFace(vg, fontname);
        nvgFillColor(vg, nvgRGBA(255, 255, 255, 160));

        nvgTextAlign(vg, NVG_ALIGN_LEFT | NVG_ALIGN_MIDDLE);
        byte[] b2 = toCstyleBytes(text);
        nvgTextJni(vg, x + 28, y + h * 0.5f, b2, 0, b2.length);

        bg = nvgBoxGradient(vg, x + 1, y + (int) (h * 0.5f) - 9 + 1, 18, 18, 3, 3, nvgRGBA(0, 0, 0, 32), nvgRGBA(0, 0, 0, 92));
        nvgBeginPath(vg);
        nvgRoundedRect(vg, x + 1, y + (int) (h * 0.5f) - 9, 18, 18, 3);
        nvgFillPaint(vg, bg);
        nvgFill(vg);

        nvgFontSize(vg, 40);
        nvgFontFace(vg, iconname);
        nvgFillColor(vg, nvgRGBA(255, 255, 255, 128));
        nvgTextAlign(vg, NVG_ALIGN_CENTER | NVG_ALIGN_MIDDLE);
        byte[] b3 = cpToUTF8(ICON_CHECK);
        nvgTextJni(vg, x + 9 + 2, y + h * 0.5f, b3, 0, b3.length);
    }

    void drawButton(long vg, int preicon, String text, float x, float y, float w, float h, float[] col) {
        byte[] bg;

        float cornerRadius = 4.0f;
        float tw = 0, iw = 0;

        bg = nvgLinearGradient(vg, x, y, x, y + h, nvgRGBA(255, 255, 255, isBlack(col) ? 16 : 32), nvgRGBA(0, 0, 0, isBlack(col) ? 16 : 32));
        nvgBeginPath(vg);
        nvgRoundedRect(vg, x + 1, y + 1, w - 2, h - 2, cornerRadius - 1);
        if (!isBlack(col)) {
            nvgFillColor(vg, col);
            nvgFill(vg);
        }
        nvgFillPaint(vg, bg);
        nvgFill(vg);

        nvgBeginPath(vg);
        nvgRoundedRect(vg, x + 0.5f, y + 0.5f, w - 1, h - 1, cornerRadius - 0.5f);
        nvgStrokeColor(vg, nvgRGBA(0, 0, 0, 48));
        nvgStroke(vg);

        nvgFontSize(vg, 20.0f);
        nvgFontFace(vg, fontname);
        byte[] b2 = toCstyleBytes(text);
        tw = nvgTextBoundsJni(vg, 0, 0, b2, 0, b2.length, null);
        byte[] b3 = cpToUTF8(preicon);
        if (preicon != 0) {
            nvgFontSize(vg, h * 1.3f);
            nvgFontFace(vg, iconname);
            iw = nvgTextBoundsJni(vg, 0, 0, b3, 0, b3.length, null);
            iw += h * 0.15f;
        }

        if (preicon != 0) {
            nvgFontSize(vg, h * 1.3f);
            nvgFontFace(vg, iconname);
            nvgFillColor(vg, nvgRGBA(255, 255, 255, 96));
            nvgTextAlign(vg, NVG_ALIGN_LEFT | NVG_ALIGN_MIDDLE);
            nvgTextJni(vg, x + w * 0.5f - tw * 0.5f - iw * 0.75f, y + h * 0.5f, b3, 0, b3.length);
        }

        nvgFontSize(vg, 20.0f);
        nvgFontFace(vg, fontname);
        nvgTextAlign(vg, NVG_ALIGN_LEFT | NVG_ALIGN_MIDDLE);
        nvgFillColor(vg, nvgRGBA(0, 0, 0, 160));
        nvgTextJni(vg, x + w * 0.5f - tw * 0.5f + iw * 0.25f, y + h * 0.5f - 1, b2, 0, b2.length);
        nvgFillColor(vg, nvgRGBA(255, 255, 255, 160));
        nvgTextJni(vg, x + w * 0.5f - tw * 0.5f + iw * 0.25f, y + h * 0.5f, b2, 0, b2.length);
    }

    void drawSlider(long vg, float pos, float x, float y, float w, float h) {
        byte[] bg, knob;
        float cy = y + (int) (h * 0.5f);
        float kr = (int) (h * 0.25f);

        nvgSave(vg);
//	nvgClearState(vg);

        // Slot
        bg = nvgBoxGradient(vg, x, cy - 2 + 1, w, 4, 2, 2, nvgRGBA(0, 0, 0, 32), nvgRGBA(0, 0, 0, 128));
        nvgBeginPath(vg);
        nvgRoundedRect(vg, x, cy - 2, w, 4, 2);
        nvgFillPaint(vg, bg);
        nvgFill(vg);

        // Knob Shadow
        bg = nvgRadialGradient(vg, x + (int) (pos * w), cy + 1, kr - 3, kr + 3, nvgRGBA(0, 0, 0, 64), nvgRGBA(0, 0, 0, 0));
        nvgBeginPath(vg);
        nvgRect(vg, x + (int) (pos * w) - kr - 5, cy - kr - 5, kr * 2 + 5 + 5, kr * 2 + 5 + 5 + 3);
        nvgCircle(vg, x + (int) (pos * w), cy, kr);
        nvgPathWinding(vg, NVG_HOLE);
        nvgFillPaint(vg, bg);
        nvgFill(vg);

        // Knob
        knob = nvgLinearGradient(vg, x, cy - kr, x, cy + kr, nvgRGBA(255, 255, 255, 16), nvgRGBA(0, 0, 0, 16));
        nvgBeginPath(vg);
        nvgCircle(vg, x + (int) (pos * w), cy, kr - 1);
        nvgFillColor(vg, nvgRGBA(40, 43, 48, 255));
        nvgFill(vg);
        nvgFillPaint(vg, knob);
        nvgFill(vg);

        nvgBeginPath(vg);
        nvgCircle(vg, x + (int) (pos * w), cy, kr - 0.5f);
        nvgStrokeColor(vg, nvgRGBA(0, 0, 0, 92));
        nvgStroke(vg);

        nvgRestore(vg);
    }

    void drawEyes(long vg, float x, float y, float w, float h, float mx, float my, float t) {
        byte[] gloss, bg;
        float ex = w * 0.23f;
        float ey = h * 0.5f;
        float lx = x + ex;
        float ly = y + ey;
        float rx = x + w - ex;
        float ry = y + ey;
        float dx, dy, d;
        float br = (ex < ey ? ex : ey) * 0.5f;
        float blink = (float) (1 - pow(sinf(t * 0.5f), 200) * 0.8f);

        bg = nvgLinearGradient(vg, x, y + h * 0.5f, x + w * 0.1f, y + h, nvgRGBA(0, 0, 0, 32), nvgRGBA(0, 0, 0, 16));
        nvgBeginPath(vg);
        nvgEllipse(vg, lx + 3.0f, ly + 16.0f, ex, ey);
        nvgEllipse(vg, rx + 3.0f, ry + 16.0f, ex, ey);
        nvgFillPaint(vg, bg);
        nvgFill(vg);

        bg = nvgLinearGradient(vg, x, y + h * 0.25f, x + w * 0.1f, y + h, nvgRGBA(220, 220, 220, 255), nvgRGBA(128, 128, 128, 255));
        nvgBeginPath(vg);
        nvgEllipse(vg, lx, ly, ex, ey);
        nvgEllipse(vg, rx, ry, ex, ey);
        nvgFillPaint(vg, bg);
        nvgFill(vg);

        dx = (mx - rx) / (ex * 10);
        dy = (my - ry) / (ey * 10);
        d = sqrtf(dx * dx + dy * dy);
        if (d > 1.0f) {
            dx /= d;
            dy /= d;
        }
        dx *= ex * 0.4f;
        dy *= ey * 0.5f;
        nvgBeginPath(vg);
        nvgEllipse(vg, lx + dx, ly + dy + ey * 0.25f * (1 - blink), br, br * blink);
        nvgFillColor(vg, nvgRGBA(32, 32, 32, 255));
        nvgFill(vg);

        dx = (mx - rx) / (ex * 10);
        dy = (my - ry) / (ey * 10);
        d = sqrtf(dx * dx + dy * dy);
        if (d > 1.0f) {
            dx /= d;
            dy /= d;
        }
        dx *= ex * 0.4f;
        dy *= ey * 0.5f;
        nvgBeginPath(vg);
        nvgEllipse(vg, rx + dx, ry + dy + ey * 0.25f * (1 - blink), br, br * blink);
        nvgFillColor(vg, nvgRGBA(32, 32, 32, 255));
        nvgFill(vg);

        gloss = nvgRadialGradient(vg, lx - ex * 0.25f, ly - ey * 0.5f, ex * 0.1f, ex * 0.75f, nvgRGBA(255, 255, 255, 128), nvgRGBA(255, 255, 255, 0));
        nvgBeginPath(vg);
        nvgEllipse(vg, lx, ly, ex, ey);
        nvgFillPaint(vg, gloss);
        nvgFill(vg);

        gloss = nvgRadialGradient(vg, rx - ex * 0.25f, ry - ey * 0.5f, ex * 0.1f, ex * 0.75f, nvgRGBA(255, 255, 255, 128), nvgRGBA(255, 255, 255, 0));
        nvgBeginPath(vg);
        nvgEllipse(vg, rx, ry, ex, ey);
        nvgFillPaint(vg, gloss);
        nvgFill(vg);
    }

    void drawGraph(long vg, float x, float y, float w, float h, float t) {
        byte[] bg;
        float[] samples = new float[6];
        float[] sx = new float[6], sy = new float[6];
        float dx = w / 5.0f;
        int i;

        samples[0] = (1 + sinf(t * 1.2345f + cosf(t * 0.33457f) * 0.44f)) * 0.5f;
        samples[1] = (1 + sinf(t * 0.68363f + cosf(t * 1.3f) * 1.55f)) * 0.5f;
        samples[2] = (1 + sinf(t * 1.1642f + cosf(t * 0.33457f) * 1.24f)) * 0.5f;
        samples[3] = (1 + sinf(t * 0.56345f + cosf(t * 1.63f) * 0.14f)) * 0.5f;
        samples[4] = (1 + sinf(t * 1.6245f + cosf(t * 0.254f) * 0.3f)) * 0.5f;
        samples[5] = (1 + sinf(t * 0.345f + cosf(t * 0.03f) * 0.6f)) * 0.5f;

        for (i = 0; i < 6; i++) {
            sx[i] = x + i * dx;
            sy[i] = y + h * samples[i] * 0.8f;
        }

        // Graph background
        bg = nvgLinearGradient(vg, x, y, x, y + h, nvgRGBA(0, 160, 192, 0), nvgRGBA(0, 160, 192, 64));
        nvgBeginPath(vg);
        nvgMoveTo(vg, sx[0], sy[0]);
        for (i = 1; i < 6; i++) {
            nvgBezierTo(vg, sx[i - 1] + dx * 0.5f, sy[i - 1], sx[i] - dx * 0.5f, sy[i], sx[i], sy[i]);
        }
        nvgLineTo(vg, x + w, y + h);
        nvgLineTo(vg, x, y + h);
        nvgFillPaint(vg, bg);
        nvgFill(vg);

        // Graph line
        nvgBeginPath(vg);
        nvgMoveTo(vg, sx[0], sy[0] + 2);
        for (i = 1; i < 6; i++) {
            nvgBezierTo(vg, sx[i - 1] + dx * 0.5f, sy[i - 1] + 2, sx[i] - dx * 0.5f, sy[i] + 2, sx[i], sy[i] + 2);
        }
        nvgStrokeColor(vg, nvgRGBA(0, 0, 0, 32));
        nvgStrokeWidth(vg, 3.0f);
        nvgStroke(vg);

        nvgBeginPath(vg);
        nvgMoveTo(vg, sx[0], sy[0]);
        for (i = 1; i < 6; i++) {
            nvgBezierTo(vg, sx[i - 1] + dx * 0.5f, sy[i - 1], sx[i] - dx * 0.5f, sy[i], sx[i], sy[i]);
        }
        nvgStrokeColor(vg, nvgRGBA(0, 160, 192, 255));
        nvgStrokeWidth(vg, 3.0f);
        nvgStroke(vg);

        // Graph sample pos
        for (i = 0; i < 6; i++) {
            bg = nvgRadialGradient(vg, sx[i], sy[i] + 2, 3.0f, 8.0f, nvgRGBA(0, 0, 0, 32), nvgRGBA(0, 0, 0, 0));
            nvgBeginPath(vg);
            nvgRect(vg, sx[i] - 10, sy[i] - 10 + 2, 20, 20);
            nvgFillPaint(vg, bg);
            nvgFill(vg);
        }

        nvgBeginPath(vg);
        for (i = 0; i < 6; i++) {
            nvgCircle(vg, sx[i], sy[i], 4.0f);
        }
        nvgFillColor(vg, nvgRGBA(0, 160, 192, 255));
        nvgFill(vg);
        nvgBeginPath(vg);
        for (i = 0; i < 6; i++) {
            nvgCircle(vg, sx[i], sy[i], 2.0f);
        }
        nvgFillColor(vg, nvgRGBA(220, 220, 220, 255));
        nvgFill(vg);

        nvgStrokeWidth(vg, 1.0f);
    }

    void drawSpinner(long vg, float cx, float cy, float r, float t) {
        float a0 = 0.0f + t * 6;
        float a1 = NVG_PI + t * 6;
        float r0 = r;
        float r1 = r * 0.75f;
        float ax, ay, bx, by;
        byte[] paint;

        nvgSave(vg);

        nvgBeginPath(vg);
        nvgArc(vg, cx, cy, r0, a0, a1, NVG_CW);
        nvgArc(vg, cx, cy, r1, a1, a0, NVG_CCW);
        nvgClosePath(vg);
        ax = cx + cosf(a0) * (r0 + r1) * 0.5f;
        ay = cy + sinf(a0) * (r0 + r1) * 0.5f;
        bx = cx + cosf(a1) * (r0 + r1) * 0.5f;
        by = cy + sinf(a1) * (r0 + r1) * 0.5f;
        paint = nvgLinearGradient(vg, ax, ay, bx, by, nvgRGBA(0, 0, 0, 0), nvgRGBA(0, 0, 0, 128));
        nvgFillPaint(vg, paint);
        nvgFill(vg);

        nvgRestore(vg);
    }

    void drawThumbnails(long vg, float x, float y, float w, float h, int[] images, float t) {
        int nimages = images.length;
        float cornerRadius = 3.0f;
        byte[] shadowPaint, imgPaint, fadePaint;
        float ix, iy, iw, ih;
        float thumb = 60.0f;
        float arry = 30.5f;
        int[] imgw = {0}, imgh = {0};
        float stackh = (nimages / 2) * (thumb + 10) + 10;
        int i;
        float u = (1 + cosf(t * 0.5f)) * 0.5f;
        float u2 = (1 - cosf(t * 0.2f)) * 0.5f;
        float scrollh, dv;

        nvgSave(vg);
//	nvgClearState(vg);

        // Drop shadow
        shadowPaint = nvgBoxGradient(vg, x, y + 4, w, h, cornerRadius * 2, 20, nvgRGBA(0, 0, 0, 128), nvgRGBA(0, 0, 0, 0));
        nvgBeginPath(vg);
        nvgRect(vg, x - 10, y - 10, w + 20, h + 30);
        nvgRoundedRect(vg, x, y, w, h, cornerRadius);
        nvgPathWinding(vg, NVG_HOLE);
        nvgFillPaint(vg, shadowPaint);
        nvgFill(vg);

        // Window
        nvgBeginPath(vg);
        nvgRoundedRect(vg, x, y, w, h, cornerRadius);
        nvgMoveTo(vg, x - 10, y + arry);
        nvgLineTo(vg, x + 1, y + arry - 11);
        nvgLineTo(vg, x + 1, y + arry + 11);
        nvgFillColor(vg, nvgRGBA(200, 200, 200, 255));
        nvgFill(vg);

        nvgSave(vg);
        nvgScissor(vg, x, y, w, h);
        nvgTranslate(vg, 0, -(stackh - h) * u);

        dv = 1.0f / (float) (nimages - 1);

        for (i = 0; i < nimages; i++) {
            float tx, ty, v, a;
            tx = x + 10;
            ty = y + 10;
            tx += (i % 2) * (thumb + 10);
            ty += (i / 2) * (thumb + 10);
            nvgImageSize(vg, images[i], imgw, imgh);
            if (imgw[0] < imgh[0]) {
                iw = thumb;
                ih = iw * (float) imgh[0] / (float) imgw[0];
                ix = 0;
                iy = -(ih - thumb) * 0.5f;
            } else {
                ih = thumb;
                iw = ih * (float) imgw[0] / (float) imgh[0];
                ix = -(iw - thumb) * 0.5f;
                iy = 0;
            }

            v = i * dv;
            a = clampf((u2 - v) / dv, 0, 1);

            if (a < 1.0f) {
                drawSpinner(vg, tx + thumb / 2, ty + thumb / 2, thumb * 0.25f, t);
            }

            imgPaint = nvgImagePattern(vg, tx + ix, ty + iy, iw, ih, 0.0f / 180.0f * NVG_PI, images[i], a);
            nvgBeginPath(vg);
            nvgRoundedRect(vg, tx, ty, thumb, thumb, 5);
            nvgFillPaint(vg, imgPaint);
            nvgFill(vg);

            shadowPaint = nvgBoxGradient(vg, tx - 1, ty, thumb + 2, thumb + 2, 5, 3, nvgRGBA(0, 0, 0, 128), nvgRGBA(0, 0, 0, 0));
            nvgBeginPath(vg);
            nvgRect(vg, tx - 5, ty - 5, thumb + 10, thumb + 10);
            nvgRoundedRect(vg, tx, ty, thumb, thumb, 6);
            nvgPathWinding(vg, NVG_HOLE);
            nvgFillPaint(vg, shadowPaint);
            nvgFill(vg);

            nvgBeginPath(vg);
            nvgRoundedRect(vg, tx + 0.5f, ty + 0.5f, thumb - 1, thumb - 1, 4 - 0.5f);
            nvgStrokeWidth(vg, 1.0f);
            nvgStrokeColor(vg, nvgRGBA(255, 255, 255, 192));
            nvgStroke(vg);
        }
        nvgRestore(vg);

        // Hide fades
        fadePaint = nvgLinearGradient(vg, x, y, x, y + 6, nvgRGBA(200, 200, 200, 255), nvgRGBA(200, 200, 200, 0));
        nvgBeginPath(vg);
        nvgRect(vg, x + 4, y, w - 8, 6);
        nvgFillPaint(vg, fadePaint);
        nvgFill(vg);

        fadePaint = nvgLinearGradient(vg, x, y + h, x, y + h - 6, nvgRGBA(200, 200, 200, 255), nvgRGBA(200, 200, 200, 0));
        nvgBeginPath(vg);
        nvgRect(vg, x + 4, y + h - 6, w - 8, 6);
        nvgFillPaint(vg, fadePaint);
        nvgFill(vg);

        // Scroll bar
        shadowPaint = nvgBoxGradient(vg, x + w - 12 + 1, y + 4 + 1, 8, h - 8, 3, 4, nvgRGBA(0, 0, 0, 32), nvgRGBA(0, 0, 0, 92));
        nvgBeginPath(vg);
        nvgRoundedRect(vg, x + w - 12, y + 4, 8, h - 8, 3);
        nvgFillPaint(vg, shadowPaint);
//	nvgFillColor(vg, nvgRGBA(255,0,0,128));
        nvgFill(vg);

        scrollh = (h / stackh) * (h - 8);
        if (scrollh > h - 8) {
            scrollh = h - 8;
        }
        shadowPaint = nvgBoxGradient(vg, x + w - 12 - 1, y + 4 + (h - 8 - scrollh) * u - 1, 8, scrollh, 3, 4, nvgRGBA(220, 220, 220, 255), nvgRGBA(128, 128, 128, 255));
        nvgBeginPath(vg);
        nvgRoundedRect(vg, x + w - 12 + 1, y + 4 + 1 + (h - 8 - scrollh) * u, 8 - 2, scrollh - 2, 2);
        nvgFillPaint(vg, shadowPaint);
//	nvgFillColor(vg, nvgRGBA(0,0,0,128));
        nvgFill(vg);

        nvgRestore(vg);
    }

    void drawColorwheel(long vg, float x, float y, float w, float h, float t) {
        int i;
        float r0, r1, ax, ay, bx, by, cx, cy, aeps, r;
        float hue = sinf(t * 0.12f);
        byte[] paint;

        nvgSave(vg);

        /*	nvgBeginPath(vg);
	nvgRect(vg, x,y,w,h);
	nvgFillColor(vg, nvgRGBA(255,0,0,128));
	nvgFill(vg);*/
        cx = x + w * 0.5f;
        cy = y + h * 0.5f;
        r1 = (w < h ? w : h) * 0.5f - 5.0f;
        r0 = r1 - 20.0f;
        aeps = 0.5f / r1;	// half a pixel arc length in radians (2pi cancels out).

        for (i = 0; i < 6; i++) {
            float a0 = (float) i / 6.0f * NVG_PI * 2.0f - aeps;
            float a1 = (float) (i + 1.0f) / 6.0f * NVG_PI * 2.0f + aeps;
            nvgBeginPath(vg);
            nvgArc(vg, cx, cy, r0, a0, a1, NVG_CW);
            nvgArc(vg, cx, cy, r1, a1, a0, NVG_CCW);
            nvgClosePath(vg);
            ax = cx + cosf(a0) * (r0 + r1) * 0.5f;
            ay = cy + sinf(a0) * (r0 + r1) * 0.5f;
            bx = cx + cosf(a1) * (r0 + r1) * 0.5f;
            by = cy + sinf(a1) * (r0 + r1) * 0.5f;
            paint = nvgLinearGradient(vg, ax, ay, bx, by, nvgHSLA(a0 / (NVG_PI * 2), 1.0f, 0.55f, (byte) 255), nvgHSLA(a1 / (NVG_PI * 2), 1.0f, 0.55f, (byte) 255));
            nvgFillPaint(vg, paint);
            nvgFill(vg);
        }

        nvgBeginPath(vg);
        nvgCircle(vg, cx, cy, r0 - 0.5f);
        nvgCircle(vg, cx, cy, r1 + 0.5f);
        nvgStrokeColor(vg, nvgRGBA(0, 0, 0, 64));
        nvgStrokeWidth(vg, 1.0f);
        nvgStroke(vg);

        // Selector
        nvgSave(vg);
        nvgTranslate(vg, cx, cy);
        nvgRotate(vg, hue * NVG_PI * 2);

        // Marker on
        nvgStrokeWidth(vg, 2.0f);
        nvgBeginPath(vg);
        nvgRect(vg, r0 - 1, -3, r1 - r0 + 2, 6);
        nvgStrokeColor(vg, nvgRGBA(255, 255, 255, 192));
        nvgStroke(vg);

        paint = nvgBoxGradient(vg, r0 - 3, -5, r1 - r0 + 6, 10, 2, 4, nvgRGBA(0, 0, 0, 128), nvgRGBA(0, 0, 0, 0));
        nvgBeginPath(vg);
        nvgRect(vg, r0 - 2 - 10, -4 - 10, r1 - r0 + 4 + 20, 8 + 20);
        nvgRect(vg, r0 - 2, -4, r1 - r0 + 4, 8);
        nvgPathWinding(vg, NVG_HOLE);
        nvgFillPaint(vg, paint);
        nvgFill(vg);

        // Center triangle
        r = r0 - 6;
        ax = cosf(120.0f / 180.0f * NVG_PI) * r;
        ay = sinf(120.0f / 180.0f * NVG_PI) * r;
        bx = cosf(-120.0f / 180.0f * NVG_PI) * r;
        by = sinf(-120.0f / 180.0f * NVG_PI) * r;
        nvgBeginPath(vg);
        nvgMoveTo(vg, r, 0);
        nvgLineTo(vg, ax, ay);
        nvgLineTo(vg, bx, by);
        nvgClosePath(vg);
        paint = nvgLinearGradient(vg, r, 0, ax, ay, nvgHSLA(hue, 1.0f, 0.5f, (byte) 255), nvgRGBA(255, 255, 255, 255));
        nvgFillPaint(vg, paint);
        nvgFill(vg);
        paint = nvgLinearGradient(vg, (r + ax) * 0.5f, (0 + ay) * 0.5f, bx, by, nvgRGBA(0, 0, 0, 0), nvgRGBA(0, 0, 0, 255));
        nvgFillPaint(vg, paint);
        nvgFill(vg);
        nvgStrokeColor(vg, nvgRGBA(0, 0, 0, 64));
        nvgStroke(vg);

        // Select circle on triangle
        ax = cosf(120.0f / 180.0f * NVG_PI) * r * 0.3f;
        ay = sinf(120.0f / 180.0f * NVG_PI) * r * 0.4f;
        nvgStrokeWidth(vg, 2.0f);
        nvgBeginPath(vg);
        nvgCircle(vg, ax, ay, 5);
        nvgStrokeColor(vg, nvgRGBA(255, 255, 255, 192));
        nvgStroke(vg);

        paint = nvgRadialGradient(vg, ax, ay, 7, 9, nvgRGBA(0, 0, 0, 64), nvgRGBA(0, 0, 0, 0));
        nvgBeginPath(vg);
        nvgRect(vg, ax - 20, ay - 20, 40, 40);
        nvgCircle(vg, ax, ay, 7);
        nvgPathWinding(vg, NVG_HOLE);
        nvgFillPaint(vg, paint);
        nvgFill(vg);

        nvgRestore(vg);

        nvgRestore(vg);
    }

    void drawLines(long vg, float x, float y, float w, float h, float t) {
        int i, j;
        float pad = 5.0f, s = w / 9.0f - pad * 2;
        float[] pts = new float[4 * 2];
        float fx, fy;
        int[] joins = {NVG_MITER, NVG_ROUND, NVG_BEVEL};
        int[] caps = {NVG_BUTT, NVG_ROUND, NVG_SQUARE};
        //NVG_NOTUSED(h);

        nvgSave(vg);
        pts[0] = -s * 0.25f + cosf(t * 0.3f) * s * 0.5f;
        pts[1] = sinf(t * 0.3f) * s * 0.5f;
        pts[2] = -s * 0.25f;
        pts[3] = 0;
        pts[4] = s * 0.25f;
        pts[5] = 0;
        pts[6] = s * 0.25f + cosf(-t * 0.3f) * s * 0.5f;
        pts[7] = sinf(-t * 0.3f) * s * 0.5f;

        for (i = 0; i < 3; i++) {
            for (j = 0; j < 3; j++) {
                fx = x + s * 0.5f + (i * 3 + j) / 9.0f * w + pad;
                fy = y - s * 0.5f + pad;

                nvgLineCap(vg, caps[i]);
                nvgLineJoin(vg, joins[j]);

                nvgStrokeWidth(vg, s * 0.3f);
                nvgStrokeColor(vg, nvgRGBA(0, 0, 0, 160));
                nvgBeginPath(vg);
                nvgMoveTo(vg, fx + pts[0], fy + pts[1]);
                nvgLineTo(vg, fx + pts[2], fy + pts[3]);
                nvgLineTo(vg, fx + pts[4], fy + pts[5]);
                nvgLineTo(vg, fx + pts[6], fy + pts[7]);
                nvgStroke(vg);

                nvgLineCap(vg, NVG_BUTT);
                nvgLineJoin(vg, NVG_BEVEL);

                nvgStrokeWidth(vg, 1.0f);
                nvgStrokeColor(vg, nvgRGBA(0, 192, 255, 255));
                nvgBeginPath(vg);
                nvgMoveTo(vg, fx + pts[0], fy + pts[1]);
                nvgLineTo(vg, fx + pts[2], fy + pts[3]);
                nvgLineTo(vg, fx + pts[4], fy + pts[5]);
                nvgLineTo(vg, fx + pts[6], fy + pts[7]);
                nvgStroke(vg);
            }
        }

        nvgRestore(vg);
    }
//
//int loadDemoData(long vg, DemoData* data)
//{
//	int i;
//
//	if (vg == null)
//		return -1;
//
//	for (i = 0; i < 12; i++) {
//		char file[128];
//		snprintf(file, 128, "../example/images/image%d.jpg", i+1);
//		data->images[i] = nvgCreateImage(vg, file, 0);
//		if (data->images[i] == 0) {
//			printf("Could not load %s.\n", file);
//			return -1;
//		}
//	}
//
//	data->fontIcons = nvgCreateFont(vg, fontname, "../example/entypo.ttf");
//	if (data->fontIcons == -1) {
//		printf("Could not add font icons.\n");
//		return -1;
//	}
//	data->fontNormal = nvgCreateFont(vg, fontname, "../example/Roboto-Regular.ttf");
//	if (data->fontNormal == -1) {
//		printf("Could not add font italic.\n");
//		return -1;
//	}
//	data->fontBold = nvgCreateFont(vg, fontname, "../example/Roboto-Bold.ttf");
//	if (data->fontBold == -1) {
//		printf("Could not add font bold.\n");
//		return -1;
//	}
//	data->fontEmoji = nvgCreateFont(vg, "emoji", "../example/NotoEmoji-Regular.ttf");
//	if (data->fontEmoji == -1) {
//		printf("Could not add font emoji.\n");
//		return -1;
//	}
//	nvgAddFallbackFontId(vg, data->fontNormal, data->fontEmoji);
//	nvgAddFallbackFontId(vg, data->fontBold, data->fontEmoji);
//
//	return 0;
//}
//
//void freeDemoData(long vg, DemoData* data)
//{
//	int i;
//
//	if (vg == null)
//		return;
//
//	for (i = 0; i < 12; i++)
//		nvgDeleteImage(vg, data->images[i]);
//}

    void drawParagraph(long vg, float x, float y, float width, float height, float mx, float my) {
        int rowCount = 5;
        int posCount = 100;
        long rows = nvgCreateNVGtextRow(rowCount);
        long glyphs = nvgCreateNVGglyphPosition(posCount);
        String text = "单程票;（旅馆等的）单人房间;[复数]（高尔夫球一对一的）二人对抗赛;[常用复数][美国、加拿大英语]未婚（或单身）男子（或女子）\nThis is longer chunk of text.\n  \n  Would have used lorem ipsum but she    was busy jumping over the lazy dog with the fox and all the men who came to the aid of the party.🎉";
        int nrows, i, nglyphs, j, lnum = 0;
        float caretx, px;
        float[] bounds = new float[4];
        float a;
        float gx = 0, gy = 0;
        int gutter = 0;
        //NVG_NOTUSED(height);

        nvgSave(vg);

        nvgFontSize(vg, 18.0f);
        nvgFontFace(vg, fontname);
        nvgTextAlign(vg, NVG_ALIGN_LEFT | NVG_ALIGN_TOP);
        float[] lineh = {0};
        nvgTextMetrics(vg, null, null, lineh);

        // The text break API can be used to fill a large buffer of rows,
        // or to iterate over the text just few lines (or just one) at a time.
        // The "next" variable of the last returned item tells where to continue.
        byte[] text_arr = toCstyleBytes(text);
        long ptr = GToolkit.getArrayDataPtr(text_arr);
        int start = 0;
        int end = text_arr.length;
        while ((nrows = nvgTextBreakLinesJni(vg, text_arr, start, end, width, rows, rowCount)) != 0) {
            for (i = 0; i < nrows; i++) {
                boolean hit = mx > x && mx < (x + width) && my >= y && my < (y + lineh[0]);
                float row_width = nvgNVGtextRow_width(rows, i);
                nvgBeginPath(vg);
                nvgFillColor(vg, nvgRGBA(255, 255, 255, hit ? 64 : 16));
                nvgRect(vg, x, y, row_width, lineh[0]);
                nvgFill(vg);

                int starti = (int) (nvgNVGtextRow_start(rows, i) - ptr);
                int endi = (int) (nvgNVGtextRow_end(rows, i) - ptr);

                nvgFillColor(vg, nvgRGBA(255, 255, 255, 255));
                nvgTextJni(vg, x, y, text_arr, starti, endi);

                if (hit) {
                    caretx = (mx < x + row_width / 2) ? x : x + row_width;
                    px = x;
                    nglyphs = nvgTextGlyphPositionsJni(vg, x, y, text_arr, starti, endi, glyphs, posCount);
                    for (j = 0; j < nglyphs; j++) {
                        float x0 = nvgNVGglyphPosition_x(glyphs, j);
                        float x1 = (j + 1 < nglyphs) ? nvgNVGglyphPosition_x(glyphs, j + 1) : x + row_width;
                        float gxx = x0 * 0.3f + x1 * 0.7f;
                        if (mx >= px && mx < gxx) {
                            caretx = nvgNVGglyphPosition_x(glyphs, j);
                        }
                        px = gxx;
                    }
                    nvgBeginPath(vg);
                    nvgFillColor(vg, nvgRGBA(255, 192, 0, 255));
                    nvgRect(vg, caretx, y, 1, lineh[0]);
                    nvgFill(vg);

                    gutter = lnum + 1;
                    gx = x - 10;
                    gy = y + lineh[0] / 2;
                }
                lnum++;
                y += lineh[0];
            }
            // Keep going...
            
            long next = nvgNVGtextRow_next(rows, nrows - 1);
            start = (int) (next - ptr);
        }
        if (gutter != 0) {
            byte[] gutter_arr = toCstyleBytes("" + gutter);
            nvgFontSize(vg, 13.0f);
            nvgTextAlign(vg, NVG_ALIGN_RIGHT | NVG_ALIGN_MIDDLE);

            nvgTextBoundsJni(vg, gx, gy, gutter_arr, 0, gutter_arr.length, bounds);

            nvgBeginPath(vg);
            nvgFillColor(vg, nvgRGBA(255, 192, 0, 255));
            nvgRoundedRect(vg, (int) bounds[0] - 4, (int) bounds[1] - 2, (int) (bounds[2] - bounds[0]) + 8, (int) (bounds[3] - bounds[1]) + 4, ((int) (bounds[3] - bounds[1]) + 4) / 2 - 1);
            nvgFill(vg);

            nvgFillColor(vg, nvgRGBA(32, 32, 32, 255));
            nvgTextJni(vg, gx, gy, gutter_arr, 0, gutter_arr.length);
        }

        y += 20.0f;

        nvgFontSize(vg, 13.0f);
        nvgTextAlign(vg, NVG_ALIGN_LEFT | NVG_ALIGN_TOP);
        nvgTextLineHeight(vg, 1.2f);

        byte[] b2 = toCstyleBytes("Hover your mouse over the text to see calculated caret position.");
        nvgTextBoxBoundsJni(vg, x, y, 150, b2, 0, b2.length, bounds);

        // Fade the tooltip out when close to it.
        gx = Math.abs((mx - (bounds[0] + bounds[2]) * 0.5f) / (bounds[0] - bounds[2]));
        gy = Math.abs((my - (bounds[1] + bounds[3]) * 0.5f) / (bounds[1] - bounds[3]));
        a = maxf(gx, gy) - 0.5f;
        a = clampf(a, 0, 1);
        nvgGlobalAlpha(vg, a);

        nvgBeginPath(vg);
        nvgFillColor(vg, nvgRGBA(220, 220, 220, 255));
        nvgRoundedRect(vg, bounds[0] - 2, bounds[1] - 2, (int) (bounds[2] - bounds[0]) + 4, (int) (bounds[3] - bounds[1]) + 4, 3);
        px = (int) ((bounds[2] + bounds[0]) / 2);
        nvgMoveTo(vg, px, bounds[1] - 10);
        nvgLineTo(vg, px + 7, bounds[1] + 1);
        nvgLineTo(vg, px - 7, bounds[1] + 1);
        nvgFill(vg);

        nvgFillColor(vg, nvgRGBA(0, 0, 0, 220));
        nvgTextBoxJni(vg, x, y, 150, b2, 0, b2.length);

        nvgRestore(vg);
        
        nvgDeleteNVGtextRow(rows);
        nvgDeleteNVGglyphPosition(glyphs);
    }

    void drawWidths(long vg, float x, float y, float width) {
        int i;

        nvgSave(vg);

        nvgStrokeColor(vg, nvgRGBA(0, 0, 0, 255));

        for (i = 0; i < 20; i++) {
            float w = (i + 0.5f) * 0.1f;
            nvgStrokeWidth(vg, w);
            nvgBeginPath(vg);
            nvgMoveTo(vg, x, y);
            nvgLineTo(vg, x + width, y + width * 0.3f);
            nvgStroke(vg);
            y += 10;
        }

        nvgRestore(vg);
    }

    void drawCaps(long vg, float x, float y, float width) {
        int i;
        int[] caps = {NVG_BUTT, NVG_ROUND, NVG_SQUARE};
        float lineWidth = 8.0f;

        nvgSave(vg);

        nvgBeginPath(vg);
        nvgRect(vg, x - lineWidth / 2, y, width + lineWidth, 40);
        nvgFillColor(vg, nvgRGBA(255, 255, 255, 32));
        nvgFill(vg);

        nvgBeginPath(vg);
        nvgRect(vg, x, y, width, 40);
        nvgFillColor(vg, nvgRGBA(255, 255, 255, 32));
        nvgFill(vg);

        nvgStrokeWidth(vg, lineWidth);
        for (i = 0; i < 3; i++) {
            nvgLineCap(vg, caps[i]);
            nvgStrokeColor(vg, nvgRGBA(0, 0, 0, 255));
            nvgBeginPath(vg);
            nvgMoveTo(vg, x, y + i * 10 + 5);
            nvgLineTo(vg, x + width, y + i * 10 + 5);
            nvgStroke(vg);
        }

        nvgRestore(vg);
    }

    void drawScissor(long vg, float x, float y, float t) {
        nvgSave(vg);

        // Draw first rect and set scissor to it's area.
        nvgTranslate(vg, x, y);
        nvgRotate(vg, nvgDegToRad(5));
        nvgBeginPath(vg);
        nvgRect(vg, -20, -20, 60, 40);
        nvgFillColor(vg, nvgRGBA(255, 0, 0, 255));
        nvgFill(vg);
        nvgScissor(vg, -20, -20, 60, 40);

        // Draw second rectangle with offset and rotation.
        nvgTranslate(vg, 40, 0);
        nvgRotate(vg, t);

        // Draw the intended second rectangle without any scissoring.
        nvgSave(vg);
        nvgResetScissor(vg);
        nvgBeginPath(vg);
        nvgRect(vg, -20, -10, 60, 30);
        nvgFillColor(vg, nvgRGBA(255, 128, 0, 64));
        nvgFill(vg);
        nvgRestore(vg);

        // Draw second rectangle with combined scissoring.
        nvgIntersectScissor(vg, -20, -10, 60, 30);
        nvgBeginPath(vg);
        nvgRect(vg, -20, -10, 60, 30);
        nvgFillColor(vg, nvgRGBA(255, 128, 0, 255));
        nvgFill(vg);

        nvgRestore(vg);
    }

    void renderDemo(long vg, float mx, float my, float width, float height,
            float t, int blowup) {
        float x, y, popy;

        drawEyes(vg, width - 250, 50, 150, 100, mx, my, t);
        drawParagraph(vg, width - 450, 50, 150, 100, mx, my);
        drawGraph(vg, 0, height / 2, width, height / 2, t);
        drawColorwheel(vg, width - 300, height - 300, 250.0f, 250.0f, t);

        // Line joints
        drawLines(vg, 120, height - 50, 600, 50, t);

        // Line caps
        drawWidths(vg, 10, 50, 30);

        // Line caps
        drawCaps(vg, 10, 300, 30);

        drawScissor(vg, 50, height - 80, t);

        nvgSave(vg);
        if (false) {
            nvgRotate(vg, sinf(t * 0.3f) * 5.0f / 180.0f * NVG_PI);
            nvgScale(vg, 2.0f, 2.0f);
        }

        // Widgets
        drawWindow(vg, "Widgets Stuff", 50, 50, 300, 400);
        x = 60;
        y = 95;
        drawSearchBox(vg, "Search", x, y, 280, 25);
        y += 40;
        drawDropDown(vg, "Effects", x, y, 280, 28);
        popy = y + 14;
        y += 45;

        // Form
        drawLabel(vg, "Login", x, y, 280, 20);
        y += 25;
        drawEditBox(vg, "Email", x, y, 280, 28);
        y += 35;
        drawEditBox(vg, "Password", x, y, 280, 28);
        y += 38;
        drawCheckBox(vg, "Remember me", x, y, 140, 28);
        drawButton(vg, ICON_LOGIN, "Sign in", x + 138, y, 140, 28, nvgRGBA(0, 96, 128, 255));
        y += 45;

        // Slider
        drawLabel(vg, "Diameter", x, y, 280, 20);
        y += 25;
        drawEditBoxNum(vg, "123.00", "px", x + 180, y, 100, 28);
        drawSlider(vg, 0.4f, x, y, 170, 28);
        y += 55;

        drawButton(vg, ICON_TRASH, "Delete", x, y, 160, 28, nvgRGBA(128, 16, 8, 255));
        drawButton(vg, 0, "Cancel", x + 170, y, 110, 28, nvgRGBA(0, 0, 0, 0));

        int[] images = {0, 0, 0, 0, 0, 0};
        // Thumbnails box
        drawThumbnails(vg, 365, popy - 30, 160, 300, images, t);
        nvgRestore(vg);
    }

    static int mini(int a, int b) {
        return a < b ? a : b;
    }

//static void unpremultiplyAlpha(unsigned char* image, int w, int h, int stride)
//{
//	int x,y;
//
//	// Unpremultiply
//	for (y = 0; y < h; y++) {
//		unsigned char *row = &image[y*stride];
//		for (x = 0; x < w; x++) {
//			int r = row[0], g = row[1], b = row[2], a = row[3];
//			if (a != 0) {
//				row[0] = (int)mini(r*255/a, 255);
//				row[1] = (int)mini(g*255/a, 255);
//				row[2] = (int)mini(b*255/a, 255);
//			}
//			row += 4;
//		}
//	}
//
//	// Defringe
//	for (y = 0; y < h; y++) {
//		unsigned char *row = &image[y*stride];
//		for (x = 0; x < w; x++) {
//			int r = 0, g = 0, b = 0, a = row[3], n = 0;
//			if (a == 0) {
//				if (x-1 > 0 && row[-1] != 0) {
//					r += row[-4];
//					g += row[-3];
//					b += row[-2];
//					n++;
//				}
//				if (x+1 < w && row[7] != 0) {
//					r += row[4];
//					g += row[5];
//					b += row[6];
//					n++;
//				}
//				if (y-1 > 0 && row[-stride+3] != 0) {
//					r += row[-stride];
//					g += row[-stride+1];
//					b += row[-stride+2];
//					n++;
//				}
//				if (y+1 < h && row[stride+3] != 0) {
//					r += row[stride];
//					g += row[stride+1];
//					b += row[stride+2];
//					n++;
//				}
//				if (n > 0) {
//					row[0] = r/n;
//					row[1] = g/n;
//					row[2] = b/n;
//				}
//			}
//			row += 4;
//		}
//	}
//}
//static void setAlpha(unsigned char* image, int w, int h, int stride, unsigned char a)
//{
//	int x, y;
//	for (y = 0; y < h; y++) {
//		unsigned char* row = &image[y*stride];
//		for (x = 0; x < w; x++)
//			row[x*4+3] = a;
//	}
//}
//static void flipHorizontal(unsigned char* image, int w, int h, int stride)
//{
//	int i = 0, j = h-1, k;
//	while (i < j) {
//		unsigned char* ri = &image[i * stride];
//		unsigned char* rj = &image[j * stride];
//		for (k = 0; k < w*4; k++) {
//			unsigned char t = ri[k];
//			ri[k] = rj[k];
//			rj[k] = t;
//		}
//		i++;
//		j--;
//	}
//}
//void saveScreenShot(int w, int h, int premult, String name)
//{
//	unsigned char* image = (unsigned char*)malloc(w*h*4);
//	if (image == null)
//		return;
//	glReadPixels(0, 0, w, h, GL_RGBA, GL_UNSIGNED_BYTE, image);
//	if (premult)
//		unpremultiplyAlpha(image, w, h, w*4);
//	else
//		setAlpha(image, w, h, w*4, 255);
//	flipHorizontal(image, w, h, w*4);
// 	stbi_write_png(name, w, h, 4, image, w*4);
// 	free(image);
//}
}
