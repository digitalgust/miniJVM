/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.mini.layout;

import org.mini.gui.GToolkit;

/**
 * @author gust
 */
public class XDef {
    public static final int NODEF = Integer.MIN_VALUE; //未定义的值

    //边框
    public static int SPACING_CELL = 3; //
    public static int SPACING_BUTTON_ADD = 30; //
    public static int SPACING_LABEL_ADD = 2; //
    public static int SPACING_CHECKBOX_ADD = 30; //


    public static final int DEFAULT_COMPONENT_HEIGHT = 30;
    public static final int DEFAULT_LIST_HEIGHT = 40;
    public static int DEFAULT_FONT_SIZE;

    static {
        DEFAULT_FONT_SIZE = (int) GToolkit.getStyle().getTextFontSize();
    }

}
