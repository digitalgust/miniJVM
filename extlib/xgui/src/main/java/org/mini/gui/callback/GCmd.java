/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.mini.gui.callback;

import org.mini.glwrap.GLUtil;

/**
 * @author Gust
 */
public class GCmd {

    static public final int GCMD_NO = 0;
    static public final int GCMD_SHOW_MESSAGE = 1;
    static public final int GCMD_CLEAR_MESSAGE = 2;
    static public final int GCMD_SHOW_KEYBOARD = 3;
    static public final int GCMD_HIDE_KEYBOARD = 4;
    static public final int GCMD_RUN_CODE = 5;

    protected int cmdId;
    protected String msg;
    protected Runnable work;

    float x, y, width, height;

    byte[] msgBytes;

    public GCmd(int cmdId) {
        if (cmdId <= 0 || cmdId >= GCMD_RUN_CODE || cmdId == GCMD_SHOW_MESSAGE) {
            throw new RuntimeException("invalid cmdId");
        }
        this.cmdId = cmdId;
    }

    public GCmd(Runnable work) {
        this.cmdId = GCMD_RUN_CODE;
        this.work = work;
    }

    public GCmd(String msg, Runnable work) {
        this.cmdId = GCMD_SHOW_MESSAGE;
        this.msg = msg;
        this.work = work;
    }

    public byte[] getBytes() {
        if (msgBytes == null) {
            msgBytes = GLUtil.toCstyleBytes(msg);
        }
        return msgBytes;
    }

    public String getMsg() {
        return msg;
    }

    public Runnable getWork() {
        return work;
    }

    public void setBoundle(float x, float y, float width, float height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public boolean isInBoundle(float px, float py) {
        if (px >= x && px <= x + width && py >= y && py <= y + height) {
            return true;
        }
        return false;
    }
}
