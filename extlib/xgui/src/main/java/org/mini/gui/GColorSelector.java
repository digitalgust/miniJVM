/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.mini.gui;

import org.mini.glfm.Glfm;

import static org.mini.gui.GToolkit.nvgRGBA;
import static org.mini.nanovg.Nanovg.*;

/**
 * 颜色选择器：色轮 + 内部 HSV 三角 + Alpha 滑条。
 * <p>
 * 外圈环：选择色相 H；内部三角：选择饱和度 S / 明度 V；底部滑条：选择透明度 Alpha。
 * 支持鼠标与触屏拖动选色，也支持通过 {@link #setColorRGBA(float[])} 编程设置当前色
 * (内部做 RGB→HSV 反向换算，把指针定位到对应位置)。
 * <p>
 * 通知约定沿用项目惯例：拖动过程中触发 {@link GObject#doStateChanged} (走 setStateChangeListener)，
 * 抬起时触发 {@link GObject#doAction} (走 setActionListener)。
 *
 * @author gust
 */
public class GColorSelector extends GObject {

    /**
     * these color can't change by user
     */
    public static final float[] RED = new float[]{1.0f, 0.0f, 0.0f, 1.0f};
    public static final float[] GREEN = new float[]{0.0f, 1.0f, 0.0f, 1.0f};
    public static final float[] BLUE = new float[]{0.0f, 0.0f, 1.0f, 1.0f};
    public static final float[] YELLOW = new float[]{1.0f, 1.0f, 0.0f, 1.0f};
    public static final float[] PURPLE = new float[]{1.0f, 0.0f, 1.0f, 1.0f};
    public static final float[] CYAN = new float[]{0.0f, 1.0f, 1.0f, 1.0f};
    public static final float[] WHITE = new float[]{1.0f, 1.0f, 1.0f, 1.0f};
    public static final float[] BLACK = new float[]{0.0f, 0.0f, 0.0f, 1.0f};
    public static final float[] GRAY = new float[]{0.5f, 0.5f, 0.5f, 1.0f};
    public static final float[] TRANSPARENT = new float[]{0.0f, 0.0f, 0.0f, 0.0f};
    public static final float[] RED_HALF = new float[]{1.0f, 0.0f, 0.0f, 0.5f};
    public static final float[] GREEN_HALF = new float[]{0.0f, 1.0f, 0.0f, 0.5f};
    public static final float[] BLUE_HALF = new float[]{0.0f, 0.0f, 1.0f, 0.5f};
    public static final float[] YELLOW_HALF = new float[]{1.0f, 1.0f, 0.0f, 0.5f};
    public static final float[] PURPLE_HALF = new float[]{1.0f, 0.0f, 1.0f, 0.5f};
    public static final float[] CYAN_HALF = new float[]{0.0f, 1.0f, 1.0f, 0.5f};
    public static final float[] WHITE_HALF = new float[]{1.0f, 1.0f, 1.0f, 0.5f};
    public static final float[] BLACK_HALF = new float[]{0.0f, 0.0f, 0.0f, 0.5f};
    public static final float[] GRAY_HALF = new float[]{0.5f, 0.5f, 0.5f, 0.5f};

    //---- HSV + Alpha 当前状态，全部 0~1 ----
    protected float hue = 0f;        //色相: 0=红, 沿外圈顺时针, 对应 atan2 角度 / 2π
    protected float sat = 1f;        //饱和度: 三角形 白(0) -> 纯色(1)
    protected float val = 1f;        //明度:  三角形 黑(0) -> 纯色(1)
    protected float alpha = 1f;      //透明度: 0~1

    //---- 绘制时算出的命中几何，供事件命中测试用 ----
    //色轮中心(组件内绝对坐标)、内外半径
    protected float wheelCx, wheelCy, r0, r1;
    //内三角顶点到中心的距离 (绘制时 = r0-6), 反解 SV 时需用同一尺度
    protected float triR;
    //Alpha 滑条命中矩形(组件内绝对坐标 left,top,w,h)
    protected float alphaRectLeft, alphaRectTop, alphaRectW, alphaRectH;
    //Alpha 滑条高度占组件高度的比例
    protected static final float ALPHA_BAR_H_RATIO = 0.08f;
    //色轮与 Alpha 条之间的间隔
    protected static final float ALPHA_BAR_GAP = 6f;

    //---- 拖动状态机 ----
    protected boolean touched;
    protected static final byte TARGET_NONE = 0, TARGET_HUE = 1, TARGET_SV = 2, TARGET_ALPHA = 3;
    protected byte dragTarget = TARGET_NONE;

    public GColorSelector(GForm form) {
        this(form, 0f, 0f, 0f, 1f, 1f);
    }

    public GColorSelector(GForm form, float pos, float left, float top, float width, float height) {
        super(form);
        //pos 历史上表示初始角度，这里折算成 hue, 保持构造签名兼容
        this.hue = pos - (float) Math.floor(pos);
        setLocation(left, top);
        setSize(width, height);
    }

    /**
     * 0.0f - 1.0f value of r,g,b,a
     *
     * @param r
     * @param g
     * @param b
     * @param a
     * @return
     */
    public static float[] getColorRGBA(float r, float g, float b, float a) {
        return new float[]{r, g, b, a};
    }

    /**
     * 0 - 255 value of r,g,b,a
     *
     * @param r
     * @param g
     * @param b
     * @param a
     * @return
     */
    public static float[] getColorRGBA(int r, int g, int b, int a) {
        return new float[]{r / 255f, g / 255f, b / 255f, a / 255f};
    }

    public static float[] getColorRGBA(int rgba) {
        int r = (rgba >> 24) & 0xff;
        int g = (rgba >> 16) & 0xff;
        int b = (rgba >> 8) & 0xff;
        int a = (rgba) & 0xff;
        return new float[]{r / 255f, g / 255f, b / 255f, a / 255f};
    }

    public static int getHexColorARGB(float[] color) {
        int r = (int) (color[0] * 255);
        int g = (int) (color[1] * 255);
        int b = (int) (color[2] * 255);
        int a = (int) (color[3] * 255);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    public static float[] copyColor(float[] color) {
        if (color == null) return new float[4];
        return new float[]{color[0], color[1], color[2], color[3]};
    }

    public static void copyColor(float[] src, float[] dest) {
        if (src == null || dest == null || src.length != 4 || dest.length != 4) return;
        System.arraycopy(src, 0, dest, 0, 4);
    }

    //======================================================================================
    //  对外取色 / 设色 API
    //======================================================================================

    /**
     * 返回当前选中颜色 (每次返回新数组, 修改不影响内部状态)。
     */
    public float[] getColor() {
        return getColorRGBA();
    }

    /**
     * 返回当前选中颜色 RGBA (0~1)。
     */
    public float[] getColorRGBA() {
        float[] rgb = hsvToRgb(hue, sat, val);
        return new float[]{rgb[0], rgb[1], rgb[2], alpha};
    }

    /**
     * 编程设置当前颜色: 内部做 RGB->HSV 反向换算, 把指针定位到对应位置, 并刷新界面。
     * 灰度色 (饱和度为 0, HSV 的 H 无定义) 时保留当前 hue 不变, 只更新 S/V/A。
     */
    public void setColorRGBA(float[] color) {
        if (color == null || color.length < 4) return;
        float r = clamp01(color[0]);
        float g = clamp01(color[1]);
        float b = clamp01(color[2]);
        float a = clamp01(color[3]);
        float[] hsv = rgbToHsv(r, g, b);
        //S==0 表示灰色, H 无意义, 保留原 hue 避免指针乱跳
        if (hsv[1] > 0f) {
            this.hue = hsv[0];
        }
        this.sat = hsv[1];
        this.val = hsv[2];
        this.alpha = a;
        GForm.flush();
    }

    public float getHue() { return hue; }

    public float getSaturation() { return sat; }

    public float getValue() { return val; }

    public float getAlpha() { return alpha; }

    //======================================================================================
    //  输入事件
    //======================================================================================

    @Override
    public void mouseButtonEvent(int button, boolean pressed, int x, int y) {
        if (pressed) {
            if (isInArea(x, y)) {
                touched = true;
                if (parent != null) parent.setCurrent(this);
                dragTarget = hitTest(x, y);
                applyDrag(x, y);
                doStateChanged(this);
            }
        } else {
            if (touched) {
                touched = false;
                dragTarget = TARGET_NONE;
                doAction();
                doStateChanged(this);
            }
        }
    }

    @Override
    public void cursorPosEvent(int x, int y) {
        //鼠标移动: 仅在按下拖动期间响应
        if (touched && dragTarget != TARGET_NONE) {
            applyDrag(x, y);
            doStateChanged(this);
            GForm.flush();
        }
    }

    @Override
    public void touchEvent(int touchid, int phase, int x, int y) {
        switch (phase) {
            case Glfm.GLFMTouchPhaseBegan: {
                if (isInArea(x, y)) {
                    touched = true;
                    if (parent != null) parent.setCurrent(this);
                    dragTarget = hitTest(x, y);
                    applyDrag(x, y);
                    doStateChanged(this);
                }
                break;
            }
            case Glfm.GLFMTouchPhaseMoved: {
                if (touched && dragTarget != TARGET_NONE) {
                    applyDrag(x, y);
                    doStateChanged(this);
                    GForm.flush();
                }
                break;
            }
            case Glfm.GLFMTouchPhaseEnded: {
                if (touched) {
                    touched = false;
                    dragTarget = TARGET_NONE;
                    doAction();
                    doStateChanged(this);
                }
                break;
            }
            default:
                break;
        }
    }

    /**
     * 命中测试: 判断 (x,y) 落在哪个可拖动区域。 (x,y) 为组件内绝对坐标。
     */
    protected byte hitTest(int x, int y) {
        //先判 Alpha 滑条(它在色轮下方, 半径判定会把它误判进色轮, 故优先)
        if (alphaRectW > 0 && alphaRectH > 0
                && x >= alphaRectLeft && x <= alphaRectLeft + alphaRectW
                && y >= alphaRectTop && y <= alphaRectTop + alphaRectH) {
            return TARGET_ALPHA;
        }
        float dx = x - wheelCx;
        float dy = y - wheelCy;
        float r = (float) Math.sqrt(dx * dx + dy * dy);
        if (r >= r0 && r <= r1) {
            return TARGET_HUE;
        }
        if (r < r0) {
            //进一步判断是否在旋转后的三角内 (略放宽: 内圆内即允许, 反解时会夹到三角边)
            return TARGET_SV;
        }
        return TARGET_NONE;
    }

    /**
     * 根据当前 dragTarget 把 (x,y) 应用到对应 HSV/Alpha 分量。
     */
    protected void applyDrag(int x, int y) {
        switch (dragTarget) {
            case TARGET_HUE: {
                float dx = x - wheelCx;
                float dy = y - wheelCy;
                //atan2: 让 0 弧度对应 hue=0(红). NanoVG 色环第 i 段起始角 i/6*2π, 0 角处正是红
                float ang = (float) (Math.atan2(dy, dx) / (Math.PI * 2.0));
                hue = ang - (float) Math.floor(ang);  //规整到 [0,1)
                break;
            }
            case TARGET_SV: {
                //把屏幕点逆旋转 hue*2π 还原到三角平面, 再反解 (sat,val)
                float dx = x - wheelCx;
                float dy = y - wheelCy;
                float a = -hue * (float) Math.PI * 2f;
                float ca = (float) Math.cos(a);
                float sa = (float) Math.sin(a);
                float lx = ca * dx - sa * dy;  //三角局部坐标
                float ly = sa * dx + ca * dy;
                float[] sv = pointToSV(lx, ly, triR);
                sat = sv[0];
                val = sv[1];
                break;
            }
            case TARGET_ALPHA: {
                if (alphaRectW > 0) {
                    float p = (x - alphaRectLeft) / alphaRectW;
                    alpha = clamp01(p);
                }
                break;
            }
            default:
                break;
        }
    }

    //======================================================================================
    //  绘制
    //======================================================================================

    /**
     * @param vg
     * @return
     */
    public boolean paint(long vg) {
        super.paint(vg);
        float x = getX();
        float y = getY();
        float w = getW();
        float h = getH();
        drawColorwheel(vg, x, y, w, h);
        return true;
    }

    void drawColorwheel(long vg, float x, float y, float w, float h) {
        int i;
        float r_outer, r_inner, ax, ay, bx, by, cx, cy, aeps, r;
        byte[] paint;

        //底部预留 Alpha 滑条的高度
        float barH = h * ALPHA_BAR_H_RATIO;
        if (barH < 8f) barH = 8f;
        float wheelH = h - barH - ALPHA_BAR_GAP;
        if (wheelH < 10f) wheelH = 10f;  //防御退化尺寸

        cx = x + w * 0.5f;
        cy = y + wheelH * 0.5f;
        r_outer = (w < wheelH ? w : wheelH) * 0.5f - 5.0f;
        if (r_outer < 10f) r_outer = 10f;
        r1 = r_outer;
        r_inner = r1 - 20.0f;
        if (r_inner < 4f) r_inner = 4f;
        r0 = r_inner;
        //记录命中几何
        wheelCx = cx;
        wheelCy = cy;

        aeps = 0.5f / r1;    // half a pixel arc length in radians (2pi cancels out).

        //外圈色相环
        for (i = 0; i < 6; i++) {
            float a0 = (float) (i / 6.0f * Math.PI * 2.0f - aeps);
            float a1 = (float) ((i + 1.0f) / 6.0f * Math.PI * 2.0f + aeps);
            nvgBeginPath(vg);
            nvgArc(vg, cx, cy, r_inner, a0, a1, NVG_CW);
            nvgArc(vg, cx, cy, r_outer, a1, a0, NVG_CCW);
            nvgClosePath(vg);
            ax = cx + (float) Math.cos(a0) * (r_inner + r_outer) * 0.5f;
            ay = cy + (float) Math.sin(a0) * (r_inner + r_outer) * 0.5f;
            bx = cx + (float) Math.cos(a1) * (r_inner + r_outer) * 0.5f;
            by = cy + (float) Math.sin(a1) * (r_inner + r_outer) * 0.5f;
            paint = nvgLinearGradient(vg, ax, ay, bx, by, nvgHSLA((float) (a0 / (Math.PI * 2)), 1.0f, 0.55f, (byte) 255), nvgHSLA(a1 / (float) (Math.PI * 2), 1.0f, 0.55f, (byte) 255));
            nvgFillPaint(vg, paint);
            nvgFill(vg);
        }

        nvgBeginPath(vg);
        nvgCircle(vg, cx, cy, r_inner - 0.5f);
        nvgCircle(vg, cx, cy, r_outer + 0.5f);
        nvgStrokeColor(vg, nvgRGBA(0, 0, 0, 64));
        nvgStrokeWidth(vg, 1.0f);
        nvgStroke(vg);

        // Selector (色相标记 + 内三角)
        nvgSave(vg);
        nvgTranslate(vg, cx, cy);
        //三角随 hue 旋转, 让纯色顶始终指向外圈对应色相位置
        nvgRotate(vg, hue * (float) Math.PI * 2f);

        // Marker on (外圈色相指针)
        nvgStrokeWidth(vg, 2.0f);
        nvgBeginPath(vg);
        nvgRect(vg, r_inner - 1, -3, r_outer - r_inner + 2, 6);
        nvgStrokeColor(vg, nvgRGBA(255, 255, 255, 192));
        nvgStroke(vg);

        paint = nvgBoxGradient(vg, r_inner - 3, -5, r_outer - r_inner + 6, 10, 2, 4, nvgRGBA(0, 0, 0, 128), nvgRGBA(0, 0, 0, 0));
        nvgBeginPath(vg);
        nvgRect(vg, r_inner - 2 - 10, -4 - 10, r_outer - r_inner + 4 + 20, 8 + 20);
        nvgRect(vg, r_inner - 2, -4, r_outer - r_inner + 4, 8);
        nvgPathWinding(vg, NVG_HOLE);
        nvgFillPaint(vg, paint);
        nvgFill(vg);

        // Center triangle (HSV: 纯色顶 / 白顶 / 黑顶)
        r = r_inner - 6;
        if (r < 2f) r = 2f;
        triR = r;  //记录三角形半径, 供 applyDrag 反解 SV 时使用同一尺度
        float[] triPure = new float[]{r, 0};                                   //纯色顶 (S=1,V=1)
        float[] triWhite = new float[]{(float) Math.cos(120.0f / 180.0f * Math.PI) * r,
                (float) Math.sin(120.0f / 180.0f * Math.PI) * r};               //白顶 (S=0)
        float[] triBlack = new float[]{(float) Math.cos(-120.0f / 180.0f * Math.PI) * r,
                (float) Math.sin(-120.0f / 180.0f * Math.PI) * r};              //黑顶 (V=0)
        ax = triPure[0];
        ay = triPure[1];
        bx = triWhite[0];
        by = triWhite[1];
        nvgBeginPath(vg);
        nvgMoveTo(vg, ax, ay);
        nvgLineTo(vg, bx, by);
        nvgLineTo(vg, triBlack[0], triBlack[1]);
        nvgClosePath(vg);
        paint = nvgLinearGradient(vg, ax, ay, bx, by, nvgHSLA(hue, 1.0f, 0.5f, (byte) 255), nvgRGBA(255, 255, 255, 255));
        nvgFillPaint(vg, paint);
        nvgFill(vg);
        paint = nvgLinearGradient(vg, (ax + bx) * 0.5f, (ay + by) * 0.5f, triBlack[0], triBlack[1], nvgRGBA(0, 0, 0, 0), nvgRGBA(0, 0, 0, 255));
        nvgFillPaint(vg, paint);
        nvgFill(vg);
        nvgStrokeColor(vg, nvgRGBA(0, 0, 0, 64));
        nvgStroke(vg);

        // 选择圆点 (由 sat/val 正向算出三角平面坐标)
        float[] sel = svToPoint(sat, val, r);
        nvgStrokeWidth(vg, 2.0f);
        nvgBeginPath(vg);
        nvgCircle(vg, sel[0], sel[1], 5);
        nvgStrokeColor(vg, nvgRGBA(255, 255, 255, 192));
        nvgStroke(vg);

        paint = nvgRadialGradient(vg, sel[0], sel[1], 7, 9, nvgRGBA(0, 0, 0, 64), nvgRGBA(0, 0, 0, 0));
        nvgBeginPath(vg);
        nvgRect(vg, sel[0] - 20, sel[1] - 20, 40, 40);
        nvgCircle(vg, sel[0], sel[1], 7);
        nvgPathWinding(vg, NVG_HOLE);
        nvgFillPaint(vg, paint);
        nvgFill(vg);

        nvgRestore(vg);

        //---- Alpha 滑条 ----
        drawAlphaBar(vg, x, y, w, h, barH);
    }

    /**
     * 在色轮下方绘制 Alpha 滑条, 并更新 alphaRect* 命中字段。
     */
    protected void drawAlphaBar(long vg, float x, float y, float w, float h, float barH) {
        float barLeft = x + 5f;
        float barTop = y + h - barH;
        float barW = w - 10f;
        if (barW < 4f) barW = 4f;
        //记录命中矩形
        alphaRectLeft = barLeft;
        alphaRectTop = barTop;
        alphaRectW = barW;
        alphaRectH = barH;

        //当前色(不透明)用于渐变右端
        float[] rgb = hsvToRgb(hue, sat, val);

        //底色: 棋盘格简化为浅灰, 表达透明
        nvgBeginPath(vg);
        nvgRoundedRect(vg, barLeft, barTop, barW, barH, 3f);
        nvgFillColor(vg, nvgRGBA(204, 204, 204, 255));
        nvgFill(vg);

        //渐变: 左端当前色透明 -> 右端当前色不透明 (约定左=透明0, 右=不透明1, 与 applyDrag 一致)
        byte[] paint = nvgLinearGradient(vg, barLeft, barTop, barLeft + barW, barTop,
                nvgRGBA((int) (rgb[0] * 255), (int) (rgb[1] * 255), (int) (rgb[2] * 255), 0),
                nvgRGBA((int) (rgb[0] * 255), (int) (rgb[1] * 255), (int) (rgb[2] * 255), 255));
        nvgBeginPath(vg);
        nvgRoundedRect(vg, barLeft, barTop, barW, barH, 3f);
        nvgFillPaint(vg, paint);
        nvgFill(vg);

        //边框
        nvgBeginPath(vg);
        nvgRoundedRect(vg, barLeft, barTop, barW, barH, 3f);
        nvgStrokeColor(vg, nvgRGBA(0, 0, 0, 64));
        nvgStrokeWidth(vg, 1f);
        nvgStroke(vg);

        //指针
        float px = barLeft + barW * alpha;
        nvgStrokeWidth(vg, 2f);
        nvgBeginPath(vg);
        nvgRect(vg, px - 2, barTop - 2, 4, barH + 4);
        nvgStrokeColor(vg, nvgRGBA(255, 255, 255, 192));
        nvgStroke(vg);
        nvgBeginPath(vg);
        nvgRect(vg, px - 2, barTop - 2, 4, barH + 4);
        nvgStrokeColor(vg, nvgRGBA(0, 0, 0, 128));
        nvgStrokeWidth(vg, 1f);
        nvgStroke(vg);
    }

    //======================================================================================
    //  三角形几何: SV <-> 局部坐标
    //  三角顶点(未旋转): 纯色(r,0) / 白(cos120*r, sin120*r) / 黑(cos-120*r, sin-120*r)
    //  HSV 映射: 点位置 = 白顶 + sat*(纯色顶-白顶) 得到"色相轴"上的点, 再向黑顶方向衰减 (1-val)
    //======================================================================================

    /**
     * (sat,val) -> 三角平面局部坐标 (未旋转)。
     * 约定: sat=0 在白顶, sat=1 在纯色顶; val=0 在黑顶, val=1 在纯色-白边上。
     */
    protected static float[] svToPoint(float sat, float val, float r) {
        float[] pure = new float[]{r, 0};
        float[] white = new float[]{(float) Math.cos(120.0f / 180.0f * Math.PI) * r,
                (float) Math.sin(120.0f / 180.0f * Math.PI) * r};
        float[] black = new float[]{(float) Math.cos(-120.0f / 180.0f * Math.PI) * r,
                (float) Math.sin(-120.0f / 180.0f * Math.PI) * r};
        //色相轴上的点: 从白顶插值到纯色顶
        float baseX = white[0] + sat * (pure[0] - white[0]);
        float baseY = white[1] + sat * (pure[1] - white[1]);
        //再向黑顶衰减 val: val=1 留在 base, val=0 到黑顶
        float px = black[0] + val * (baseX - black[0]);
        float py = black[1] + val * (baseY - black[1]);
        return new float[]{px, py};
    }

    /**
     * 三角平面局部坐标 (未旋转, 实际尺度 r) -> (sat,val)。点在三角外时夹到最近位置。
     *
     * @param lx 局部 x (与顶点同尺度, 像素)
     * @param ly 局部 y
     * @param r  三角形顶点到中心的距离 (即绘制时的 r0-6)
     */
    protected static float[] pointToSV(float lx, float ly, float r) {
        if (r < 1e-3f) {
            return new float[]{0f, 1f};  //退化: 三角太小
        }
        //三顶点(实际尺度 r): P0=纯色(r,0), P1=白(cos120*r, sin120*r), P2=黑(cos-120*r, sin-120*r)
        float px = r, py = 0f;
        float wx = (float) Math.cos(120f / 180f * Math.PI) * r, wy = (float) Math.sin(120f / 180f * Math.PI) * r;
        float bx = (float) Math.cos(-120f / 180f * Math.PI) * r, by = (float) Math.sin(-120f / 180f * Math.PI) * r;
        //标准重心坐标 (输入点与顶点必须同一尺度, 故不做归一化)
        float det = (py - wy) * (bx - px) - (px - wx) * (by - py);
        float l2 = ((py - wy) * (lx - px) - (px - wx) * (ly - py)) / det;  //黑顶权重
        float l1 = ((by - py) * (lx - px) - (bx - px) * (ly - py)) / det;  //白顶权重
        float l0 = 1f - l1 - l2;                                           //纯色顶权重
        float a = clamp01(l0), b = clamp01(l1), c = clamp01(l2);
        float sum = a + b + c;
        if (sum <= 1e-6f) {
            return new float[]{0f, 1f};  //退化: 回到白边
        }
        a /= sum;
        b /= sum;
        c /= sum;
        //映射关系: 点 = val*base + (1-val)*B ; base 的重心 = (sat, 1-sat, 0)
        //  => 纯色顶权重 a = val*sat, 白顶权重 b = val*(1-sat), 黑顶权重 c = 1-val
        float val = clamp01(1f - c);
        float sat;
        if (val > 1e-6f) {
            sat = clamp01(a / val);
        } else {
            sat = 0f;
        }
        return new float[]{sat, val};
    }

    //======================================================================================
    //  颜色数学 (HSV<->RGB), 仓库内无现成实现, 自写
    //======================================================================================

    /**
     * HSV -> RGB. h,s,v ∈ [0,1], 返回 float[3] ∈ [0,1]。
     */
    protected static float[] hsvToRgb(float h, float s, float v) {
        h = clamp01(h);
        s = clamp01(s);
        v = clamp01(v);
        float r, g, b;
        if (s <= 0f) {
            //灰色
            r = g = b = v;
        } else {
            float hh = (h - (float) Math.floor(h)) * 6f;  //0~6
            int i = (int) hh;
            float ff = hh - i;
            float p = v * (1f - s);
            float q = v * (1f - s * ff);
            float t = v * (1f - s * (1f - ff));
            switch (i) {
                case 0: r = v; g = t; b = p; break;
                case 1: r = q; g = v; b = p; break;
                case 2: r = p; g = v; b = t; break;
                case 3: r = p; g = q; b = v; break;
                case 4: r = t; g = p; b = v; break;
                case 5:
                default: r = v; g = p; b = q; break;
            }
        }
        return new float[]{r, g, b};
    }

    /**
     * RGB -> HSV. r,g,b ∈ [0,1], 返回 float[3] (h,s,v), h ∈ [0,1]。
     * 灰色时 h 返回 0 (调用方需自行决定是否保留旧 hue)。
     */
    protected static float[] rgbToHsv(float r, float g, float b) {
        r = clamp01(r);
        g = clamp01(g);
        b = clamp01(b);
        float max = Math.max(Math.max(r, g), b);
        float min = Math.min(Math.min(r, g), b);
        float delta = max - min;
        float v = max;
        float s = max <= 0f ? 0f : delta / max;
        float h;
        if (delta <= 0f) {
            h = 0f;  //灰色, h 无定义
        } else {
            if (max == r) {
                h = (g - b) / delta;
            } else if (max == g) {
                h = 2f + (b - r) / delta;
            } else {
                h = 4f + (r - g) / delta;
            }
            h /= 6f;
            if (h < 0f) h += 1f;
        }
        return new float[]{h, s, v};
    }

    protected static float clamp01(float v) {
        if (Float.isNaN(v) || v < 0f) return 0f;
        if (v > 1f) return 1f;
        return v;
    }
}
