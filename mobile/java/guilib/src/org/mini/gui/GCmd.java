/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.mini.gui;

/**
 *
 * @author Gust
 */
public class GCmd {

    static public final int GCMD_DESTORY_TEXTURE = 0;
    static public final int GCMD_SHOW_MESSAGE = 1;
    static public final int GCMD_CLEAR_MESSAGE = 2;
    static public final int GCMD_SHOW_KEYBOARD = 3;
    static public final int GCMD_HIDE_KEYBOARD = 4;

    int cmdId;
    Object attachment;

    public GCmd(int cmdId) {
        this.cmdId = cmdId;
    }

    public GCmd(int cmdId, Object att) {
        this.cmdId = cmdId;
        this.attachment = att;
    }
}
