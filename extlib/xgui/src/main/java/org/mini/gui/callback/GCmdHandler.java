/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.mini.gui.callback;

import org.mini.glfm.Glfm;
import org.mini.gui.GObject;
import org.mini.gui.GPanel;
import org.mini.gui.GToolkit;
import org.mini.nanovg.Nanovg;

import java.util.*;

import static org.mini.nanovg.Nanovg.*;

/**
 * Many of glfm function need call by glcontext thread ,if call by other thread
 * ,maybe error occur so , call these glfm function, post a cmd to GForm , it
 * would call by glcontext callback thread.
 *
 * @author Gust
 */
public class GCmdHandler extends GPanel implements GCallbackUI {


    final static List<GCmd> cmds = Collections.synchronizedList(new ArrayList());
    private static final int MAX_SHOW_MSG = 3;
    static List<GCmd> cmdQueue = new ArrayList();

    final static List<GCmd> message = new ArrayList();
    static LinkedHashMap<Long, GCmd> curShowMessage = new LinkedHashMap<>();
    float[] insets = new float[4];
    boolean disapper = false;
    static float PAD = 20;

    public GCmdHandler() {
        super(null);
        layer = LAYER_INNER;
        paintWhenOutOfScreen = true;//GCmdHandler 在屏幕外也需要绘制
    }

    public void addCmd(GCmd cmd) {
        if (cmd == null) {
            return;
        }
        cmds.add(cmd);
    }

    public void addCmd(int cmdId) {
        cmds.add(new GCmd(cmdId));
    }

    public void addCmd(Runnable work) {
        cmds.add(new GCmd(work));
    }

    public void addCmd(String msg, Runnable work) {
        cmds.add(new GCmd(msg, work));
    }

    public void process() {
        synchronized (cmds) {
            cmdQueue.clear();
            cmdQueue.addAll(cmds);
            cmds.clear();
        }

        for (int i = 0, imax = cmdQueue.size(); i < imax; i++) {
            GCmd cmd = cmdQueue.get(i);
            try {
                switch (cmd.cmdId) {
                    case GCmd.GCMD_SHOW_MESSAGE: {
                        message.add(cmd);
                        break;
                    }
                    case GCmd.GCMD_CLEAR_MESSAGE: {
                        message.clear();
                        break;
                    }
                    case GCmd.GCMD_SHOW_KEYBOARD: {
                        Glfm.glfmSetKeyboardVisible(GCallBack.getInstance().getDisplay(), true);
                        break;
                    }
                    case GCmd.GCMD_HIDE_KEYBOARD: {
                        Glfm.glfmSetKeyboardVisible(GCallBack.getInstance().getDisplay(), false);
                        break;
                    }
                    case GCmd.GCMD_RUN_CODE: {
                        if (cmd.work instanceof Runnable) {
                            cmd.work.run();
                        }
                        break;
                    }
                    default: {

                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public boolean paint(long vg) {
        super.paint(vg);
        GCallBack.getInstance().getInsets(insets);
        float panW = GCallBack.getInstance().getDeviceWidth() - PAD * 2 + PAD;
        float panX = PAD * .5f;
        float panY = insets[0] + PAD * .5f;
        while (curShowMessage.size() < MAX_SHOW_MSG) {
            if (message.size() > 0) {
                GCmd cmd = message.remove(0);
                curShowMessage.put(System.currentTimeMillis(), cmd);
                setLocation(panX, panY);
                disapper = false;
            } else {
                break;
            }
        }
        float panH = 30 * curShowMessage.size();
        setSize(panW, panH);
        if (disapper) {
            setLocation(getLocationLeft(), getLocationTop() - 2);
//            if (getY() + getH() <= 0) {//如果在屏幕外，不放回屏幕内，将永远不会重绘，GContainer里有检测如果在屏幕外则不会调用paint()
//                setLocation(0, 0);
//                setSize(1, 1);
//                disapper = false;
//            }
        }
        //
        if (curShowMessage.size() > 0) {
            float x = getX();
            float y = getY();
            float w = getW();
            float h = getH();

            GToolkit.drawRoundedRect(vg, x, y, w, h, 5, getColor());


            long curt = System.currentTimeMillis();
            int i = 0;
            for (Iterator<Long> it = curShowMessage.keySet().iterator(); it.hasNext(); ) {
                Long t = it.next();
                if (curt - t < 5 * 1000) {
                    GCmd cmd = curShowMessage.get(t);
                    float dx = x + 6;
                    float dy = y + 6 + i * 30;
                    float dw = panW - 12;
                    float lineh = 25;
                    nvgScissor(vg, dx, dy, dw, lineh);
                    if (cmd.getWork() != null) {
                        GToolkit.drawEmoj(vg, dx, dy, 30, lineh, GObject.ICON_LOGIN_BYTE, GToolkit.getStyle().getIconFontSize(), getBgColor());
                        dx += 30;
                    }
                    GToolkit.drawTextLine(vg, dx, dy, cmd.getMsg(), getFontSize(), getBgColor(), NVG_ALIGN_TOP | NVG_ALIGN_LEFT);
                    cmd.setBoundle(dx, dy, dw, lineh);
                    i++;
                } else {
                    it.remove();
                }
            }
            GDesktop.flush();
        } else {
            setSize(1, 1);
        }
        return true;
    }


    public int size() {
        return cmds.size();
    }

    @Override
    public boolean dragEvent(int button, float dx, float dy, float x, float y) {
        if (dy < 0) {
            setLocation(getLocationLeft(), getLocationTop() + dy);
            disapper = true;
            return true;
        }
        return super.dragEvent(button, dx, dy, x, y);
    }

    @Override
    public void touchEvent(int touchid, int phase, int x, int y) {
        boolean press;
        if (phase == Glfm.GLFMTouchPhaseBegan) press = true;
        else if (phase == Glfm.GLFMTouchPhaseEnded) press = false;
        else return;
        mouseButtonEvent(touchid, press, x, y);
    }

    @Override
    public void mouseButtonEvent(int button, boolean pressed, int x, int y) {
        if (pressed) return;
        for (Iterator<Long> it = curShowMessage.keySet().iterator(); it.hasNext(); ) {
            Long t = it.next();
            GCmd cmd = curShowMessage.get(t);
            if (cmd.isInBoundle(x, y)) {
                Runnable work = cmd.getWork();
                if (work != null) {
                    work.run();
                }
            }
        }
    }
}
