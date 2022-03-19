/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.mini.gui;

import org.mini.glfm.Glfm;
import org.mini.glwrap.GLUtil;
import org.mini.nanovg.Nanovg;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.TimerTask;

import static org.mini.nanovg.Nanovg.*;

/**
 * Many of glfm function need call by glcontext thread ,if call by other thread
 * ,maybe error occur so , call these glfm function, post a cmd to GForm , it
 * would call by glcontext callback thread.
 *
 * @author Gust
 */
public class GCmdHandler {

    final static List<GCmd> cmds = Collections.synchronizedList(new ArrayList());

    final static List<String> message = new ArrayList();
    static byte[] curShowMessage;

    public void addCmd(GCmd cmd) {
        cmds.add(cmd);
    }

    public void addCmd(int cmdId) {
        cmds.add(new GCmd(cmdId));
    }

    public void addCmd(int cmdId, Object attachment) {
        cmds.add(new GCmd(cmdId, attachment));
    }

    public void process(GForm form) {
        synchronized (cmds) {
            for (int i = 0, imax = cmds.size(); i < imax; i++) {
                GCmd cmd = cmds.get(i);
                try {
                    switch (cmd.cmdId) {
                        case GCmd.GCMD_DESTORY_TEXTURE: {
                            Integer tex = (Integer) cmd.attachment;
                            if (tex != null) {
                                Nanovg.nvgDeleteImage(form.getNvContext(), tex);
                                //System.out.println("delete image " + tex);
                            }
                            break;
                        }
                        case GCmd.GCMD_SHOW_MESSAGE: {
                            message.add((String) cmd.attachment);
                            break;
                        }
                        case GCmd.GCMD_CLEAR_MESSAGE: {
                            message.clear();
                            break;
                        }
                        case GCmd.GCMD_SHOW_KEYBOARD: {
                            Glfm.glfmSetKeyboardVisible(form.getWinContext(), true);
                            break;
                        }
                        case GCmd.GCMD_HIDE_KEYBOARD: {
                            Glfm.glfmSetKeyboardVisible(form.getWinContext(), false);
                            break;
                        }
                        case GCmd.GCMD_RUN_CODE: {
                            if (cmd.attachment instanceof Runnable) {
                                Runnable runnable = (Runnable) cmd.attachment;
                                runnable.run();
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
            cmds.clear();
        }
    }

    void update(GForm form) {
        if (curShowMessage == null) {
            if (message.size() > 0) {
                curShowMessage = GLUtil.toUtf8(message.remove(0));
                GForm.timer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        try {
                            curShowMessage = null;
                            GForm.flush();
                        } catch (Exception e) {
                        }
                    }
                }, 1500);
            }
        } else {
            long vg = form.getNvContext();
            float pad = 20;
            float panW = form.callback.getDeviceWidth() - pad * 2;
            float[] bond = new float[4];
            nvgFontSize(vg, GToolkit.getStyle().getTextFontSize());
            nvgFontFace(vg, GToolkit.getFontWord());
            nvgTextAlign(vg, Nanovg.NVG_ALIGN_TOP | Nanovg.NVG_ALIGN_LEFT);
            Nanovg.nvgTextBoxBoundsJni(vg, 0, 0, panW, curShowMessage, 0, curShowMessage.length, bond);

            GToolkit.drawRoundedRect(vg, pad * .5f, pad * .5f, panW + pad, bond[GObject.HEIGHT] - bond[GObject.TOP] + pad, 5, Nanovg.nvgRGBf(1.f, 1.f, 1.f));

            nvgFillColor(vg, Nanovg.nvgRGBf(0.f, 0.f, 0.f));
            Nanovg.nvgTextBoxJni(vg, pad, pad, panW, curShowMessage, 0, curShowMessage.length);
        }
    }


    public int size() {
        return cmds.size();
    }
}
