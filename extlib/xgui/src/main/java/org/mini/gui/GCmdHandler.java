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
    static List<GCmd> cmdQueue = new ArrayList();

    final static List<String> message = new ArrayList();
    static byte[] curShowMessage;
    float[] insets = new float[4];
    float[] bond = new float[4];
    GCmd msgCmd;

    public void addCmd(GCmd cmd) {
        if (cmd == null) {
            return;
        }
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
            cmdQueue.clear();
            cmdQueue.addAll(cmds);
            cmds.clear();
        }
        for (int i = 0, imax = cmdQueue.size(); i < imax; i++) {
            GCmd cmd = cmdQueue.get(i);
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
    }

    void paint(GForm form) {
        if (curShowMessage == null) {
            if (message.size() > 0) {
                curShowMessage = GLUtil.toCstyleBytes(message.remove(0));
                msgCmd = new GCmd(new Runnable() {
                    int tick = 0;

                    @Override
                    public void run() {
                        GForm.flush();
                        if (tick++ < 50) {
                            GForm.addCmd(msgCmd);
                        } else {
                            curShowMessage = null;
                            msgCmd = null;
                        }
                    }
                });
                GForm.addCmd(msgCmd);
            }
        } else {
            long vg = form.getNvContext();
            float pad = 20;
            float panW = form.getW() - pad * 2;
            GCallBack.getInstance().getInsets(insets);
            nvgFontSize(vg, GToolkit.getStyle().getTextFontSize());
            nvgFontFace(vg, GToolkit.getFontWord());
            nvgTextAlign(vg, Nanovg.NVG_ALIGN_TOP | Nanovg.NVG_ALIGN_LEFT);
            Nanovg.nvgTextBoxBoundsJni(vg, 0, 0, panW, curShowMessage, 0, curShowMessage.length, bond);

            GToolkit.drawRoundedRect(vg, pad * .5f, insets[0] + pad * .5f, panW + pad, bond[GObject.HEIGHT] - bond[GObject.TOP] + pad, 5, GToolkit.getStyle().getTextFontColor());

            nvgFillColor(vg, GToolkit.getStyle().getBackgroundColor());
            Nanovg.nvgTextBoxJni(vg, pad, insets[0] + pad, panW, curShowMessage, 0, curShowMessage.length);
        }
    }


    public int size() {
        return cmds.size();
    }


}
